package de.carsten.android.muzzic

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

object AppConfig {
    object Persistence {
        const val ALBUM_ART_LIMIT = 9
    }

    object Service {
        val PLAYLIST_SYNC_DURATION = 5.minutes
        val PROGRESS_DELAY = 500.milliseconds
    }

    object Ui {
        const val NUM_OF_TOP_SONGS = 5
    }
}
