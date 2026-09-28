package de.carsten.android.muzzic.visualization.render

import android.opengl.GLES30
import android.util.Log
import de.carsten.android.muzzic.visualization.MAX_RENDER_DT_SEC
import de.carsten.android.muzzic.visualization.MAX_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.MIN_FRAME_DT_SEC
import de.carsten.android.muzzic.visualization.NANOS_PER_MILLISECOND
import de.carsten.android.muzzic.visualization.NANOS_PER_SECOND
import de.carsten.android.muzzic.visualization.RENDERER_IDLE_VALUE_THRESHOLD
import de.carsten.android.muzzic.visualization.STALE_FRAME_GRACE_NANOS
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.render.shaders.BarsFragment
import de.carsten.android.muzzic.visualization.render.shaders.FullscreenVertex
import de.carsten.android.muzzic.visualization.render.shaders.LedFragment

/**
 * Render styles supported by [LedBarRenderer].
 */
enum class RenderStyle {
    /** Bottom-up LED tower with green/yellow/red zones and peak-hold markers. */
    LED,

    /** Classic mirrored segmented bars: bass in the horizontal center, bidirectional from the middle. */
    MIRRORED_BARS,
}

/**
 * Maps a visual column to a spectrum band with horizontal center-mirror (bass in the middle).
 *
 * With `columnCount = 2 * bandCount` this reproduces the legacy Canvas bar ordering exactly.
 *
 * @param column Visual column 0..columnCount-1.
 * @param columnCount Total visual columns (even).
 * @param bandCount Spectrum bands available in the bus.
 */
fun mirroredBandIndex(column: Int, columnCount: Int, bandCount: Int): Int {
    val half = columnCount / 2
    val mapped = if (column < half) half - 1 - column else column - half
    return mapped.coerceIn(0, bandCount - 1)
}

private const val TAG = "LedBarRenderer"

/**
 * OpenGL ES 3.0 renderer implementation for the LED bar spectrum visualizer.
 *
 * Platform-agnostic: the host (currently a `TextureView` via [EglManager]) owns the
 * EGL context and calls [onSurfaceCreated], [onSurfaceChanged], and [onDrawFrame]
 * on a single thread with the context current.
 */
