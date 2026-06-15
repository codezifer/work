package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _monthlyStats = MutableStateFlow<List<MonthlyPlayCount>>(emptyList())
    val monthlyStats = _monthlyStats.asStateFlow()

    private val _genreStats = MutableStateFlow<List<GenrePlayCount>>(emptyList())
    val genreStats = _genreStats.asStateFlow()

    private val _topSongs = MutableStateFlow<List<SongPlayCount>>(emptyList())
    val topSongs = _topSongs.asStateFlow()

    init {
        // Initial load
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _monthlyStats.value = repository.getMonthlyStats()
            _genreStats.value = repository.getGenreStats()
            _topSongs.value = repository.getTopSongs()
        }
    }
}
