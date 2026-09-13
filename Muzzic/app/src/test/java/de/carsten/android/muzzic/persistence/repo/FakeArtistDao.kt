package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistAggregation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [ArtistDao] for repository unit tests.
 *
 * @param songs backing songs.
 */
class FakeArtistDao(private val songs: List<Song>) : ArtistDao {

    /** Variant lists passed to [getArtistAggregationsByGenres], in call order. */
    val genresCalls = mutableListOf<List<String>>()

    override suspend fun getSongsByArtist(artist: String): List<Song> = songs.filter { it.artist == artist }

    override suspend fun searchArtists(query: String): List<ArtistAggregation> = aggregations(songs.filter { it.artist.contains(query, ignoreCase = true) })

    override fun getArtistAggregations(): Flow<List<ArtistAggregation>> = flowOf(aggregations(songs))

    override fun getArtistAggregationsByGenre(genre: String): Flow<List<ArtistAggregation>> = flowOf(aggregations(songs.filter { it.genre == genre }))

    override fun getArtistAggregationsByGenres(genres: List<String>): Flow<List<ArtistAggregation>> {
        genresCalls.add(genres)
        return flowOf(aggregations(songs.filter { it.genre in genres }))
    }

    private fun aggregations(songs: List<Song>): List<ArtistAggregation> = songs
        .groupBy { it.artist }
        .map { (artist, grouped) ->
            ArtistAggregation(
                artistName = artist,
                albumCount = grouped.map { it.album }.distinct().size,
                songCount = grouped.size,
            )
        }.sortedBy { it.artistName }
}
