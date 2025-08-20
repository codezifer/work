package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    val songs = repository.getAllSongs().asLiveData()
    val playlists = repository.getAllPlaylists().asLiveData()

    private val _artists = MutableLiveData<List<String>>()
    val artists: LiveData<List<String>> = _artists

    private val _albums = MutableLiveData<List<Song>>()
    val albums: LiveData<List<Song>> = _albums

    private val _genres = MutableLiveData<List<String>>()
    val genres: LiveData<List<String>> = _genres

    init {
        loadLibraryData()
    }

    private fun loadLibraryData() {
        viewModelScope.launch {
            _artists.value = repository.songDao.getAllArtists()
            _albums.value = repository.songDao.getAllAlbums()
            _genres.value = repository.songDao.getAllGenres()
        }
    }
}
