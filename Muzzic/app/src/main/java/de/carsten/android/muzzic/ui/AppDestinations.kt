package de.carsten.android.muzzic.ui

object AppDestinations {
    const val PLAYER = "player"
    const val LIBRARY_GRAPH = "library_graph"
    const val LIBRARY = "library"
    const val QUEUE = "queue"
    const val PLAYLISTS = "playlists"
    const val STATISTICS = "statistics"

    const val ARTIST_ALBUMS = "artist/{artistName}/albums"
    const val ALBUM_SONGS = "album/{artistName}/{albumName}/songs"

    // Legacy constants if needed
    const val ARTIST = "artist"
    const val ALBUM = "album"
    const val SONG = "song"
    const val GENRE = "genre"
    const val PLAYLIST = "playlist"

    // For navigation helper
    fun artistAlbums(artistName: String) = "artist/$artistName/albums"
    fun albumSongs(artistName: String, albumName: String) = "album/$artistName/$albumName/songs"
}
