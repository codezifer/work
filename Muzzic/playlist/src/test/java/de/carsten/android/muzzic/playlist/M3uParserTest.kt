package de.carsten.android.muzzic.playlist

import de.carsten.android.muzzic.logging.MuzzicLogger
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class M3uParserTest {

    private val mockLogger = mockk<MuzzicLogger>(relaxed = true)

    @Test
    fun testParseAbsolutePaths() {
        val tempDir = Files.createTempDirectory("muzzic_test").toFile()
        val playlistFile = File(tempDir, "test.m3u")

        // On Linux/Android, absolute paths start with /
        val absolutePath = "/storage/emulated/0/Music/song.mp3"

        playlistFile.writeText("#EXTM3U\n$absolutePath")

        val entries = M3uParser.parse(playlistFile, mockLogger)

        try {
            assertThat(entries).hasSize(1)
            assertThat(entries[0].path).isEqualTo(absolutePath)
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

        val entries = M3uParser.parse(playlistFile, mockLogger)

        try {
            assertThat(entries).hasSize(1)
            assertThat(entries[0].path).isEqualTo(songPath)
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

        val entries = M3uParser.parse(playlistFile, mockLogger)

        try {
            assertThat(entries).hasSize(1)
            assertThat(entries[0].path).isEqualTo(expectedPath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testParseInputStreamKeepsRelativePaths() {
        val content = "#EXTM3U\n#EXTINF:187,Artist - Title\nMusic/song.mp3\ncontent://media/song2.mp3"
        val entries = M3uParser.parse(content.byteInputStream(Charsets.UTF_8), mockLogger)

        assertThat(entries).hasSize(2)
        assertThat(entries[0].path).isEqualTo("Music/song.mp3")
        assertThat(entries[0].title).isEqualTo("Artist - Title")
        assertThat(entries[0].duration).isEqualTo(187)
        assertThat(entries[1].path).isEqualTo("content://media/song2.mp3")
    }
}
