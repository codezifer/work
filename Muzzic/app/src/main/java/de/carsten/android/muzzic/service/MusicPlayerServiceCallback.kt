package de.carsten.android.muzzic.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import androidx.core.net.toUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

@UnstableApi
class MusicPlayerServiceCallback(
    private val serviceScope: CoroutineScope,
    private val musicRepository: MusicRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val genreRepository: GenreRepository,
    private val playlistRepository: PlaylistRepository,
    private val playingQueueRepository: PlayingQueueRepository,
) : MediaLibrarySession.Callback {

    companion object {
        const val ROOT_ID = "[ROOT]"
        const val ARTISTS_ID = "[ARTISTS]"
        const val ALBUMS_ID = "[ALBUMS]"
        const val SONGS_ID = "[SONGS]"
        const val PLAYLISTS_ID = "[PLAYLISTS]"
        const val GENRES_ID = "[GENRES]"

        const val ARTIST_PREFIX = "[ARTIST]:"
        const val ALBUM_PREFIX = "[ALBUM]:"
        const val PLAYLIST_PREFIX = "[PLAYLIST]:"
        const val GENRE_PREFIX = "[GENRE]:"
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootItem = MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.future {
            val items = when (parentId) {
                ROOT_ID -> getRootChildren()
                ARTISTS_ID -> getArtists()
                ALBUMS_ID -> getAlbums()
                SONGS_ID -> getAllSongs()
                PLAYLISTS_ID -> getPlaylists()
                GENRES_ID -> getGenres()
                else -> {
                    if (parentId.startsWith(ARTIST_PREFIX)) {
                        val artistName = parentId.removePrefix(ARTIST_PREFIX)
                        getAlbumsByArtist(artistName)
                    } else if (parentId.startsWith(ALBUM_PREFIX)) {
                        // Expected format [ALBUM]:artistName:albumName
                        val parts = parentId.removePrefix(ALBUM_PREFIX).split(":", limit = 2)
                        if (parts.size == 2) {
                            getSongsByAlbum(parts[0], parts[1])
                        } else {
                            emptyList()
                        }
                    } else if (parentId.startsWith(PLAYLIST_PREFIX)) {
                        val playlistName = parentId.removePrefix(PLAYLIST_PREFIX)
                        getSongsByPlaylist(playlistName)
                    } else if (parentId.startsWith(GENRE_PREFIX)) {
                        val genreName = parentId.removePrefix(GENRE_PREFIX)
                        getSongsByGenre(genreName)
                    } else {
                        emptyList()
                    }
                }
            }
            LibraryResult.ofItemList(items, params)
        }
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return serviceScope.future {
            val song = songRepository.getSongById(mediaId)
            if (song != null) {
                LibraryResult.ofItem(song.toMediaItem(), null)
            } else {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            }
        }
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        return serviceScope.future {
            val songs = songRepository.searchSongs(query)
            val artists = artistRepository.searchArtists(query)
            val albums = albumRepository.searchAlbums(query)
            val playlists = playlistRepository.searchPlaylists(query)

            val totalCount = songs.size + artists.size + albums.size + playlists.size
            session.notifySearchResultChanged(browser, query, totalCount, params)
            LibraryResult.ofVoid()
        }
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.future {
            val songs = songRepository.searchSongs(query).map { it.toMediaItem() }
            val artists = artistRepository.searchArtists(query).map { it.toMediaItem() }
            val albums = albumRepository.searchAlbums(query).map { it.toMediaItem() }
            val playlists = playlistRepository.playlistDao.searchPlaylists(query).map { it.toMediaItem() }

            val allItems = mutableListOf<MediaItem>()
            allItems.addAll(artists)
            allItems.addAll(albums)
            allItems.addAll(playlists)
            allItems.addAll(songs)

            LibraryResult.ofItemList(ImmutableList.copyOf(allItems), params)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return serviceScope.future {
            val queue = playingQueueRepository.getPlayingQueue()
            if (queue.isNotEmpty()) {
                MediaSession.MediaItemsWithStartPosition(queue, 0, 0L)
            } else {
                val allSongs = musicRepository.getAllSongs().first().map { it.toMediaItem() }
                MediaSession.MediaItemsWithStartPosition(allSongs, 0, 0L)
            }
        }
    }

    // --- Helper methods to fetch data ---

    private fun getRootChildren(): List<MediaItem> {
        return listOf(
            createBrowsableItem(ARTISTS_ID, "Artists"),
            createBrowsableItem(ALBUMS_ID, "Albums"),
            createBrowsableItem(SONGS_ID, "Songs"),
            createBrowsableItem(PLAYLISTS_ID, "Playlists"),
            createBrowsableItem(GENRES_ID, "Genres")
        )
    }

    private suspend fun getArtists(): List<MediaItem> {
        return artistRepository.getArtistInformation().first().map { it.toMediaItem() }
    }

    private suspend fun getAlbums(): List<MediaItem> {
        return albumRepository.getAlbumInformation().first().map { it.toMediaItem() }
    }

    private suspend fun getAlbumsByArtist(artistName: String): List<MediaItem> {
        return albumRepository.getAlbumsByArtist(artistName).first().map { it.toMediaItem() }
    }

    private suspend fun getSongsByAlbum(artistName: String, albumName: String): List<MediaItem> {
        return albumRepository.getSongsByAlbum(artistName, albumName).first().map { it.toMediaItem() }
    }

    private suspend fun getAllSongs(): List<MediaItem> {
        return musicRepository.getAllSongs().first().map { it.toMediaItem() }
    }

    private suspend fun getGenres(): List<MediaItem> {
        return genreRepository.getGenreInformation().first().map { it.toMediaItem() }
    }

    private suspend fun getSongsByGenre(genreName: String): List<MediaItem> {
        return musicRepository.getAllSongs().first()
            .filter { it.genre == genreName }
            .map { it.toMediaItem() }
    }

    private suspend fun getPlaylists(): List<MediaItem> {
        val playlists = playlistRepository.playlistDao.getAllPlaylists().first()
        return playlists.map { it.toMediaItem() }
    }

    private suspend fun getSongsByPlaylist(playlistName: String): List<MediaItem> {
        val playlist = playlistRepository.playlistDao.getAllPlaylists().first().find { it.name == playlistName }
        return if (playlist != null) {
            playlistRepository.playlistDao.getSongsInPlaylist(playlist.id).map { it.toMediaItem() }
        } else {
            emptyList()
        }
    }

    private fun createBrowsableItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun ArtistDto.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$ARTIST_PREFIX$artistName")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(artistName)
                    .setArtworkUri(lastAlbumArt?.toUri())
                    .setExtras(android.os.Bundle().apply {
                        putInt(MediaKeys.ALBUM_COUNT, albumCount)
                        putInt(MediaKeys.SONG_COUNT, songCount)
                    })
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun AlbumDto.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$ALBUM_PREFIX$artistName:$albumName")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(albumName)
                    .setArtist(artistName)
                    .setReleaseYear(albumYear)
                    .setArtworkUri(lastAlbumArt?.toUri())
                    .setExtras(android.os.Bundle().apply {
                        putInt(MediaKeys.SONG_COUNT, songCount)
                        putLong(MediaKeys.DURATION, albumDuration)
                    })
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun Playlist.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$PLAYLIST_PREFIX$name")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setExtras(android.os.Bundle().apply {
                        putBoolean(MediaKeys.IS_AUTO_GENERATED, isAutoGenerated)
                        putString(MediaKeys.GENRE, genre)
                    })
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun GenreDto.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$GENRE_PREFIX$genreName")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(genreName)
                    .setExtras(android.os.Bundle().apply {
                        putInt(MediaKeys.ARTIST_COUNT, artistCount)
                        putInt(MediaKeys.ALBUM_COUNT, albumCount)
                        putInt(MediaKeys.SONG_COUNT, songCount)
                        putLong(MediaKeys.DURATION, genreDuration)
                    })
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }
}
