package de.carsten.android.muzzic.model

/**
 * Constants for MediaItem metadata extras used across the app.
 */
object MediaKeys {
    // Shared counts and metadata
    const val ALBUM_COUNT = "album_count"
    const val ARTIST_COUNT = "artist_count"
    const val SONG_COUNT = "song_count"
    const val DURATION = "duration"
    const val GENRE = "genre"
    const val IS_AUTO_GENERATED = "is_auto_generated"
    const val PLAYLIST_ID = "playlist_id"

    // Prefix constants for MediaItems
    const val ARTIST_PREFIX = "[ARTIST]:"
    const val ALBUM_PREFIX = "[ALBUM]:"
    const val PLAYLIST_PREFIX = "[PLAYLIST]:"
    const val GENRE_PREFIX = "[GENRE]:"

    // Song/Media playback specific extras
    const val PLAY_COUNT = "playCount"
    const val LAST_PLAYED = "lastPlayed"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
}
