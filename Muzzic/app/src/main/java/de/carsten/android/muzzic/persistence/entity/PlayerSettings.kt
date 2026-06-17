package de.carsten.android.muzzic.persistence.entity

import androidx.media3.common.Player
import androidx.room.Entity

/**
 * Entity representing the player settings like shuffle and repeat mode.
 */
@Entity(tableName = "player_settings")
data class PlayerSettings(
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
) : AbstractEntity() {
    init {
        id = SETTINGS_ID
    }

    companion object {
        const val SETTINGS_ID = "singleton_settings"
    }
}
