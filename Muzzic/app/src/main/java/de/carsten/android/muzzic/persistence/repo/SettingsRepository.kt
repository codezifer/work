package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.PlayerSettingsDao
import de.carsten.android.muzzic.persistence.entity.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val playerSettingsDao: PlayerSettingsDao) {

    fun observeSettings(): Flow<PlayerSettings> = playerSettingsDao.observeSettings()
        .map { it ?: PlayerSettings() }

    suspend fun getSettings(): PlayerSettings = playerSettingsDao.getSettings() ?: PlayerSettings()

    suspend fun saveShuffleMode(enabled: Boolean) {
        val current = getSettings()
        playerSettingsDao.insertOrUpdate(current.copy(shuffleEnabled = enabled))
    }

    suspend fun saveRepeatMode(repeatMode: Int) {
        val current = getSettings()
        playerSettingsDao.insertOrUpdate(current.copy(repeatMode = repeatMode))
    }
}
