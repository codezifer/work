package de.carsten.android.muzzic.ui.component

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Precision
import de.carsten.android.muzzic.ui.PREFETCH_AHEAD_COUNT
import de.carsten.android.muzzic.ui.PREFETCH_LOOKBEHIND_COUNT
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Data specification for preloading an image with Coil 3.
 *
 * @param data The image source (path string, Uri, AlbumArtUri, etc.).
 * @param cacheKey Stable cache key (content identity), or null if no explicit key is used.
 * Defaults to [data] when it is a String, otherwise null.
 * @param sizePx Target square dimension in pixels, or null for default/original size.
 * @param allowHardware Whether hardware bitmap allocation is permitted (set false for Palette).
 * @param crossfade Whether crossfade animation is enabled when loading.
 * @param keySuffix Memory cache key suffix for variant entries (palette, blur), or null for the
 * display entry. The disk key always remains [cacheKey], so all variants share one raw blob.
 */
data class CoverPreloadSpec(
    val data: Any?,
    val cacheKey: String? = data as? String,
    val sizePx: Int? = null,
    val allowHardware: Boolean = true,
    val crossfade: Boolean = true,
    val keySuffix: String? = null,
)

/**
 * Prefetches image assets ahead of the scroll position in a [LazyGridState].
 *
 * @param gridState The state of the lazy grid.
 * @param items The backing list of items.
 * @param prefetchCount The number of items ahead of the visible area to prefetch.
 * @param extractCovers Lambda extracting image preload specifications for a given item.
 */
@Composable
fun <T> LazyGridPrefetchEffect(gridState: LazyGridState, items: List<T>, prefetchCount: Int = PREFETCH_AHEAD_COUNT, extractCovers: (T) -> List<CoverPreloadSpec>) {
    PrefetchHost(
        stateKey = gridState,
        itemCount = items.size,
        prefetchCount = prefetchCount,
        latestItems = items,
        latestExtract = extractCovers,
        visibleIndices = { gridState.layoutInfo.visibleItemsInfo.map { it.index } },
    )
}

/**
 * Prefetches image assets ahead of the scroll position in a [LazyListState].
 *
 * @param listState The state of the lazy list.
 * @param items The backing list of items.
 * @param prefetchCount The number of items ahead of the visible area to prefetch.
 * @param extractCovers Lambda extracting image preload specifications for a given item.
 */
@Composable
fun <T> LazyListPrefetchEffect(listState: LazyListState, items: List<T>, prefetchCount: Int = PREFETCH_AHEAD_COUNT, extractCovers: (T) -> List<CoverPreloadSpec>) {
    PrefetchHost(
        stateKey = listState,
        itemCount = items.size,
        prefetchCount = prefetchCount,
        latestItems = items,
        latestExtract = extractCovers,
        visibleIndices = { listState.layoutInfo.visibleItemsInfo.map { it.index } },
    )
}

/**
 * Shared prefetch host for lazy lists and grids.
 *
 * Keys the [LaunchedEffect] on the scroll state, the item count and the prefetch
 * count only (instead of the whole list) to avoid O(n) list-equality checks on
 * every recomposition. The current items and extractor are observed via
 * [rememberUpdatedState] so content updates are picked up without restarting
 * the effect and clearing the deduplication set.
 *
 * No dispatcher hop: [ImageLoader.enqueue] is non-suspending and cheap, Coil
 * performs the actual I/O internally, so this runs on the main thread without
 * blocking it.
 *
 * @param stateKey The scroll state object identifying the lazy layout.
 * @param itemCount The number of backing items.
 * @param prefetchCount The number of items ahead of the visible area to prefetch.
 * @param latestItems The current backing list of items.
 * @param latestExtract Lambda extracting image preload specifications for a given item.
 * @param visibleIndices Returns the indices of the currently visible items.
 */
@Composable
private fun <T> PrefetchHost(
    stateKey: Any,
    itemCount: Int,
    prefetchCount: Int,
    latestItems: List<T>,
    latestExtract: (T) -> List<CoverPreloadSpec>,
    visibleIndices: () -> List<Int>,
) {
    val context = LocalContext.current
    val currentItems by rememberUpdatedState(latestItems)
    val currentExtract by rememberUpdatedState(latestExtract)

    LaunchedEffect(stateKey, itemCount, prefetchCount) {
        if (itemCount == 0) return@LaunchedEffect

        val imageLoader = SingletonImageLoader.get(context)
        val enqueuedKeys = HashSet<String>()

        snapshotFlow {
            val visible = visibleIndices()
            if (visible.isEmpty()) {
                IntRange.EMPTY
            } else {
                val start = (visible.min() - PREFETCH_LOOKBEHIND_COUNT).coerceAtLeast(0)
                val end = (visible.max() + prefetchCount).coerceAtMost(itemCount - 1)
                start..end
            }
        }
            .distinctUntilChanged()
            .collect { range ->
                if (range.isEmpty()) return@collect
                val itemsSnapshot = currentItems
                val extract = currentExtract
                for (index in range) {
                    val item = itemsSnapshot.getOrNull(index) ?: continue
                    extract(item).forEach { spec ->
                        enqueueSpec(context, imageLoader, spec, enqueuedKeys)
                    }
                }
            }
    }
}

private fun enqueueSpec(context: Context, imageLoader: ImageLoader, spec: CoverPreloadSpec, enqueuedKeys: HashSet<String>) {
    val data = spec.data ?: return
    val memoryKey = spec.cacheKey?.plus(spec.keySuffix.orEmpty())
    val dedupKey = "${data}_${memoryKey}_${spec.sizePx}_${spec.allowHardware}_${spec.crossfade}"
    if (!enqueuedKeys.add(dedupKey)) return

    val builder =
        ImageRequest
            .Builder(context)
            .data(data)
            .allowHardware(spec.allowHardware)
            .crossfade(spec.crossfade)
            .precision(Precision.INEXACT)

    if (memoryKey != null) {
        builder.memoryCacheKey(memoryKey).diskCacheKey(spec.cacheKey)
        if (spec.keySuffix == null) {
            builder.placeholderMemoryCacheKey(memoryKey)
        }
    }
    spec.sizePx?.let { builder.size(it) }

    imageLoader.enqueue(builder.build())
}
