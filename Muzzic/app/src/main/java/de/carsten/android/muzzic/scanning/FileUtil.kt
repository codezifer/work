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

        val resolver = context.contentResolver
        val chunk = mutableSetOf<ScannedFile>()

        // Pre-compile extensions to lowercase to avoid memory/string allocations inside loops
        val lowerExtensions = supportedFiles.map { it.lowercase() }.toSet()

        // Use an iterative stack (ArrayDeque) instead of function recursion to safeguard memory and stack depth
        val dirStack = ArrayDeque<String>()
        dirStack.add(DocumentsContract.getTreeDocumentId(uri))

        while (dirStack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val currentDocumentId = dirStack.removeLast()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, currentDocumentId)

            val preparedFiles = resolver.query(childrenUri, ScannedFile.PROJECTION, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(ScannedFile.prepare(cursor))
                    }
                }
            } ?: emptyList()

            for (prepared in preparedFiles) {
                if (prepared == null) continue

                when {
                    prepared.mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        dirStack.add(prepared.documentId)
                    }

                    else -> {
                        val ext = prepared.displayName.substringAfterLast('.', "").lowercase()
                        if (ext in lowerExtensions) {
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
