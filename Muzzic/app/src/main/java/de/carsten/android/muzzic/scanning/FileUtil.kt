package de.carsten.android.muzzic.scanning

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import de.carsten.android.muzzic.logging.logger
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FileUtil {

    private val logger = logger()

    /**
     * Gets file from parcel file descriptor of an uri.
     *
     * @param context android [Context]
     * @param uri file [Uri]
     * @return resolved [File]
     */
    suspend fun getFileFromUri(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            File("/proc/self/fd/${pfd.fd}")
        } catch (e: Exception) {
            null
        } finally {
            pfd?.close()
        }
    }

    /**
     * Get all files from root directory
     *
     * @param root directory as [File]
     * @param extensions supported file extensions
     * @return result as [List] of [File]
     */
    suspend fun getFiles(root: File, extensions: Set<String>): List<File> = withContext(Dispatchers.IO) {
        if (!root.exists()) {
            emptyList()
        } else {
            val results = mutableListOf<File>()
            root.walkTopDown()
                .onEnter {
                    ensureActive()
                    true
                }
                .filter {
                    it.isFile && it.extension.lowercase() in extensions
                }
                .forEach {
                    ensureActive()
                    results.add(it)
                }

            results.toList()
        }
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
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                return resolveExternalStoragePath(docId)
            }
        } else if (DocumentsContract.isTreeUri(uri)) {
            if (isExternalStorageDocument(uri)) {
                val treeId = DocumentsContract.getTreeDocumentId(uri)
                return resolveExternalStoragePath(treeId)
            }
        }

        return when (uri.scheme) {
            "content" -> {
                getDataColumn(context, uri, null, null)
            }
            "file" -> {
                uri.path
            }
            else -> null
        }
    }

    /**
     * Checks if the URI is from ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean = uri.authority == "com.android.externalstorage.documents"

    /**
     * Resolves the ExternalStorageProvider document/tree ID to an absolute file path.
     */
    private fun resolveExternalStoragePath(id: String): String? {
        val split = id.split(":")
        if (split.size < 2) return null
        val type = split[0]
        val path = split.drop(1).joinToString(":")

        return if ("primary".equals(type, ignoreCase = true)) {
            Environment.getExternalStorageDirectory().toString() + "/" + path
        } else {
            // TODO: Handle secondary storage (SD cards) if needed
            null
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
