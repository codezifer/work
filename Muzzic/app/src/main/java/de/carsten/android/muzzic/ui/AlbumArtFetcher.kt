package de.carsten.android.muzzic.ui

import android.content.Context
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.sink

class AlbumArtFetcher(private val data: AlbumArtUri, private val options: Options, private val context: Context, private val okHttpClient: OkHttpClient) : Fetcher {
    private val logger = logger()

    override suspend fun fetch(): FetchResult? {
        if (data.size <= 0) {
            logger.debug("Skipping fetch: size is 0 for ${data.filePath}")
            return null
        }
        logger.debug("Fetching: ${data.filePath} offset=${data.offset} size=${data.size}")
        if (data.filePath.startsWith("http")) {
            return handleHttpFile()
        }

        return handleLocalFile()
    }

    /**
     * Reads the embedded image bytes for a local file.
     *
     * Content URIs are opened via the [android.content.ContentResolver],
     * plain paths via [File].
     *
     * @return fetch result with the image bytes.
     */
    private suspend fun handleLocalFile(): FetchResult {
        val buffer =
            withContext(Dispatchers.IO) {
                try {
                    val inputStream =
                        if (data.filePath.startsWith("content://")) {
                            context.contentResolver.openInputStream(data.filePath.toUri())
                        } else {
                            File(data.filePath).inputStream()
                        } ?: return@withContext null
                    inputStream.use { input ->
                        skipFully(input, data.offset)
                        val bytes = ByteArray(data.size.toInt())
                        var totalRead = 0
                        while (totalRead < data.size) {
                            val read = input.read(bytes, totalRead, data.size.toInt() - totalRead)
                            if (read == -1) break
                            totalRead += read
                        }

                        if (totalRead != data.size.toInt()) {
                            logger.error("Failed to read full album art data (expected ${data.size}, read $totalRead) for ${data.filePath}")
                            return@withContext null
                        }

                        Buffer().apply {
                            write(bytes)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error reading album art from file ${data.filePath}", e)
                    null
                }
            } ?: throw IllegalArgumentException("Failed to read image data from ${data.filePath}")

        return SourceFetchResult(
            source =
            ImageSource(
                source = buffer,
                fileSystem = FileSystem.SYSTEM,
            ),
            mimeType = data.mimeType,
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun handleHttpFile(): FetchResult {
        val url = data.filePath
        val (path, mimeType) =
            withContext(Dispatchers.IO) {
                val cacheFile = cacheFile(url)

                // cache hit
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    return@withContext Pair(cacheFile.toOkioPath(), null)
                }

                // cache miss
                val req = Request.Builder().url(url).build()
                val res = okHttpClient.newCall(req).execute()

                check(res.isSuccessful) {
                    "Request failed: ${res.code} ${res.message}"
                }

                val body =
                    checkNotNull(res.body) {
                        "Empty response body for $url!"
                    }

                val tmpFile = File(cacheFile.parent, "${cacheFile.name}.tmp")
                tmpFile.parentFile?.mkdirs()

                body.source().use { source ->
                    tmpFile.sink().buffer().use { sink ->
                        sink.writeAll(source)
                    }
                }
                tmpFile.renameTo(cacheFile)

                Pair(cacheFile.toOkioPath(), res.header("Content-Type"))
            }

        return SourceFetchResult(
            source =
            ImageSource(
                file = path,
                fileSystem = FileSystem.SYSTEM,
            ),
            mimeType = mimeType,
            dataSource = DataSource.NETWORK,
        )
    }

    /**
     * Skips exactly [bytes] bytes from the stream.
     *
     * A single [InputStream.skip] call is not guaranteed to skip the requested
     * amount, so repeat until the position is reached or the end is hit.
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

    private fun cacheFile(url: String): File {
        val cacheDir = File(context.cacheDir, "album_art_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val hash = url.md5()
        return File(cacheDir, hash)
    }

    private fun String.md5(): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    class Factory(private val context: Context, private val okHttpClient: OkHttpClient) : Fetcher.Factory<AlbumArtUri> {
        override fun create(data: AlbumArtUri, options: Options, imageLoader: ImageLoader): Fetcher = AlbumArtFetcher(data, options, context, okHttpClient)
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
