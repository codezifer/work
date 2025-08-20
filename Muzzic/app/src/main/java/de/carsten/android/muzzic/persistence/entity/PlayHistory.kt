package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity

@Entity(tableName = "play_history")
data class PlayHistory(
    val songId: String,
    val playedAt: Long = System.currentTimeMillis()
) : AbstractEntity()
