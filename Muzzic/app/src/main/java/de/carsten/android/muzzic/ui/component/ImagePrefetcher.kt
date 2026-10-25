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
 * Defaults to [data] when it is a String, otherwise null. Album art strings are normalized
 * through [coverBaseKey], so tracks sharing the same embedded cover share one cache entry.
 * @param sizePx Target square dimension in pixels, or null for default/original size.
 * @param allowHardware Whether hardware bitmap allocation is permitted (set false for Palette).
 * @param crossfade Whether crossfade animation is enabled when loading.
 * @param keySuffix Memory cache key suffix for variant entries (palette, blur), or null for the
 * display entry. The disk key always remains [cacheKey], so network covers share one disk entry
 * across all variants.
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
 * Indices that are currently visible are skipped: their images are already requested by the
 * composables on screen, and Coil does not join duplicate in-flight requests.
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
 * Indices that are currently visible are skipped: their images are already requested by the
 * composables on screen, and Coil does not join duplicate in-flight requests.
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
 * Visible indices are excluded via [prefetchRanges]: they are already composed, so their
 * images are requested by the UI. Prefetching them as well would start a second request for
 * the same image because Coil does not join in-flight requests.
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
                visible.min()..visible.max()
            }
        }
            .distinctUntilChanged()
            .collect { visibleRange ->
                val ranges = prefetchRanges(visibleRange, itemCount, prefetchCount, PREFETCH_LOOKBEHIND_COUNT)
                if (ranges.isEmpty()) return@collect
                val itemsSnapshot = currentItems
                val extract = currentExtract
                for (range in ranges) {
                    for (index in range) {
                        val item = itemsSnapshot.getOrNull(index) ?: continue
                        extract(item).forEach { spec ->
                            enqueueSpec(context, imageLoader, spec, enqueuedKeys)
                        }
                    }
                }
            }
    }
}

/**
 * Computes the index ranges to prefetch around the visible items.
 *
 * The visible indices are excluded on purpose: they are already composed, so their images are
 * already being requested by the UI. Prefetching them again would start a second request for
 * the same image because Coil does not join in-flight requests.
 *
 * @param visibleRange The range of currently visible indices, or [IntRange.EMPTY] if none.
 * @param itemCount The number of backing items.
 * @param aheadCount The number of items after the visible range to prefetch.
 * @param behindCount The number of items before the visible range to prefetch.
 * @return up to two disjoint ranges in ascending order (lookbehind, then lookahead), clamped
 * to the list bounds.
 */
internal fun prefetchRanges(visibleRange: IntRange, itemCount: Int, aheadCount: Int, behindCount: Int): List<IntRange> {
    if (itemCount <= 0 || visibleRange.isEmpty()) return emptyList()

    val first = visibleRange.first.coerceIn(0, itemCount - 1)
    val last = visibleRange.last.coerceIn(first, itemCount - 1)

    return buildList(2) {
        val behindStart = (first - behindCount).coerceAtLeast(0)
        if (behindStart < first) add(behindStart until first)

        val aheadEnd = (last + aheadCount).coerceAtMost(itemCount - 1)
        if (last < aheadEnd) add((last + 1)..aheadEnd)
    }
}

private fun enqueueSpec(context: Context, imageLoader: ImageLoader, spec: CoverPreloadSpec, enqueuedKeys: HashSet<String>) {
    val data = spec.data ?: return
    val cacheKey = coverBaseKey(spec.cacheKey)
    val memoryKey = cacheKey?.plus(spec.keySuffix.orEmpty())
    // Deduplicate on the resolved cache key: a song list repeats the same album cover for
    // every track, so enqueuing per song would only race for the same memory entry.
    val dedupKey = "${memoryKey ?: data}_${spec.sizePx}_${spec.allowHardware}_${spec.crossfade}"
    if (!enqueuedKeys.add(dedupKey)) return

    val builder =
        ImageRequest
            .Builder(context)
            .data(data)
            .allowHardware(spec.allowHardware)
            .crossfade(spec.crossfade)
            .precision(Precision.INEXACT)

    if (memoryKey != null) {
        builder.memoryCacheKey(memoryKey).diskCacheKey(cacheKey)
        if (spec.keySuffix == null) {
            builder.placeholderMemoryCacheKey(memoryKey)
        }
    }
    spec.sizePx?.let { builder.size(it) }

    imageLoader.enqueue(builder.build())
}
