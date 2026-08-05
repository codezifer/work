package de.carsten.android.muzzic.scanning

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilFlowTest {

    @Test
    fun `getFilesFlow emits only matching extensions including nested files`() = runTest {
        val root = Files.createTempDirectory("muzzic_flow").toFile()
        try {
            val nested = File(root, "nested").apply { mkdirs() }
            val mp3 = File(nested, "song.mp3").apply { writeText("") }
            val m4a = File(root, "track.m4a").apply { writeText("") }
            File(root, "notes.txt").apply { writeText("") }

            val files = FileUtil.getFilesFlow(root, setOf("mp3", "m4a")).toList()

            assertEquals(
                setOf(mp3.absolutePath, m4a.absolutePath),
                files.map { it.absolutePath }.toSet(),
            )
            assertTrue(files.none { it.name == "notes.txt" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `getFilesFlow emits nothing for non-existing root`() = runTest {
        val files = FileUtil.getFilesFlow(File("/nonexistent/muzzic/path"), setOf("mp3")).toList()

        assertTrue(files.isEmpty())
    }

    @Test
    fun `countFiles counts only matching files`() = runTest {
        val root = Files.createTempDirectory("muzzic_count").toFile()
        try {
            File(root, "a.mp3").apply { writeText("") }
            File(root, "b.mp3").apply { writeText("") }
            File(root, "c.txt").apply { writeText("") }

            assertEquals(2, FileUtil.countFiles(root, setOf("mp3")))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `countFiles applies the given predicate`() = runTest {
        val root = Files.createTempDirectory("muzzic_count_predicate").toFile()
        try {
            File(root, "keep.mp3").apply { writeText("") }
            File(root, "skip.mp3").apply { writeText("") }

            val counted =
                FileUtil.countFiles(root, setOf("mp3")) { file ->
                    file.name == "keep.mp3"
                }

            assertEquals(1, counted)
        } finally {
            root.deleteRecursively()
        }
    }
}
