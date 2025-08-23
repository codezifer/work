package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"]
        )
    ],
    indices = [
        Index("songId")
    ]
)
data class PlayHistory(
    val songId: String,
    val playedAt: Long = System.currentTimeMillis()
) : AbstractEntity()
