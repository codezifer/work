package de.carsten.android.muzzic.visualization.projectm

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Extracts bundled Milkdrop preset files from APK assets into local app storage.
 *
 * The synchronous [ensurePresetsExtracted] copies only missing presets and removes
 * stale `.milk` files that are no longer bundled. The asynchronous
 * [ensurePresetsExtractedAsync] runs the same work on a shared daemon thread so
 * GL threads never block on file I/O; re-running init after completion only
 * rescans the directory on the native side.
 */
object PresetManager {

    private const val TAG = "PresetManager"
    private const val ASSETS_PRESET_DIR = "presets"
    private const val LOCAL_PRESET_DIR_NAME = "projectm/presets"

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "PresetExtractor").apply { isDaemon = true }
    }

    /**
     * Returns the preset directory, creating it when missing. Performs no asset I/O.
     */
    fun presetDir(context: Context): File {
        val targetDir = File(context.filesDir, LOCAL_PRESET_DIR_NAME)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    /**
     * Returns the absolute preset directory path, creating it when missing.
     */
    fun presetDirPath(context: Context): String = presetDir(context).absolutePath

    /**
     * Ensures default presets are extracted to filesDir and returns the directory path.
     */
    fun ensurePresetsExtracted(context: Context): String {
        val targetDir = presetDir(context)

        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list(ASSETS_PRESET_DIR) ?: emptyArray()
            val assetSet = assetFiles.toSet()

            for (fileName in assetFiles) {
                val destFile = File(targetDir, fileName)
                if (!destFile.exists()) {
                    try {
                        assetManager.open("$ASSETS_PRESET_DIR/$fileName").use { inputStream ->
                            FileOutputStream(destFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        Log.d(TAG, "Extracted preset: $fileName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting preset: $fileName", e)
                    }
                }
            }

            // Remove stale presets from older app versions (one failure must not abort cleanup).
            targetDir.listFiles { _, name -> name.endsWith(".milk", ignoreCase = true) }?.forEach { file ->
                if (file.name !in assetSet) {
                    try {
                        if (file.delete()) {
                            Log.d(TAG, "Removed stale preset: ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing stale preset: ${file.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting presets from assets", e)
        }

        return targetDir.absolutePath
    }

    /**
     * Extracts presets on a shared background thread and invokes [onDone] with the
     * directory path. Never throws: failures are logged and still report the path.
     */
    fun ensurePresetsExtractedAsync(context: Context, onDone: (String) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val path = ensurePresetsExtracted(appContext)
            try {
                onDone(path)
            } catch (e: Exception) {
                Log.e(TAG, "Preset extraction callback failed", e)
            }
        }
    }

    /**
     * Returns a list of available .milk preset file names.
     */
    fun getAvailablePresets(context: Context): List<String> {
        val dirPath = ensurePresetsExtracted(context)
        val dir = File(dirPath)
        return dir.listFiles { _, name -> name.endsWith(".milk", ignoreCase = true) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Returns the full file path for a specific preset file name.
     */
    fun getPresetFilePath(context: Context, fileName: String): String {
        val dirPath = ensurePresetsExtracted(context)
        return File(dirPath, fileName).absolutePath
    }
}
