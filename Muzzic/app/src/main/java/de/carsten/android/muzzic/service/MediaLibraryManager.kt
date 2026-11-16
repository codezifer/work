package de.carsten.android.muzzic.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class MediaLibraryManager(context: Context) {
    private val _browser = MutableStateFlow<MediaBrowser?>(null)
    val browser: StateFlow<MediaBrowser?> = _browser.asStateFlow()

    init {
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, MusicPlayerService::class.java),
            )
        val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        browserFuture.addListener(
            {
                _browser.value = browserFuture.get()
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun playContent(mediaItem: MediaItem) {
        val browser = _browser.value ?: return
        browser.setMediaItem(mediaItem)
        browser.prepare()
        browser.play()
    }

    fun playPlaylist(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        val browser = _browser.value ?: return
        browser.setMediaItems(mediaItems, startIndex, 0L)
        browser.prepare()
        browser.play()
    }

    fun clearPlaylist() {
        val browser = _browser.value ?: return
        browser.setMediaItems(emptyList())
        browser.prepare()
    }
}
