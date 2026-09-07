package de.carsten.android.muzzic.model

/**
 * Constants for MediaItem metadata extras used across the app.
 */
object MediaKeys {
    // Shared counts and metadata
    const val ALBUM_ART = "album_art"
    const val ALBUM_COUNT = "album_count"
    const val ARTIST_COUNT = "artist_count"
    const val SONG_COUNT = "song_count"
    const val DURATION = "duration"
    const val GENRE = "genre"
    const val IS_AUTO_GENERATED = "is_auto_generated"
    const val PLAYLIST_ID = "playlist_id"

    const val ROOT_ID = "[ROOT]"
    const val ARTISTS_ID = "[ARTISTS]"
    const val ALBUMS_ID = "[ALBUMS]"
    const val SONGS_ID = "[SONGS]"
    const val PLAYLISTS_ID = "[PLAYLISTS]"
    const val GENRES_ID = "[GENRES]"
    const val CURRENT_QUEUE = "CURRENT_QUEUE"
    const val SONG_ID = "songId"

    // Prefix constants for MediaItems
    const val ARTIST_PREFIX = "$ARTISTS_ID:"
    const val ALBUM_PREFIX = "$ALBUMS_ID:"

    const val SONGS_PREFIX = "$SONGS_ID:"
    const val PLAYLIST_PREFIX = "$PLAYLISTS_ID:"
    const val GENRE_PREFIX = "$GENRES_ID:"

    // Song/Media playback specific extras
    const val PLAY_COUNT = "playCount"
    const val LAST_PLAYED = "lastPlayed"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val QUEUE_POSITION = "queuePosition"
}
