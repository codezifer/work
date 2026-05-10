package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A floating pill-shaped toolbar displayed during multi-selection mode.
 * Shows the number of selected items and provides actions to confirm or cancel selection.
 *
 * @param selectedCount The total number of items currently selected.
 * @param onConfirm Callback invoked when the user confirms the selection to add items to the queue.
 * @param onCancel Callback invoked when the user cancels the selection mode.
 * @param modifier Modifier for the toolbar container.
 */
@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .widthIn(min = 200.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$selectedCount selected",
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onConfirm) {
                Icon(Icons.Default.Add, contentDescription = "Add to Queue")
            }
        }
    }
}
