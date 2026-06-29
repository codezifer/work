package de.carsten.android.muzzic.scanning

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.mediaId
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.playlist.M3uParser
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Implementation of [FileScanner] that imports M3U playlists from the file system.
 */
class PlaylistFileScanner(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val appSettingsRepository: AppSettingsRepository,
) : FileScanner {

    override val scannerId: String = "PlaylistScanner"
    override val notificationTitleRes: Int = R.string.scan_playlists

    override suspend fun scan(onProgress: ((String, Int) -> Unit)?) = withContext(Dispatchers.IO) {
        val configuredDir = appSettingsRepository.getPlaylistDirectory() ?: return@withContext
        val roots = getScanningRoots(configuredDir)

        val playlistUris = scanForFiles(context, roots, setOf("m3u", "m3u8"))

        val totalPlaylists = playlistUris.size
        playlistUris.forEachIndexed { index, uriString ->
            coroutineContext.ensureActive()
            val entries = M3uParser.parse(context, uriString)
            if (entries.isNotEmpty()) {
                val document = DocumentFile.fromSingleUri(context, Uri.parse(uriString))
                val playlistName = document?.name?.substringBeforeLast('.') ?: "Playlist $index"
                val playlistId = mediaId(playlistName).toString()
                val playlist = Playlist(playlistName).apply { id = playlistId }

                playlistDao.insertPlaylist(playlist)
                playlistDao.clearPlaylist(playlistId)

                // Match songs in DB by path (URI string)
                val allSongs = songDao.getAllSongs().first()
                val pathToSong = allSongs.associateBy { it.filePath }

                entries.forEachIndexed { songIndex, entry ->
                    val matchedSong = pathToSong[entry.path]
                    if (matchedSong != null) {
                        playlistDao.insertPlaylistSong(
                            PlaylistSong(playlistId, matchedSong.id, songIndex),
                        )
                    }
                }
            }
            val progress = ((index + 1).toFloat() / totalPlaylists * 100).toInt()
            onProgress?.invoke("Importing $totalPlaylists playlists...", progress)
        }
    }
}
