package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.PlayingQueueDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import java.util.UUID

/**
 * A modern and stylish single item in the playing queue.
 */
@Composable
fun PlayingQueueItem(
    playingQueueDto: PlayingQueueDto,
    isPlaying: Boolean = false,
    isCurrentSong: Boolean = false,
    progress: Float = 0f,
    isDragging: Boolean = false,
    isSelected: Boolean = false,
    dragModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
) {
    val containerColor =
        when {
            isDragging -> MaterialTheme.colorScheme.surfaceVariant
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isCurrentSong -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                modifier =
                    dragModifier
                        .size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = playingQueueDto.title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                        ),
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = if (playingQueueDto.trackNumber > 0) "${playingQueueDto.trackNumber}. ${playingQueueDto.artist} • ${playingQueueDto.album}" else "${playingQueueDto.artist} • ${playingQueueDto.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            IconButton(
                onClick = onTogglePlayPause,
                colors =
                    if (isCurrentSong) {
                        IconButtonDefaults.filledTonalIconButtonColors()
                    } else {
                        IconButtonDefaults.iconButtonColors()
                    },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentSong && isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (isCurrentSong && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(3.dp)
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun PlayingQueueItemPausedPreview() {
    AppTheme {
        PlayingQueueItem(
            playingQueueDto = PlayingQueueDto(
                title = "Puritania",
                trackNumber = 5,
                totalTracks = 12,
                artist = "Dimmu Borgir",
                album = "Puritanical Euphoric Mesantrophia",
                genre = "Black Metal",
                queuePosition = 1,
                duration = 210000L,
                enqueued = true,
                songId = UUID.randomUUID().toString(),
                mediaId = UUID.randomUUID().toString(),
            ),
            isPlaying = false,
            isCurrentSong = true,
            isSelected = true,
            isDragging = false,
            progress = 0.6f,
        )
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun PlayingQueueItemPlayingPreview() {
    AppTheme {
        PlayingQueueItem(
            playingQueueDto = PlayingQueueDto(
                title = "Puritania",
                trackNumber = 5,
                totalTracks = 12,
                artist = "Dimmu Borgir",
                album = "Puritanical Euphoric Mesantrophia",
                genre = "Black Metal",
                queuePosition = 1,
                duration = 210000L,
                enqueued = true,
                songId = UUID.randomUUID().toString(),
                mediaId = UUID.randomUUID().toString(),
            ),
            isPlaying = true,
            isCurrentSong = false,
            isSelected = false,
            isDragging = false,
            progress = 0.6f,
        )
    }
}
