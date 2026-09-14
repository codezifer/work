package de.carsten.android.muzzic

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

object AppConfig {
    object Persistence {
        const val ALBUM_ART_LIMIT = 9
        const val DATABASE_NAME = "muzzic.db"
        const val NUM_TOP_SONGS = 100
    }

    object Service {
        val PLAYLIST_SYNC_DURATION = 5.minutes
        val PROGRESS_DELAY_MS = 500.milliseconds
    }

    object Ui {
        const val NUM_OF_TOP_SONGS = 10
        const val NUM_OF_TOP_GENRES = 10
        const val NUM_OF_MONTHS = 6
        const val ALBUM_ART_FADE_IN_OUT = 500
    }

    object Scanning {
        const val CHUNK_SIZE = 50
        const val MAX_PROGRESS = 100
        const val INDETERMINATE_PROGRESS = false
        val SUPPORTED_MUSIC_FILES = setOf("mp3")
        val SUPPORTED_PLAYLIST_FILES = setOf("m3u", "m3u8")
    }
}
