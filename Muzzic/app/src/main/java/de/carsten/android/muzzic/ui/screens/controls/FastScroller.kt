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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * A fast scroller component that displays a vertical alphabet bar.
 * Enables quick navigation through lists (e.g., artists, albums, songs).
 *
 * When the available height cannot show every letter at [FAST_SCROLL_ITEM_MIN_HEIGHT], the
 * alphabet is progressively compressed to evenly spread letters (first and last always included)
 * with bullet dots in between; the letter font shrinks as a final fallback, so letters are never
 * partially covered at any screen height.
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
            delay(FAST_SCROLL_HIDE_DELAY_MS.milliseconds)
            isVisible = false
        }
    }

    // The actual height of the component in pixels to perform the index calculation
    var columnHeight by remember { mutableIntStateOf(0) }

    // Density is needed to correctly convert calculated pixel values to DP
    val density = LocalDensity.current

    // Prioritize the currently dragged letter over the system-reported active letter
    val effectiveLetter = draggingLetter ?: activeLetter

    // Layout model: which slots are actually rendered for the available height. The pointer
    // mapping stays proportional to the full alphabet, so every letter remains selectable
    // even when this model compresses the bar.
    val displayItems =
        remember(alphabet, columnHeight) {
            when {
                alphabet.isEmpty() -> emptyList()

                columnHeight > 0 -> {
                    val usableHeightDp =
                        with(density) { (columnHeight - 2 * SPACING_MEDIUM.toPx()).toDp() }
                    buildFastScrollerDisplayItems(
                        alphabet = alphabet,
                        // Minimum height per rendered slot (letter or dot) to avoid overlap
                        maxVisibleItems = (usableHeightDp / FAST_SCROLL_ITEM_MIN_HEIGHT).toInt(),
                    )
                }

                // Fallback before the first measurement: render one slot per letter
                else -> alphabet.map { FastScrollerDisplayItem.Letter(it) }
            }
        }

    // Dynamically shrink the letter font so its line box always fits into one slot. The cap keeps
    // the regular caption size whenever the slots are high enough.
    val letterFontSize =
        if (columnHeight > 0 && displayItems.isNotEmpty()) {
            with(density) {
                val slotHeight = (columnHeight - 2 * SPACING_MEDIUM.toPx()) / displayItems.size
                val fittingFontSize = slotHeight.toDp().toSp()
                if (fittingFontSize.value < FONT_SIZE_CAPTION.value) fittingFontSize else FONT_SIZE_CAPTION
            }
        } else {
            FONT_SIZE_CAPTION
        }

    val letterIndex =
        remember(effectiveLetter, alphabet) {
            alphabet.indexOf(effectiveLetter).coerceAtLeast(-1)
        }

    // Maps a pointer y-position onto its alphabet index. The mapping stays proportional to the
    // full alphabet, so dragging across a compressed bar still selects every intermediate letter.
    fun letterIndexAt(pointerY: Float): Int {
        if (columnHeight <= 0 || alphabet.isEmpty()) return -1
        val topPaddingPx = with(density) { SPACING_MEDIUM.toPx() }
        val usableHeightPx = (columnHeight - 2 * topPaddingPx).coerceAtLeast(1f)
        val fraction = ((pointerY - topPaddingPx) / usableHeightPx).coerceIn(0f, 1f)
        return (fraction * (alphabet.size - 1)).roundToInt()
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
                        letterIndexAt(offset.y)
                            .takeIf { it >= 0 }
                            ?.let { onLetterSelected(alphabet[it]) }
                    }
                }
                // Handles the swipe gesture (drag) along the bar
                .pointerInput(alphabet) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            letterIndexAt(offset.y)
                                .takeIf { it >= 0 }
                                ?.let { index ->
                                    draggingLetter = alphabet[index]
                                    onLetterSelected(alphabet[index])
                                }
                        },
                        onDragEnd = { draggingLetter = null },
                        onDragCancel = { draggingLetter = null },
                        onDrag = { change, _ ->
                            letterIndexAt(change.position.y)
                                .takeIf { it >= 0 }
                                ?.let { index ->
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
                // Position the handle proportionally to the full alphabet — mirroring the pointer
                // mapping, so it follows the finger exactly even across compressed dot slots
                val handleOffsetPx =
                    with(density) {
                        val topPaddingPx = SPACING_MEDIUM.toPx()
                        val usableHeightPx = (columnHeight - 2 * topPaddingPx).coerceAtLeast(1f)
                        val letterFraction =
                            if (alphabet.size > 1) letterIndex.toFloat() / (alphabet.size - 1) else 0.5f
                        topPaddingPx + letterFraction * usableHeightPx
                    }
                // Convert pixels to DP for the modifier.offset, half the handle diameter to center
                // the circle on the letter position
                val handleOffsetDp = with(density) { (handleOffsetPx - ICON_SIZE_MEDIUM.toPx() / 2).toDp() }

                // Smoothly animates the handle movement between positions
                val animatedOffset by animateDpAsState(
                    targetValue = handleOffsetDp,
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
                    )
                    .padding(vertical = SPACING_MEDIUM),
            ) {
                // Every rendered slot gets an equal share; the display model never produces more
                // slots than fit the minimum item height, so letters are never clipped
                displayItems.forEach { item ->
                    FastScrollerLetter(
                        letter =
                        when (item) {
                            is FastScrollerDisplayItem.Letter -> item.value
                            FastScrollerDisplayItem.Dot -> BULLET_POINT
                        },
                        fontSize = letterFontSize,
                        isActive = item is FastScrollerDisplayItem.Letter && effectiveLetter == item.value,
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

/**
 * Renders a single FastScroller slot (a letter or a bullet dot).
 *
 * The line box is forced to the font size itself, so the slot can never clip the glyph, even when
 * the font is dynamically shrunk on short screens.
 *
 * @param letter The character to render (a letter or [BULLET_POINT]).
 * @param fontSize Dynamically fitted font size (never taller than the slot).
 * @param isActive Whether this letter is currently the active one (bold + accent color).
 * @param modifier Modifier for the letter's slot box.
 * @param colorSource current colors from selected album art
 */
@Composable
private fun FastScrollerLetter(letter: String, fontSize: TextUnit, isActive: Boolean, modifier: Modifier = Modifier, colorSource: ColorSource = composableColorSource()) {
    Box(
        modifier =
        modifier
            .width(ICON_SIZE_LARGE),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            fontSize = fontSize,
            lineHeight = fontSize,
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

/**
 * A single rendered slot in the [FastScroller] bar: an alphabet letter or a bullet dot filler.
 */
private sealed interface FastScrollerDisplayItem {
    /** A rendered alphabet letter. */
    data class Letter(val value: String) : FastScrollerDisplayItem

    /** A decorative bullet dot between two displayed letters. */
    data object Dot : FastScrollerDisplayItem
}

/**
 * Builds the slot model rendered by the [FastScroller] for the available bar height.
 *
 * If [maxVisibleItems] slots cannot fit one alphabet letter each, the alphabet is compressed to
 * evenly spread letters (first and last letter always included) alternating with bullet dots, so
 * every rendered slot keeps the minimum item height. Every alphabet letter stays selectable via
 * the proportional pointer mapping.
 *
 * @param alphabet The full alphabet to display.
 * @param maxVisibleItems Number of slots that fit into the available height (>= 1).
 * @return The slots in top-to-bottom order.
 */
private fun buildFastScrollerDisplayItems(alphabet: List<String>, maxVisibleItems: Int): List<FastScrollerDisplayItem> {
    if (alphabet.isEmpty()) return emptyList()

    // Compress to roughly half the slot budget: every second slot becomes a bullet dot
    val letterCount = if (maxVisibleItems >= alphabet.size) alphabet.size else (maxVisibleItems + 1) / 2

    val letterIndices =
        when {
            letterCount <= 1 -> listOf(alphabet.size / 2)

            letterCount == alphabet.size -> alphabet.indices.toList()

            // Evenly spread the letters, always keeping the first and the last letter
            else ->
                (0 until letterCount).map { slot ->
                    (slot.toDouble() * (alphabet.size - 1) / (letterCount - 1)).roundToInt()
                }
        }

    val letters = letterIndices.map { FastScrollerDisplayItem.Letter(alphabet[it]) }
    if (letterCount == alphabet.size) return letters

    // Compressed state: fill the gap between consecutive letters with a bullet dot
    return buildList {
        letters.forEachIndexed { index, item ->
            add(item)
            if (index < letters.lastIndex) add(FastScrollerDisplayItem.Dot)
        }
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

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "FastScrollerShort_Dark")
fun FastScrollerShortPreview() {
    // Full alphabet on a short bar: letters compress to evenly spread letters with dots in between
    FastScroller(
        alphabet = ('A'..'Z').map { it.toString() },
        onLetterSelected = {},
        isScrolling = true,
        activeLetter = "M",
        modifier = Modifier.height(300.dp),
    )
}
