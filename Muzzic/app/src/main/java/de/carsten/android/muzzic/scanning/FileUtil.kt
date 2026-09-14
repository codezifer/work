package de.carsten.android.muzzic.scanning

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.logging.logger
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * File utility class.
 */
object FileUtil {

    private val logger = logger()

    /**
     * Streams all files from root directory matching the given extensions.
     *
     * Each matching file is emitted as soon as it is discovered, so callers can start
     * processing while the directory walk is still in progress. The caller is
     * responsible for running the flow on a background dispatcher (e.g. via `flowOn`).
     *
     * @param root directory as [File]
     * @param extensions supported file extensions
     * @return [Flow] of matching [File]
     */
    fun getFilesFlow(root: File, extensions: Set<String>): Flow<File> = flow {
        if (!root.exists()) return@flow
        val walkJob = currentCoroutineContext()[Job]
        root.walkTopDown()
            .onEnter {
                walkJob?.ensureActive()
                true
            }
            .filter {
                it.isFile && it.extension.lowercase() in extensions
            }
            .forEach {
                walkJob?.ensureActive()
                emit(it)
            }
    }

    /**
     * Scan files for configured tree root chunk wise
     *
     * @param context android [Context]
     * @param uri tree root [Uri]
     * @param chunkSize chunk size [Int]
     * @return [Flow] of [List] of [ScannedFile]
     */
    fun getScannedFileFlow(context: Context, uri: Uri?, chunkSize: Int = AppConfig.Scanning.CHUNK_SIZE, supportedFiles: Set<String> = emptySet()): Flow<Set<ScannedFile>> = flow {
        if (uri == null) {
            emit(setOf())
            return@flow
        }

        val chunk = mutableSetOf<ScannedFile>()
        val lowerExtensions = supportedFiles.map { it.lowercase() }.toSet()

        traverseSafTree(context, uri, ScannedFile.PROJECTION) { cursor, dirStack ->
            val indices = ScannedFile.Indices.from(cursor)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(indices.mime)
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    dirStack.add(cursor.getString(indices.id))
                } else {
                    val displayName = cursor.getString(indices.name)
                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    if (ext in lowerExtensions) {
                        val prepared = ScannedFile.prepare(cursor, indices)
                        if (prepared != null) {
                            val processed = prepared.copy(uri = DocumentsContract.buildDocumentUriUsingTree(uri, prepared.documentId))
                            chunk.add(processed)
                            if (chunk.size >= chunkSize) {
                                emit(chunk.toSet())
                                chunk.clear()
                            }
                        }
                    }
                }
            }
        }

        if (chunk.isNotEmpty()) emit(chunk.toSet())
    }.flowOn(Dispatchers.IO)

    /**
     * Optimized method to count files matching the supported extensions.
     *
     * @param context android [Context]
     * @param uri tree root [Uri]
     * @param supportedFiles supported file extensions
     * @return count of supported files
     */
    suspend fun countFiles(context: Context, uri: Uri?, supportedFiles: Set<String>): Int = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext 0
        var count = 0
        val lowerExtensions = supportedFiles.map { it.lowercase() }.toSet()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        logger.debug("Start counting files ...")
        traverseSafTree(context, uri, projection) { cursor, dirStack ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeIdx)
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    dirStack.add(cursor.getString(idIdx))
                } else {
                    val displayName = cursor.getString(nameIdx)
                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    if (ext in lowerExtensions) {
                        count++
                    }
                }
            }
        }

        logger.debug("... Finished counting files, $count counted files.")

        count
    }

    /**
     * Returns the file path from uri.
     * Handles Storage Access Framework (SAF) URIs from ExternalStorageProvider.
     *
     * @param context android context
     * @param uri to resolve
     * @return the string
     */
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        // Handle Storage Access Framework
        if (DocumentsContract.isDocumentUri(context, uri)) {
            logger.debug("Uri is document")
            if (isExternalStorageDocument(uri)) {
                logger.debug("Uri is external storage document")
                val docId = DocumentsContract.getDocumentId(uri)
                return resolveExternalStoragePath(context, docId)
            }
        } else if (DocumentsContract.isTreeUri(uri)) {
            logger.debug("Uri is tree")
            if (isExternalStorageDocument(uri)) {
                logger.debug("Uri is external storage tree")
                val treeId = DocumentsContract.getTreeDocumentId(uri)
                return resolveExternalStoragePath(context, treeId)
            }
        }

        logger.debug("Uri schema: ${uri.scheme}")

        return when (uri.scheme) {
            "content" -> {
                logger.debug("Using content scheme")
                getDataColumn(context, uri, null, null)
            }

            "file" -> {
                logger.debug("Using file scheme")
                uri.path
            }

            else -> {
                logger.debug("Unknown scheme")
                null
            }
        }
    }

    /**
     * Checks if the URI is from ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean = uri.authority == "com.android.externalstorage.documents"

    /**
     * Resolves the ExternalStorageProvider document/tree ID to an absolute file path.
     */
    private fun resolveExternalStoragePath(context: Context, id: String): String? {
        val split = id.split(":")
        if (split.size < 2) {
            logger.error("Invalid document/tree ID: $id")
            return null
        }

        val type = split[0]
        val path = split.drop(1).joinToString(":")

        return if ("primary".equals(type, ignoreCase = true)) {
            logger.debug("Using primary storage")
            Environment.getExternalStorageDirectory().toString() + "/" + path
        } else {
            logger.debug("Using secondary storage")
            resolveSecondaryStoragePath(context, type, path)
        }
    }

    private fun resolveSecondaryStoragePath(context: Context, type: String, path: String): String? {
        val storageUtil = StorageUtil(context)
        val volume = storageUtil.getMountedVolumes().firstOrNull { volume -> volume.contains(type) }
        if (volume != null) {
            return "$volume/$path"
        }
        return null
    }

    private suspend inline fun traverseSafTree(
        context: Context,
        treeUri: Uri,
        projection: Array<String>,
        crossinline onCursorReady: suspend (cursor: Cursor, dirStack: ArrayDeque<String>) -> Unit,
    ) {
        val resolver = context.contentResolver
        val dirStack = ArrayDeque<String>()
        dirStack.add(DocumentsContract.getTreeDocumentId(treeUri))

        while (dirStack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val currentDocumentId = dirStack.removeLast()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocumentId)

            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                onCursorReady(cursor, dirStack)
            }
        }
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            logger.error("Failed to query _data column for URI: $uri", e)
        } finally {
            cursor?.close()
        }

        return null
    }
}
