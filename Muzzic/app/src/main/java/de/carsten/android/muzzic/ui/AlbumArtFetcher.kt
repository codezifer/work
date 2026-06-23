package de.carsten.android.muzzic.ui

import android.content.Context
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
import java.io.RandomAccessFile
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
            logger.debug("Skipping fetch: size is 0 for ${data.albumArt}")
            return null
        }
        logger.debug("Fetching: ${data.albumArt} offset=${data.offset} size=${data.size}")
        if (data.albumArt.startsWith("http")) {
            return handleHttpFile()
        }

        return handleLocalFile()
    }

    private suspend fun handleLocalFile(): FetchResult {
        val buffer =
            withContext(Dispatchers.IO) {
                val file = File(data.albumArt)
                if (!file.exists()) {
                    logger.error("File does not exist: ${data.albumArt}")
                    return@withContext null
                }

                val randomAccessFile = RandomAccessFile(file, "r")
                val fileLength = file.length()

                if (data.offset !in 0..<fileLength) {
                    logger.error("Invalid offset: ${data.offset} (File size: $fileLength) for ${data.albumArt}")
                    randomAccessFile.close()
                    return@withContext null
                }

                if (data.offset + data.size > fileLength) {
                    logger.error("Invalid size: ${data.size} at offset ${data.offset} (File size: $fileLength) for ${data.albumArt}")
                    randomAccessFile.close()
                    return@withContext null
                }

                randomAccessFile.seek(data.offset)
                val bytes = ByteArray(data.size.toInt())
                randomAccessFile.readFully(bytes)

                // Magic Number Check (Sanity check for common image formats)
                if (bytes.size > 4) {
                    val header = bytes.take(4).joinToString("") { "%02x".format(it) }
                    logger.debug("Magic Number Header: $header for ${data.albumArt}")
                    // JPEG: ffd8ffe0, PNG: 89504e47, etc.
                }

                randomAccessFile.close()

                Buffer().apply {
                    write(bytes)
                }
            } ?: throw IllegalArgumentException("Failed to read image data from ${data.albumArt}")

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
        val url = data.albumArt
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
