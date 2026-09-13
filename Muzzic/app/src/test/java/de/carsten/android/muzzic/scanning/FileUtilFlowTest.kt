package de.carsten.android.muzzic.scanning

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import de.carsten.android.muzzic.TestConfig
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class FileUtilFlowTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver

    private val root: Uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")

    @Before
    fun setup() {
        context = mockk()
        resolver = mockk()
        every { context.contentResolver } returns resolver
    }

    private fun cursorFor(vararg rows: Array<Any?>): MatrixCursor = MatrixCursor(ScannedFile.PROJECTION).apply {
        rows.forEach { addRow(it) }
    }

    private fun stubChildren(vararg rows: Array<Any?>) {
        every { resolver.query(any(), any(), any(), any(), any()) } answers {
            cursorFor(*rows)
        }
    }

    @Test
    fun `getFilesFlow emits only matching extensions including nested files`() = runTest {
        val root = Files.createTempDirectory("muzzic_flow").toFile()
        try {
            val nested = File(root, "nested").apply { mkdirs() }
            val mp3 = File(nested, "song.mp3").apply { writeText("") }
            val m4a = File(root, "track.m4a").apply { writeText("") }
            File(root, "notes.txt").apply { writeText("") }

            val files = FileUtil.getFilesFlow(root, setOf("mp3", "m4a")).toList()

            assertThat(files.map { it.absolutePath }).containsExactlyInAnyOrder(mp3.absolutePath, m4a.absolutePath)
            assertThat(files).extracting("name").doesNotContain("notes.txt")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `getFilesFlow emits nothing for non-existing root`() = runTest {
        val files = FileUtil.getFilesFlow(File("/nonexistent/muzzic/path"), setOf("mp3")).toList()

        assertThat(files).isEmpty()
    }
}
