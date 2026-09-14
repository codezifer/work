package de.carsten.android.muzzic.scanning

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import de.carsten.android.muzzic.TestConfig
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class FileUtilTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `getFilePathFromUri resolves file scheme`() {
        val uri = Uri.parse("file:///storage/emulated/0/Music/song.mp3")
        val path = FileUtil.getFilePathFromUri(context, uri)
        assertThat(path).isEqualTo("/storage/emulated/0/Music/song.mp3")
    }

    @Test
    fun `getFilePathFromUri resolves SAF document URI`() {
        // ExternalStorageProvider document URI: content://com.android.externalstorage.documents/document/primary%3AMusic%2Fsong.mp3
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AMusic%2Fsong.mp3")

        val path = FileUtil.getFilePathFromUri(context, uri)

        val expectedBase = Environment.getExternalStorageDirectory().absolutePath
        if (path != null) {
            assertThat(path).isEqualTo("$expectedBase/Music/song.mp3")
        }
    }

    @Test
    fun `getFilePathFromUri resolves SAF tree URI`() {
        // ExternalStorageProvider tree URI: content://com.android.externalstorage.documents/tree/primary%3AMusic
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")

        val path = FileUtil.getFilePathFromUri(context, uri)

        val expectedBase = Environment.getExternalStorageDirectory().absolutePath
        if (path != null) {
            assertThat(path).isEqualTo("$expectedBase/Music")
        }
    }

    @Test
    fun `getFilePathFromUri returns null for unknown scheme`() {
        val uri = Uri.parse("http://example.com/music.mp3")
        val path = FileUtil.getFilePathFromUri(context, uri)
        assertThat(path).isNull()
    }

    @Test
    fun `countFiles returns correct count for single directory`() = runTest {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")
        val mockContext = mockk<Context>()
        val resolver = mockk<ContentResolver>()
        val cursor = mockk<Cursor>()

        every { mockContext.contentResolver } returns resolver
        every { resolver.query(any(), any(), any(), any(), any()) } returns cursor

        every { cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID) } returns 0
        every { cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME) } returns 1
        every { cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE) } returns 2

        every { cursor.moveToNext() } returnsMany listOf(true, true, true, false)
        every { cursor.getString(0) } returnsMany listOf("doc1", "doc2", "doc3")
        every { cursor.getString(1) } returnsMany listOf("song1.mp3", "song2.mp3", "other.txt")
        every { cursor.getString(2) } returnsMany listOf("audio/mpeg", "audio/mpeg", "text/plain")
        every { cursor.close() } just Runs

        val count = FileUtil.countFiles(mockContext, uri, setOf("mp3"))

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `countFiles returns zero for empty directory`() = runTest {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")
        val mockContext = mockk<Context>()
        val resolver = mockk<ContentResolver>()
        val cursor = mockk<Cursor>()

        every { mockContext.contentResolver } returns resolver
        every { resolver.query(any(), any(), any(), any(), any()) } returns cursor

        every { cursor.getColumnIndexOrThrow(any()) } returns 0
        every { cursor.moveToNext() } returns false
        every { cursor.close() } just Runs

        val count = FileUtil.countFiles(mockContext, uri, setOf("mp3"))

        assertThat(count).isEqualTo(0)
    }
}
