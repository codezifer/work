package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.BuildConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.CARD_CORNER_RADIUS
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatTime

/**
 * A dialog that displays information about the application.
 *
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.about_muzzic),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SPACING_MEDIUM),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.muzzic_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(CARD_CORNER_RADIUS)),
                )

                Spacer(modifier = Modifier.height(SPACING_LARGE))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(SPACING_SMALL))

                Text(
                    text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = stringResource(R.string.build_date_format, formatTime(BuildConfig.BUILD_TIME)), // Placeholder date
                    style = MaterialTheme.typography.bodyMedium,
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
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun AboutDialogPreview() {
    AppTheme {
        AboutDialog()
    }
}
