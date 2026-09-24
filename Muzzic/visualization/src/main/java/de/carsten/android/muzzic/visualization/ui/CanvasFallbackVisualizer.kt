package de.carsten.android.muzzic.visualization.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import de.carsten.android.muzzic.visualization.MAX_RENDER_DT_SEC
import de.carsten.android.muzzic.visualization.MIN_FRAME_DT_SEC
import de.carsten.android.muzzic.visualization.NANOS_PER_SECOND
import de.carsten.android.muzzic.visualization.PEAK_VISIBILITY_THRESHOLD
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.render.LedBarSmoother
import kotlin.math.min
import kotlinx.coroutines.isActive

/**
 * Fallback Compose Canvas implementation of the LED bar spectrum analyzer.
 *
 * Renders rounded LED bars with zone colors, peak markers, and off-state LEDs when GLES 3.0 is unavailable.
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

    var lastNanos = remember { System.nanoTime() }

    LaunchedEffect(isPlaying) {
        while (isActive) {
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
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
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

        drawRect(color = theme.background)

        for (b in 0 until bandCount) {
            val barVal = smoother.bands[b]
            val peakVal = smoother.peaks[b]
            val peakSeg = (peakVal * segCount).toInt().coerceIn(0, segCount - 1)

            val xCenter = (b + 0.5f) * cellW

            for (s in 0 until segCount) {
                val yCenter = height - (s + 0.5f) * cellH
                val t = (s + 0.5f) / segCount

                val isLit = (s + 0.5f) / segCount <= barVal
                val isPeak = s == peakSeg && peakVal > PEAK_VISIBILITY_THRESHOLD
                val isOn = isLit || isPeak

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
