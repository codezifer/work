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
import androidx.compose.foundation.layout.wrapContentSize
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
import de.carsten.android.muzzic.ui.FAST_SCROLL_THUMB_TAIL_OVERLAP
import de.carsten.android.muzzic.ui.FAST_SCROLL_THUMB_TAIL_SIZE
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.ICON_SIZE_FAST_SCROLL_THUMB
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_BACKGROUND
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_BORDER
import de.carsten.android.muzzic.ui.OPACITY_FAST_SCROLL_HANDLE
import de.carsten.android.muzzic.ui.OPACITY_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.shape.IndicatorShape
import de.carsten.android.muzzic.ui.shape.TailDirection
import de.carsten.android.muzzic.ui.utils.contrastColor
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** Bubble shape with a right tail pointing at the letter row. */
private val fastScrollerThumbShape =
    IndicatorShape(
        tailSize = FAST_SCROLL_THUMB_TAIL_SIZE,
        cornerRadius = ICON_SIZE_FAST_SCROLL_THUMB / 2,
        direction = TailDirection.Right,
        tailOverlap = FAST_SCROLL_THUMB_TAIL_OVERLAP,
    )

/**
 * Vertical alphabet bar for quick list navigation (artists, albums, songs).
 *
 * Compresses letters with bullet dots when the bar is too short; the font shrinks as a fallback.
 *
 * @param alphabet Letters to display (e.g. A-Z).
 * @param onLetterSelected Invoked when a letter is selected.
 * @param isScrolling Whether the associated list is currently scrolling.
 * @param modifier Modifier for the outer Box layout.
 * @param activeLetter Letter currently visible in the list (controlled by the parent).
 * @param colorSource Colors from the selected album art.
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

    // Letter under the finger while dragging
    var draggingLetter by remember { mutableStateOf<String?>(null) }

    var isVisible by remember { mutableStateOf(false) }

    // Show while scrolling/dragging, hide after a delay
    LaunchedEffect(isScrolling, draggingLetter) {
        if (isScrolling || draggingLetter != null) {
            isVisible = true
        } else {
            delay(FAST_SCROLL_HIDE_DELAY_MS.milliseconds)
            isVisible = false
        }
    }

    // Bar height in pixels, used for index mapping
    var columnHeight by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current

    // Dragged letter wins over the active letter reported by the list
    val effectiveLetter = draggingLetter ?: activeLetter

    // Rendered slots for the current height; pointer mapping stays proportional to the full alphabet
    val displayItems =
        remember(alphabet, columnHeight) {
            when {
                alphabet.isEmpty() -> emptyList()

                columnHeight > 0 -> {
                    val usableHeightDp =
                        with(density) { (columnHeight - 2 * SPACING_MEDIUM.toPx()).toDp() }
                    buildFastScrollerDisplayItems(
                        alphabet = alphabet,
                        // At most one slot per minimum item height
                        maxVisibleItems = (usableHeightDp / FAST_SCROLL_ITEM_MIN_HEIGHT).toInt(),
                    )
                }

                // Before the first measurement: one slot per letter
                else -> alphabet.map { FastScrollerDisplayItem.Letter(it) }
            }
        }

    // Shrink the font to fit a slot; capped at the caption size
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

    // Maps a pointer y-position to its alphabet index (proportional to the full alphabet)
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
                // Measure the bar height for index mapping
                .onGloballyPositioned { columnHeight = it.size.height }
                // Tap on a letter
                .pointerInput(alphabet) {
                    detectTapGestures { offset ->
                        letterIndexAt(offset.y)
                            .takeIf { it >= 0 }
                            ?.let { onLetterSelected(alphabet[it]) }
                    }
                }
                // Drag along the bar
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
            // Vertical center of the current letter, shared by handle and preview bubble
            val centerOffsetDp =
                if (letterIndex >= 0 && columnHeight > 0) {
                    with(density) {
                        val topPaddingPx = SPACING_MEDIUM.toPx()
                        val usableHeightPx = (columnHeight - 2 * topPaddingPx).coerceAtLeast(1f)
                        val letterFraction =
                            if (alphabet.size > 1) letterIndex.toFloat() / (alphabet.size - 1) else 0.5f
                        (topPaddingPx + letterFraction * usableHeightPx).toDp()
                    }
                } else {
                    0.dp
                }

            // Animated so the handle and the bubble move in lockstep
            val animatedCenter by animateDpAsState(
                targetValue = centerOffsetDp,
                label = "fastScrollerCenter",
            )

            // Handle marking the current position
            if (letterIndex != -1 && alphabet.isNotEmpty() && columnHeight > 0) {
                Box(
                    modifier =
                    Modifier
                        .offset(y = animatedCenter - ICON_SIZE_MEDIUM / 2)
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
                // Equal share per slot; the display model never overflows
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

            // Preview bubble left of the bar; its tail points at the current letter
            draggingLetter?.let { letter ->
                val maxThumbOffset =
                    (with(density) { columnHeight.toDp() } - ICON_SIZE_FAST_SCROLL_THUMB).coerceAtLeast(0.dp)
                // Clamp so the bubble stays inside the bar at the first and last letter
                val thumbOffset =
                    (animatedCenter - ICON_SIZE_FAST_SCROLL_THUMB / 2).coerceIn(0.dp, maxThumbOffset)
                // Tail tip sits just left of the bar edge
                val thumbOffsetX = -(ICON_SIZE_FAST_SCROLL_THUMB + FAST_SCROLL_THUMB_TAIL_SIZE + SPACING_TINY)

                Box(
                    modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = thumbOffsetX, y = thumbOffset)
                        // wrapContentSize: the 40dp bar must not shrink or center the wider bubble
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .width(ICON_SIZE_FAST_SCROLL_THUMB + FAST_SCROLL_THUMB_TAIL_SIZE)
                        .height(ICON_SIZE_FAST_SCROLL_THUMB)
                        .shadow(ELEVATION_MEDIUM, fastScrollerThumbShape)
                        .clip(fastScrollerThumbShape)
                        .background(colorSource.accentColor),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Center the letter in the round body, not in body + tail
                    Box(
                        modifier = Modifier.size(ICON_SIZE_FAST_SCROLL_THUMB),
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
}

/**
 * Renders one slot: a letter or a bullet dot, centered and never clipped.
 *
 * @param letter Character to render (a letter or [BULLET_POINT]).
 * @param fontSize Font size fitted to the slot height.
 * @param isActive Whether this letter is the active one (bold + accent color).
 * @param modifier Modifier for the letter's slot box.
 * @param colorSource Colors from the selected album art.
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

/** A rendered slot: an alphabet letter or a decorative bullet dot. */
private sealed interface FastScrollerDisplayItem {
    /** A rendered alphabet letter. */
    data class Letter(val value: String) : FastScrollerDisplayItem

