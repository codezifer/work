package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount

data class StatisticsUiState(
    val monthlyStats: List<MonthlyPlayCount> = emptyList(),
    val genreStats: List<GenrePlayCount> = emptyList(),
    val topSongs: List<SongPlayCount> = emptyList(),
    val monthStats: List<MonthlyPlayCount> = emptyList(),
    val monthGenreStats: List<GenrePlayCount> = emptyList(),
    val topMonthSongs: List<SongPlayCount> = emptyList(),
)
