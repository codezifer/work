package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData(0L)
    val currentPosition: LiveData<Long> = _currentPosition

    val songs = repository.getAllSongs().asLiveData()

    fun playSong(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true

        viewModelScope.launch {
            repository.recordPlay(song.id)
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !(_isPlaying.value ?: false)
    }

    fun updateRating(rating: Int) {
        currentSong.value?.let { song ->
            viewModelScope.launch {
                repository.updateSongRating(song.id, rating)
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            repository.scanMusicLibrary()
        }
    }
}
