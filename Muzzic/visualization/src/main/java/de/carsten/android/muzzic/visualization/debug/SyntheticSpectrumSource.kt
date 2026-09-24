package de.carsten.android.muzzic.visualization.debug

import de.carsten.android.muzzic.visualization.DEFAULT_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.MAX_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.MIN_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.SYNTH_FRAME_INTERVAL_MICROS
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Generates synthetic spectrum frames for debugging and visual tuning without active audio playback.
 */
class SyntheticSpectrumSource(val bus: SpectrumBus) {

    enum class Mode {
        SWEEP,
        PINK,
        BEAT,
        SILENCE,
        FULL_SCALE,
    }

    @Volatile var mode: Mode = Mode.BEAT

    @Volatile var activeBandCount: Int = DEFAULT_SPECTRUM_BANDS

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null
    private val random = Random()
    private var phase = 0f

    private val bandValues = FloatArray(MAX_SPECTRUM_BANDS)

    /**
     * Starts background frame generation at ~86 Hz (~11.6ms interval).
     */
    fun start() {
        stop()
        future = executor.scheduleWithFixedDelay({
            generateFrame()
        }, 0L, SYNTH_FRAME_INTERVAL_MICROS, TimeUnit.MICROSECONDS)
    }

    /**
     * Stops background frame generation.
     */
    fun stop() {
        future?.cancel(true)
        future = null
    }

    private fun generateFrame() {
        val now = System.nanoTime()
        val count = activeBandCount.coerceIn(MIN_SPECTRUM_BANDS, MAX_SPECTRUM_BANDS)
        phase += 0.05f

        when (mode) {
            Mode.SWEEP -> {
                val center = ((sin(phase.toDouble()) + 1.0) / 2.0 * (count - 1)).toFloat()
                for (i in 0 until count) {
                    val dist = abs(i - center)
                    bandValues[i] = (1f - dist / 3f).coerceIn(0f, 1f)
                }
            }

            Mode.PINK -> {
                for (i in 0 until count) {
                    val noise = random.nextFloat()
                    val falloff = 1f / (i + 1).toFloat().pow(0.5f)
                    bandValues[i] = (noise * falloff).coerceIn(0.05f, 1f)
                }
            }

            Mode.BEAT -> {
                val pulse = (cos((phase * 2f).toDouble()) + 1.0) / 2.0
                val bassValue = pulse.toFloat().coerceIn(0.2f, 1.0f)
                for (i in 0 until count) {
                    if (i < count / 4) {
                        bandValues[i] = bassValue
                    } else {
                        val n = random.nextFloat() * 0.4f
                        bandValues[i] = n
                    }
                }
            }

            Mode.SILENCE -> {
                bandValues.fill(0f)
            }

            Mode.FULL_SCALE -> {
                for (i in 0 until count) {
                    bandValues[i] = 1f
                }
            }
        }

        bus.write(now, bandValues, count)
    }
}
