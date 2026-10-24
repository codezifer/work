package de.carsten.android.muzzic.persistence.entity.aggregation

data class ArtistPlayCount(val artist: String, val month: String, val playCount: Int) : Comparable<ArtistPlayCount> {
    override fun compareTo(other: ArtistPlayCount): Int {
        var result = artist.compareTo(other.artist)
        return if (result != 0) {
            result
        } else {
            result = month.compareTo(other.month)
            if (result != 0) {
                result
            } else {
                playCount.compareTo(other.playCount)
            }
        }
    }
}
