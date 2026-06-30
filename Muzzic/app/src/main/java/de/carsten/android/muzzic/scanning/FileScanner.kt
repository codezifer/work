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
     * Converting configured directory path starting with content:// or file path to a File.
     *
     * @param context android [Context]
     * @param configuredDir [String] path or URI
     * @return resolved [File]
     */
    fun getScanningRoot(context: Context, configuredDir: String?): File? = if (configuredDir == null) {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    } else {
        FileUtil.getFilePathFromUri(context, configuredDir.toUri())?.let {
            File(it)
        }
    }

    /**
     * Scans multiple roots for files with the specified extensions.
     */
    suspend fun scanForFiles(context: Context, rootDir: File, extensions: Set<String>): List<File> = FileUtil.getFiles(rootDir, extensions)

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
