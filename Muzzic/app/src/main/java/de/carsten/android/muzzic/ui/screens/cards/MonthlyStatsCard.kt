package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.viewmodel.StatisticsViewModel

@Composable
fun MonthlyStatsCard(viewModel: StatisticsViewModel) {
    val monthlyPlayCountsState = viewModel.monthlyStats.observeAsState(emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.monthly_plays),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )

            // simple row chart
            val months = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun")
            val plays = listOf(420, 380, 510, 290, 450, 380)
            val maxPlays = plays.maxOrNull() ?: 1

            months.forEachIndexed { index, month ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val barHeight = (plays[index].toFloat() / maxPlays * 80).dp

                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight)
                            .background(
                                Color(0xFF8B5CF6),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = month,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
