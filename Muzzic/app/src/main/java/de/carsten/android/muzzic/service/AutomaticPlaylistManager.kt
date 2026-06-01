package de.carsten.android.muzzic.service

import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages the automatic generation and updating of playlists.
 *
 * This manager is responsible for keeping "Top 100" and other auto-generated
 * playlists up to date while the app is running.
 */
class AutomaticPlaylistManager(private val musicRepository: MusicRepository) {
    private val logger = logger()

    /**
     * Starts the periodic update loop.
     *
     * @param scope The scope in which to run the update loop.
     */
    fun startMonitoring(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                try {
                    musicRepository.updateAutomaticPlaylists()
                    logger.info("Automatic playlists refreshed periodically")
                } catch (e: Exception) {
                    logger.error("Failed to refresh automatic playlists periodically", e)
                }
                // Refresh every 1 seconds
                delay(1.seconds)
            }
        }
    }

    /**
     * Triggers an immediate update of automatic playlists.
     */
    suspend fun triggerUpdate() {
        try {
            musicRepository.updateAutomaticPlaylists()
            logger.info("Automatic playlists refreshed on trigger")
        } catch (e: Exception) {
            logger.error("Failed to refresh automatic playlists on trigger", e)
        }
    }
}
