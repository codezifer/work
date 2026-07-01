package de.carsten.android.muzzic.ui

object AppDestinations {
    const val PLAYER = "player"
    const val LIBRARY_GRAPH = "library_graph"
    const val LIBRARY = "library"
    const val QUEUE = "queue"
    const val PLAYLISTS = "playlists"

    const val GENRES = "genres"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    const val ARTIST_ARGUMENT = "artistName"
    const val ARTIST_ALBUMS = "artist/{$ARTIST_ARGUMENT}/albums"

    const val ALBUM_ARGUMENT = "albumName"
    const val ALBUM_SONGS = "album/{$ARTIST_ARGUMENT}/{$ALBUM_ARGUMENT}/songs"

    const val GENRE_ARGUMENT = "genreName"
    const val GENRE_ARTISTS = "genre/{$GENRE_ARGUMENT}/artists"

    // Legacy constants if needed
    const val ARTIST = "artist"
    const val ALBUM = "album"
    const val SONG = "song"
    const val GENRE = "genre"
    const val PLAYLIST = "playlist"

    // For navigation helper
    fun artistAlbums(artistName: String) = "artist/$artistName/albums"

    fun albumSongs(artistName: String, albumName: String) = "album/$artistName/$albumName/songs"

    fun genreArtists(genreName: String) = "genre/$genreName/artists"
}