    /** Decorative dot between two letters. */
    data object Dot : FastScrollerDisplayItem
}

/**
 * Builds the slots rendered for the available bar height.
 *
 * Compresses the alphabet with dots when [maxVisibleItems] can't fit one letter per slot; the first
 * and last letter are always kept.
 *
 * @param alphabet Full alphabet to display.
 * @param maxVisibleItems Slots that fit into the available height (>= 1).
 * @return Slots in top-to-bottom order.
 */
private fun buildFastScrollerDisplayItems(alphabet: List<String>, maxVisibleItems: Int): List<FastScrollerDisplayItem> {
    if (alphabet.isEmpty()) return emptyList()

    // Compressed: every second slot is a dot
    val letterCount = if (maxVisibleItems >= alphabet.size) alphabet.size else (maxVisibleItems + 1) / 2

    val letterIndices =
        when {
            letterCount <= 1 -> listOf(alphabet.size / 2)

            letterCount == alphabet.size -> alphabet.indices.toList()

            // Evenly spread, keeping first and last
            else ->
                (0 until letterCount).map { slot ->
                    (slot.toDouble() * (alphabet.size - 1) / (letterCount - 1)).roundToInt()
                }
        }

    val letters = letterIndices.map { FastScrollerDisplayItem.Letter(alphabet[it]) }
    if (letterCount == alphabet.size) return letters

    // Alternate letters with dots
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
    // Full alphabet on a short bar: letters compress with dots in between
    FastScroller(
        alphabet = ('A'..'Z').map { it.toString() },
        onLetterSelected = {},
        isScrolling = true,
        activeLetter = "M",
        modifier = Modifier.height(300.dp),
    )
}
