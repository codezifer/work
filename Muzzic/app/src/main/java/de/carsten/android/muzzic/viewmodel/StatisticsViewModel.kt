package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.SongPlayCount
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.launch

class StatisticsViewModel(private val repository: MusicRepository) : ViewModel() {
    private val _monthlyStats = MutableLiveData<List<MonthlyPlayCount>>()
    val monthlyStats: LiveData<List<MonthlyPlayCount>> = _monthlyStats

    private val _genreStats = MutableLiveData<List<GenrePlayCount>>()
    val genreStats: LiveData<List<GenrePlayCount>> = _genreStats

    private val _topSongs = MutableLiveData<List<SongPlayCount>>()
    val topSongs: LiveData<List<SongPlayCount>> = _topSongs

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _monthlyStats.value = repository.getMonthlyStats()
            _genreStats.value = repository.getGenreStats()
            _topSongs.value = repository.getTopSongs()
        }
    }
}
