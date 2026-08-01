package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * A single playback event, stored for accurate per-month statistics.
 *
 * @property songId The id of the song that was played.
 * @property playedAt The timestamp (epoch millis) of the playback.
 */
@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("songId"),
    ],
)
data class PlayHistory(val songId: String, val playedAt: Long = System.currentTimeMillis()) : AbstractEntity()
