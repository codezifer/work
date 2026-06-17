package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.PlayerSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerSettingsDao {
    @Query("SELECT * FROM player_settings WHERE id = :id")
    suspend fun getSettings(id: String = PlayerSettings.SETTINGS_ID): PlayerSettings?

    @Query("SELECT * FROM player_settings WHERE id = :id")
    fun observeSettings(id: String = PlayerSettings.SETTINGS_ID): Flow<PlayerSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PlayerSettings)
}
