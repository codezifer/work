package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.gradient1Color
import de.carsten.android.muzzic.ui.gradient2Color
import de.carsten.android.muzzic.ui.gradient3Color
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun AlbumCoverCollage(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    val columns = if (covers.size == 4) 2 else covers.size.coerceIn(1, 3)

    AppTheme {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                // modifier = Modifier.fillMaxSize()
            ) {
                items(covers) { cover ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        gradient1Color,
                                        gradient2Color,
                                        gradient3Color
                                    )
                                )
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cover,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "AlbumCoverCollagePreview_Dark")
fun AlbumCoverCollagePreview() {
    AlbumCoverCollage(
        covers = listOf(
            "Test-Cover1",
            "Test-Cover2",
            "Test-Cover3",
            "Test-Cover4",
            "Test-Cover5",
            "Test-Cover6",
            "Test-Cover7",
            "Test-Cover8"
        )
    )
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "AlbumCoverCollagePreview4_Dark")
fun AlbumCoverCollagePreview4() {
    AlbumCoverCollage(
        covers = listOf(
            "Test-Cover1",
            "Test-Cover2",
            "Test-Cover3",
            "Test-Cover4",
        )
    )
}
