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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.BORDER_WIDTH_NORMAL
import de.carsten.android.muzzic.ui.BULLET_POINT
import de.carsten.android.muzzic.ui.ELEVATION_FAST_SCROLL_IDLE
import de.carsten.android.muzzic.ui.ELEVATION_MEDIUM
import de.carsten.android.muzzic.ui.FAST_SCROLL_HIDE_DELAY_MS
import de.carsten.android.muzzic.ui.FAST_SCROLL_ITEM_MIN_HEIGHT
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.ICON_SIZE_FAST_SCROLL_THUMB
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_PREVIEW_PADDING
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_BACKGROUND
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_BORDER
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_HANDLE
import de.carsten.android.muzzic.ui.OPACITY_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.utils.contrastColor
import kotlin.math.ceil
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
 * @param colorSource current colors from selected album art
 */
@Composable
fun FastScroller(
    alphabet: List<String>,
    onLetterSelected: (String) -> Unit,
    isScrolling: Boolean,
    modifier: Modifier = Modifier,
    activeLetter: String? = null,
    colorSource: ColorSource = composableColorSource(),
) {
    val haptic = LocalHapticFeedback.current

    // Stores the letter currently hovered by the finger while dragging
    var draggingLetter by remember { mutableStateOf<String?>(null) }

    // Controls the visibility of the scroller
    var isVisible by remember { mutableStateOf(false) }

    // Effect to handle visibility with a delay
    LaunchedEffect(isScrolling, draggingLetter) {
        if (isScrolling || draggingLetter != null) {
            isVisible = true
        } else {
            // Wait before hiding after scrolling/dragging stops
            delay(FAST_SCROLL_HIDE_DELAY_MS)
            isVisible = false
        }
    }

    // The actual height of the component in pixels to perform the index calculation
    var columnHeight by remember { mutableIntStateOf(0) }

    // Density is needed to correctly convert calculated pixel values to DP
    val density = LocalDensity.current

    // Prioritize the currently dragged letter over the system-reported active letter
    val effectiveLetter = draggingLetter ?: activeLetter

    val step =
        remember(alphabet.size, columnHeight) {
            if (columnHeight > 0) {
                val columnHeightDp = with(density) { columnHeight.toDp() }
                // Minimum height per displayed item (letter or dot) to avoid overlap
                val maxVisibleItems = (columnHeightDp / FAST_SCROLL_ITEM_MIN_HEIGHT).toInt().coerceAtLeast(1)
                // Compress the alphabet to letters + dots whenever not every letter fits comfortably
                ceil(alphabet.size.toFloat() / maxVisibleItems).toInt().coerceAtLeast(1)
            } else {
                1
            }
        }

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
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    // Half the handle diameter to center the circle on the letter center
                    targetValue = handleOffsetDp - ICON_SIZE_MEDIUM / 2,
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
                        .background(colorSource.accentColor.copy(alpha = OPACITY_FAST_SCROLL_HANDLE)),
                )
            }

            // The vertical list of letters
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                Modifier
                    .padding(vertical = SPACING_MEDIUM)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (draggingLetter != null) ELEVATION_MEDIUM else ELEVATION_FAST_SCROLL_IDLE,
                        shape = RoundedCornerShape(50),
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = OPACITY_FAST_SCROLL_BACKGROUND),
                        shape = RoundedCornerShape(50),
                    )
                    .border(
                        width = BORDER_WIDTH_NORMAL,
                        color = colorSource.accentColor.copy(alpha = OPACITY_FAST_SCROLL_BORDER),
                        shape = RoundedCornerShape(50),
                    ),
            ) {
                alphabet.forEachIndexed { index, letter ->
                    val isFirst = index == 0
                    val isLast = index == alphabet.size - 1
                    val isStepMatch = index % step == 0
                    val isDotPosition = !isStepMatch && (index % step == step / 2)

                    val displayText =
                        when {
                            isFirst || isLast || isStepMatch -> letter
                            isDotPosition && step > 1 -> BULLET_POINT
                            else -> ""
                        }

                    FastScrollerLetter(
                        letter = displayText,
                        isActive = effectiveLetter == letter,
                        modifier = Modifier.weight(1f),
                        colorSource = colorSource,
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
                        .shadow(ELEVATION_MEDIUM, CircleShape)
                        .clip(CircleShape)
                        .background(colorSource.accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorSource.accentColor.contrastColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FastScrollerLetter(letter: String, isActive: Boolean, modifier: Modifier = Modifier, colorSource: ColorSource = composableColorSource()) {
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
                colorSource.accentColor
            } else {
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = OPACITY_MEDIUM,
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
