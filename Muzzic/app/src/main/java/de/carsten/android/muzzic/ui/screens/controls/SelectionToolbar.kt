package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.ELEVATION_MEDIUM
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.TOOLBAR_MIN_WIDTH
import de.carsten.android.muzzic.ui.component.TooltipIconButton
import de.carsten.android.muzzic.ui.model.ColorSource

private val SPACE_WIDTH = SPACING_MEDIUM
private val PADDING_H = SPACING_MEDIUM
private val PADDING_V = SPACING_SMALL
private val FONT_SIZE = FONT_SIZE_SUBTITLE

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
    QUEUE_MGMT,
}

/**
 * A floating pill-shaped toolbar displayed contextually.
 * Switches between multi-selection actions and queue management actions.
 *
 * @param modifier Modifier for the toolbar container.
 * @param mode The current display mode.
 * @param colorSource Current colors from [ColorSource]
 * @param selectedCount Number of items selected (used in SELECTION mode).
 * @param onConfirm Callback for adding selected items to queue (SELECTION mode).
 * @param onCancel Callback for clearing selection (SELECTION mode).
 * @param onClearQueue Callback for emptying the playing queue (QUEUE_MGMT mode).
 * @param onPersistQueue Callback for saving the queue state to database (QUEUE_MGMT mode).
 * @param onSaveAsPlaylist Callback for saving the queue as a new playlist (QUEUE_MGMT mode).
 */
@Composable
fun SelectionToolbar(
    modifier: Modifier = Modifier,
    mode: ToolbarMode,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
    selectedCount: Int = 0,
    confirmIcon: ImageVector = Icons.Default.Add,
    confirmLabel: String = "Add to Queue",
    onConfirm: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onClearQueue: (String) -> Unit = {},
    onPersistQueue: (String) -> Unit = {},
    onSaveAsPlaylist: (String) -> Unit = {},
) {
    Card(
        modifier =
        modifier
            .wrapContentWidth()
            .widthIn(min = TOOLBAR_MIN_WIDTH)
            .padding(SPACING_LARGE)
            .graphicsLayer {
                // Enable hardware acceleration for smooth visibility transitions
                clip = true
                shape = RoundedCornerShape(50)
            },
        shape = RoundedCornerShape(50),
        colors =
        CardDefaults.cardColors(
            containerColor = colorSource.accentColor,
            contentColor = colorSource.contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_MEDIUM),
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
            },
            label = "ToolbarModeTransition",
        ) { targetMode ->
            Row(
                modifier =
                Modifier
                    .padding(horizontal = PADDING_H, vertical = PADDING_V),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (targetMode) {
                    ToolbarMode.SELECTION -> {
                        TooltipIconButton(
                            onClick = { onCancel("Cancel Selection") },
                            icon = Icons.Default.Close,
                            contentDescription = "Cancel Selection",
                        )
                        Spacer(modifier = Modifier.width(SPACE_WIDTH))
                        Text(
                            text = "$selectedCount selected",
                            fontSize = FONT_SIZE,
                            color = colorSource.labelColor,
                            modifier = Modifier.padding(horizontal = PADDING_H),
                        )
                        Spacer(modifier = Modifier.width(SPACE_WIDTH))
                        TooltipIconButton(
                            onClick = { onConfirm(confirmLabel) },
                            icon = confirmIcon,
                            contentDescription = confirmLabel,
                        )
                    }

                    ToolbarMode.QUEUE_MGMT -> {
                        TooltipIconButton(
                            onClick = { onClearQueue("Clear Queue") },
                            icon = Icons.Default.Delete,
                            contentDescription = "Clear Queue",
                        )
                        Spacer(modifier = Modifier.width(SPACE_WIDTH))
                        TooltipIconButton(
                            onClick = { onSaveAsPlaylist("Save as Playlist") },
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Save as Playlist",
                        )
                        Spacer(modifier = Modifier.width(SPACE_WIDTH))
                        TooltipIconButton(
                            onClick = { onPersistQueue("Persist Queue") },
                            icon = Icons.Default.Save,
                            contentDescription = "Persist Queue",
                        )
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
        selectedCount = 12,
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
