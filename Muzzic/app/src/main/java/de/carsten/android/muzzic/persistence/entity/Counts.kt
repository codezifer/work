package de.carsten.android.muzzic.persistence.entity

import androidx.room.Embedded

data class MonthlyPlayCount(val month: String, val count: Int)
data class GenrePlayCount(val genre: String, val count: Int)
data class SongPlayCount(@Embedded val song: Song, val totalCount: Int)
