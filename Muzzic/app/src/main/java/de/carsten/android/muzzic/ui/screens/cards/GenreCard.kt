package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.MAINTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlin.math.absoluteValue

@Composable
fun GenreCard(genre: GenreDto) {
    val colors =
        listOf(
            listOf(Color(0xFFEF4444), Color(0xFFF97316)),
            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
            listOf(Color(0xFF3B82F6), Color(0xFF4F46E5)),
            listOf(Color(0xFF10B981), Color(0xFF059669)),
            listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        )

    val colorPair = colors[genre.hashCode().absoluteValue % colors.size]

    AppTheme {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(colorPair),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = genre.genreName,
                            color = Color.White,
                            fontSize = MAINTITLE_FONTSIZE,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SubtitleInformation(
                    listOf(
                        Pair(Icons.Default.Person, "${genre.artistCount} Artists"),
                        Pair(Icons.Default.Album, "${genre.albumCount} Albums"),
                        Pair(Icons.Default.MusicNote, "${genre.songCount} Songs"),
                    ),
                    fontColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "GenreCardPreview_Dark")
fun GenreCardPreview() {
    GenreCard(
        GenreDto(
            genreName = "Black Metal",
            artistCount = 60,
            albumCount = 450,
            songCount = 4200,
            genreDuration = 24 * 60 * 60 * 1000L,
            lastAlbumArt = null,
        ),
    )
}
