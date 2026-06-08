package de.carsten.android.muzzic.persistence.entity.aggregation

data class MonthlyPlayCount(val month: String, val count: Int) : Comparable<MonthlyPlayCount> {

    override fun compareTo(other: MonthlyPlayCount): Int {
        val c1 = this.month.compareTo(other.month)
        return if (c1 != 0) {
            c1
        } else {
            this.count.compareTo(other.count) * -1 // desc. sorting
        }
    }
}
