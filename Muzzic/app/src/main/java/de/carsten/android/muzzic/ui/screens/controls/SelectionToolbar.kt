package de.carsten.android.muzzic.ui.screens.controls

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE

private val SPACE_WIDTH = 8.dp
private val PADDING_H = 8.dp
private val PADDING_V = 4.dp
private val FONT_SIZE = 16.sp

/**
 * Modes for the floating toolbar.
 */
enum class ToolbarMode {
    /**
     * Displayed when items (artists, albums, songs) are selected.
     */
    SELECTION,

    /**
     * Displayed when viewing the playing queue to manage its state.
     */
    QUEUE_MGMT
}

/**
 * A floating pill-shaped toolbar displayed contextually.
 * Switches between multi-selection actions and queue management actions.
 *
 * @param modifier Modifier for the toolbar container.
 * @param mode The current display mode.
 * @param selectedCount Number of items selected (used in SELECTION mode).
 * @param onConfirm Callback for adding selected items to queue (SELECTION mode).
 * @param onCancel Callback for clearing selection (SELECTION mode).
 * @param onClearQueue Callback for emptying the playing queue (QUEUE_MGMT mode).
 * @param onPersistQueue Callback for saving the queue state to database (QUEUE_MGMT mode).
 */
@Composable
fun SelectionToolbar(
    modifier: Modifier = Modifier,
    mode: ToolbarMode,
    selectedCount: Int = 0,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onPersistQueue: () -> Unit = {},
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
                .padding(horizontal = PADDING_H, vertical = PADDING_V),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (mode) {
                ToolbarMode.SELECTION -> {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                    }
                    Spacer(modifier = Modifier.width(SPACE_WIDTH))
                    Text(
                        text = "$selectedCount selected",
                        fontSize = FONT_SIZE,
                        modifier = Modifier.padding(horizontal = PADDING_H)
                    )
                    Spacer(modifier = Modifier.width(SPACE_WIDTH))
                    IconButton(onClick = onConfirm) {
                        Icon(Icons.Default.Add, contentDescription = "Add to Queue")
                    }
                }

                ToolbarMode.QUEUE_MGMT -> {
                    IconButton(onClick = onClearQueue) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Queue")
                    }
                    Spacer(modifier = Modifier.width(SPACE_WIDTH))
                    Text(
                        text = "Manage",
                        fontSize = FONT_SIZE,
                        modifier = Modifier.padding(horizontal = PADDING_H)
                    )
                    Spacer(modifier = Modifier.width(SPACE_WIDTH))
                    IconButton(onClick = onPersistQueue) {
                        Icon(Icons.Default.Save, contentDescription = "Persist Queue")
                    }
                }
            }
        }
    }
}

@Composable
@Preview()
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SelectionToolbarPreviewSelection() {
    SelectionToolbar(
        mode = ToolbarMode.SELECTION,
        selectedCount = 12
    )
}

@Composable
@Preview()
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SelectionToolbarPreviewQueueMgmt() {
    SelectionToolbar(
        mode = ToolbarMode.QUEUE_MGMT,
    )
}
