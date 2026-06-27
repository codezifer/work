package de.carsten.android.muzzic.scanning

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File

/**
 * Common interface for components that scan the file system for music-related files.
 */
interface FileScanner {
    /**
     * Unique identifier for this scanner, used for WorkManager unique work name.
     */
    val scannerId: String

    /**
     * Resource ID for the notification title shown during the scan.
     */
    val notificationTitleRes: Int

    /**
     * Performs a scan of the file system.
     *
     * @param onProgress Callback to report scan progress (status message, percentage 0-100).
     */
    suspend fun scan(onProgress: ((String, Int) -> Unit)? = null)

    /**
     * Converting configured directory path starting with content:// to a valid [File] objects
     *
     * @param configuredDir [String] starting with content://
     * @return [List] of [File]
     */
    fun contentToFiles(configuredDir: String?): List<File> = if (configuredDir == null) {
        listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        )
    } else {
        val dir = if (configuredDir.startsWith("content://")) {
            val uri = configuredDir.toUri()
            if (uri.scheme == "file") {
                File(uri.path ?: throw IllegalArgumentException("Invalid configured directory path $configuredDir!"))
            }

            throw UnsupportedOperationException("Direct File access for content:// URIs is restricted. Consider refactoring MusicFileScanner to use DocumentFile or ContentResolver.")
        } else {
            File(configuredDir)
        }

        listOfNotNull(dir)
    }


    /**
     * Enqueues a scan task using [WorkManager].
     *
     * @param context Android context to access WorkManager.
     */
    fun enqueue(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ScannerWorker>()
            .setInputData(workDataOf(ScannerWorker.KEY_SCANNER_ID to scannerId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(scannerId)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            scannerId,
            ExistingWorkPolicy.KEEP,
            workRequest,
        )
    }
}
