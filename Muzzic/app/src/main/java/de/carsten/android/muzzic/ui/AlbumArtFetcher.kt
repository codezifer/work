package de.carsten.android.muzzic.ui

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.ContentMetadata
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.map.Mapper as CoilMapper
import coil3.request.Options
import coil3.toUri as toCoilUri
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import java.io.File
import okio.buffer
import okio.source

/**
 * Fetches embedded album art bytes from a local audio file.
 *
 * The requested byte range is exposed to Coil as a sliced [AssetFileDescriptor] instead of
 * being copied into memory. That keeps the fetcher allocation-free for the image bytes and
 * let's Coil's `ImageDecoder` read the cover directly from the file, which enables hardware
 * bitmaps for the display request.
 *
 * @param data the parsed album art location (file path, byte offset and length).
 * @param options the current request options; supplies the file system.
 * @param context used to open `content://` streams.
 */
class AlbumArtFetcher(private val data: AlbumArtUri, private val options: Options, private val context: Context) : Fetcher {
    private val logger = logger()

    override suspend fun fetch(): FetchResult? {
        if (data.size <= 0) {
            logger.debug("Skipping fetch: size is 0 for ${data.filePath}")
            return null
        }

        val descriptor = openSlice()
        return try {
            SourceFetchResult(
                source =
                ImageSource(
                    source = descriptor.createInputStream().source().buffer(),
                    fileSystem = options.fileSystem,
                    metadata = ContentMetadata(data.filePath.toCoilUri(), descriptor),
                ),
                mimeType = data.mimeType,
                dataSource = DataSource.DISK,
            )
        } catch (e: Exception) {
            runCatching { descriptor.close() }
            logger.error("Failed to open album art slice for ${data.filePath}", e)
            throw e
        }
    }

    /**
     * Opens the audio file and narrows it to the embedded image range.
     *
     * The returned descriptor owns the underlying file descriptor. It is handed to Coil's
     * [ImageSource], which closes it after decoding.
     *
     * @return the descriptor limited to [AlbumArtUri.offset] and [AlbumArtUri.size].
     */
    private fun openSlice(): AssetFileDescriptor {
        val uri = data.filePath.toUri()
        val source =
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.openAssetFileDescriptor(uri, "r")
            } else {
                val fileDescriptor = ParcelFileDescriptor.open(File(data.filePath), ParcelFileDescriptor.MODE_READ_ONLY)
                AssetFileDescriptor(fileDescriptor, 0L, fileDescriptor.statSize)
            } ?: error("Failed to open album art source for ${data.filePath}")

        val available = source.length
        if (data.offset < 0 || (available >= 0 && data.offset + data.size > available)) {
            runCatching { source.close() }
            throw IllegalArgumentException(
                "Album art range is out of bounds for ${data.filePath}: offset=${data.offset}, size=${data.size}, available=$available",
            )
        }

        // Share the underlying fd: the range is validated above and the slice is closed by ImageSource.
        return AssetFileDescriptor(source.parcelFileDescriptor, source.startOffset + data.offset, data.size)
    }

    class Factory(private val context: Context) : Fetcher.Factory<AlbumArtUri> {
        override fun create(data: AlbumArtUri, options: Options, imageLoader: ImageLoader): Fetcher = AlbumArtFetcher(data, options, context)
    }

    class Mapper : CoilMapper<String, AlbumArtUri> {
        override fun map(data: String, options: Options): AlbumArtUri? {
            if (data.startsWith(ALBUMART_SCHEME)) {
                return AlbumArtUri.parse(data)
            }
            return null
        }
    }
}
