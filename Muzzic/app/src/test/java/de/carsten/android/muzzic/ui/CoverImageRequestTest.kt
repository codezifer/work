package de.carsten.android.muzzic.ui

import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.ui.component.coverBaseKey
import java.io.ByteArrayInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CoverImageRequestTest {
    @Test
    fun `string data is used as is`() {
        val path = "albumart://content://track.mp3?offset=0&size=10"
        assertThat(coverBaseKey(path)).isEqualTo(path)
    }

    @Test
    fun `album art uri resolves to its serialized form`() {
        val uri = AlbumArtUri(filePath = "content://track.mp3", offset = 10L, size = 20L, hashCode = 30, mimeType = "image/jpeg")
        assertThat(coverBaseKey(uri)).isEqualTo(uri.get())
    }

    @Test
    fun `null data has no key`() {
        assertThat(coverBaseKey(null)).isNull()
    }

    @Test
    fun `stream data has no key`() {
        assertThat(coverBaseKey(ByteArrayInputStream(byteArrayOf(1, 2, 3)))).isNull()
    }
}
