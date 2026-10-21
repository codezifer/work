package de.carsten.android.muzzic.playlist

import de.carsten.android.muzzic.logging.MuzzicLogger
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

/**
 * A lightweight, custom parser for M3U and M3U8 files.
 * Supports basic and extended formats (handling `#EXTINF`).
 */
object M3uParser {
    private const val EXT_INF_PREFIX = "#EXTINF:"

    /**
     * Parses the given M3U/M3U8 file and returns a list of [M3uEntry] items.
     * Resolves relative paths based on the playlist file's parent directory.
     *
     * @param file The M3U/M3U8 file.
     * @return A list of parsed entries.
     */
    fun parse(file: File, logger: MuzzicLogger): List<M3uEntry> {
        if (!file.exists()) return emptyList()
        val parentDir = file.parentFile ?: return emptyList()
        return file.inputStream().use { input -> parsePlaylist(parentDir, input, logger, file.name) }
    }

    /**
     * Parses M3U/M3U8 content from an input stream and returns a list of [M3uEntry] items.
     *
     * Without a parent directory, relative track paths are kept as-is while
     * absolute paths and URIs pass through unchanged.
     *
     * @param input The M3U/M3U8 content stream.
     * @param fileId optional file identifier
     * @return A list of parsed entries.
     */
    fun parse(input: InputStream, logger: MuzzicLogger, fileId: String? = null): List<M3uEntry> {
        val logFileId = fileId ?: ""
        try {
            logger.info("Start to parse playlist $logFileId ...")
            return parsePlaylist(null, input, logger, fileId)
        } finally {
            logger.info("... Finished to parse playlist $logFileId.")
        }
    }

    private fun parsePlaylist(parentDir: File?, inputStream: InputStream, logger: MuzzicLogger, fileId: String? = null): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var currentTitle: String? = null
            var currentDuration: Int? = null

            for (rawLine in reader.lineSequence()) {
                var trimmedLine = rawLine.trim()

                // Remove UTF-8 BOM if present
                if (trimmedLine.startsWith("\uFEFF")) {
                    trimmedLine = trimmedLine.substring(1).trim()
                }

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
                logger.debug("Added track $resolvedPath to playlist $fileId")

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
        val content = line.substring(EXT_INF_PREFIX.length)
        val commaIndex = content.indexOf(',')
        if (commaIndex == -1) return null

        // The duration may be followed by attributes separated by a space.
        val duration = content.substring(0, commaIndex).trim().substringBefore(' ').toIntOrNull() ?: -1
        val title = content.substring(commaIndex + 1).trim()
        return Pair(duration, title)
    }

    private fun resolvePath(trackPath: String, parentDir: File?): String {
        // If it's a URI with a scheme (e.g., http://, file://, content://), treat it as absolute
        if (trackPath.contains("://")) {
            return trackPath
        }

        // If it's an absolute filesystem path
        val file = File(trackPath)
        if (file.isAbsolute) {
            return trackPath
        }

        // Resolve relative path against playlist parent directory
        if (parentDir != null) {
            var current: File = parentDir
            val parts = trackPath.split('/', '\\')
            for (part in parts) {
                if (part == "." || part.isEmpty()) continue
                current = if (part == "..") {
                    current.parentFile ?: current
                } else {
                    File(current, part)
                }
            }
            return current.absolutePath
        }
        return trackPath
    }
}
