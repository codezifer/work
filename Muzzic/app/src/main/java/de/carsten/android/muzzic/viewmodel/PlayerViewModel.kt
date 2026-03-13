package de.carsten.android.muzzic.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MusicPlayerService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

@OptIn(ExperimentalCoroutinesApi::class)
open class PlayerViewModel(
    private val repository: MusicRepository,
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {
    private val logger = this.logger()

    private val _musicService = MutableStateFlow<MusicPlayerService?>(null)
    val musicService: StateFlow<MusicPlayerService?> = _musicService.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    val isPlaying: StateFlow<Boolean> =
        musicService
            .flatMapLatest { service ->
                service?.isPlayingFlow ?: flowOf(false)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    val currentPosition: StateFlow<Long> =
        musicService
            .flatMapLatest { service ->
                service?.currentPositionFlow ?: flowOf(0L)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0L,
            )

    val duration: StateFlow<Long> =
        musicService
            .flatMapLatest { service ->
                service?.durationFlow ?: flowOf(0L)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0L,
            )

    val progress: StateFlow<Float> =
        musicService
            .flatMapLatest { service ->
                service?.let { s ->
                    s.currentPositionFlow.map { pos ->
                        val dur = s.durationFlow.value
                        if (dur > 0) pos.toFloat() / dur else 0f
                    }
                } ?: flowOf(0f)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0f,
            )

    val songs =
        repository.getAllSongs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                val binder = service as? MusicPlayerService.MusicPlayerBinder
                _musicService.value = binder?.getService()
                _isConnected.value = true
                logger.debug("MusicPlayerService connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                _musicService.value = null
                _isConnected.value = false
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
        scanLibrary()
    }

    private fun bindToMusicService() {
        val intent = Intent(getApplication(), MusicPlayerService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        viewModelScope.launch {
            repository.recordPlay(song.id)
            // Ideally we'd also tell the service to play here if not done elsewhere
        }
    }

    fun togglePlayPause() {
        val service = _musicService.value ?: return
        if (service.isPlaying()) {
            service.pause()
        } else {
            service.play()
        }
    }

    fun onPrevClicked() {
        _musicService.value?.previous()
    }

    fun onNextClicked() {
        _musicService.value?.next()
    }

    fun onProgressChanged(progress: Float) {
        _musicService.value?.changeProgress(progress)
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

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
