package de.carsten.android.muzzic.scanning

import android.content.Context
import androidx.core.net.toUri
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.mediaId
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.playlist.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

/**
 * Implementation of [FileScanner] that imports M3U playlists from the file system.
 *
 * Playlists are discovered chunk-wise via [FileScanner.scanForFiles] (Storage Access Framework)
 * and read from their content [android.net.Uri] without requiring direct file access.
 */
class PlaylistFileScanner(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val appSettingsRepository: AppSettingsRepository,
) : FileScanner {

    companion object {
        const val SCANNER_ID = "PlaylistFileScanner"
        private val logger = logger()
    }

    override val scannerId: String = SCANNER_ID
    override val notificationTitleRes: Int = R.string.scan_playlists

    override suspend fun scan(onProgress: ((String, Int) -> Unit)?) = withContext(Dispatchers.IO) {
        val configuredDir = appSettingsRepository.getPlaylistDirectory()?.toUri() ?: return@withContext

        val playlistFiles =
            scanForFiles(context, configuredDir, AppConfig.Scanning.SUPPORTED_PLAYLIST_FILES)
                .toList()
                .flatten()

        val totalPlaylists = playlistFiles.size
        playlistFiles.forEachIndexed { index, file ->
            coroutineContext.ensureActive()
            val entries =
                try {
                    context.contentResolver.openInputStream(file.uri)?.use { M3uParser.parse(it) }
                } catch (e: Exception) {
                    logger.error("Failed to import playlist ${file.displayName}", e)
                    null
                } ?: emptyList()
            if (entries.isNotEmpty()) {
                val playlistName = file.displayName.substringBeforeLast('.')
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
