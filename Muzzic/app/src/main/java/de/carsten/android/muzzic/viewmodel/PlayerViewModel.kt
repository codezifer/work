package de.carsten.android.muzzic.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.logger.AndroidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerViewModel(private val repository: MusicRepository, application: Application) :
    AndroidViewModel(application), KoinComponent {
    private val logger: AndroidLogger by inject()

    private val _musicService = MutableStateFlow<MusicPlayerService?>(null)
    val musicService: StateFlow<MusicPlayerService?> = _musicService.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData(0L)
    val currentPosition: LiveData<Long> = _currentPosition

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlayerService.MusicPlayerBinder
            _musicService.value = binder?.getService()
            _isConnected.value = true
            logger.debug("MusicPlayerService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _musicService.value = null
            logger.debug("MusicPlayerService disconnected")
        }

        override fun onBindingDied(name: ComponentName?) {
            onServiceDisconnected(name)
            logger.debug("MusicPlayerService binding died")
        }

        override fun onNullBinding(name: ComponentName?) {
            logger.error("Null binding from MusicPlayerService")
        }
    }

    init {
        bindToMusicService()
    }

    private fun bindToMusicService() {
        val intent = Intent(getApplication(), MusicPlayerService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

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
