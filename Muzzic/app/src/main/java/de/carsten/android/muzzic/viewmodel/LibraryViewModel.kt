package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val genreRepository: GenreRepository,
    private val playlistRepository: PlaylistRepository,
    private val playingQueueRepository: PlayingQueueRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : AbstractViewModel(playingQueueRepository, mediaLibraryManager) {
    val songs: StateFlow<List<Song>> =
        musicRepository.getAllSongs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val artists: StateFlow<List<ArtistDto>> =
        artistRepository.getArtistInformation().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val albums: StateFlow<List<AlbumDto>> =
        albumRepository.getAlbumInformation().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val genres: StateFlow<List<GenreDto>> =
        genreRepository.getGenreInformation().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val playlists: StateFlow<List<PlaylistDto>> =
        playlistRepository.getPlaylistInformation().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun playSong(song: Song) {
        mediaLibraryManager.playContent(song.toMediaItem())
    }

    fun playArtist(artistName: String) {
        viewModelScope.launch {
            enqueue(artistRepository.getSongsByArtist(artistName))
        }
    }

    fun playAlbum(artistName: String, albumName: String) {
        viewModelScope.launch {
            enqueue(albumRepository.getSongsByAlbum(artistName, albumName).first())
        }
    }

    fun playPlaylist(playlistId: String) {
        viewModelScope.launch {
            enqueue(playlistRepository.getSongsInPlaylist(playlistId))
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun setIntoPlayingQueue(playlistId: String) {
        viewModelScope.launch {
            playingQueueRepository.clear()
            playingQueueRepository.addSongs(
                mediaItems =
                playlistRepository
                    .getSongsInPlaylist(playlistId)
                    .map { it.toMediaItem() },
            )
        }
    }

    fun playGenre(genreName: String) {
        viewModelScope.launch {
            enqueue(genreRepository.getSongsByGenre(genreName))
        }
    }
}
