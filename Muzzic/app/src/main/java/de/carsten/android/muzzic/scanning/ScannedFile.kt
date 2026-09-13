package de.carsten.android.muzzic.scanning

import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import de.carsten.android.muzzic.logging.logger

data class ScannedFile(val uri: Uri = Uri.EMPTY, val documentId: String, val displayName: String, val mimeType: String, val size: Long, val lastModified: Long) {
    companion object {
        private val logger = logger()
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        fun prepare(cursor: Cursor): ScannedFile? {
            try {
                val documentIdIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val displayNameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val lastModifiedIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                return ScannedFile(
                    documentId = cursor.getString(documentIdIdx),
                    displayName = cursor.getString(displayNameIdx),
                    mimeType = cursor.getString(mimeTypeIdx),
                    size = cursor.getLong(sizeIdx),
                    lastModified = cursor.getLong(lastModifiedIdx),
                )
            } catch (e: Exception) {
                logger.error("Unable to iterate over current curser!", e)
                return null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScannedFile

        if (size != other.size) return false
        if (lastModified != other.lastModified) return false
        if (uri != other.uri) return false
        if (documentId != other.documentId) return false
        if (displayName != other.displayName) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size.hashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
