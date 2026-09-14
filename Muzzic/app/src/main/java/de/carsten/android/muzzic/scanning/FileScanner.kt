package de.carsten.android.muzzic.scanning

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.carsten.android.muzzic.AppConfig
import kotlinx.coroutines.flow.Flow

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
     * Calculates and reports scan progress.
     *
     * @param current Current number of processed items.
     * @param total Total number of items to process.
     * @param status Status message to display.
     * @param onProgress Callback to report progress to.
     * @return The calculated percentage (0-100), or -1 if indeterminate.
     */
    fun reportScanProgress(current: Int, total: Int, status: String, onProgress: ((String, Int) -> Unit)?): Int {
        val progress = if (total > 0) {
            ((current.toFloat() / total) * AppConfig.Scanning.MAX_PROGRESS).toInt()
        } else {
            -1
        }
        onProgress?.invoke(status, progress)
        return progress
    }

    /**
     * Streams files from root directory with the specified extensions.
     *
     * @param context android [Context]
     * @param uri configured directory as [Uri]
     * @param extensions supported file extensions
     * @return [Flow] of chunked [Set] of [ScannedFile]
     */
    fun scanForFiles(context: Context, uri: Uri?, extensions: Set<String>): Flow<Set<ScannedFile>> = FileUtil.getScannedFileFlow(
        context = context,
        uri = uri,
        supportedFiles = extensions,
    )

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
