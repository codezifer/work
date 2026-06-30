package de.carsten.android.muzzic.model

import androidx.core.net.toUri
import de.carsten.android.muzzic.ALBUMART_SCHEME

data class AlbumArtUri(val filePath: String, val offset: Long = 0L, val size: Long = 0L, val hashCode: Int = 0, val mimeType: String? = null) {

    /**
     * Assume offset, size and mimeType for equality properties are enough
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false

        other as AlbumArtUri

        if (size != other.size) return false
        if (hashCode != other.hashCode) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hashCode
        result = 31 * result + size.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }

    fun get(): String {
        if (filePath.startsWith("http")) return filePath
        val path = "${ALBUMART_SCHEME}${filePath.replace(" ", "%20")}?offset=$offset&size=$size&hashCode=$hashCode"
        return if (mimeType == null) path else "$path&mimeType=$mimeType"
    }

    companion object {
        fun parse(albumArtPath: String): AlbumArtUri {
            val uri = albumArtPath.toUri()
            val albumArt = albumArtPath.substringBefore("?").removePrefix(ALBUMART_SCHEME).replace("%20", " ")
            val offset = uri.getQueryParameter("offset")?.toLongOrNull() ?: 0L
            val size = uri.getQueryParameter("size")?.toLongOrNull() ?: 0L
            val hashCode = uri.getQueryParameter("hashCode")?.toIntOrNull() ?: 0
            val mimeType = uri.getQueryParameter("mimeType")
            return AlbumArtUri(albumArt, offset, size, hashCode, mimeType)
        }
    }
}

fun String?.toAlbumArtUri(): AlbumArtUri? {
    if (this == null) return null
    return AlbumArtUri.parse(this)
}
