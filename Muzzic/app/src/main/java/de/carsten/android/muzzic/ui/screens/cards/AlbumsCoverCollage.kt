package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors

/**
 * A collage of album covers displayed in a grid.
 *
 * @param covers The list of [CoverSource]s to display in the collage. Up to 9 covers will be shown.
 * @param modifier The modifier to be applied to the layout.
 * @param useCard Whether to wrap the collage in a [Card]. Defaults to true.
 */
@Composable
fun AlbumCoverCollage(covers: List<CoverSource>, modifier: Modifier = Modifier, useCard: Boolean = true) {
    val displayedCovers = remember(covers) { covers.take(9) }

    // Logic to determine grid dimensions for a square look
    val (rows, columns) =
        remember(displayedCovers.size) {
            when (displayedCovers.size) {
                1 -> 1 to 1
                2 -> 1 to 2
                3, 4 -> 2 to 2
                5, 6 -> 2 to 3
                else -> 3 to 3
            }
        }

    val g1 = CustomColors.gradient1
    val g2 = CustomColors.gradient2
    val g3 = CustomColors.gradient3
    val placeholderBrush =
        remember(g1, g2, g3) {
            Brush.linearGradient(
                colors =
                listOf(
                    g1,
                    g2,
                    g3,
                ),
            )
        }

    val content = @Composable {
        if (displayedCovers.isEmpty()) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(placeholderBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Placeholder")
            }
        } else {
            CollageGrid(displayedCovers, rows, columns)
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

/**
 * Renders the grid layout for the collage.
 */
@Composable
private fun CollageGrid(displayedCovers: List<CoverSource>, rows: Int, columns: Int) {
    Column(modifier = Modifier.fillMaxSize()) {
        val chunkedCovers =
            remember(displayedCovers, columns) {
                displayedCovers.chunked(columns)
            }
        chunkedCovers.forEach { rowCovers ->
            Row(modifier = Modifier.weight(1f)) {
                rowCovers.forEach { cover ->
                    CollageItem(
                        cover = cover,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill empty slots in the last row if necessary to maintain layout
                if (rowCovers.size < columns) {
                    repeat(columns - rowCovers.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // If there are fewer rows than planned, add empty rows to keep square proportion
        if (chunkedCovers.size < rows) {
            repeat(rows - chunkedCovers.size) {
                Row(modifier = Modifier.weight(1f)) {
                    repeat(columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Renders a single item within the collage grid.
 */
@Composable
private fun CollageItem(cover: CoverSource, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        modifier
            .fillMaxSize()
            .border(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        when (cover) {
            is CoverSource.FromPath -> {
                AsyncImage(
                    model = cover.path,
                    contentDescription = stringResource(R.string.album_art),
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

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumCoverCollagePreview_Dark")
fun AlbumCoverCollagePreview() {
    AppTheme {
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
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumCoverCollagePreview4_Dark")
fun AlbumCoverCollagePreview4x4() {
    AppTheme {
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
}
