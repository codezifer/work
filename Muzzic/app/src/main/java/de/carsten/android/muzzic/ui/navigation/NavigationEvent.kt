package de.carsten.android.muzzic.ui.navigation

/**
 * Sealed class representing all navigation events in the app.
 * This allows for a type-safe and centralized way to handle navigation actions.
 */
sealed class NavigationEvent {
    /**
     * Navigates to the Player screen.
     */
    data object ToPlayer : NavigationEvent()

    /**
     * Navigates to the Library screen.
     */
    data object ToLibrary : NavigationEvent()

    /**
     * Navigates to the Playing Queue screen.
     */
    data object ToQueue : NavigationEvent()

    /**
     * Navigates to the Playlists screen.
     */
    data object ToPlaylists : NavigationEvent()

    /**
     * Navigates to the Statistics screen.
     */
    data object ToStatistics : NavigationEvent()

    /**
     * Navigates to the Settings screen.
     */
    data object ToSettings : NavigationEvent()

    /**
     * Navigates to the Genres screen.
     */
    data object ToGenres : NavigationEvent()

    /**
     * Navigates back in the backstack.
     */
    data object Back : NavigationEvent()

    /**
     * Navigates to the Artist Albums screen.
     * @property artistName The name of the artist.
     */
    data class ToArtistAlbums(val artistName: String) : NavigationEvent()

    /**
     * Navigates to the Album Songs screen.
     * @property artistName The name of the artist.
     * @property albumName The name of the album.
     */
    data class ToAlbumSongs(val artistName: String, val albumName: String) : NavigationEvent()

    /**
     * Navigates to the Genre Artists screen.
     * @property genreName The name of the genre.
     */
    data class ToGenreArtists(val genreName: String) : NavigationEvent()
}
