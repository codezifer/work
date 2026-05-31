package de.carsten.android.muzzic.service

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import de.carsten.android.muzzic.persistence.repo.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusicPlayerService : MediaLibraryService(), KoinComponent {
    private val logger = logger()
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val playbackManager: PlaybackManager by inject()
    private val queueManager: QueueManager by inject()
    private val analytics: PlaybackAnalytics by inject()
    private val stateManager: PlaybackStateManager by inject()
    private val playlistManager: AutomaticPlaylistManager by inject()

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

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        setupPlayerListeners()
        analytics.startMonitoring(serviceScope, playbackManager.exoPlayer)

        mediaLibrarySession = MediaLibrarySession.Builder(
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
                playingQueueRepository
            )
        ).build()

        queueManager.loadPersistedQueue(serviceScope, playbackManager.exoPlayer)
        queueManager.observeQueueChanges(serviceScope, playbackManager.exoPlayer)
        playlistManager.startMonitoring(serviceScope)
        logger.info("MusicPlayerService created")
    }

    private fun setupPlayerListeners() {
        playbackManager.exoPlayer.addListener(object : Player.Listener {
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
                    isPlaying
                )
                prevPlaybackState = playbackState
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
        serviceJob.cancel()
    }
}
