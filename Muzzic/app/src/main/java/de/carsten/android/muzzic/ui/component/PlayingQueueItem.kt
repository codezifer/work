package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem

/**
 * A modern and stylish single item in the playing queue.
 */
@Composable
fun PlayingQueueItem(
    item: MediaItem,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    progress: Float,
    isDragging: Boolean,
    isSelected: Boolean,
    dragModifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
) {
    val containerColor = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCurrentSong -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                modifier = dragModifier
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = item.mediaMetadata.title?.toString() ?: "Unknown Title",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${item.mediaMetadata.artist} • ${item.mediaMetadata.albumTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                colors = if (isCurrentSong) {
                    IconButtonDefaults.filledTonalIconButtonColors()
                } else {
                    IconButtonDefaults.iconButtonColors()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentSong && isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isCurrentSong && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(3.dp)
                    .padding(horizontal = 12.dp, vertical = 0.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
