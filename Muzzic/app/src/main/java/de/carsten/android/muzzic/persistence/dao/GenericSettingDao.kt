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
    @Query("SELECT * FROM generic_settings WHERE key = :key")
    suspend fun getSetting(key: String): GenericSetting?

    /**
     * Observes a setting by its key.
     */
    @Query("SELECT * FROM generic_settings WHERE key = :key")
    fun observeSetting(key: String): Flow<GenericSetting?>

    /**
     * Inserts or updates a setting.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: GenericSetting)

    /**
     * Deletes a setting by its key.
     */
    @Query("DELETE FROM generic_settings WHERE key = :key")
    suspend fun deleteSetting(key: String)
}
