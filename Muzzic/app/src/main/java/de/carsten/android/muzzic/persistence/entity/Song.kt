package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity

@Entity(tableName = "songs")
data class Song(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val duration: Long? = null, // in milliseconds
    val filePath: String? = null,
    val albumArt: String? = null,
    val rating: Int? = 0, // 0-5 stars
    val playCount: Int? = 0,
    val lastPlayed: Long? = 0,
    val dateAdded: Long? = System.currentTimeMillis()
) : AbstractEntity()
