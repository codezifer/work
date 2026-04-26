package de.carsten.android.muzzic.persistence.repo

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusicScanWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val repository: MusicRepository by inject()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "library_scan_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            setForeground(
                createForegroundInfo(
                    applicationContext.getString(R.string.scan_status_scanning),
                    0,
                )
            )

            repository.performLibraryScan { status, progress ->
                // Update notification progress
                val info = createForegroundInfo(status, progress)
                // We use setForeground to ensure the worker stays in foreground and updates the notification
                // Note: In WorkManager 2.9+, setForeground handles this.
                // However, setForeground can only be called while the worker is running.
                // We wrap it in a try-catch just in case the worker is cancelled during the scan.
                try {
                    // Update the notification directly for smoother updates if already in foreground
                    notificationManager.notify(NOTIFICATION_ID, info.notification)
                } catch (ignored: Exception) {
                    // Ignore
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(
            applicationContext.getString(R.string.scan_status_scanning),
            0,
        )
    }

    private fun createForegroundInfo(status: String, progress: Int): ForegroundInfo {
        // NotificationChannel is required for Android O+ (API 26)
        // Since minSdk is 34, this is technically redundant but good practice for robustness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.scan_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.scan_notification_title))
            .setSmallIcon(R.drawable.disc) // Use existing disc icon
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
