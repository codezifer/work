package de.carsten.android.muzzic.ui.screens

// MainActivity is no longer needed here, so the import can be removed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.carsten.android.muzzic.getTestFileFromAssets
import de.carsten.android.muzzic.model.TestingTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerScreenContextTest {

    @get:Rule
    val composeTestRule = createComposeRule() // Use the simple rule for isolated composable testing

    @Test
    fun testAlbumArtIsProvided() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().context
        val testAlbumArtPath = getTestFileFromAssets(context, "test_file1.mp3")

        // when
        composeTestRule.setContent {
            PlayerScreenContext(
                albumArtPath = testAlbumArtPath,
                songTitle = "Test Song",
                artistName = "Test Artist",
                isPlaying = true,
                progress = 0.5f,
                duration = 180000L,
                onNextClicked = {},
                onPlayPauseClicked = {},
                onPreviousClicked = {},
                onProgressChanged = {},
            )
        }

        // then
        // Using assertIsDisplayed() is a direct and clear assertion
        composeTestRule.onNodeWithTag(TestingTags.Screens.SuccessAsyncImage).assertIsDisplayed()
    }

    @Test
    fun testFallbackAlbumArtIsProvided() {
        // given
        val testAlbumArtPath = null

        // when
        composeTestRule.setContent {
            PlayerScreenContext(
                albumArtPath = testAlbumArtPath,
                songTitle = "Test Song",
                artistName = "Test Artist",
                isPlaying = true,
                progress = 0.5f,
                duration = 180000L,
                onNextClicked = {},
                onPlayPauseClicked = {},
                onPreviousClicked = {},
                onProgressChanged = {},
            )
        }

        // then
        composeTestRule.onNodeWithTag(TestingTags.Screens.FallbackPainter).assertIsDisplayed()
    }
}
