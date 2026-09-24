import de.carsten.android.muzzic.visualization.SmootherConfig
import de.carsten.android.muzzic.visualization.render.LedBarSmoother
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedBarSmootherTest {

    @Test
    fun `LedBarSmoother attack is immediate`() {
        val smoother = LedBarSmoother(maxBands = 4)
        val cfg = SmootherConfig()
        val target = floatArrayOf(0.8f, 0.5f, 0f, 0f)

        smoother.update(target, bandCount = 2, dtSec = 0.016f, cfg = cfg)

        assertEquals(0.8f, smoother.bands[0], 1e-4f)
        assertEquals(0.5f, smoother.bands[1], 1e-4f)
        assertEquals(0.8f, smoother.peaks[0], 1e-4f)
        assertEquals(0.5f, smoother.peaks[1], 1e-4f)
    }

    @Test
    fun `LedBarSmoother decay drops bar linearly according to fallPerSec`() {
        val smoother = LedBarSmoother(maxBands = 2)
        val cfg = SmootherConfig(fallPerSec = 1.0f)
        val targetInitial = floatArrayOf(1.0f, 0f)
        val targetZero = floatArrayOf(0.0f, 0f)

        smoother.update(targetInitial, bandCount = 1, dtSec = 0.016f, cfg = cfg)
        assertEquals(1.0f, smoother.bands[0], 1e-4f)

        // Drop for 0.05 seconds with fallPerSec = 1.0 -> expected value = 0.95f
        smoother.update(targetZero, bandCount = 1, dtSec = 0.05f, cfg = cfg)
        assertEquals(0.95f, smoother.bands[0], 1e-4f)
    }

    @Test
    fun `LedBarSmoother peak holds value during holdSec`() {
        val smoother = LedBarSmoother(maxBands = 2)
        val cfg = SmootherConfig(fallPerSec = 2.0f, holdSec = 0.35f, peakFallPerSec = 0.5f)
        val targetInitial = floatArrayOf(1.0f, 0f)
        val targetZero = floatArrayOf(0.0f, 0f)

        smoother.update(targetInitial, bandCount = 1, dtSec = 0.016f, cfg = cfg)
        assertEquals(1.0f, smoother.peaks[0], 1e-4f)

        // Drop bar for 0.1s: bar drops, but peak stays at 1.0 during holdSec (0.35s)
        smoother.update(targetZero, bandCount = 1, dtSec = 0.05f, cfg = cfg)
        smoother.update(targetZero, bandCount = 1, dtSec = 0.05f, cfg = cfg)

        assertTrue("Bar should have dropped", smoother.bands[0] < 1.0f)
        assertEquals("Peak should stay at 1.0 during hold", 1.0f, smoother.peaks[0], 1e-4f)
    }
}
