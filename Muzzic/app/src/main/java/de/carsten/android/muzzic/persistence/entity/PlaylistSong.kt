package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"]),
        ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["songId"])
    ],
    indices = [
        Index("playlistId"),
        Index("songId")
    ]
)
data class PlaylistSong(
    val playlistId: String,
    val songId: String,
    val position: Int = 0
)
