package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity

@Entity(tableName = "songs")
data class Song(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val duration: Long, // in milliseconds
    val filePath: String,
    val albumArt: String? = null,
    val rating: Int = 0, // 0-5 stars
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val dateAdded: Long = System.currentTimeMillis()
) : AbstractEntity()
