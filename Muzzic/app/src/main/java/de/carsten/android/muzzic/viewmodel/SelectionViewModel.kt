package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
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
     * Tracks songs that were enqueued during the current selection session.
     * This allows for rolling back the enqueue operation if the user cancels.
     */
    private val recentlyEnqueuedSongs = mutableListOf<MediaItem>()

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
        recentlyEnqueuedSongs.clear()
    }

    /**
     * Rolls back the songs that were enqueued during the current selection session.
     * This is triggered when the user clicks the "x" (cancel) button in the selection toolbar.
     */
    fun rollbackEnqueued(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            if (recentlyEnqueuedSongs.isNotEmpty()) {
                playingQueueRepository.removeSongs(recentlyEnqueuedSongs.toList())
                recentlyEnqueuedSongs.clear()
                onComplete("Queue rollback complete")
            } else {
                onComplete("Selection cleared")
            }
            clearSelection()
        }
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
            val songIds = state.selectedSongs.toList()

            if (songIds.isNotEmpty()) {
                val mediaItemsToRemove = songIds.map { id ->
                    MediaItem.Builder().setMediaId(id).build()
                }
                playingQueueRepository.removeSongs(mediaItemsToRemove)
            }

            clearSelection()
            onComplete()
        }
    }

    /**
     * Confirms the current selection, resolves all selected entities into
     * individual songs (already resolved in this implementation), and adds them to the playing queue.
     *
     * @param onComplete Callback invoked after songs have been added to the queue.
     */
    fun confirmSelection(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val state = _selectionState.value
            val songsToEnqueue = mutableListOf<Song>()

            // Songs are already resolved in selectedSongs set
            val songIds = state.selectedSongs.toTypedArray()
            songsToEnqueue.addAll(songRepository.getSongsByIds(*songIds))

            if (songsToEnqueue.isNotEmpty()) {
                val mediaItems = songsToEnqueue.map { it.toMediaItem() }
                val addedEntities = playingQueueRepository.addSongs(mediaItems, enqueued = true)
                recentlyEnqueuedSongs.addAll(addedEntities.map { it.toMediaItem() })
                toggleEnqueued()
                onComplete("Added ${songsToEnqueue.size} songs to queue")
            } else {
                onComplete("No songs selected")
            }
        }
    }
}

