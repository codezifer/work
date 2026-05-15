package de.carsten.android.muzzic.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.toAlbumDto
import de.carsten.android.muzzic.ui.model.toArtistDto
import de.carsten.android.muzzic.ui.model.toGenreDto
import de.carsten.android.muzzic.ui.model.toPlaylistDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import org.koin.core.component.KoinComponent

@UnstableApi
class MediaLibraryManager(private val context: Context) : KoinComponent {
    private val _browser = MutableStateFlow<MediaBrowser?>(null)
    val browser: StateFlow<MediaBrowser?> = _browser.asStateFlow()

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        browserFuture.addListener({
            _browser.value = browserFuture.get()
        }, MoreExecutors.directExecutor())
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        val browser = _browser.value ?: return emptyList()
        val result = browser.getChildren(parentId, 0, Int.MAX_VALUE, null).await()
        return result.value ?: emptyList()
    }

    suspend fun getArtists(): List<ArtistDto> =
        getChildren(MusicPlayerServiceCallback.ARTISTS_ID).map { it.toArtistDto() }

    suspend fun getAlbums(): List<AlbumDto> =
        getChildren(MusicPlayerServiceCallback.ALBUMS_ID).map { it.toAlbumDto() }

    suspend fun getSongs(): List<MediaItem> =
        getChildren(MusicPlayerServiceCallback.SONGS_ID)

    suspend fun getGenres(): List<GenreDto> =
        getChildren(MusicPlayerServiceCallback.GENRES_ID).map { it.toGenreDto() }

    suspend fun getPlaylists(): List<PlaylistDto> =
        getChildren(MusicPlayerServiceCallback.PLAYLISTS_ID).map { it.toPlaylistDto() }

    suspend fun getAlbumsByArtist(artistName: String): List<AlbumDto> =
        getChildren("${MediaKeys.ARTIST_PREFIX}$artistName").map { it.toAlbumDto() }

    suspend fun getSongsByAlbum(artistName: String, albumName: String): List<MediaItem> =
        getChildren("${MediaKeys.ALBUM_PREFIX}$artistName:$albumName")

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

    fun preparePlaylist(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        val browser = _browser.value ?: return
        browser.setMediaItems(mediaItems, startIndex, 0L)
        browser.prepare()
    }
}
