package de.carsten.android.muzzic.scanning

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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
     * Converting configured directory path starting with content:// or file path to a list of URI strings.
     *
     * @param configuredDir [String] path or URI
     * @return [List] of URI [String]
     */
    fun getScanningRoots(configuredDir: String?): List<String> = if (configuredDir == null) {
        listOf(
            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)).toString(),
            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).toString(),
        )
    } else {
        listOf(configuredDir)
    }

    /**
     * Resolves a [DocumentFile] from a root URI string.
     */
    fun getRootDocument(context: Context, rootUri: String): DocumentFile? = if (rootUri.startsWith("content://")) {
        DocumentFile.fromTreeUri(context, rootUri.toUri())
    } else {
        DocumentFile.fromFile(File(rootUri.toUri().path ?: ""))
    }

    /**
     * Recursively scans a [DocumentFile] for files with the specified extensions.
     */
    suspend fun findFilesRecursively(directory: DocumentFile, extensions: Set<String>, results: MutableList<String>) {
        currentCoroutineContext().ensureActive()
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                findFilesRecursively(file, extensions, results)
            } else {
                val name = file.name?.lowercase() ?: ""
                if (extensions.any { name.endsWith(".$it") }) {
                    results.add(file.uri.toString())
                }
            }
        }
    }

    /**
     * Scans multiple roots for files with the specified extensions.
     */
    suspend fun scanForFiles(context: Context, roots: List<String>, extensions: Set<String>): List<String> {
        val results = mutableListOf<String>()
        roots.forEach { rootUri ->
            currentCoroutineContext().ensureActive()
            val rootDoc = getRootDocument(context, rootUri)
            if (rootDoc != null && rootDoc.exists() && rootDoc.isDirectory) {
                findFilesRecursively(rootDoc, extensions, results)
            }
        }
        return results
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
