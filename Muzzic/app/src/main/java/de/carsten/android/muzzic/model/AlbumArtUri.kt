package de.carsten.android.muzzic.model

import androidx.core.net.toUri

data class AlbumArtUri(val filePath: String, val offset: Long, val size: Long) {
    fun get(): String = "file://${filePath.replace(" ", "%20")}?offset=$offset&size=$size"

    companion object {
        fun parse(uriString: String): AlbumArtUri {
            val uri = uriString.toUri()
            val filePath = uri.path?.removePrefix("/")?.replace("%20", " ") ?: ""
            val offset = uri.getQueryParameter("offset")?.toLongOrNull() ?: 0L
            val size = uri.getQueryParameter("size")?.toLongOrNull() ?: 0L
            return AlbumArtUri(filePath, offset, size)
        }
    }
}
