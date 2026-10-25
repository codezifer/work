package de.carsten.android.muzzic.model

import androidx.core.net.toUri
import de.carsten.android.muzzic.ALBUMART_CONTENT_KEY
import de.carsten.android.muzzic.ALBUMART_SCHEME

data class AlbumArtUri(val filePath: String, val offset: Long = 0L, val size: Long = 0L, val hashCode: Int = 0, val mimeType: String? = null) {

    companion object {
        fun parse(albumArtPath: String): AlbumArtUri {
            val uri = albumArtPath.toUri()
            val albumArt = albumArtPath.substringBefore("?").removePrefix(ALBUMART_SCHEME)
            val offset = uri.getQueryParameter("offset")?.toLongOrNull() ?: 0L
            val size = uri.getQueryParameter("size")?.toLongOrNull() ?: 0L
            val hashCode = uri.getQueryParameter("hashCode")?.toIntOrNull() ?: 0
            val mimeType = uri.getQueryParameter("mimeType")
            return AlbumArtUri(albumArt, offset, size, hashCode, mimeType)
        }
    }

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
        result = 31 * result + mimeType.hashCode()
        return result
    }

    /**
     * Builds a cache key from the image content instead of its location.
     *
     * Embedded artwork is stored once per audio file, so every track of an album carries a
     * byte-identical copy of the same cover. [filePath] and [offset] are therefore excluded:
     * keying by content lets all those copies share one decoded bitmap, one palette and one
     * prefetch entry.
     *
     * [hashCode] is only 32 bits wide, so [size] (the image's byte length) and [mimeType] are
     * part of the key as well. That keeps the collision probability negligible without a
     * bigger persisted hash.
     *
     * @return the content-based key, or `null` when the artwork has no usable content identity
     * (HTTP sources, a missing hash, or an invalid byte length). Callers must fall back to
     * [get] in that case so different covers are never treated as identical.
     */
    fun contentKey(): String? {
        if (filePath.startsWith("http")) return null
        if (hashCode == 0 || size <= 0) return null
        return "$ALBUMART_CONTENT_KEY$hashCode:$size:${mimeType.orEmpty()}"
    }

    fun get(): String {
        if (filePath.startsWith("http")) return filePath
        val path = "${ALBUMART_SCHEME}$filePath?offset=$offset&size=$size&hashCode=$hashCode"
        return if (mimeType == null) path else "$path&mimeType=$mimeType"
    }
}
