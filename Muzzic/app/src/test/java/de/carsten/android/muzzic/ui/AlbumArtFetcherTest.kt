package de.carsten.android.muzzic.ui

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import coil3.decode.ContentMetadata
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import de.carsten.android.muzzic.TestConfig
import de.carsten.android.muzzic.model.AlbumArtUri
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class AlbumArtFetcherTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val options = Options(context = context)

    @Test
    fun `fetch returns the requested file slice`() = runTest {
        val prefix = "0123456789".toByteArray()
        val cover = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 4, 5)
        val file = tempAudioFile(prefix + cover + "audio-data".toByteArray())
        val data = albumArt(file, offset = prefix.size.toLong(), size = cover.size.toLong(), mimeType = "image/jpeg")

        val result = fetch(data)

        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.dataSource).isEqualTo(DataSource.DISK)
        assertThat(result.source.source().readByteArray(data.size)).isEqualTo(cover)
        result.source.close()
    }

    @Test
    fun `fetch returns null when size is zero`() = runTest {
        val data = AlbumArtUri(filePath = "/does/not/matter.mp3", size = 0L)

        assertThat(AlbumArtFetcher(data, options, context).fetch()).isNull()
    }

    @Test
    fun `fetch slices a content descriptor that already has a start offset`() = runTest {
        val prefix = "0123456789".toByteArray()
        val cover = byteArrayOf(9, 8, 7, 6, 5)
        val file = tempAudioFile(prefix + cover + "audio-data".toByteArray())
        val startOffset = 4L
        val offsetInSlice = 6L
        val descriptor = assetDescriptor(file, startOffset)
        val mockedContext = contextWithDescriptor(descriptor)
        val data = albumArt(
            filePath = "content://test/album.mp3",
            offset = offsetInSlice,
            size = cover.size.toLong(),
            mimeType = "image/png",
        )

        val result = fetch(data, mockedContext)
        val metadata = result.source.metadata as ContentMetadata

        assertThat(metadata.assetFileDescriptor.startOffset).isEqualTo(startOffset + offsetInSlice)
        assertThat(metadata.assetFileDescriptor.length).isEqualTo(cover.size.toLong())
        assertThat(result.source.source().readByteArray(data.size)).isEqualTo(cover)
        result.source.close()
    }

    @Test
    fun `fetch rejects a range outside the file`() = runTest {
        val file = tempAudioFile("0123456789".toByteArray())
        val data = albumArt(file, offset = 8L, size = 5L, mimeType = "image/jpeg")

        val error = runCatching { AlbumArtFetcher(data, options, context).fetch() }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("out of bounds")
    }

    @Test
    fun `mapper maps the album art scheme only`() {
        val mapper = AlbumArtFetcher.Mapper()
        val uri = AlbumArtUri(filePath = "content://track.mp3", offset = 10L, size = 20L, hashCode = 30, mimeType = "image/jpeg")

        assertThat(mapper.map(uri.get(), options)).isEqualTo(uri)
        assertThat(mapper.map("content://track.mp3", options)).isNull()
        assertThat(mapper.map("https://example.com/cover.jpg", options)).isNull()
    }

    private suspend fun fetch(data: AlbumArtUri, fetcherContext: Context = context): SourceFetchResult = AlbumArtFetcher(data, options, fetcherContext).fetch() as SourceFetchResult

    private fun albumArt(file: File, offset: Long, size: Long, mimeType: String) = AlbumArtUri(filePath = file.absolutePath, offset = offset, size = size, mimeType = mimeType)

    private fun albumArt(filePath: String, offset: Long, size: Long, mimeType: String) = AlbumArtUri(filePath = filePath, offset = offset, size = size, mimeType = mimeType)

    private fun tempAudioFile(bytes: ByteArray): File = File.createTempFile("album-art", ".mp3").apply {
        writeBytes(bytes)
        deleteOnExit()
    }

    private fun assetDescriptor(file: File, startOffset: Long): AssetFileDescriptor {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(fileDescriptor, startOffset, file.length() - startOffset)
    }

    private fun contextWithDescriptor(descriptor: AssetFileDescriptor): Context {
        val resolver = mockk<ContentResolver>()
        every { resolver.openAssetFileDescriptor(any(), "r") } returns descriptor
        return mockk<Context>().also {
            every { it.contentResolver } returns resolver
        }
    }
}
