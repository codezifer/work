package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.BORDER_WIDTH_NORMAL
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.ICON_SIZE_FAST_SCROLL_THUMB
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_PREVIEW_PADDING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import kotlinx.coroutines.delay

/**
 * A fast scroller component that displays a vertical alphabet bar.
 * Enables quick navigation through lists (e.g., artists, albums, songs).
 *
 * @param alphabet A list of letters (A-Z) to be displayed.
 * @param onLetterSelected Callback invoked when a letter is selected.
 * @param isScrolling Whether the associated list is currently scrolling.
 * @param modifier Modifier for the outer Box layout.
 * @param activeLetter The letter currently visible in the list (controlled by the parent).
 */
@Composable
fun FastScroller(alphabet: List<String>, onLetterSelected: (String) -> Unit, isScrolling: Boolean, modifier: Modifier = Modifier, activeLetter: String? = null) {
    // Stores the letter currently hovered by the finger while dragging
    var draggingLetter by remember { mutableStateOf<String?>(null) }

    // Controls the visibility of the scroller
    var isVisible by remember { mutableStateOf(false) }

    // Effect to handle visibility with a delay
    LaunchedEffect(isScrolling, draggingLetter) {
        if (isScrolling || draggingLetter != null) {
            isVisible = true
        } else {
            // Wait for 2 seconds after scrolling/dragging stops before hiding
            delay(2000)
            isVisible = false
        }
    }

    // The actual height of the component in pixels to perform the index calculation
    var columnHeight by remember { mutableIntStateOf(0) }

    // Density is needed to correctly convert calculated pixel values to DP
    val density = LocalDensity.current

    // Prioritize the currently dragged letter over the system-reported active letter
    val effectiveLetter = draggingLetter ?: activeLetter
    val letterIndex =
        remember(effectiveLetter, alphabet) {
            alphabet.indexOf(effectiveLetter).coerceAtLeast(-1)
        }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier.fillMaxHeight(),
    ) {
        Box(
            modifier =
            Modifier
                .width(ICON_SIZE_LARGE)
                // Measures the height of the bar once it's placed in the layout
                .onGloballyPositioned { columnHeight = it.size.height }
                // Handles simple tapping on a letter
                .pointerInput(alphabet) {
                    detectTapGestures { offset ->
                        if (columnHeight > 0) {
                            val index =
                                (offset.y / columnHeight * alphabet.size)
                                    .toInt()
                                    .coerceIn(0, alphabet.size - 1)
                            onLetterSelected(alphabet[index])
                        }
                    }
                }
                // Handles the swipe gesture (drag) along the bar
                .pointerInput(alphabet) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (columnHeight > 0) {
                                val index =
                                    (offset.y / columnHeight * alphabet.size)
                                        .toInt()
                                        .coerceIn(0, alphabet.size - 1)
                                draggingLetter = alphabet[index]
                                onLetterSelected(alphabet[index])
                            }
                        },
                        onDragEnd = { draggingLetter = null },
                        onDragCancel = { draggingLetter = null },
                        onDrag = { change, _ ->
                            if (columnHeight > 0) {
                                val index =
                                    (change.position.y / columnHeight * alphabet.size)
                                        .toInt()
                                        .coerceIn(0, alphabet.size - 1)
                                val letter = alphabet[index]
                                // Only update if the letter under the finger has changed
                                if (draggingLetter != letter) {
                                    draggingLetter = letter
                                    onLetterSelected(letter)
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            // The "handle" (colored indicator) showing the current position
            if (letterIndex != -1 && alphabet.isNotEmpty() && columnHeight > 0) {
                val itemHeight = columnHeight.toFloat() / alphabet.size
                // Calculate the center of the letter in pixels
                val handleOffsetPx = (letterIndex * itemHeight) + (itemHeight / 2)
                // Convert pixels to DP for the modifier.offset
                val handleOffsetDp = with(density) { handleOffsetPx.toDp() }

                // Smoothly animates the handle movement between positions
                val animatedOffset by animateDpAsState(
                    targetValue = handleOffsetDp - SPACING_NORMAL, // -12dp to center the circle (24dp)
                    label = "handleOffset",
                )

                Box(
                    modifier =
                    Modifier
                        .offset(y = animatedOffset)
                        .padding(horizontal = SPACING_MEDIUM)
                        .width(ICON_SIZE_MEDIUM)
                        .height(ICON_SIZE_MEDIUM)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                )
            }

            // The vertical list of letters
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                Modifier
                    .fillMaxHeight()
                    .border(
                        width = BORDER_WIDTH_NORMAL,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                    ),
            ) {
                alphabet.forEach { letter ->
                    FastScrollerLetter(
                        letter = letter,
                        isActive = effectiveLetter == letter,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Large letter preview (bubble) that appears to the left of the bar while dragging
            draggingLetter?.let { letter ->
                Box(
                    modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = -INDICATOR_SLIDER_PREVIEW_PADDING) // Positioning to the left of the FastScroller
                        .size(ICON_SIZE_FAST_SCROLL_THUMB)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.inversePrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun FastScrollerLetter(letter: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .width(ICON_SIZE_LARGE),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            fontSize = FONT_SIZE_CAPTION,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color =
            if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.6f,
                )
            },
        )
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "FastScroller_Dark")
fun FastScrollerPreview() {
    FastScroller(
        alphabet = listOf("A", "B", "C"),
        onLetterSelected = {},
        isScrolling = true,
        activeLetter = "C",
    )
}
