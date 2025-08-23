package de.carsten.android.muzzic.persistence.entity

import androidx.room.Embedded

data class SongPlayCount(@Embedded val song: Song, val totalCount: Int)
