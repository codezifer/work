package de.carsten.android.muzzic.ui

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.buffer
import okio.source

/**
 * Fetches embedded album art bytes from a local audio file.
 *
 * The extracted bytes are cached in Coil's [DiskCache] under [Options.diskCacheKey], so an audio
 * file only has to be read once per cover: later requests (display, palette, blur) are served
 * from one shared disk entry. The byte range is reached with a real seek instead of reading
 * through the stream, which matters for `content://` URIs.
 *
 * @param data the parsed album art location (file path, byte offset and length).
 * @param options the current request options; supplies the disk cache key and policy.
 * @param context used to open `content://` streams.
 * @param diskCache Coil's disk cache, or null when caching is unavailable.
 */
class AlbumArtFetcher(private val data: AlbumArtUri, private val options: Options, private val context: Context, private val diskCache: DiskCache?) : Fetcher {
    private val logger = logger()

    /**
     * Content identity of the cover, shared by every consumer variant (display, palette, blur).
     */
    private val diskCacheKey: String
        get() = options.diskCacheKey ?: data.get()

    override suspend fun fetch(): FetchResult? {
        if (data.size <= 0) {
            logger.debug("Skipping fetch: size is 0 for ${data.filePath}")
            return null
        }

        readFromDiskCache()?.let { return it }

        return writeToDiskCache() ?: readToBuffer()
    }

    /**
     * Returns the cover bytes from Coil's disk cache if they were already extracted.
     */
    private fun readFromDiskCache(): FetchResult? {
        val cache = diskCache ?: return null
        if (!options.diskCachePolicy.readEnabled) return null

        val snapshot = cache.openSnapshot(diskCacheKey) ?: return null
        logger.debug("Disk cache hit for ${data.filePath}")
        return SourceFetchResult(
            source =
            ImageSource(
                file = snapshot.data,
                fileSystem = cache.fileSystem,
                diskCacheKey = diskCacheKey,
                closeable = snapshot,
            ),
            mimeType = data.mimeType,
            dataSource = DataSource.DISK,
        )
    }

    /**
     * Extracts the cover bytes and stores them in Coil's disk cache.
     *
     * Bytes are streamed straight from the audio file into the cache entry, so no full-size
     * byte array is allocated.
     *
     * @return a result backed by the new cache entry, or null if it could not be written.
     */
    private suspend fun writeToDiskCache(): FetchResult? = withContext(Dispatchers.IO) {
        val cache = diskCache ?: return@withContext null
        if (!options.diskCachePolicy.writeEnabled) return@withContext null

        val editor = cache.openEditor(diskCacheKey) ?: return@withContext null

        try {
            openStreamAtOffset().use { input ->
                cache.fileSystem.sink(editor.data).buffer().use { sink ->
                    sink.write(input.source(), data.size)
                }
            }

            val snapshot = editor.commitAndOpenSnapshot() ?: return@withContext null
            logger.debug("Extracted ${data.size} bytes of album art for ${data.filePath}")
            SourceFetchResult(
                source =
                ImageSource(
                    file = snapshot.data,
                    fileSystem = cache.fileSystem,
                    diskCacheKey = diskCacheKey,
                    closeable = snapshot,
                ),
                mimeType = data.mimeType,
                dataSource = DataSource.DISK,
            )
        } catch (e: Exception) {
            editor.abort()
            logger.error("Failed to write album art to the disk cache for ${data.filePath}", e)
            null
        }
    }

    /**
     * Reads the embedded image bytes into memory, without touching the disk cache.
     *
     * Used as a fallback when no disk cache is available or the cache write failed.
     */
    private suspend fun readToBuffer(): FetchResult = withContext(Dispatchers.IO) {
        val buffer = Buffer()
        openStreamAtOffset().use { input ->
            val bytes = ByteArray(data.size.toInt())
            var totalRead = 0
            while (totalRead < data.size) {
                val read = input.read(bytes, totalRead, data.size.toInt() - totalRead)
                if (read == -1) break
                totalRead += read
            }

            if (totalRead != data.size.toInt()) {
                logger.error("Failed to read full album art data (expected ${data.size}, read $totalRead) for ${data.filePath}")
                throw IllegalArgumentException("Failed to read image data from ${data.filePath}")
            }

            buffer.write(bytes)
        }

        SourceFetchResult(
            source =
            ImageSource(
                source = buffer,
                fileSystem = FileSystem.SYSTEM,
            ),
            mimeType = data.mimeType,
            dataSource = DataSource.DISK,
        )
    }

    /**
     * Opens the audio file positioned at the start of the embedded image.
     *
     * `content://` URIs and plain paths are seeked directly. Providers that do not expose a
     * seekable descriptor fall back to opening the stream and skipping [AlbumArtUri.offset]
     * bytes sequentially.
     *
     * @return the positioned stream; the caller is responsible for closing it.
     */
    private fun openStreamAtOffset(): InputStream {
        val uri = data.filePath.toUri()

        if (data.filePath.startsWith("content://")) {
            val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            if (descriptor != null) {
                val stream = AssetFileDescriptor.AutoCloseInputStream(descriptor)
                try {
                    // openInputStream applies startOffset implicitly; a raw descriptor does not.
                    stream.channel.position(descriptor.startOffset + data.offset)
                    return stream
                } catch (e: IOException) {
                    logger.debug("Descriptor is not seekable, falling back to skip for ${data.filePath}")
                    stream.close()
                }
            }
        } else {
            val stream = FileInputStream(data.filePath)
            stream.channel.position(data.offset)
            return stream
        }

        val stream =
            if (data.filePath.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)
            } else {
                FileInputStream(data.filePath)
            }

        return checkNotNull(stream) { "Failed to open image data stream for ${data.filePath}" }.also {
            skipFully(it, data.offset)
        }
    }

    /**
     * Skips exactly [bytes] bytes from the stream.
     *
     * A single [InputStream.skip] call is not guaranteed to skip the requested amount, so repeat
     * until the position is reached or the end is hit.
     *
     * @param input source stream.
     * @param bytes number of bytes to skip.
     */
    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<AlbumArtUri> {
        override fun create(data: AlbumArtUri, options: Options, imageLoader: ImageLoader): Fetcher = AlbumArtFetcher(data, options, context, imageLoader.diskCache)
    }

    class Mapper : coil3.map.Mapper<String, AlbumArtUri> {
        override fun map(data: String, options: Options): AlbumArtUri? {
            if (data.startsWith(ALBUMART_SCHEME)) {
                return AlbumArtUri.parse(data)
            }
            return null
        }
    }
}
