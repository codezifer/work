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
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.DefaultAlbumArt
import de.carsten.android.muzzic.ui.component.rememberCoverImageRequest
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * Renders the album art for the given [albumArtInput].
 *
 * A missing cover ([AlbumArtInput.None]) is resolved before Coil is involved: the default
 * artwork is drawn directly, so no data-less [coil3.request.ImageRequest] is ever executed.
 *
 * @param albumArtInput the cover source; [AlbumArtInput.None] shows the default artwork.
 * @param modifier the modifier applied to the album art container.
 */
@Composable
fun AlbumArtControl(albumArtInput: AlbumArtInput = AlbumArtInput.None, modifier: Modifier = Modifier) {
    AlbumArtContent(albumArtInput, modifier)
}

@Composable
private fun AlbumArtContent(albumArtInput: AlbumArtInput, modifier: Modifier = Modifier) {
    val data: Any? = albumArtInput.toImageSource()

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CARD_CORNER_RADIUS))
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (data == null) {
            // No cover available: draw the default artwork instead of asking Coil to load nothing.
            DefaultAlbumArt(
                modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(FALLBACK_PAINTER),
            )
        } else {
            AsyncImage(
                model = rememberCoverImageRequest(data),
                contentDescription = stringResource(R.string.album_art),
                placeholder = painterResource(R.drawable.disc),
                error = painterResource(R.drawable.disc),
                contentScale = ContentScale.Crop,
                modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(SUCCESS_ASYNC_IMAGE),
            )
        }
    }
}

/**
 * Maps the album art input to the data handed to Coil.
 *
 * @return the Coil data, or null when no cover is available.
 */
private fun AlbumArtInput.toImageSource(): Any? = when (this) {
    is AlbumArtInput.FromPath -> audioFilePath
    is AlbumArtInput.FromInputStream -> inputStream
    is AlbumArtInput.FromUri -> audioFileUri
    AlbumArtInput.None -> null
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumArtControlPreview_Dark")
fun AlbumArtControlPreview() {
    AppTheme {
        AlbumArtControl()
    }
}
