package de.carsten.android.muzzic.ui.component.fastscroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Wraps scrollable content (list or grid) in a [Box] and overlays a [FastScroller]
 * for alphabetical navigation.
 *
 * Labels are mapped once in encounter order, so the caller must already deliver a label-sorted list.
 *
 * @param items The list of items to navigate through.
 * @param getLabel Extracts the display string used to derive the alphabet (e.g. a name).
 * @param firstVisibleItemIndex Provides the index of the first visible item in the list or grid.
 * @param isScrollInProgress Whether the associated list or grid is currently scrolling.
 * @param scrollToItem Scrolls the list or grid to the item with the given index.
 * @param modifier Modifier for the outer Box layout.
 * @param colorSource Current colors from the selected album art.
 * @param content The scrollable content (e.g. `LazyColumn` or `LazyVerticalGrid`) placed inside the Box.
 */
@Composable
fun <T> FastScrollBox(
    items: List<T>,
    getLabel: (T) -> String,
    firstVisibleItemIndex: () -> Int,
    isScrollInProgress: () -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val labels = remember(items) { items.map { getLabel(it).uppercase(Locale.ROOT) } }

    // Encounter order: the first index per label keeps the letter→index mapping monotonic
    val letterToIndexMap = remember(labels) { buildMap { labels.forEachIndexed { index, label -> putIfAbsent(label, index) } } }

    val alphabet = remember(letterToIndexMap) { letterToIndexMap.keys.toList() }
    val activeLetter by remember(labels) { derivedStateOf { labels.getOrNull(firstVisibleItemIndex()) } }

    // Only the latest scroll target should win
    val scrollJob = remember { mutableStateOf<Job?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (alphabet.isNotEmpty()) {
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
