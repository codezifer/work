package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.gradient1Color
import de.carsten.android.muzzic.ui.gradient2Color
import de.carsten.android.muzzic.ui.gradient3Color
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun AlbumCoverCollage(
    covers: List<CoverSource>,
    modifier: Modifier = Modifier,
    useCard: Boolean = true,
) {
    val columns = if (covers.size == 4) 2 else covers.size.coerceIn(1, 3)

    AppTheme {
        val content = @Composable {
            if (covers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        gradient1Color,
                                        gradient2Color,
                                        gradient3Color,
                                    ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Placeholder")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(covers) { cover ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            when (cover) {
                                is CoverSource.FromPath -> {
                                    AsyncImage(
                                        model = cover.path,
                                        contentDescription = "Album Art",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(R.drawable.disc),
                                        error = painterResource(R.drawable.disc),
                                    )
                                }

                                is CoverSource.FromVector -> {
                                    Icon(
                                        imageVector = cover.imageVector,
                                        contentDescription = "Placeholder Icon",
                                        modifier = Modifier.fillMaxSize(0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (useCard) {
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(8.dp),
            ) {
                content()
            }
        } else {
            Box(modifier = modifier) {
                content()
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumCoverCollagePreview_Dark")
fun AlbumCoverCollagePreview() {
    AlbumCoverCollage(
        covers =
            listOf(
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
            ),
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumCoverCollagePreview4_Dark")
fun AlbumCoverCollagePreview4x4() {
    AlbumCoverCollage(
        covers =
            listOf(
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
                CoverSource.FromVector(Icons.Default.MusicNote),
            ),
    )
}
