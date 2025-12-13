package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.utils.extractAlbumArt

@Composable
fun AlbumArtControl(albumArtPath: String? = null) {
    AppTheme {
        AlbumArtContent(
            albumArtPainter = if (albumArtPath == null) null else rememberAsyncImagePainter(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(
                        extractAlbumArt(
                            context = LocalContext.current,
                            audioFilePath = albumArtPath,
                        ),
                    ).build(),
            )
        )
    }
}

@Composable
private fun AlbumArtContent(albumArtPainter: Painter? = null) {
    when (albumArtPainter) {
        is AsyncImagePainter -> HandleAsyncImagePainter(albumArtPainter)
        else -> FallbackPainter()
    }
}

@Composable
private fun HandleAsyncImagePainter(asyncImagePainter: AsyncImagePainter) {
    when (asyncImagePainter.state) {
        is AsyncImagePainter.State.Loading -> {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 22.dp,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(100)),

                )
        }

        is AsyncImagePainter.State.Empty,
        is AsyncImagePainter.State.Error -> {
            FallbackPainter()
        }

        is AsyncImagePainter.State.Success -> {
            AsyncImage(
                model = asyncImagePainter,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.DiscFull),
                error = rememberVectorPainter(Icons.Default.Error),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1.0f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
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
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumArtControlPreview_Dark")
fun AlbumArtControlPreview() {
    AlbumArtControl()
}
