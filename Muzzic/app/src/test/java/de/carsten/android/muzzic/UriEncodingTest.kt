package de.carsten.android.muzzic

import de.carsten.android.muzzic.model.AlbumArtUri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class UriEncodingTest {

    @Test
    fun `toPlayableUri should encode raw path with brackets correctly`() {
        val rawPath = "/storage/emulated/0/Music/[Artist]/Song.mp3"
        val uri = rawPath.toPlayableUri()

        // Uri.fromFile encodes brackets to %5B and %5D
        assertEquals("file:///storage/emulated/0/Music/%5BArtist%5D/Song.mp3", uri.toString())
    }

    @Test
    fun `toPlayableUri should not double-encode already encoded file URI`() {
        val encodedUriString = "file:///storage/emulated/0/Music/%5BArtist%5D/Song.mp3"
        val uri = encodedUriString.toPlayableUri()

        // Should remain unchanged, NOT become %255B
        assertEquals(encodedUriString, uri.toString())
    }

    @Test
    fun `toPlayableUri should handle spaces in raw path`() {
        val rawPath = "/storage/emulated/0/Music/Artist Name/Song Name.mp3"
        val uri = rawPath.toPlayableUri()

        assertEquals("file:///storage/emulated/0/Music/Artist%20Name/Song%20Name.mp3", uri.toString())
    }

    @Test
    fun `toPlayableUri should handle http URIs`() {
        val httpUrl = "http://example.com/music/song.mp3"
        val uri = httpUrl.toPlayableUri()

        assertEquals(httpUrl, uri.toString())
    }

    @Test
    fun `AlbumArtUri should encode special characters in path`() {
        val filePath = "/storage/emulated/0/Music/[Artist]/Album Art.jpg"
        val albumArtUri = AlbumArtUri(filePath, offset = 100, size = 500, hashCode = 123)
        val uriString = albumArtUri.get()

        // Path should be encoded, including brackets and spaces
        // format: albumart://<encoded_path>?offset=...
        val expectedEncodedPath = "%2Fstorage%2Femulated%2F0%2FMusic%2F%5BArtist%5D%2FAlbum%20Art.jpg"
        assertEquals("albumart://$expectedEncodedPath?offset=100&size=500&hashCode=123", uriString)
    }
}
