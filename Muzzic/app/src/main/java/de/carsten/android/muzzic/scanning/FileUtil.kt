package de.carsten.android.muzzic.scanning

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FileUtil {

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
     * Returns the file path from uri
     *
     * @param context android context
     * @param uri to resolve
     * @return the string
     */
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        when (uri.scheme) {
            "content" -> {
                filePath = getDataColumn(context, uri, null, null)
            }

            "file" -> {
                filePath = uri.path
            }
        }

        return filePath
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
        } finally {
            cursor?.close()
        }

        return null
    }
}
