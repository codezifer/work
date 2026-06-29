package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenericSettingDao
import de.carsten.android.muzzic.persistence.entity.GenericSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing application settings using [GenericSetting].
 */
class AppSettingsRepository(private val genericSettingDao: GenericSettingDao) {

    companion object {
        const val KEY_MUSIC_DIRECTORY = "music_directory"
        const val KEY_PLAYLIST_DIRECTORY = "playlist_directory"
    }

    /**
     * Observes the music directory setting.
     */
    fun observeMusicDirectory(): Flow<String?> = genericSettingDao.observeSetting(KEY_MUSIC_DIRECTORY).map { it?.value }

    /**
     * Gets the music directory setting.
     */
    suspend fun getMusicDirectory(): String? = genericSettingDao.getSetting(KEY_MUSIC_DIRECTORY)?.value

    /**
     * Saves the music directory setting.
     */
    suspend fun saveMusicDirectory(path: String) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_MUSIC_DIRECTORY, path))
    }

    /**
     * Observes the playlist directory setting.
     */
    fun observePlaylistDirectory(): Flow<String?> = genericSettingDao.observeSetting(KEY_PLAYLIST_DIRECTORY).map { it?.value }

    /**
     * Gets the playlist directory setting.
     */
    suspend fun getPlaylistDirectory(): String? = genericSettingDao.getSetting(KEY_PLAYLIST_DIRECTORY)?.value

    /**
     * Saves the playlist directory setting.
     */
    suspend fun savePlaylistDirectory(path: String) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_PLAYLIST_DIRECTORY, path))
    }
}
