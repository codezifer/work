package de.carsten.android.muzzic.ui.component.fastscroll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.FastScrollMode
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.IndicatorScreen
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Wraps scrollable content (list or grid) and adds quick navigation.
 *
 * [FastScrollMode.FAST_SCROLLER] overlays a [FastScroller]; [FastScrollMode.INDICATOR_SCREEN] shows an
 * [IndicatorScreen] first and filters the content once a letter is chosen.
 *
 * Labels are mapped once in encounter order, so the caller must already deliver a label-sorted list.
 *
 * @param items The list of items to navigate through.
 * @param getLabel Extracts the display string used to derive the alphabet (e.g. a name).
 * @param firstVisibleItemIndex Provides the index of the first visible item in the list or grid.
 * @param isScrollInProgress Whether the associated list or grid is currently scrolling.
 * @param scrollToItem Scrolls the list or grid to the item with the given index.
 * @param modifier Modifier for the outer layout.
 * @param fastScrollMode Quick-navigation style.
 * @param colorSource Current colors from the selected album art.
 * @param content The scrollable content (e.g. `LazyColumn` or `LazyVerticalGrid`), receiving the items to render.
 */
@Composable
fun <T> FastScrollBox(
    items: List<T>,
    getLabel: (T) -> String,
    firstVisibleItemIndex: () -> Int,
    isScrollInProgress: () -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
    colorSource: ColorSource = composableColorSource(),
    content: @Composable (visibleItems: List<T>) -> Unit,
) {
    val scope: CoroutineScope = rememberCoroutineScope()
    val labels: List<String> = remember(items) { items.map { getLabel(it).uppercase(Locale.ROOT) } }

    // Encounter order: the first index per label keeps the letter→index mapping monotonic
    val letterToIndexMap: Map<String, Int> = remember(labels) { buildMap { labels.forEachIndexed { index, label -> putIfAbsent(label, index) } } }
    val alphabet: List<String> = remember(letterToIndexMap) { letterToIndexMap.keys.toList().distinct().sorted() }

    // Only the indicator mode needs the counts
    val itemCounts: Map<String, Int> = remember(labels, fastScrollMode) {
        if (fastScrollMode == FastScrollMode.INDICATOR_SCREEN) {
            labels.groupingBy { it }.eachCount()
        } else {
            emptyMap()
        }
    }

    var selectedLetter: String? by rememberSaveable { mutableStateOf<String?>(null) }

    // Guard against letters that vanished after a data change
    val effectiveLetter: String? = selectedLetter?.takeIf { it in itemCounts }

    LaunchedEffect(alphabet, fastScrollMode) {
        if (selectedLetter != null && effectiveLetter == null) selectedLetter = null
    }

    BackHandler(enabled = effectiveLetter != null) { selectedLetter = null }

    // O(n) once per letter choice, not per recomposition
    val visibleItems: List<T> = remember(items, effectiveLetter) {
        if (effectiveLetter == null) items else items.filterIndexed { index, _ -> labels[index] == effectiveLetter }
    }

    val activeLetter: String? by remember(labels) { derivedStateOf { labels.getOrNull(firstVisibleItemIndex()) } }

    // Only the latest scroll target should win
    val scrollJob: MutableState<Job?> = remember { mutableStateOf<Job?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (effectiveLetter != null) {
            LetterFilterHeader(letter = effectiveLetter, onClear = { selectedLetter = null })
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                fastScrollMode == FastScrollMode.INDICATOR_SCREEN && alphabet.isNotEmpty() && effectiveLetter == null ->
                    IndicatorScreen(
                        letters = alphabet,
                        itemCounts = itemCounts,
                        onLetterClick = { letter ->
                            selectedLetter = letter
                            // Filtered content starts at the top
                            scope.launch { scrollToItem(0) }
                        },
                        colorSource = colorSource,
                    )

                else -> content(visibleItems)
            }

            if (fastScrollMode == FastScrollMode.FAST_SCROLLER && alphabet.isNotEmpty()) {
                FastScroller(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = SPACING_NORMAL),
                    alphabet = alphabet,
                    activeLetter = { activeLetter },
                    isScrolling = { isScrollInProgress() },
                    onLetterSelected = { letter ->
                        letterToIndexMap[letter]?.let { index ->
                            scrollJob.value?.cancel()
                            scrollJob.value = scope.launch { scrollToItem(index) }
                        }
                    },
                    colorSource = colorSource,
                )
            }
        }
    }
}

@Composable
private fun LetterFilterHeader(letter: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GRID_HORIZONTAL_PADDING, vertical = SPACING_MEDIUM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = true,
            onClick = onClear,
            label = { Text(text = letter) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.back),
                )
            },
        )
    }
}
