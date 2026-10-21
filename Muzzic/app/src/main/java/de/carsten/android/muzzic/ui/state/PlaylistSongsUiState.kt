package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.model.SongDto

/**
 * State of the playlist detail screen.
 *
 * @property playlistId id of the playlist shown on the screen.
 * @property playlistName name of the playlist; empty until the playlist is loaded.
 * @property songs songs of the playlist in playlist order.
 * @property totalDurationMs summed duration of all [songs] in milliseconds.
 */
data class PlaylistSongsUiState(val playlistId: String = "", val playlistName: String = "", val songs: List<SongDto> = emptyList(), val totalDurationMs: Long = 0L)
