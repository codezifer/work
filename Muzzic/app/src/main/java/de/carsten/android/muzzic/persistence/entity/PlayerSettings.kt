package de.carsten.android.muzzic.persistence.entity

import androidx.media3.common.Player
import androidx.room.Entity

/**
 * Entity representing the player settings like shuffle and repeat mode.
 */
@Entity(tableName = PlayerSettings.TABLE_NAME)
data class PlayerSettings(val shuffleEnabled: Boolean = false, val repeatMode: Int = Player.REPEAT_MODE_OFF) : AbstractEntity() {
    companion object {
        const val TABLE_NAME = "player_settings"
        const val SETTINGS_ID = "singleton_settings"
    }

    init {
        id = SETTINGS_ID
    }
}
