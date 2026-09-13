package de.carsten.android.muzzic.scanning

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.mediaId
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.playlist.M3uEntry
import de.carsten.android.muzzic.playlist.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

/**
 * Implementation of [FileScanner] that imports M3U playlists from the file system.
 *
 * Playlists are discovered chunk-wise via [scanForFiles] (Storage Access Framework)
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
        try {
            logger.info("Start to scan for playlists files ...")
            val configuredDir = appSettingsRepository.getPlaylistDirectory()?.toUri() ?: return@withContext

            val playlistFiles =
                scanForFiles(context, configuredDir, AppConfig.Scanning.SUPPORTED_PLAYLIST_FILES)
                    .toList()
                    .flatten()

            val totalPlaylists = playlistFiles.size

            // Match songs in DB by path - Load once before the loop to prevent CursorWindow OutOfMemory errors
            // Resolve SAF content:// URIs back to real filesystem absolute paths for proper comparison
            val allSongs = songDao.getAllSongs().first()
            val pathToSong = mutableMapOf<String, Song>()
            allSongs.forEach { song ->
                val filePath = song.filePath
                if (filePath != null) {
                    // Map by raw string path first
                    pathToSong[filePath] = song

                    // Also try resolving SAF content URI to real absolute path
                    try {
                        val resolvedFsPath = FileUtil.getFilePathFromUri(context, Uri.parse(filePath))
                        if (resolvedFsPath != null) {
                            pathToSong[resolvedFsPath] = song
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for individual songs
                    }
                }
            }

            playlistFiles.forEachIndexed { index, file ->
                coroutineContext.ensureActive()
                val entries: List<M3uEntry> = try {
                    context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                        M3uParser.parse(inputStream, logger, file.displayName)
                    }
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

                    val playlistSongs = mutableListOf<PlaylistSong>()
                    entries.forEachIndexed { songIndex, entry ->
                        val matchedSong = pathToSong[entry.path]
                        if (matchedSong != null) {
                            playlistSongs.add(
                                PlaylistSong(playlistId, matchedSong.id, songIndex),
                            )
                        }
                    }
                    if (playlistSongs.isNotEmpty()) {
                        playlistDao.insertPlaylistSongs(playlistSongs)
                    }
                }
                val progress = ((index + 1).toFloat() / totalPlaylists * AppConfig.Scanning.MAX_PROGRESS).toInt()
                onProgress?.invoke("Importing $totalPlaylists playlists...", progress)
            }
        } finally {
            logger.info("... Finished to scan playlist files.")
        }
    }
}
