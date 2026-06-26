package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.BULLET_POINT
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SUBTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.SUBTITLE_ICONSIZE
import de.carsten.android.muzzic.ui.SUBTITLE_ICON_TEXT_SPACING
import de.carsten.android.muzzic.ui.SUBTITLE_SPACING
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun SubtitleInformation(iconTextPairs: List<Pair<ImageVector, String>>, fontColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SUBTITLE_SPACING, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(SUBTITLE_SPACING),
    ) {
        iconTextPairs.forEachIndexed { idx, (img, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = img,
                    tint = fontColor,
                    modifier = Modifier.size(SUBTITLE_ICONSIZE),
                    contentDescription = "icon",
                )
                Spacer(modifier = Modifier.width(SUBTITLE_ICON_TEXT_SPACING))
                Text(
                    text = text,
                    color = fontColor,
                    fontSize = SUBTITLE_FONTSIZE,
                )
            }

            if (idx < iconTextPairs.size - 1) {
                Text(
                    text = BULLET_POINT,
                    color = fontColor,
                    fontSize = SUBTITLE_FONTSIZE,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SubtitleInformationPreview() {
    AppTheme {
        SubtitleInformation(
            listOf(
                Pair(Icons.Default.Person, "50 Artists"),
                Pair(Icons.Default.Album, "500 Albums"),
                Pair(Icons.Default.MusicNote, "5000 Songs"),
            ),
        )
    }
}

@Composable
@Preview(widthDp = 300)
@Preview(widthDp = 300, uiMode = PREVIEW_DARK_MODE)
fun SubtitleInformation300WidthPreview() {
    AppTheme {
        SubtitleInformation(
            listOf(
                Pair(Icons.Default.Person, "50 Artists"),
                Pair(Icons.Default.Album, "500 Albums"),
                Pair(Icons.Default.MusicNote, "5000 Songs"),
                Pair(Icons.Default.Person, "50 Extra Artists"),
                Pair(Icons.Default.Album, "500 Extra Albums"),
                Pair(Icons.Default.MusicNote, "5000 Extra Songs"),
            ),
        )
    }
}

@Composable
@Preview(widthDp = 100)
@Preview(widthDp = 100, uiMode = PREVIEW_DARK_MODE)
fun SubtitleInformation100WidthPreview() {
    AppTheme {
        SubtitleInformation(
            listOf(
                Pair(Icons.Default.Person, "50 Artists"),
                Pair(Icons.Default.Album, "500 Albums"),
                Pair(Icons.Default.MusicNote, "5000 Songs"),
                Pair(Icons.Default.Person, "50 Extra Artists"),
                Pair(Icons.Default.Album, "500 Extra Albums"),
                Pair(Icons.Default.MusicNote, "5000 Extra Songs"),
            ),
        )
    }
}
