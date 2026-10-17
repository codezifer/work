package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = PlaylistSong.TABLE_NAME,
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("playlistId"),
        Index("songId"),
    ],
)
data class PlaylistSong(val playlistId: String, val songId: String, val position: Int = 0) : AbstractTimestampEntity() {
    companion object {
        const val TABLE_NAME = "playlist_songs"
    }
}
