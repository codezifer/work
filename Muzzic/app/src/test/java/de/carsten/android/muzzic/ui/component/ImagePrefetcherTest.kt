package de.carsten.android.muzzic.ui.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImagePrefetcherTest {
    @Test
    fun `visible range is excluded from the prefetch windows`() {
        val ranges = prefetchRanges(visibleRange = 10..12, itemCount = 30, aheadCount = 4, behindCount = 2)

        assertThat(ranges).containsExactly(8..9, 13..16)
    }

    @Test
    fun `visible range at the start has no lookbehind window`() {
        val ranges = prefetchRanges(visibleRange = 0..2, itemCount = 30, aheadCount = 4, behindCount = 2)

        assertThat(ranges).containsExactly(3..6)
    }

    @Test
    fun `visible range at the end has no lookahead window`() {
        val ranges = prefetchRanges(visibleRange = 27..29, itemCount = 30, aheadCount = 4, behindCount = 2)

        assertThat(ranges).containsExactly(25..26)
    }

    @Test
    fun `prefetch windows are clamped to the item count`() {
        val ranges = prefetchRanges(visibleRange = 0..0, itemCount = 3, aheadCount = 10, behindCount = 2)

        assertThat(ranges).containsExactly(1..2)
    }

    @Test
    fun `single visible item in a single item list prefetches nothing`() {
        val ranges = prefetchRanges(visibleRange = 0..0, itemCount = 1, aheadCount = 8, behindCount = 2)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `fully visible list prefetches nothing`() {
        val ranges = prefetchRanges(visibleRange = 0..9, itemCount = 10, aheadCount = 8, behindCount = 2)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `empty visible range prefetches nothing`() {
        val ranges = prefetchRanges(visibleRange = IntRange.EMPTY, itemCount = 10, aheadCount = 8, behindCount = 2)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `empty list prefetches nothing`() {
        val ranges = prefetchRanges(visibleRange = 0..0, itemCount = 0, aheadCount = 8, behindCount = 2)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `zero sized windows prefetch nothing`() {
        val ranges = prefetchRanges(visibleRange = 5..5, itemCount = 10, aheadCount = 0, behindCount = 0)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun `visible range beyond the item count is clamped defensively`() {
        val ranges = prefetchRanges(visibleRange = 50..60, itemCount = 10, aheadCount = 2, behindCount = 2)

        assertThat(ranges).containsExactly(7..8)
    }

    @Test
    fun `no returned range contains a visible index`() {
        val visible = 10..12

        val prefetched = prefetchRanges(visibleRange = visible, itemCount = 30, aheadCount = 4, behindCount = 2)
            .flatMap { it.toList() }

        assertThat(prefetched.none { it in visible }).isTrue()
    }
}
