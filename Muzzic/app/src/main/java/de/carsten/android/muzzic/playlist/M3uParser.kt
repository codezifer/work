package de.carsten.android.muzzic.playlist

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Data class representing an entry within an M3U playlist file.
 *
 * @property path The absolute or relative path to the music file.
 * @property title The title of the song extracted from `#EXTINF`, if available.
 * @property duration The duration of the song in seconds from `#EXTINF`, if available.
 */
data class M3uEntry(
    val path: String,
    val title: String? = null,
    val duration: Int? = null
)

/**
 * A lightweight, custom parser for M3U and M3U8 files.
 * Supports basic and extended formats (handling `#EXTINF`).
 */
object M3uParser {

    private const val EXT_M3U_HEADER = "#EXTM3U"
    private const val EXT_INF_PREFIX = "#EXTINF:"

    /**
     * Parses the given M3U/M3U8 file and returns a list of [M3uEntry] items.
     * Resolves relative paths based on the playlist file's parent directory.
     *
     * @param file The M3U/M3U8 file to parse.
     * @return A list of parsed entries.
     */
    fun parse(file: File): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        if (!file.exists() || !file.isFile) return entries

        val parentDir = file.parentFile

        BufferedReader(FileReader(file)).use { reader ->
            var line: String?
            var currentTitle: String? = null
            var currentDuration: Int? = null

            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                if (trimmedLine.isEmpty()) continue

                if (trimmedLine.startsWith("#")) {
                    if (trimmedLine.startsWith(EXT_INF_PREFIX)) {
                        parseExtInf(trimmedLine)?.let { inf ->
                            currentDuration = inf.first
                            currentTitle = inf.second
                        }
                    }
                    // Ignore other tags or the main header
                    continue
                }

                // This line is a track path
                val resolvedPath = resolvePath(trimmedLine, parentDir)
                entries.add(M3uEntry(path = resolvedPath, title = currentTitle, duration = currentDuration))

                // Reset for next entry
                currentTitle = null
                currentDuration = null
            }
        }

        return entries
    }

    private fun parseExtInf(line: String): Pair<Int, String>? {
        // Format: #EXTINF:<duration>,<title>
        // Or sometimes with extra attributes: #EXTINF:-1 tvg-id="..." ,Title
        try {
            val content = line.substring(EXT_INF_PREFIX.length)
            val commaIndex = content.indexOf(',')
            if (commaIndex == -1) return null

            val durationPart = content.substring(0, commaIndex).trim()
            // Extract the first integer part from durationPart (could have attributes separated by space)
            val durationStr = durationPart.split(' ')[0]
            val duration = durationStr.toIntOrNull() ?: -1

            val title = content.substring(commaIndex + 1).trim()
            return Pair(duration, title)
        } catch (e: Exception) {
            return null
        }
    }

    private fun resolvePath(trackPath: String, parentDir: File?): String {
        val file = File(trackPath)
        if (file.isAbsolute) {
            return file.absolutePath
        }
        // Resolve relative path against playlist parent directory
        if (parentDir != null) {
            return File(parentDir, trackPath).normalize().absolutePath
        }
        return file.absolutePath
    }
}
