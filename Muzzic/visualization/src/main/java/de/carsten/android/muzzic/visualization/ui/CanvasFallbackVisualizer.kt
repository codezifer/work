package de.carsten.android.muzzic.visualization.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import de.carsten.android.muzzic.visualization.MAX_RENDER_DT_SEC
import de.carsten.android.muzzic.visualization.MIN_FRAME_DT_SEC
import de.carsten.android.muzzic.visualization.NANOS_PER_SECOND
import de.carsten.android.muzzic.visualization.OFF_INTENSITY_VISIBILITY_THRESHOLD
import de.carsten.android.muzzic.visualization.PEAK_VISIBILITY_THRESHOLD
import de.carsten.android.muzzic.visualization.RENDERER_IDLE_VALUE_THRESHOLD
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.render.LedBarSmoother
import kotlin.math.min
import kotlinx.coroutines.isActive

/**
 * Fallback Compose Canvas implementation of the LED bar spectrum analyzer.
 *
 * Renders rounded LED bars with zone colors, peak markers, an outer halo
 * around lit segments (`VisualizerConfig.glowStrength`), and off-state LEDs
 * when GLES 3.0 is unavailable.
 *
 * The frame loop auto-stops once bars have settled while not playing (same
 * idle contract as [RenderDriver]), so pausedCPU/GPU load drops to ~0%.
 * Invisible off-state LEDs are skipped entirely when the theme's off intensity
 * is below [OFF_INTENSITY_VISIBILITY_THRESHOLD].
 */
@Composable
fun CanvasFallbackVisualizer(
    bus: SpectrumBus,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    config: VisualizerConfig = VisualizerConfig(),
    theme: VisualizerTheme = VisualizerTheme.ClassicGreen,
) {
    val smoother = remember(config.bandCount) { LedBarSmoother(config.bandCount) }
    val targetValues = remember(config.bandCount) { FloatArray(config.bandCount) }

    // Frame tick invalidates the Canvas below once per analyzed frame; the draw
    // scope reads plain (non-State) smoother arrays and would otherwise freeze.
    var frameTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, config) {
        var lastNanos = System.nanoTime()
        var settled = false
        while (isActive && !settled) {
            withFrameNanos { now ->
                val dtSec = ((now - lastNanos) / NANOS_PER_SECOND).toFloat().coerceIn(MIN_FRAME_DT_SEC, MAX_RENDER_DT_SEC)
                lastNanos = now

                val latencyNanos = config.visualLatencyMs * 1_000_000L
                val targetNanos = now - latencyNanos

                val read = bus.readAtOrBefore(targetNanos, targetValues)
                if (read <= 0 || !isPlaying) {
                    targetValues.fill(0f)
                }

                smoother.update(targetValues, config.bandCount, dtSec, config.smoother)

                if (!isPlaying && smoother.maxValue(config.bandCount) < RENDERER_IDLE_VALUE_THRESHOLD) {
                    // Settled while paused: stop the loop instead of spinning at display rate.
                    settled = true
                } else {
                    frameTick++
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Subscribe to frame updates (see frameTick above).
        @Suppress("UNUSED_EXPRESSION")
        frameTick

        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val bandCount = config.bandCount
        val segCount = config.segmentCount
        val cellW = width / bandCount
        val cellH = height / segCount

        val halfW = config.ledHalfSize.first * cellW
        val halfH = config.ledHalfSize.second * cellH
        val cornerR = config.cornerRadius * min(cellW, cellH)
        val skipOffLeds = theme.offIntensity <= OFF_INTENSITY_VISIBILITY_THRESHOLD

        drawRect(color = theme.background)

        for (b in 0 until bandCount) {
            val barVal = smoother.bands[b]
            val peakVal = smoother.peaks[b]
            val peakSeg = (peakVal * segCount).toInt().coerceIn(0, segCount - 1)

            val xCenter = (b + 0.5f) * cellW

            for (s in 0 until segCount) {
                val t = (s + 0.5f) / segCount

                val isLit = (s + 0.5f) / segCount <= barVal
                val isPeak = s == peakSeg && peakVal > PEAK_VISIBILITY_THRESHOLD
                val isOn = isLit || isPeak
                if (!isOn && skipOffLeds) continue

                val yCenter = height - (s + 0.5f) * cellH

                val zoneColor = when {
                    t < theme.zoneStart.first -> theme.colLow
                    t < theme.zoneStart.second -> theme.colMid
                    else -> theme.colHigh
                }

                val finalColor = if (isOn) {
                    zoneColor
                } else {
                    zoneColor.copy(alpha = theme.offIntensity)
                }

                if (isOn && config.glowStrength > 0f) {
                    drawRoundRect(
                        color = zoneColor.copy(alpha = zoneColor.alpha * HALO_ALPHA_FACTOR * config.glowStrength),
                        topLeft = Offset(xCenter - halfW * HALO_EXPAND_X, yCenter - halfH * HALO_EXPAND_Y),
                        size = Size(halfW * 2f * HALO_EXPAND_X, halfH * 2f * HALO_EXPAND_Y),
                        cornerRadius = CornerRadius(cornerR * HALO_EXPAND_X, cornerR * HALO_EXPAND_X),
                    )
                }

                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(xCenter - halfW, yCenter - halfH),
                    size = Size(halfW * 2f, halfH * 2f),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            }
        }
    }
}

private const val HALO_ALPHA_FACTOR = 0.5f
private const val HALO_EXPAND_X = 1.6f
private const val HALO_EXPAND_Y = 2.0f
