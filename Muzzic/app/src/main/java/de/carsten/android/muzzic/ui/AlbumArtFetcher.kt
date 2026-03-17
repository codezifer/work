package de.carsten.android.muzzic.ui

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import de.carsten.android.muzzic.model.AlbumArtUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import java.io.RandomAccessFile

class AlbumArtFetcher(
    private val data: AlbumArtUri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Use RandomAccessFile to read specific bytes without loading the whole file
        val buffer = withContext(Dispatchers.IO) {
            val randomAccessFile = RandomAccessFile(data.filePath, "r")
            randomAccessFile.seek(data.offset)
            val bytes = ByteArray(data.size.toInt())
            randomAccessFile.readFully(bytes)
            randomAccessFile.close()
            Buffer().apply {
                write(bytes)
            }
        }
        return SourceFetchResult(
            source = ImageSource(
                source = buffer,
                fileSystem = FileSystem.SYSTEM
            ),
            mimeType = data.mimeType,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<AlbumArtUri> {
        override fun create(data: AlbumArtUri, options: Options, imageLoader: ImageLoader): Fetcher {
            return AlbumArtFetcher(data, options)
        }
    }
}
