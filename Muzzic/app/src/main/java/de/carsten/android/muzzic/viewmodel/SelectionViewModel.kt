package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import de.carsten.android.muzzic.viewmodel.states.Selection
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelectionViewModel(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val playingQueueRepository: PlayingQueueRepository,
) : ViewModel() {

    private val _selectionState = MutableStateFlow(SelectionState())

    /**
     * State representing the current multi-selection.
     * Includes whether selection is active and the sets of selected artists, albums, and songs.
     */
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    /**
     * Toggles the selection status of an artist.
     * Resolves to all songs of the artist immediately.
     *
     * @param artistName The name of the artist to toggle.
     */
    fun toggleArtistSelection(artistName: String) {
        viewModelScope.launch {
            val songs = artistRepository.getSongsByArtist(artistName)
            val songIds = songs.map { it.id }.toSet()
            val current = _selectionState.value
            val newSongs = current.selectedSongs.toMutableSet()
            val newArtists = current.selectedArtists.toMutableSet()

            if (newArtists.contains(artistName)) {
                newArtists.remove(artistName)
                newSongs.removeAll(songIds)
            } else {
                newArtists.add(artistName)
                newSongs.addAll(songIds)
            }

            _selectionState.value = current.copy(
                selectedSongs = newSongs,
                selectedArtists = newArtists,
                value = if (newSongs.isNotEmpty()) Selection.MARKED else Selection.DEFAULT,
            )
        }
    }

    /**
     * Toggles the selection status of an album.
     * Resolves to all songs of the album immediately.
     *
     * @param artistName The name of the artist who owns the album.
     * @param albumName The name of the album to toggle.
     */
    fun toggleAlbumSelection(artistName: String, albumName: String) {
        viewModelScope.launch {
            val songs = albumRepository.getSongsByAlbum(artistName, albumName).first()
            val songIds = songs.map { it.id }.toSet()
            val key = "$artistName|$albumName"
            val current = _selectionState.value
            val newSongs = current.selectedSongs.toMutableSet()
            val newAlbums = current.selectedAlbums.toMutableSet()

            if (newAlbums.contains(key)) {
                newAlbums.remove(key)
                newSongs.removeAll(songIds)
            } else {
                newAlbums.add(key)
                newSongs.addAll(songIds)
            }

            _selectionState.value = current.copy(
                selectedSongs = newSongs,
                selectedAlbums = newAlbums,
                value = if (newSongs.isNotEmpty()) Selection.MARKED else Selection.DEFAULT
            )
        }
    }

    /**
     * Toggles the selection status of a single song.
     *
     * @param songId The unique ID of the song to toggle.
     */
    fun toggleSongSelection(songId: String) {
        val current = _selectionState.value
        val newSongs = current.selectedSongs.toMutableSet()
        if (newSongs.contains(songId)) {
            newSongs.remove(songId)
        } else {
            newSongs.add(songId)
        }
        _selectionState.value = current.copy(
            selectedSongs = newSongs,
            value = if (newSongs.isNotEmpty()) Selection.MARKED else Selection.DEFAULT,
        )
    }

    /**
     * Toggles the selection status of songs are enqueued
     */
    fun toggleEnqueued() {
        val current = _selectionState.value
        _selectionState.value = current.copy(
            value = Selection.ENQUEUED
        )
    }

    /**
     * Clears all current selections and deactivates selection mode.
     */
    fun clearSelection() {
        _selectionState.value = SelectionState()
    }

    /**
     * Confirms the current selection for removal, resolves selected songs,
     * and removes them from the playing queue.
     *
     * @param onComplete Callback invoked after songs have been removed.
     */
    fun confirmRemoval(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _selectionState.value
            val songIds = state.selectedSongs.toTypedArray()
            val songsToRemove = songRepository.getSongsByIds(*songIds)

            if (songsToRemove.isNotEmpty()) {
                playingQueueRepository.removeSongs(songsToRemove.map { it.toMediaItem() })
            }

            clearSelection()
            onComplete()
        }
    }

    /**
     * Confirms the current selection, resolves all selected entities into
     * individual songs (already resolved in this implementation), and adds them to the playing queue.
     *
     * @param onComplete Callback invoked after songs have been added to the queue. Note that the selection
     * is NOT cleared automatically to allow the user to see their selection when returning to the library.
     */
    fun confirmSelection(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _selectionState.value
            val songsToEnqueue = mutableListOf<Song>()

            // Songs are already resolved in selectedSongs set
            val songIds = state.selectedSongs.toTypedArray()
            songsToEnqueue.addAll(songRepository.getSongsByIds(*songIds))

            if (songsToEnqueue.isNotEmpty()) {
                playingQueueRepository.addSongs(songsToEnqueue.map { it.toMediaItem() })
            }

            clearSelection()
            onComplete()
        }
    }
}

