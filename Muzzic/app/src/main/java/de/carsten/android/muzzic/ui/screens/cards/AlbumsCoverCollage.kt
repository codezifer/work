package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.borderColor
import de.carsten.android.muzzic.ui.containerColor
import de.carsten.android.muzzic.ui.gradient1Color
import de.carsten.android.muzzic.ui.gradient2Color
import de.carsten.android.muzzic.ui.gradient3Color

@Composable
fun AlbumCoverCollage(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize()
        ) {
            items(minOf(covers.size, 9)) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(gradient1Color, gradient2Color, gradient3Color)
                            )
                        )
                        .border(0.5.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = covers[index],
                        fontSize = 12.sp
                    )
                }
            }

            // Fill empty spaces
            items(9 - minOf(covers.size, 9)) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color(0xFF4B5563))
                        .border(0.5.dp, Color(0xFF374151)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
