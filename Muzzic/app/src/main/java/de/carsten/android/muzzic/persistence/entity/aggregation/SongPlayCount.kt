package de.carsten.android.muzzic.persistence.entity.aggregation

import androidx.room.Embedded
import de.carsten.android.muzzic.persistence.entity.Song

data class SongPlayCount(@Embedded val song: Song, val totalCount: Int)
