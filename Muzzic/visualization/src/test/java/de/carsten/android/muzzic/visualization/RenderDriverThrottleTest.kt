package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.ui.RenderDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RenderDriverThrottleTest {

    @Test
    fun `first frame after start always renders`() {
        assertThat(RenderDriver.shouldRenderAt(nowNanos = 1_000L, lastNanos = 0L, maxFps = 30)).isTrue()
    }

    @Test
    fun `frames within cap interval are skipped`() {
        // 30 fps -> ~33.3ms interval.
        assertThat(RenderDriver.shouldRenderAt(nowNanos = 10_000_000L, lastNanos = 0L, maxFps = 30)).isTrue()
        assertThat(RenderDriver.shouldRenderAt(nowNanos = 20_000_000L, lastNanos = 10_000_000L, maxFps = 30)).isFalse()
        assertThat(RenderDriver.shouldRenderAt(nowNanos = 43_400_000L, lastNanos = 10_000_000L, maxFps = 30)).isTrue()
    }

    @Test
    fun `non-positive cap disables throttling`() {
        assertThat(RenderDriver.shouldRenderAt(nowNanos = 11L, lastNanos = 10L, maxFps = 0)).isTrue()
        assertThat(RenderDriver.remainingDelayMillis(nowNanos = 11L, lastNanos = 10L, maxFps = -1)).isZero()
    }

    @Test
    fun `remaining delay shrinks as deadline approaches`() {
        val early = RenderDriver.remainingDelayMillis(nowNanos = 10_000_000L, lastNanos = 10_000_000L, maxFps = 30)
        val late = RenderDriver.remainingDelayMillis(nowNanos = 30_000_000L, lastNanos = 10_000_000L, maxFps = 30)

        assertThat(early).isGreaterThan(late)
        assertThat(late).isGreaterThanOrEqualTo(0L)
    }

    @Test
    fun `overdue frame has zero delay`() {
        assertThat(RenderDriver.remainingDelayMillis(nowNanos = 100_000_000L, lastNanos = 10_000_000L, maxFps = 30)).isZero()
    }
}
