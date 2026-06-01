package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.model.TestingTags.Screens.CIRCULAR_PROGRESS_INDICATOR
import de.carsten.android.muzzic.model.TestingTags.Screens.FALLBACK_PAINTER
import de.carsten.android.muzzic.model.TestingTags.Screens.SUCCESS_ASYNC_IMAGE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun AlbumArtControl(albumArtInput: AlbumArtInput = AlbumArtInput.None) {
    val coilModel =
        when (albumArtInput) {
            is AlbumArtInput.FromPath -> albumArtInput.audioFilePath
            is AlbumArtInput.FromInputStream -> albumArtInput.inputStream
            is AlbumArtInput.FromUri -> albumArtInput.audioFileUri
            else -> null
        }

    AppTheme {
        AlbumArtContent(coilModel)
    }
}

@Composable
private fun AlbumArtContent(coilModel: Any?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        SubcomposeAsyncImage(
            model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(coilModel)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
        ) {
            val state by painter.state.collectAsState()
            HandleAsyncImageState(state)
        }
    }
}

@Composable
private fun HandleAsyncImageState(state: AsyncImagePainter.State) {
    when (state) {
        is AsyncImagePainter.State.Empty,
        is AsyncImagePainter.State.Loading,
        -> {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 22.dp,
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(22.dp)
                    .testTag(CIRCULAR_PROGRESS_INDICATOR),
            )
        }

        is AsyncImagePainter.State.Error -> {
            FallbackPainter()
        }

        is AsyncImagePainter.State.Success -> {
            Image(
                painter = state.painter,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .testTag(SUCCESS_ASYNC_IMAGE),
            )
        }
    }
}

@Composable
private fun FallbackPainter() {
    Image(
        painter = painterResource(R.drawable.disc),
        contentDescription = "Empty or error state fallback",
        contentScale = ContentScale.Crop,
        modifier =
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .testTag(FALLBACK_PAINTER),
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumArtControlPreview_Dark")
fun AlbumArtControlPreview() {
    AlbumArtControl()
}
