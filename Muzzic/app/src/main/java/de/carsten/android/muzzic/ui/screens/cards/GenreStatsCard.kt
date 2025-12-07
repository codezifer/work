package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
fun GenreStatsCard(viewModel: StatisticsViewModel) {
    val genrePlayCountsState = viewModel.genreStats.observeAsState(emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Genre-Verteilung",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            val genres =
                listOf(
                    "Rock" to 35 to Color(0xFF8884D8),
                    "Pop" to 25 to Color(0xFF82CA9D),
                    "Jazz" to 20 to Color(0xFFE0E0E0),
                    "Electronic" to 15 to Color(0xFFFF7C7C),
                    "Classical" to 5 to Color(0xFF8DD1E1),
                )

            genres.forEach { (pair, color) ->
                val (name, percentage) = pair

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(12.dp)
                                .background(color, CircleShape),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = "$percentage%",
                        color = Color.Gray,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
