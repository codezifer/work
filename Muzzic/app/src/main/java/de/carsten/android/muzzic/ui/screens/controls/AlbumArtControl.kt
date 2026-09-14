package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.model.TestingTags.Screens.FALLBACK_PAINTER
import de.carsten.android.muzzic.model.TestingTags.Screens.SUCCESS_ASYNC_IMAGE
import de.carsten.android.muzzic.ui.CARD_CORNER_RADIUS
import de.carsten.android.muzzic.ui.PLAYER_ART_PX
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.rememberCoverImageRequest
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun AlbumArtControl(albumArtInput: AlbumArtInput = AlbumArtInput.None, modifier: Modifier = Modifier) {
    AlbumArtContent(albumArtInput, modifier)
}

@Composable
private fun AlbumArtContent(albumArtInput: AlbumArtInput, modifier: Modifier = Modifier) {
    val data: Any? = when (albumArtInput) {
        is AlbumArtInput.FromPath -> albumArtInput.audioFilePath
        is AlbumArtInput.FromInputStream -> albumArtInput.inputStream
        is AlbumArtInput.FromUri -> albumArtInput.audioFileUri
        else -> null
    }
    val cacheKey: String? = when (albumArtInput) {
        is AlbumArtInput.FromPath -> albumArtInput.audioFilePath
        is AlbumArtInput.FromUri -> albumArtInput.audioFileUri.toString()
        else -> null
    }
    val request = rememberCoverImageRequest(data, cacheKey, PLAYER_ART_PX)

    val testTag = if (albumArtInput is AlbumArtInput.None) FALLBACK_PAINTER else SUCCESS_ASYNC_IMAGE

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CARD_CORNER_RADIUS))
            .background(MaterialTheme.colorScheme.background),
    ) {
        AsyncImage(
            model = request,
            contentDescription = stringResource(R.string.album_art),
            placeholder = painterResource(R.drawable.disc),
            error = painterResource(R.drawable.disc),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag),
        )
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumArtControlPreview_Dark")
fun AlbumArtControlPreview() {
    AppTheme {
        AlbumArtControl()
    }
}
