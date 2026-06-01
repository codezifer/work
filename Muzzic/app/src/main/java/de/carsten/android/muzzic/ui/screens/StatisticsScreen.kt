package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.cards.GenreStatsCard
import de.carsten.android.muzzic.ui.screens.cards.MonthlyStatsCard
import de.carsten.android.muzzic.ui.screens.cards.SongStatsCard
import de.carsten.android.muzzic.viewmodel.StatisticsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier, appState: MusicAppState, viewModel: StatisticsViewModel = koinViewModel()) {
    LazyColumn(
        modifier =
        modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Text(
                text = stringResource(R.string.statistics),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        // Monthly stats. chart
        item {
            MonthlyStatsCard(viewModel)
        }

        // Genre distribution
        item {
            GenreStatsCard(viewModel)
        }

        // Top songs
        item {
            SongStatsCard(viewModel)
        }
    }
}
