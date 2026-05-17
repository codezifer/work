package de.carsten.android.muzzic.service

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
import de.carsten.android.muzzic.utils.playbackStateToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusicPlayerService : MediaLibraryService(), KoinComponent {
    private val logger = logger()
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var exoPlayer: ExoPlayer

    private val musicRepository: MusicRepository by inject()
    private val songRepository: SongRepository by inject()
    private val artistRepository: ArtistRepository by inject()
    private val albumRepository: AlbumRepository by inject()
    private val genreRepository: GenreRepository by inject()
    private val playlistRepository: PlaylistRepository by inject()
    private val playingQueueRepository: PlayingQueueRepository by inject()

    private val serviceJob = SupervisorJob()

    // Scope for service-wide tasks, defaulting to Main.immediate to ensure
    // thread-safe interaction with ExoPlayer and atomic StateFlow updates.
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    private var currentPlaylistIndex = -1
    private var currentLastPlaylistIndex = -1

    private var prevPlaybackState: Int = Player.STATE_IDLE
    private var currPlaybackState: Int = Player.STATE_IDLE
    private var playbackStateTransition: PlaybackStateTransition? = null

    private var isCurrentSongCounted = false
    private var playing = false
    private var playlist = emptyList<MediaItem>()
    private var currentSong: MediaItem? = null
    private var currentDuration: Long? = null
    private var currentPosition: Long? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playing = isPlaying
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        currentSong = mediaItem
                        currentDuration = exoPlayer.duration
                        isCurrentSongCounted = false
                        // update current playlist index
                        mediaItem?.let {
                            currentPlaylistIndex = playlist.indexOfFirst { item -> item.mediaId == it.mediaId }
                            currentLastPlaylistIndex = playlist.lastIndex
                        }
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            currentSong = exoPlayer.currentMediaItem
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val oldState = prevPlaybackState
                        val newState = playbackState
                        currPlaybackState = newState
                        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                            currentDuration = exoPlayer.duration
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            // TODO: handle repead mode here
                        }

                        resolvesPlaybackStateTransition(oldState, newState)

                        prevPlaybackState = newState
                    }

                    override fun onTimelineChanged(
                        timeline: Timeline,
                        reason: Int,
                    ) {
                        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                            val mediaItems = mutableListOf<MediaItem>()
                            for (i in 0 until exoPlayer.mediaItemCount) {
                                mediaItems.add(exoPlayer.getMediaItemAt(i))
                            }
                            playlist = mediaItems
                        }
                    }
                },
            )
        }

        // Periodically update current position and check for play count threshold.
        // This loop runs on Dispatchers.Main because ExoPlayer is not thread-safe
        // and must be accessed from the thread it was created on (typically Main).
        serviceScope.launch {
            while (isActive) {
                if (playing) {
                    val currentPos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    currentPosition = currentPos

                    // Check if 50% of the song has been played
                    if (!isCurrentSongCounted && duration > 0 && currentPos >= duration / 2) {
                        isCurrentSongCounted = true
                        currentSong?.mediaId?.let { songId ->
                            // Database operations are offloaded to background threads (handled by Repositories)
                            serviceScope.launch {
                                musicRepository.recordPlay(songId)
                            }
                        }
                    }
                }
                delay(500) // Non-blocking delay (gives the thread back to other tasks)
            }
        }

        // init media session
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
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

        loadPersistedQueue()
        observePlayingQueue()
        logger.info("MusicPlayerService created")
    }

    private fun observePlayingQueue() {
        serviceScope.launch {
            playingQueueRepository.observeEnqueued().collect { dbQueue ->
                syncPlayerWithDb(dbQueue)
            }
        }
    }

    private fun syncPlayerWithDb(dbQueue: List<MediaItem>) {
        val currentPlayerItems = mutableListOf<MediaItem>()
        for (i in 0 until exoPlayer.mediaItemCount) {
            currentPlayerItems.add(exoPlayer.getMediaItemAt(i))
        }

        if (currentPlayerItems.size == dbQueue.size &&
            currentPlayerItems.zip(dbQueue).all { (p, d) -> p.mediaId == d.mediaId }
        ) {
            // Already in sync
            return
        }

        logger.info("Syncing player with database queue (${dbQueue.size} items)")

        // Simple strategy for now: if current song is in the new queue, preserve it
        val currentMediaItem = exoPlayer.currentMediaItem
        val currentPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        val newIndex = if (currentMediaItem != null) {
            dbQueue.indexOfFirst { it.mediaId == currentMediaItem.mediaId }
        } else {
            -1
        }

        if (newIndex != -1) {
            exoPlayer.setMediaItems(dbQueue, newIndex, currentPosition)
        } else {
            exoPlayer.setMediaItems(dbQueue)
        }

        exoPlayer.prepare()
        if (wasPlaying) {
            exoPlayer.play()
        }
    }

    private fun loadPersistedQueue() {
        serviceScope.launch {
            val queue = playingQueueRepository.getPlayingQueue()
            if (queue.isNotEmpty()) {
                setPlaylist(queue)
                logger.info("Restored ${queue.size} items to the playing queue")
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        serviceJob.cancel()
    }

    // --- playlist management ---

    fun setPlaylist(
        newPlaylist: List<MediaItem>,
        startIndex: Int = 0,
    ) {
        playlist = newPlaylist
        if (newPlaylist.isNotEmpty() && 0 <= startIndex && startIndex < newPlaylist.size) {
            currentPlaylistIndex = startIndex
            currentLastPlaylistIndex = newPlaylist.lastIndex
            exoPlayer.setMediaItems(newPlaylist, startIndex, 0L)
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
        } else {
            // clear player if playlist is empty or start index invalid
            exoPlayer.clearMediaItems()
            exoPlayer.stop()
            currentPlaylistIndex = -1
            currentLastPlaylistIndex = -1
            currentSong = null
        }
    }

    fun clearPlaylist() {
        this.setPlaylist(emptyList())
    }

    fun addMediaItemToPlaylist(mediaItem: MediaItem) {
        val currentList = playlist.toMutableList()
        currentList.add(mediaItem)
        playlist = currentList
        exoPlayer.addMediaItem(mediaItem)
    }

    fun playSongFromPlaylist(index: Int) {
        if (index >= 0 && index < playlist.size) {
            currentPlaylistIndex = index
            currentLastPlaylistIndex = playlist.lastIndex
            exoPlayer.seekToDefaultPosition(index) // More robust way to switch within playlist
            exoPlayer.playWhenReady = true // Ensure it plays
            exoPlayer.prepare() // Call prepare if not already prepared or after seek
            exoPlayer.play()
            currentSong = exoPlayer.currentMediaItem // Update current song immediately
        }
    }

    fun playContent(mediaItem: MediaItem) {
        setPlaylist(listOf(mediaItem))
        exoPlayer.play()
    }

    // --- Playback Control Functions ---

    fun pause() = exoPlayer.pause()

    fun play() {
        // If there's a playlist and a valid index, ensure player is ready for that item
        if (exoPlayer.currentMediaItem == null &&
            currentPlaylistIndex != -1 &&
            currentPlaylistIndex < playlist.size
        ) {
            exoPlayer.seekToDefaultPosition(currentPlaylistIndex)
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    fun stop() = exoPlayer.stop()

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            // ExoPlayer's onMediaItemTransition will update currentPlaylistIndex and _currentSong
        } else {
            // Handle end of playlist: stop, loop, etc.
            if (exoPlayer.repeatMode == Player.REPEAT_MODE_OFF) {
                handleSeekToDefault()
            } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
                exoPlayer.seekToDefaultPosition(0)
            }
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            // ExoPlayer's onMediaItemTransition will update currentPlaylistIndex and _currentSong
        } else {
            // Handle beginning of playlist
            if (exoPlayer.repeatMode == Player.REPEAT_MODE_OFF) {
                handleSeekToDefault()
            } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
                exoPlayer.seekTo(currentLastPlaylistIndex, 0L)
            }
        }
    }

    fun changeProgress(progress: Float) {
        // TODO: convert progess 0f..1f to positionMs
    }

    fun isPlaying() = exoPlayer.isPlaying ?: false

    fun getCurrentPosition() = exoPlayer.currentPosition ?: 0L

    fun getDuration() = exoPlayer.duration ?: 0L

    fun seekTo(position: Long) = exoPlayer.seekTo(position)

    fun setRepeatMode(repeatMode: Int) { // Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL
        exoPlayer.repeatMode = repeatMode
    }

    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        exoPlayer.shuffleModeEnabled = shuffleModeEnabled
    }

    // --- private section ---
    private fun handleSeekToDefault() {
        exoPlayer.seekToDefaultPosition(0)
        stop()
    }

    private fun resolvesPlaybackStateTransition(
        oldState: Int,
        newState: Int,
    ) {
        // --- Pattern matching for state transitions ---
        playbackStateTransition = when (Pair(oldState, newState)) {
            Pair(Player.STATE_IDLE, Player.STATE_BUFFERING) -> {
                PlaybackStateTransition.IDLE_BUFFERING
            }

            Pair(Player.STATE_IDLE, Player.STATE_READY) -> {
                // This might happen if media is already buffered/short
                if (exoPlayer.playWhenReady) {
                    PlaybackStateTransition.IDLE_PLAYING // Or a more specific state
                } else {
                    PlaybackStateTransition.IDLE_READY
                }
            }

            Pair(Player.STATE_BUFFERING, Player.STATE_READY) -> {
                if (exoPlayer.playWhenReady && exoPlayer.isPlaying) {
                    // About to start playing or is already playing
                    PlaybackStateTransition.BUFFERING_PLAYING
                } else {
                    // Just became ready, but not necessarily playing yet
                    PlaybackStateTransition.BUFFERING_READY
                }
            }

            Pair(Player.STATE_BUFFERING, Player.STATE_IDLE) -> {
                // e.g. error during buffering or stop() called
                PlaybackStateTransition.BUFFERING_IDLE
            }

            Pair(Player.STATE_READY, Player.STATE_ENDED) -> {
                PlaybackStateTransition.PLAYING_ENDED // Assuming it was playing before ending
            }

            Pair(Player.STATE_READY, Player.STATE_BUFFERING) -> {
                // e.g., rebuffering during playback
                PlaybackStateTransition.PLAYING_BUFFERING // Or PAUSED_BUFFERING
            }

            Pair(Player.STATE_READY, Player.STATE_IDLE) -> {
                // Player was stopped while in ready state
                PlaybackStateTransition.READY_IDLE // Could be from playing or paused
            }

            Pair(Player.STATE_ENDED, Player.STATE_BUFFERING) -> {
                // e.g., preparing next item after current one ended
                PlaybackStateTransition.ENDED_BUFFERING
            }

            Pair(Player.STATE_ENDED, Player.STATE_IDLE) -> {
                // e.g. playlist finished and player stopped
                PlaybackStateTransition.ENDED_IDLE
            }

            // Add more specific transitions as needed
            // Case for when state doesn't actually change (e.g. READY -> READY)
            // This is important if you want to also consider isPlaying changes
            Pair(Player.STATE_READY, Player.STATE_READY) -> {
                // The playback state itself (IDLE, BUFFERING, READY, ENDED) hasn't changed.
                // However, the 'isPlaying' status might have.
                // This is better handled by onIsPlayingChanged, but if you want one central place:
                val wasPlaying = playing // Value before onIsPlayingChanged updates it
                val isNowPlaying = exoPlayer.isPlaying ?: false // Current actual status
                if (!wasPlaying && isNowPlaying) {
                    println("Transition: PAUSED (READY) -> PLAYING (READY)")
                    PlaybackStateTransition.PAUSED_PLAYING
                } else if (wasPlaying && !isNowPlaying) {
                    println("Transition: PLAYING (READY) -> PAUSED (READY)")
                    PlaybackStateTransition.PLAYING_PAUSED
                } else {
                    // No change in playing status while READY
                    PlaybackStateTransition.READY_READY
                }
            }

            else -> {
                logger.warning(
                    "Unknown or Unhandled Transition: ${playbackStateToString(oldState)} -> ${
                        playbackStateToString(
                            newState,
                        )
                    }",
                )
                // Determine a default or current state transition
                if (oldState == newState) {
                    when (newState) {
                        Player.STATE_IDLE -> {
                            PlaybackStateTransition.IDLE_IDLE
                        }

                        Player.STATE_BUFFERING -> {
                            PlaybackStateTransition.BUFFERING_BUFFERING
                        }

                        Player.STATE_READY -> {
                            if (exoPlayer.isPlaying) {
                                PlaybackStateTransition.PLAYING_PLAYING
                            } else {
                                PlaybackStateTransition.PAUSED_PAUSED
                            }
                        }

                        Player.STATE_ENDED -> {
                            PlaybackStateTransition.ENDED_ENDED
                        }

                        else -> {
                            PlaybackStateTransition.UNKNOWN
                        }
                    }
                } else {
                    PlaybackStateTransition.UNKNOWN // Or a more specific default based on newState
                }
            }
        }
    }
}
