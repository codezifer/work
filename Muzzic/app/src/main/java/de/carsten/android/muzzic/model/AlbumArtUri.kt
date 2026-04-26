package de.carsten.android.muzzic.model

import androidx.core.net.toUri

data class AlbumArtUri(val filePath: String, val offset: Long = 0L, val size: Long = 0L, val mimeType: String? = null) {
    fun get(): String {
        if (filePath.startsWith("http")) return filePath
        val path = "file://${filePath.replace(" ", "%20")}?offset=$offset&size=$size"
        return if (mimeType == null) path else "$path&mimeType=$mimeType"
    }

    companion object {
        fun parse(uriString: String): AlbumArtUri {
            val uri = uriString.toUri()
            val filePath = uri.path?.replace("%20", " ") ?: ""
            val offset = uri.getQueryParameter("offset")?.toLongOrNull() ?: 0L
            val size = uri.getQueryParameter("size")?.toLongOrNull() ?: 0L
            val mimeType = uri.getQueryParameter("mimeType")
            return AlbumArtUri(filePath, offset, size, mimeType)
        }
    }
}
