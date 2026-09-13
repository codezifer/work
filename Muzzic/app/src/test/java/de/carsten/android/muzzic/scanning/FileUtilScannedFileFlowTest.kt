package de.carsten.android.muzzic.scanning

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import de.carsten.android.muzzic.TestConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [FileUtil.getScannedFileFlow].
 *
 * Mocks the [ContentResolver] to return [MatrixCursor] instances so the
 * Storage Access Framework walk can be exercised without a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class FileUtilScannedFileFlowTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver

    private val root: Uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")
    private val dirMimeType: String = DocumentsContract.Document.MIME_TYPE_DIR

    @Before
    fun setup() {
        context = mockk()
        resolver = mockk()
        every { context.contentResolver } returns resolver
    }

    /**
     * Creates a fresh cursor with [ScannedFile.PROJECTION] columns for the given rows.
     *
     * A new instance is required per query because [FileUtil.getScannedFileFlow]
     * closes the cursor via `use`.
     *
     * @param rows rows as documentId, displayName, mimeType, size, lastModified.
     * @return configured [MatrixCursor].
     */
    private fun cursorFor(vararg rows: Array<Any?>): MatrixCursor = MatrixCursor(ScannedFile.PROJECTION).apply {
        rows.forEach { addRow(it) }
    }

    /**
     * Stubs [ContentResolver.query] to serve the given children URI to cursor mapping.
     *
     * Each invocation returns a fresh cursor copy so closing does not affect
     * subsequent queries for the same URI.
     *
     * @param cursors mapping from children [Uri] to rows.
     */
    private fun stubQueries(cursors: Map<Uri, List<Array<Any?>>>) {
        every { resolver.query(any(), any(), any(), any(), any()) } answers {
            val uri = firstArg<Uri>()
            val rows = cursors[uri] ?: emptyList()
            cursorFor(*rows.toTypedArray())
        }
    }

    @Test
    fun `getScannedFileFlow emits supported files and recurses into subdirectories`() = runTest {
        val treeId = DocumentsContract.getTreeDocumentId(root)
        val rootChildren = DocumentsContract.buildChildDocumentsUriUsingTree(root, treeId)
        val subdirId = "primary:Music/Subdir"
        val subdirChildren = DocumentsContract.buildChildDocumentsUriUsingTree(root, subdirId)

        stubQueries(
            mapOf(
                rootChildren to
                    listOf(
                        arrayOf<Any?>("primary:Music/song1.mp3", "song1.mp3", "audio/mpeg", 1234L, 1000L),
                        arrayOf<Any?>("primary:Music/notes.txt", "notes.txt", "text/plain", 10L, 1000L),
                        arrayOf<Any?>(subdirId, "Subdir", dirMimeType, 0L, 1000L),
                    ),
                subdirChildren to
                    listOf(
                        arrayOf<Any?>("primary:Music/Subdir/song2.mp3", "song2.mp3", "audio/mpeg", 5678L, 2000L),
                    ),
            ),
        )

        val chunks = FileUtil.getScannedFileFlow(context, root, chunkSize = 50, supportedFiles = setOf("mp3")).toList()

        assertThat(chunks).hasSize(1)
        val files = chunks.single()
        assertThat(files.map { it.displayName }).containsExactlyInAnyOrder("song1.mp3", "song2.mp3")
        assertThat(files.map { it.documentId }).containsExactlyInAnyOrder(
            "primary:Music/song1.mp3",
            "primary:Music/Subdir/song2.mp3",
        )
        // Resolved URIs must point at the tree document, not the raw children URI.
        assertThat(files.map { it.uri }).containsExactlyInAnyOrder(
            DocumentsContract.buildDocumentUriUsingTree(root, "primary:Music/song1.mp3"),
            DocumentsContract.buildDocumentUriUsingTree(root, "primary:Music/Subdir/song2.mp3"),
        )
    }

    @Test
    fun `getScannedFileFlow chunks emissions when chunkSize is reached`() = runTest {
        val treeId = DocumentsContract.getTreeDocumentId(root)
        val rootChildren = DocumentsContract.buildChildDocumentsUriUsingTree(root, treeId)

        stubQueries(
            mapOf(
                rootChildren to
                    listOf(
                        arrayOf<Any?>("primary:Music/a.mp3", "a.mp3", "audio/mpeg", 1L, 1L),
                        arrayOf<Any?>("primary:Music/b.mp3", "b.mp3", "audio/mpeg", 2L, 2L),
                        arrayOf<Any?>("primary:Music/c.mp3", "c.mp3", "audio/mpeg", 3L, 3L),
                    ),
            ),
        )

        val chunks = FileUtil.getScannedFileFlow(context, root, chunkSize = 2, supportedFiles = setOf("mp3")).toList()

        assertThat(chunks).hasSize(2)
        assertThat(chunks[0]).hasSize(2)
        assertThat(chunks[1]).hasSize(1)
        assertThat(chunks.flatten().map { it.displayName }).containsExactly("a.mp3", "b.mp3", "c.mp3")
    }

    @Test
    fun `getScannedFileFlow emits nothing when no supported files exist`() = runTest {
        val treeId = DocumentsContract.getTreeDocumentId(root)
        val rootChildren = DocumentsContract.buildChildDocumentsUriUsingTree(root, treeId)

        stubQueries(
            mapOf(
                rootChildren to
                    listOf(
                        arrayOf<Any?>("primary:Music/notes.txt", "notes.txt", "text/plain", 10L, 1000L),
                        arrayOf<Any?>("primary:Music/cover.jpg", "cover.jpg", "image/jpeg", 20L, 1000L),
                    ),
            ),
        )

        val chunks = FileUtil.getScannedFileFlow(context, root).toList()

        assertThat(chunks).isEmpty()
    }

    @Test
    fun `getScannedFileFlow emits nothing when query returns null`() = runTest {
        every { resolver.query(any(), any(), any(), any(), any()) } returns null

        val chunks = FileUtil.getScannedFileFlow(context, root).toList()

        assertThat(chunks).isEmpty()
    }
}
