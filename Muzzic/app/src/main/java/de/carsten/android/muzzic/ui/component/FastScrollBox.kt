package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import kotlinx.coroutines.launch

/**
 * Wraps scrollable content (list or grid) in a [Box] and overlays a [FastScroller]
 * for alphabetical navigation.
 *
 * The alphabet, the letter-to-index mapping and the currently visible letter are
 * derived from [items] and the provided scroll-state accessors, so callers only
 * need to supply their scroll state (e.g. `LazyListState` or `LazyGridState`).
 *
 * @param items The list of items to navigate through.
 * @param label Extracts the display string used to derive the alphabet (e.g. a name).
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
    label: (T) -> String,
    firstVisibleItemIndex: () -> Int,
    isScrollInProgress: () -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val alphabet by remember(items) {
        derivedStateOf {
            items.map { label(it).take(1).uppercase() }.distinct().sorted()
        }
    }

    val letterToIndexMap by remember(items) {
        derivedStateOf {
            items.foldIndexed(mutableMapOf<String, Int>()) { index, map, item ->
                val letter = label(item).take(1).uppercase()
                if (!map.containsKey(letter)) {
                    map[letter] = index
                }
                map
            }
        }
    }

    val activeLetter by remember(items) {
        derivedStateOf {
            val index = firstVisibleItemIndex()
            if (index in items.indices) {
                label(items[index]).take(1).uppercase()
            } else {
                null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (alphabet.isNotEmpty()) {
            FastScroller(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SPACING_NORMAL),
                alphabet = alphabet,
                activeLetter = activeLetter,
                isScrolling = isScrollInProgress(),
                onLetterSelected = { letter ->
                    letterToIndexMap[letter]?.let { index ->
                        scope.launch {
                            scrollToItem(index)
                        }
                    }
                },
                colorSource = colorSource,
            )
        }
    }
}
