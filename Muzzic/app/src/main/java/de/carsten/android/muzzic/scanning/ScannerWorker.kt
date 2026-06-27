package de.carsten.android.muzzic.scanning

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.logging.logger
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * Generic [CoroutineWorker] that executes a [FileScanner] based on the provided scanner ID.
 */
class ScannerWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    companion object {
        const val KEY_SCANNER_ID = "scanner_id"
        private const val CHANNEL_ID = "scanning_channel"
        private const val NOTIFICATION_ID = 1002
        private val logger = logger()
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val scannerId = inputData.getString(KEY_SCANNER_ID) ?: return Result.failure()
        val scanner: FileScanner by inject(named(scannerId))

        return try {
            setForeground(createForegroundInfo(scanner, applicationContext.getString(R.string.scan_status_scanning), 0))

            scanner.scan { status, progress ->
                try {
                    val info = createForegroundInfo(scanner, status, progress)
                    notificationManager.notify(NOTIFICATION_ID, info.notification)
                } catch (e: Exception) {
                    logger.error("While scanning an error occurred:", e)
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            logger.error("Scan work for $scannerId was cancelled", e)
            throw e
        } catch (e: Exception) {
            logger.error("Error during scan work for $scannerId", e)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // We don't have the scanner ID here during initial setup if called by system,
        // so we use a generic placeholder or wait for doWork to set it.
        // Actually, we can try to get it from inputData.
        val scannerId = inputData.getString(KEY_SCANNER_ID) ?: "Unknown"
        val title = applicationContext.getString(R.string.scan_notification_title)

        return createForegroundInfo(title, applicationContext.getString(R.string.scan_status_scanning), 0)
    }

    private fun createForegroundInfo(scanner: FileScanner, status: String, progress: Int): ForegroundInfo {
        val title = applicationContext.getString(scanner.notificationTitleRes)
        return createForegroundInfo(title, status, progress)
    }

    private fun createForegroundInfo(title: String, status: String, progress: Int): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.scan_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.disc)
            .setContentText(status)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
