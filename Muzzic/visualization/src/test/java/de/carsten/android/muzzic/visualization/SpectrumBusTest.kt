import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import org.junit.Assert.assertEquals
import org.junit.Test

class SpectrumBusTest {

    @Test
    fun `SpectrumBus reads newest slot at or before target timestamp`() {
        val bus = SpectrumBus(capacity = 16, maxBands = 8)

        val values1 = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val values2 = floatArrayOf(0.5f, 0.6f, 0.7f, 0.8f)

        bus.write(1000L, values1, 4)
        bus.write(2000L, values2, 4)

        val outValues = FloatArray(8)

        val count = bus.readAtOrBefore(1500L, outValues)
        assertEquals(4, count)
        assertEquals(0.1f, outValues[0], 1e-4f)
        assertEquals(0.2f, outValues[1], 1e-4f)

        val count2 = bus.readAtOrBefore(2500L, outValues)
        assertEquals(4, count2)
        assertEquals(0.5f, outValues[0], 1e-4f)
        assertEquals(0.6f, outValues[1], 1e-4f)
    }

    @Test
    fun `SpectrumBus returns zero count if target timestamp is before all slots`() {
        val bus = SpectrumBus(capacity = 8, maxBands = 4)
        val values = floatArrayOf(1f, 1f, 1f, 1f)

        bus.write(5000L, values, 4)

        val outValues = FloatArray(4)
        val count = bus.readAtOrBefore(1000L, outValues)

        assertEquals(0, count)
    }

    @Test
    fun `SpectrumBus newestTimestampNanos returns newest write timestamp`() {
        val bus = SpectrumBus(capacity = 8, maxBands = 4)
        val values = floatArrayOf(1f, 1f, 1f, 1f)

        assertEquals(0L, bus.newestTimestampNanos())

        bus.write(100L, values, 4)
        bus.write(300L, values, 4)

        assertEquals(300L, bus.newestTimestampNanos())
    }
}
