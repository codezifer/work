package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playing_queue",
    foreignKeys = [
        ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["songId"])
    ]
)
data class PlayingQueue(
    val songId: String? = null
) : AbstractEntity()
