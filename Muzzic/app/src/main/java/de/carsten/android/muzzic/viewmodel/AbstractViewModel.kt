package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager

@UnstableApi
abstract class AbstractViewModel(private val playingQueueRepository: PlayingQueueRepository, private val mediaLibraryManager: MediaLibraryManager) : ViewModel() {

    protected suspend fun enqueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val mediaItems = songs.mapIndexed { index, song -> song.toMediaItem(index) }
        playingQueueRepository.clear()
        playingQueueRepository.addSongs(mediaItems)
        mediaLibraryManager.playPlaylist(mediaItems, 0)
    }
}
