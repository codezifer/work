package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenericSettingDao
import de.carsten.android.muzzic.persistence.entity.GenericSetting
import de.carsten.android.muzzic.visualization.component.VisualizerColorSource
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing application settings using [GenericSetting].
 */
class AppSettingsRepository(private val genericSettingDao: GenericSettingDao) {

    companion object {
        const val KEY_MUSIC_DIRECTORY = "music_directory"
        const val KEY_PLAYLIST_DIRECTORY = "playlist_directory"
        const val KEY_VISUALIZER_ENGINE = "visualizer_engine"
        const val KEY_PROJECTM_PRESET = "projectm_preset"
        const val KEY_BARS_SHIMMER_ENABLED = "bars_shimmer_enabled"
        const val KEY_BARS_TIP_GLOW_ENABLED = "bars_tip_glow_enabled"
        const val KEY_VISUALIZER_COLOR_SOURCE = "visualizer_color_source"
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

    /**
     * Observes the active visualizer engine setting (defaults to [VisualizerEngine.BARS]).
     */
    fun observeVisualizerEngine(): Flow<VisualizerEngine> = genericSettingDao.observeSetting(KEY_VISUALIZER_ENGINE).map { setting ->
        setting?.value?.let { name ->
            runCatching { VisualizerEngine.valueOf(name) }.getOrNull()
        } ?: VisualizerEngine.BARS
    }

    /**
     * Gets the active visualizer engine setting.
     */
    suspend fun getVisualizerEngine(): VisualizerEngine {
        val value = genericSettingDao.getSetting(KEY_VISUALIZER_ENGINE)?.value ?: return VisualizerEngine.BARS
        return runCatching { VisualizerEngine.valueOf(value) }.getOrDefault(VisualizerEngine.BARS)
    }

    /**
     * Saves the active visualizer engine setting.
     */
    suspend fun saveVisualizerEngine(engine: VisualizerEngine) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_VISUALIZER_ENGINE, engine.name))
    }

    /**
     * Observes the selected ProjectM preset name.
     */
    fun observeProjectMPreset(): Flow<String?> = genericSettingDao.observeSetting(KEY_PROJECTM_PRESET).map { it?.value }

    /**
     * Gets the selected ProjectM preset name.
     */
    suspend fun getProjectMPreset(): String? = genericSettingDao.getSetting(KEY_PROJECTM_PRESET)?.value

    /**
     * Saves the selected ProjectM preset name.
     */
    suspend fun saveProjectMPreset(presetName: String) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_PROJECTM_PRESET, presetName))
    }

    /**
     * Observes the mirrored-BARS shimmer effect toggle (defaults to true).
     */
    fun observeBarsShimmerEnabled(): Flow<Boolean> = genericSettingDao.observeSetting(KEY_BARS_SHIMMER_ENABLED).map { setting ->
        setting?.value?.toBooleanStrictOrNull() ?: true
    }

    /**
     * Saves the mirrored-BARS shimmer effect toggle.
     */
    suspend fun saveBarsShimmerEnabled(enabled: Boolean) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_BARS_SHIMMER_ENABLED, enabled.toString()))
    }

    /**
     * Observes the mirrored-BARS tip-glow effect toggle (defaults to true).
     */
    fun observeBarsTipGlowEnabled(): Flow<Boolean> = genericSettingDao.observeSetting(KEY_BARS_TIP_GLOW_ENABLED).map { setting ->
        setting?.value?.toBooleanStrictOrNull() ?: true
    }

    /**
     * Saves the mirrored-BARS tip-glow effect toggle.
     */
    suspend fun saveBarsTipGlowEnabled(enabled: Boolean) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_BARS_TIP_GLOW_ENABLED, enabled.toString()))
    }

    /**
     * Observes the visualizer color source setting (defaults to [VisualizerColorSource.ALBUM_ART]).
     */
    fun observeVisualizerColorSource(): Flow<VisualizerColorSource> = genericSettingDao.observeSetting(KEY_VISUALIZER_COLOR_SOURCE).map { setting ->
        setting?.value?.let { name ->
            runCatching { VisualizerColorSource.valueOf(name) }.getOrNull()
        } ?: VisualizerColorSource.ALBUM_ART
    }

    /**
     * Gets the visualizer color source setting.
     */
    suspend fun getVisualizerColorSource(): VisualizerColorSource {
        val value = genericSettingDao.getSetting(KEY_VISUALIZER_COLOR_SOURCE)?.value ?: return VisualizerColorSource.ALBUM_ART
        return runCatching { VisualizerColorSource.valueOf(value) }.getOrDefault(VisualizerColorSource.ALBUM_ART)
    }

    /**
     * Saves the visualizer color source setting.
     */
    suspend fun saveVisualizerColorSource(colorSource: VisualizerColorSource) {
        genericSettingDao.insertOrUpdate(GenericSetting(KEY_VISUALIZER_COLOR_SOURCE, colorSource.name))
    }
}
