package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.GenericSetting
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [GenericSetting].
 */
@Dao
interface GenericSettingDao {
    /**
     * Gets a setting by its key.
     */
    @Query("SELECT * FROM generic_settings WHERE settingName = :settingName")
    suspend fun getSetting(settingName: String): GenericSetting?

    /**
     * Observes a setting by its key.
     */
    @Query("SELECT * FROM generic_settings WHERE settingName = :settingName")
    fun observeSetting(settingName: String): Flow<GenericSetting?>

    /**
     * Inserts or updates a setting.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: GenericSetting)

    /**
     * Deletes a setting by its key.
     */
    @Query("DELETE FROM generic_settings WHERE settingName = :settingName")
    suspend fun deleteSetting(settingName: String)
}
