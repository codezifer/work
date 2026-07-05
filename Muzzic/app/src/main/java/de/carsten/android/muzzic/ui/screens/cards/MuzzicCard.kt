package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.CARD_CORNER_RADIUS
import de.carsten.android.muzzic.ui.CARD_INTERNAL_PADDING
import de.carsten.android.muzzic.ui.CARD_SELECTION_ICON_PADDING
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.ICON_SIZE_SMALL
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.screens.controls.PlayButton
import de.carsten.android.muzzic.ui.theme.AppTheme
import androidx.compose.material3.Text

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuzzicCard(
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    contentHeight: Dp? = null,
    content: @Composable (contentColor: Color) -> Unit,
) {
    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER_RADIUS))
            .combinedClickable(
                onClick = onClick ?: {},
                onLongClick = onLongClick,
            ),
        colors =
        CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                header?.let {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        it()

                        if (onPlayClick != null && !isSelected) {
                            PlayButton(
                                onClick = onPlayClick,
                                borderColor = borderColor,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(SPACING_MEDIUM),
                            )
                        }
                    }
                }
                Column(
                    modifier =
                    Modifier
                        .padding(CARD_INTERNAL_PADDING)
                        .fillMaxWidth()
                        .then(
                            if (contentHeight != null) {
                                Modifier.height(contentHeight)
                            } else {
                                Modifier
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    content(contentColor)
                }
            }

            if (isSelected) {
                // Overlay for selected state
                Box(
                    modifier =
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                )

                // Check symbol
                Box(
                    modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(CARD_SELECTION_ICON_PADDING)
                        .size(ICON_SIZE_MEDIUM)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(ICON_SIZE_SMALL),
                    )
                }
            }
        }
    }
}

@Composable
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun MuzzicCardPreview() {
    AppTheme {
        MuzzicCard(
            modifier = Modifier.padding(16.dp),
            onClick = {},
            onPlayClick = {}
        ) {
            Text(
                text = "Muzzic Card Content",
                style = MaterialTheme.typography.bodyLarge,
                color = it
            )
        }
    }
}
