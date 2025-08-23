package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.viewmodel.StatisticsViewModel

@Composable
fun SongStatsCard(viewModel: StatisticsViewModel) {
    val songPlayCountsState = viewModel.topSongs.observeAsState(emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Top 5 Songs diesen Monat",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val demoTopSongs = listOf(
                "Stairway to Heaven" to "Led Zeppelin" to 87 to 5,
                "Hotel California" to "Eagles" to 76 to 5,
                "Bohemian Rhapsody" to "Queen" to 72 to 5,
                "Sweet Child O' Mine" to "Guns N' Roses" to 65 to 4,
                "November Rain" to "Guns N' Roses" to 58 to 4
            )

            demoTopSongs.forEachIndexed { index, songData ->
                val (titleArtistAndPlayCount, rating) = songData
                val (titleAndArtist, playCount) = titleArtistAndPlayCount
                val (title, artist) = titleAndArtist

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = Color(0xFF8B5CF6),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = artist,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$playCount Plays",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        StarRating(
                            rating = rating,
                            onRatingChanged = { },
                            size = 10.dp
                        )
                    }
                }
            }
        }
    }
}
