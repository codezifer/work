package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.SPACING_MEDIUM

/**
 * A reusable dialog for capturing text input from the user.
 *
 * @param title The title of the dialog.
 * @param onConfirm Callback invoked when the user confirms the input.
 * @param onDismiss Callback invoked when the dialog is dismissed or cancelled.
 * @param initialValue Optional initial text for the input field.
 * @param label Optional label for the input field.
 * @param confirmLabel Label for the confirmation button. Defaults to [R.string.dialog_confirm].
 * @param dismissLabel Label for the dismiss button. Defaults to [R.string.cancel].
 */
@Composable
fun TextInputDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
    label: String = "",
    confirmLabel: String? = null,
    dismissLabel: String? = null,
) {
    var text by remember { mutableStateOf(initialValue) }
    val resolvedConfirmLabel = confirmLabel ?: stringResource(R.string.dialog_confirm)
    val resolvedDismissLabel = dismissLabel ?: stringResource(R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.padding(top = SPACING_MEDIUM)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { if (label.isNotEmpty()) Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(resolvedConfirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(resolvedDismissLabel)
            }
        },
    )
}
