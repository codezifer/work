package de.carsten.android.muzzic.ui

import de.carsten.android.muzzic.TestConfig
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.ui.component.coverBaseKey
import java.io.ByteArrayInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class CoverImageRequestTest {
    @Test
    fun `plain string data is used as is`() {
        val path = "https://example.com/cover.jpg"
        assertThat(coverBaseKey(path)).isEqualTo(path)
    }

    @Test
    fun `album art is keyed by content instead of file path`() {
        val first = albumArt(filePath = "content://track-a.mp3", offset = 10L)
        val second = albumArt(filePath = "content://track-b.mp3", offset = 512L)

        assertThat(coverBaseKey(first.get()))
            .isEqualTo("albumart-content:30:20:image/jpeg")
            .isEqualTo(coverBaseKey(second.get()))
    }

    @Test
    fun `album art uri uses the same content key as its serialized form`() {
        val uri = albumArt(filePath = "content://track.mp3", offset = 10L)

        assertThat(coverBaseKey(uri)).isEqualTo(coverBaseKey(uri.get()))
    }

    @Test
    fun `different image hash produces a different key`() {
        val first = albumArt(hashCode = 30)
        val second = albumArt(hashCode = 31)

        assertThat(coverBaseKey(first.get())).isNotEqualTo(coverBaseKey(second.get()))
    }

    @Test
    fun `different byte length produces a different key`() {
        val first = albumArt(size = 20L)
        val second = albumArt(size = 21L)

        assertThat(coverBaseKey(first.get())).isNotEqualTo(coverBaseKey(second.get()))
    }

    @Test
    fun `different mime type produces a different key`() {
        val first = albumArt(mimeType = "image/jpeg")
        val second = albumArt(mimeType = "image/png")

        assertThat(coverBaseKey(first.get())).isNotEqualTo(coverBaseKey(second.get()))
    }

    @Test
    fun `unknown content hash falls back to the file based uri`() {
        val first = albumArt(filePath = "content://track-a.mp3", hashCode = 0)
        val second = albumArt(filePath = "content://track-b.mp3", hashCode = 0)

        assertThat(coverBaseKey(first.get())).isEqualTo(first.get())
        assertThat(coverBaseKey(first.get())).isNotEqualTo(coverBaseKey(second.get()))
    }

    @Test
    fun `album art without size falls back to the file based uri`() {
        val uri = albumArt(size = 0L)

        assertThat(coverBaseKey(uri)).isEqualTo(uri.get())
    }

    @Test
    fun `http album art is keyed by its url`() {
        val uri = AlbumArtUri(filePath = "https://example.com/cover.jpg")

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

    private fun albumArt(filePath: String = "content://track.mp3", offset: Long = 10L, size: Long = 20L, hashCode: Int = 30, mimeType: String = "image/jpeg") = AlbumArtUri(
        filePath = filePath,
        offset = offset,
        size = size,
        hashCode = hashCode,
        mimeType = mimeType,
    )
}
