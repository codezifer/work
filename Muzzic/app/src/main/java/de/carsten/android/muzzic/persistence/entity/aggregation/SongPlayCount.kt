package de.carsten.android.muzzic.persistence.entity.aggregation

import androidx.room.Embedded
import de.carsten.android.muzzic.persistence.entity.Song

data class SongPlayCount(@Embedded val song: Song, val totalCount: Int) : Comparable<SongPlayCount> {

    override fun compareTo(other: SongPlayCount): Int {
        val c1 = this.song.rating.compareTo(other.song.rating) * -1 // desc. sorting
        return if (c1 != 0) {
            c1
        } else {
            val c2 = this.totalCount.compareTo(other.totalCount) * -1 // desc. sorting
            if (c2 != 0) {
                c2
            } else {
                this.song.compareTo(other.song)
            }
        }
    }
}