class LedBarRenderer(
    @Volatile var bus: SpectrumBus,
    config: VisualizerConfig = VisualizerConfig(),
    theme: VisualizerTheme = VisualizerTheme.ClassicGreen,
    style: RenderStyle = RenderStyle.LED,
) : FrameRenderer {

    @Volatile override var isIdle: Boolean = false
        private set

    var config: VisualizerConfig = config
        set(value) {
            field = value
            staticUniformsDirty = true
        }

    var theme: VisualizerTheme = theme
        set(value) {
            field = value
            staticUniformsDirty = true
        }

    var style: RenderStyle = style
        set(value) {
            field = value
            staticUniformsDirty = true
        }

    /**
     * Set by the host thread on every `config`/`theme`/`style` change; read by
     * the GL thread. Volatile write releases the preceding field writes, so the
     * GL thread observes a consistent snapshot after seeing `true`.
     */
    @Volatile private var staticUniformsDirty = true

    val smoother = LedBarSmoother(config.bandCount)

    private var program = 0
    private var barsProgram = 0
    private var uResLoc = -1
    private var uBandCountLoc = -1
    private var uSegmentsLoc = -1
    private var uBandsLoc = -1
    private var uPeaksLoc = -1
    private var uTrailLoc = -1
    private var uTrailStrengthLoc = -1
    private var uColLowLoc = -1
    private var uColMidLoc = -1
    private var uColHighLoc = -1
    private var uZoneStartLoc = -1
    private var uLedHalfSizeLoc = -1
    private var uCornerRadiusLoc = -1
    private var uOffIntensityLoc = -1
    private var uBackgroundLoc = -1
    private var uGlowLoc = -1
    private var uShimmerLoc = -1
    private var uHotCoreLoc = -1
    private var uSpecularLoc = -1
    private var uBleedLoc = -1
    private var uGlowGradeLoc = -1
    private var uFadeLoc = -1
    private var uTimeLoc = -1

    // Mirrored-BARS program uniform locations.
    private var bResLoc = -1
    private var bColumnsLoc = -1
    private var bBandCountLoc = -1
    private var bSegmentsLoc = -1
    private var bBandsLoc = -1
    private var bColBaseLoc = -1
    private var bColTargetLoc = -1
    private var bShimmerLoc = -1
    private var bTipGlowLoc = -1
    private var bTimeLoc = -1
    private var bAlphaLoc = -1
    private var bLedHalfSizeLoc = -1
    private var bCornerRadiusLoc = -1
    private var bOffIntensityLoc = -1
    private var bBackgroundLoc = -1
    private var bGlowLoc = -1

    private val targetValues = FloatArray(MAX_SPECTRUM_BANDS)
    private val packedBands = FloatArray(MAX_SPECTRUM_BANDS)
    private val packedPeaks = FloatArray(MAX_SPECTRUM_BANDS)
    private val packedTrail = FloatArray(MAX_SPECTRUM_BANDS)

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var lastDrawNanos = System.nanoTime()
    private var renderTimeSec = 0f

    override fun onSurfaceCreated() {
        program = GlUtil.createProgram(FullscreenVertex.SOURCE, LedFragment.SOURCE)
        barsProgram = GlUtil.createProgram(FullscreenVertex.SOURCE, BarsFragment.SOURCE)
        if (program == 0) {
            Log.w(TAG, "LED program failed to compile/link; LED style unavailable")
        } else {
            uResLoc = GLES30.glGetUniformLocation(program, "uRes")
            uBandCountLoc = GLES30.glGetUniformLocation(program, "uBandCount")
            uSegmentsLoc = GLES30.glGetUniformLocation(program, "uSegments")
            uBandsLoc = GLES30.glGetUniformLocation(program, "uBands")
            uPeaksLoc = GLES30.glGetUniformLocation(program, "uPeaks")
            uTrailLoc = GLES30.glGetUniformLocation(program, "uTrail")
            uTrailStrengthLoc = GLES30.glGetUniformLocation(program, "uTrailStrength")
            uColLowLoc = GLES30.glGetUniformLocation(program, "uColLow")
            uColMidLoc = GLES30.glGetUniformLocation(program, "uColMid")
            uColHighLoc = GLES30.glGetUniformLocation(program, "uColHigh")
            uZoneStartLoc = GLES30.glGetUniformLocation(program, "uZoneStart")
            uLedHalfSizeLoc = GLES30.glGetUniformLocation(program, "uLedHalfSize")
            uCornerRadiusLoc = GLES30.glGetUniformLocation(program, "uCornerRadius")
            uOffIntensityLoc = GLES30.glGetUniformLocation(program, "uOffIntensity")
            uBackgroundLoc = GLES30.glGetUniformLocation(program, "uBackground")
            uGlowLoc = GLES30.glGetUniformLocation(program, "uGlow")
            uShimmerLoc = GLES30.glGetUniformLocation(program, "uShimmer")
            uHotCoreLoc = GLES30.glGetUniformLocation(program, "uHotCore")
            uSpecularLoc = GLES30.glGetUniformLocation(program, "uSpecular")
            uBleedLoc = GLES30.glGetUniformLocation(program, "uBleed")
            uGlowGradeLoc = GLES30.glGetUniformLocation(program, "uGlowGrade")
            uFadeLoc = GLES30.glGetUniformLocation(program, "uFade")
            uTimeLoc = GLES30.glGetUniformLocation(program, "uTime")
        }
        if (barsProgram == 0) {
            Log.w(TAG, "BARS program failed to compile/link; falling back to LED style")
        } else {
            bResLoc = GLES30.glGetUniformLocation(barsProgram, "uRes")
            bColumnsLoc = GLES30.glGetUniformLocation(barsProgram, "uColumns")
            bBandCountLoc = GLES30.glGetUniformLocation(barsProgram, "uBandCount")
            bSegmentsLoc = GLES30.glGetUniformLocation(barsProgram, "uSegments")
            bBandsLoc = GLES30.glGetUniformLocation(barsProgram, "uBands")
            bColBaseLoc = GLES30.glGetUniformLocation(barsProgram, "uColBase")
            bColTargetLoc = GLES30.glGetUniformLocation(barsProgram, "uColTarget")
            bShimmerLoc = GLES30.glGetUniformLocation(barsProgram, "uShimmer")
            bTipGlowLoc = GLES30.glGetUniformLocation(barsProgram, "uTipGlow")
            bTimeLoc = GLES30.glGetUniformLocation(barsProgram, "uTime")
            bAlphaLoc = GLES30.glGetUniformLocation(barsProgram, "uAlpha")
            bLedHalfSizeLoc = GLES30.glGetUniformLocation(barsProgram, "uLedHalfSize")
            bCornerRadiusLoc = GLES30.glGetUniformLocation(barsProgram, "uCornerRadius")
            bOffIntensityLoc = GLES30.glGetUniformLocation(barsProgram, "uOffIntensity")
            bBackgroundLoc = GLES30.glGetUniformLocation(barsProgram, "uBackground")
            bGlowLoc = GLES30.glGetUniformLocation(barsProgram, "uGlow")
        }
        lastDrawNanos = System.nanoTime()
        staticUniformsDirty = true
        applyStaticUniformsIfNeeded()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame() {
        if (viewportWidth <= 0 || viewportHeight <= 0) return

        val now = System.nanoTime()
        val dtSec = ((now - lastDrawNanos) / NANOS_PER_SECOND).toFloat().coerceIn(MIN_FRAME_DT_SEC, MAX_RENDER_DT_SEC)
        lastDrawNanos = now
        renderTimeSec += dtSec

        val latencyNanos = config.visualLatencyMs * NANOS_PER_MILLISECOND
        val targetNanos = now - latencyNanos

        // 1. Fetch spectrum values from bus
        val readBands = bus.readAtOrBefore(targetNanos, targetValues)
        val newestTs = bus.newestTimestampNanos()
        val isStale = (now - newestTs) > (latencyNanos + STALE_FRAME_GRACE_NANOS)

        if (readBands <= 0 || isStale) {
            targetValues.fill(0f)
        }

        // 2. Smooth bar dynamics
        smoother.update(targetValues, config.bandCount, dtSec, config.smoother)

        // 3. Evaluate idle state
        val maxVal = smoother.maxValue(config.bandCount)
        isIdle = (readBands <= 0 || isStale) && maxVal < RENDERER_IDLE_VALUE_THRESHOLD

        // 4. Pack band, peak, and trail arrays for vec4 uniforms
        val vec4Count = (config.bandCount + 3) / 4
        System.arraycopy(smoother.bands, 0, packedBands, 0, config.bandCount)
        System.arraycopy(smoother.peaks, 0, packedPeaks, 0, config.bandCount)
        System.arraycopy(smoother.trail, 0, packedTrail, 0, config.bandCount)

        // 5. Apply static uniforms (colors, geometry) only when config/theme/style changed
        applyStaticUniformsIfNeeded()

        // 6. Render Scene (style branch, falls back to the working program)
        if (style == RenderStyle.MIRRORED_BARS && barsProgram != 0) {
            drawMirroredBars()
        } else if (program != 0) {
            drawLed(vec4Count)
        }
    }

    /**
     * Uploads all frame-independent uniforms (band/segment counts, colors, zones,
     * geometry, background, effect strengths) when [staticUniformsDirty] is set.
     * Must be called on the GL thread with the context current.
     */
    private fun applyStaticUniformsIfNeeded() {
        if (!staticUniformsDirty) return
        staticUniformsDirty = false

        if (program != 0) {
            GLES30.glUseProgram(program)
            GLES30.glUniform1i(uBandCountLoc, config.bandCount)
            GLES30.glUniform1f(uSegmentsLoc, config.segmentCount.toFloat())
            GLES30.glUniform3f(uColLowLoc, theme.colLow.red, theme.colLow.green, theme.colLow.blue)
            GLES30.glUniform3f(uColMidLoc, theme.colMid.red, theme.colMid.green, theme.colMid.blue)
            GLES30.glUniform3f(uColHighLoc, theme.colHigh.red, theme.colHigh.green, theme.colHigh.blue)
            GLES30.glUniform2f(uZoneStartLoc, theme.zoneStart.first, theme.zoneStart.second)
            GLES30.glUniform2f(uLedHalfSizeLoc, config.ledHalfSize.first, config.ledHalfSize.second)
            GLES30.glUniform1f(uCornerRadiusLoc, config.cornerRadius)
            GLES30.glUniform1f(uOffIntensityLoc, theme.offIntensity)
            GLES30.glUniform3f(uBackgroundLoc, theme.background.red, theme.background.green, theme.background.blue)
            GLES30.glUniform1f(uGlowLoc, config.glowStrength)
            GLES30.glUniform1f(uShimmerLoc, config.shimmerStrength)
            GLES30.glUniform1f(uHotCoreLoc, config.hotCoreStrength)
            GLES30.glUniform1f(uSpecularLoc, config.specularStrength)
            GLES30.glUniform1f(uBleedLoc, config.bleedStrength)
            GLES30.glUniform1f(uGlowGradeLoc, config.glowGradeStrength)
            GLES30.glUniform1f(uFadeLoc, config.fadeStrength)
            GLES30.glUniform1f(uTrailStrengthLoc, config.trailStrength)
        }
        if (barsProgram != 0) {
            GLES30.glUseProgram(barsProgram)
            GLES30.glUniform1i(bColumnsLoc, config.columnCount)
            GLES30.glUniform1i(bBandCountLoc, config.bandCount)
            GLES30.glUniform1f(bSegmentsLoc, config.segmentCount.toFloat())
            GLES30.glUniform3f(bColBaseLoc, theme.colLow.red, theme.colLow.green, theme.colLow.blue)
            GLES30.glUniform3f(bColTargetLoc, theme.colMid.red, theme.colMid.green, theme.colMid.blue)
            GLES30.glUniform1f(bShimmerLoc, config.shimmerStrength)
            GLES30.glUniform1f(bTipGlowLoc, config.tipGlowStrength)
            GLES30.glUniform1f(bAlphaLoc, theme.colLow.alpha)
            GLES30.glUniform2f(bLedHalfSizeLoc, config.ledHalfSize.first, config.ledHalfSize.second)
            GLES30.glUniform1f(bCornerRadiusLoc, config.cornerRadius)
            GLES30.glUniform1f(bOffIntensityLoc, theme.offIntensity)
            GLES30.glUniform3f(bBackgroundLoc, theme.background.red, theme.background.green, theme.background.blue)
            GLES30.glUniform1f(bGlowLoc, config.glowStrength)
        }
    }

    private fun drawLed(vec4Count: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        GLES30.glClearColor(theme.background.red, theme.background.green, theme.background.blue, theme.background.alpha)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // Non-premultiplied alpha blending against the translucent surface.
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glUseProgram(program)

        // Per-frame uniforms only; everything else is cached by applyStaticUniformsIfNeeded().
        GLES30.glUniform2f(uResLoc, viewportWidth.toFloat(), viewportHeight.toFloat())
        GLES30.glUniform4fv(uBandsLoc, vec4Count, packedBands, 0)
        GLES30.glUniform4fv(uPeaksLoc, vec4Count, packedPeaks, 0)
        GLES30.glUniform4fv(uTrailLoc, vec4Count, packedTrail, 0)
        GLES30.glUniform1f(uTimeLoc, renderTimeSec)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun drawMirroredBars() {
        val vec4Count = (config.bandCount + 3) / 4

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // Non-premultiplied alpha blending against the translucent surface.
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glUseProgram(barsProgram)

        // Per-frame uniforms only; everything else is cached by applyStaticUniformsIfNeeded().
        GLES30.glUniform2f(bResLoc, viewportWidth.toFloat(), viewportHeight.toFloat())
        GLES30.glUniform4fv(bBandsLoc, vec4Count, packedBands, 0)
        GLES30.glUniform1f(bTimeLoc, renderTimeSec)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    /**
     * Releases OpenGL resources when surface is destroyed.
     */
    override fun release() {
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        if (barsProgram != 0) {
            GLES30.glDeleteProgram(barsProgram)
            barsProgram = 0
        }
    }
}
