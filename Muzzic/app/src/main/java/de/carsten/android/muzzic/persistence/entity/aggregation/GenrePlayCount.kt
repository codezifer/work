package de.carsten.android.muzzic.persistence.entity.aggregation

data class GenrePlayCount(val genre: String, val count: Int) : Comparable<GenrePlayCount> {

    override fun compareTo(other: GenrePlayCount): Int {
        val c1 = this.count.compareTo(other.count) * -1 // desc. sorting
        return if (c1 != 0) {
            c1
        } else {
            this.genre.compareTo(other.genre)
        }
    }
}
