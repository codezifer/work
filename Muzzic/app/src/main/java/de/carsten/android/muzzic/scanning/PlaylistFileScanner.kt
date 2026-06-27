package de.carsten.android.muzzic.scanning

import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.mediaId
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.playlist.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Implementation of [FileScanner] that imports M3U playlists from the file system.
 */
class PlaylistFileScanner(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val appSettingsRepository: AppSettingsRepository,
) : FileScanner {

    override val scannerId: String = "PlaylistScanner"
    override val notificationTitleRes: Int = R.string.scan_playlists

    override suspend fun scan(onProgress: ((String, Int) -> Unit)?) = withContext(Dispatchers.IO) {
        val configuredDir = appSettingsRepository.getPlaylistDirectory() ?: return@withContext
        val playlistFolders = contentToFiles(configuredDir)
        if (playlistFolders.isEmpty()) return@withContext

        val playlistFiles = playlistFolders.flatMap { folder ->
            folder.walkTopDown().filter { file ->
                file.isFile && (file.extension.lowercase() == "m3u" || file.extension.lowercase() == "m3u8")
            }.distinctBy { it.absolutePath }.toList()
        }

        val totalPlaylists = playlistFiles.size
        playlistFiles.forEachIndexed { index, file ->
            val entries = M3uParser.parse(file)
            if (entries.isNotEmpty()) {
                val playlistName = file.nameWithoutExtension
                val playlistId = mediaId(playlistName).toString()
                val playlist = Playlist(playlistName).apply { id = playlistId }

                playlistDao.insertPlaylist(playlist)
                playlistDao.clearPlaylist(playlistId)

                // Match songs in DB by path
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
