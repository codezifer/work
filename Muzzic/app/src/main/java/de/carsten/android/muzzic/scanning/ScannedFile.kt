package de.carsten.android.muzzic.scanning

import android.net.Uri
import android.provider.DocumentsContract

data class ScannedFile(val uri: Uri = Uri.EMPTY, val documentId: String, val displayName: String, val mimeType: String, val size: Long, val lastModified: Long) {

    companion object {
        const val DOCUMENT_ID_IDX = 0
        const val DISPLAY_NAME_IDX = 1
        const val MIME_TYPE_IDX = 2
        const val SIZE_IDX = 3
        const val LAST_MODIFIED_IDX = 4

        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
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
