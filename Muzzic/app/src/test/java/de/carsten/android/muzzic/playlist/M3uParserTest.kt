package de.carsten.android.muzzic.playlist

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {

    @Test
    fun testParseAbsolutePaths() {
        val tempDir = Files.createTempDirectory("muzzic_test").toFile()
        val playlistFile = File(tempDir, "test.m3u")

        // On Linux/Android, absolute paths start with /
        val absolutePath = "/storage/emulated/0/Music/song.mp3"

        playlistFile.writeText("#EXTM3U\n$absolutePath")

        val entries = M3uParser.parse(playlistFile)

        try {
            assertEquals(1, entries.size)
            assertEquals(absolutePath, entries[0].path)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testParseWithBOM() {
        val tempDir = Files.createTempDirectory("muzzic_test_bom").toFile()
        val playlistFile = File(tempDir, "test.m3u")

        val songPath = "/storage/emulated/0/Music/song.mp3"
        // BOM + #EXTM3U
        val content = "\uFEFF#EXTM3U\n$songPath"

        playlistFile.writeText(content, Charsets.UTF_8)

        val entries = M3uParser.parse(playlistFile)

        try {
            assertEquals(1, entries.size)
            assertEquals(songPath, entries[0].path)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testParseRelativePaths() {
        val tempDir = Files.createTempDirectory("muzzic_test_rel").toFile()
        val playlistFile = File(tempDir, "test.m3u")

        val relativePath = "Music/song.mp3"
        val expectedPath = File(tempDir, "Music/song.mp3").absolutePath

        playlistFile.writeText("#EXTM3U\n$relativePath")

        val entries = M3uParser.parse(playlistFile)

        try {
            assertEquals(1, entries.size)
            assertEquals(expectedPath, entries[0].path)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
