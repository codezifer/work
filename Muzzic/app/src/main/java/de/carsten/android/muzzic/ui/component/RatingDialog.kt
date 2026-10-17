package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * Dialog that lets the user rate a song from 0 (miserable) to 5 (very good)
 * in 0.5 steps by dragging or tapping the star row.
 *
 * Changes are applied immediately via [onRatingChanged]; tapping the already
 * set value clears the rating back to 0. The dialog closes via [onDismiss].
 *
 * @param rating current rating in the range 0f..5f.
 * @param onRatingChanged invoked with the new rating (0f..5f) on every change.
 * @param onDismiss invoked when the dialog is dismissed or confirmed.
 * @param modifier optional [Modifier] for the dialog.
 */
@Composable
fun RatingDialog(rating: Float, onRatingChanged: (Float) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.rating_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = SPACING_MEDIUM),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM),
            ) {
                StarRating(
                    rating = rating,
                    starSize = ICON_SIZE_LARGE,
                    editable = true,
                    onRatingChanged = onRatingChanged,
                    onRatingCleared = { onRatingChanged(0f) },
                )
                Text(
                    text = stringResource(R.string.rating_value_format, rating),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Composable
@Preview(showBackground = true, name = "Light")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark")
fun RatingDialogPreview() {
    AppTheme {
        RatingDialog(
            rating = 3.5f,
            onRatingChanged = {},
            onDismiss = {},
        )
    }
}
