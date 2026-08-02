package de.carsten.android.muzzic.service

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.persistence.repo.SettingsRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(UnstableApi::class)
class MusicPlayerService :
    MediaLibraryService(),
    KoinComponent {
    private val logger = logger()
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val playbackManager: PlaybackManager by inject()
    private val queueManager: QueueManager by inject()
    private val analytics: PlaybackAnalytics by inject()
    private val stateManager: PlaybackStateManager by inject()
    private val playlistManager: AutomaticPlaylistManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val bitmapLoader: BitmapLoader by inject()

    // Repositories for the Callback (consider moving these as well if possible)
    private val musicRepository: MusicRepository by inject()
    private val songRepository: SongRepository by inject()
    private val artistRepository: ArtistRepository by inject()
    private val albumRepository: AlbumRepository by inject()
    private val genreRepository: GenreRepository by inject()
    private val playlistRepository: PlaylistRepository by inject()
    private val playingQueueRepository: PlayingQueueRepository by inject()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    private var prevPlaybackState: Int = Player.STATE_IDLE
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()

        setupPlayerListeners()
        analytics.startMonitoring(serviceScope, playbackManager.exoPlayer)

        mediaLibrarySession =
            MediaLibrarySession
                .Builder(
                    this,
                    playbackManager.exoPlayer,
                    MusicPlayerServiceCallback(
                        serviceScope,
                        musicRepository,
                        songRepository,
                        artistRepository,
                        albumRepository,
                        genreRepository,
                        playlistRepository,
                        playingQueueRepository,
                    ),
                )
                .setId("MuzzicPlayerSession")
                .setBitmapLoader(bitmapLoader)
                .build()

        queueManager.loadPersistedQueue(serviceScope, playbackManager.exoPlayer)
        loadPlayerSettings()
        queueManager.observeQueueChanges(serviceScope, playbackManager.exoPlayer)
        playlistManager.startMonitoring(serviceScope)
        logger.info("MusicPlayerService created")
    }

    private fun setupPlayerListeners() {
        playbackManager.exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    this@MusicPlayerService.isPlaying = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    analytics.onNewSong()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    stateManager.resolveTransition(
                        playbackManager.exoPlayer,
                        prevPlaybackState,
                        playbackState,
                        isPlaying,
                    )
                    prevPlaybackState = playbackState
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    serviceScope.launch {
                        settingsRepository.saveShuffleMode(shuffleModeEnabled)
                    }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    serviceScope.launch {
                        settingsRepository.saveRepeatMode(repeatMode)
                    }
                }
            },
        )
    }

    private fun loadPlayerSettings() {
        serviceScope.launch {
            val settings = settingsRepository.getSettings()
            playbackManager.exoPlayer.shuffleModeEnabled = settings.shuffleEnabled
            playbackManager.exoPlayer.repeatMode = settings.repeatMode
            logger.info("Restored player settings: shuffle=${settings.shuffleEnabled}, repeat=${settings.repeatMode}")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        logger.info("MusicPlayerService being destroyed")
        if (::mediaLibrarySession.isInitialized) {
            mediaLibrarySession.release()
        }
        serviceJob.cancel()
        playbackManager.release()
        super.onDestroy()
    }
}
