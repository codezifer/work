package de.carsten.android.muzzic.visualization.projectm

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts bundled Milkdrop preset files from APK assets into local app storage.
 */
object PresetManager {

    private const val TAG = "PresetManager"
    private const val ASSETS_PRESET_DIR = "presets"
    private const val LOCAL_PRESET_DIR_NAME = "projectm/presets"

    /**
     * Ensures default presets are extracted to filesDir and returns the directory path.
     */
    fun ensurePresetsExtracted(context: Context): String {
        val targetDir = File(context.filesDir, LOCAL_PRESET_DIR_NAME)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list(ASSETS_PRESET_DIR) ?: emptyArray()

            for (fileName in assetFiles) {
                val destFile = File(targetDir, fileName)
                if (!destFile.exists()) {
                    assetManager.open("$ASSETS_PRESET_DIR/$fileName").use { inputStream ->
                        FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Extracted preset: $fileName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting presets from assets", e)
        }

        return targetDir.absolutePath
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
