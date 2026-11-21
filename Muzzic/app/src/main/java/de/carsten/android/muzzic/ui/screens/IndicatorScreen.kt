package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.BORDER_WIDTH_THIN
import de.carsten.android.muzzic.ui.CARD_CORNER_RADIUS
import de.carsten.android.muzzic.ui.FONT_SIZE_HUGE_TITLE
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.contrastColor

/**
 * Grid of letter tiles used for quick navigation.
 *
 * @param letters Letters in encounter order of the underlying list.
 * @param itemCounts Number of items per letter, shown as a subtitle when present.
 * @param onLetterClick Invoked with the selected letter.
 * @param modifier Modifier for the grid layout.
 * @param colorSource Colors from the selected album art.
 */
@Composable
fun IndicatorScreen(
    letters: List<String>,
    itemCounts: Map<String, Int> = emptyMap(),
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(AppConfig.Ui.NUM_OF_INDICATOR_COLUMNS),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = GRID_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
    ) {
        items(
            items = letters,
            key = { it },
            contentType = { "indicator_letter" },
        ) { letter ->
            IndicatorTile(
                letter = letter,
                itemCount = itemCounts[letter],
                onClick = { onLetterClick(letter) },
                colorSource = colorSource,
            )
        }
    }
}

@Composable
private fun IndicatorTile(letter: String, itemCount: Int?, onClick: () -> Unit, colorSource: ColorSource) {
    val countLabel = itemCount?.let { pluralStringResource(R.plurals.indicator_item_count, it, it) }
    val description = if (countLabel == null) letter else "$letter, $countLabel"

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = colorSource.accentColor),
        border = BorderStroke(BORDER_WIDTH_THIN, colorSource.contentColor),
    ) {
        Column(
            // The card already announces letter and count; keep children out of the semantics tree
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = letter,
                color = colorSource.accentColor.contrastColor(),
                fontSize = FONT_SIZE_HUGE_TITLE,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (countLabel != null) {
                Text(
                    text = countLabel,
                    color = colorSource.accentColor.contrastColor(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun IndicatorScreenPreview() {
    AppTheme {
        IndicatorScreen(
            letters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"),
            itemCounts = mapOf("A" to 12, "B" to 3, "C" to 1),
            onLetterClick = {},
        )
    }
}
