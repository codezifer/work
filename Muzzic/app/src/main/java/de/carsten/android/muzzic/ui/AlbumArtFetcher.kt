package de.carsten.android.muzzic.ui

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
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
    companion object {
        private const val TAG = "AlbumArtFetcher"
    }

    override suspend fun fetch(): FetchResult? {
        if (data.size <= 0) {
            Log.d(TAG, "Skipping fetch: size is 0 for ${data.filePath}")
            return null
        }
        Log.d(TAG, "Fetching: ${data.filePath} offset=${data.offset} size=${data.size}")
        if (data.filePath.startsWith("http")) {
            return handleHttpFile()
        }

        return handleLocalFile()
    }

    private suspend fun handleLocalFile(): FetchResult {
        val buffer =
            withContext(Dispatchers.IO) {
                val file = File(data.filePath)
                if (!file.exists()) {
                    Log.e(TAG, "File does not exist: ${data.filePath}")
                    return@withContext null
                }

                val randomAccessFile = RandomAccessFile(file, "r")
                val fileLength = file.length()

                if (data.offset < 0 || data.offset >= fileLength) {
                    Log.e(TAG, "Invalid offset: ${data.offset} (File size: $fileLength) for ${data.filePath}")
                    randomAccessFile.close()
                    return@withContext null
                }

                if (data.offset + data.size > fileLength) {
                    Log.e(TAG, "Invalid size: ${data.size} at offset ${data.offset} (File size: $fileLength) for ${data.filePath}")
                    randomAccessFile.close()
                    return@withContext null
                }

                randomAccessFile.seek(data.offset)
                val bytes = ByteArray(data.size.toInt())
                randomAccessFile.readFully(bytes)

                // Magic Number Check (Sanity check for common image formats)
                if (bytes.size > 4) {
                    val header = bytes.take(4).joinToString("") { "%02x".format(it) }
                    Log.d(TAG, "Magic Number Header: $header for ${data.filePath}")
                    // JPEG: ffd8ffe0, PNG: 89504e47, etc.
                }

                randomAccessFile.close()

                Buffer().apply {
                    write(bytes)
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
}
