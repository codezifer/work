package de.carsten.android.muzzic.visualization.render.shaders

/**
 * Shared GLSL snippets composed into fragment shaders via string templates.
 *
 * GLES has no `#include` mechanism, so sharing happens at the Kotlin level.
 * Every snippet documents the variables it expects in scope.
 */
object ShaderSnippets {

    /**
     * Pixel-space signed distance field for non-distorted rounded rectangle LEDs.
     *
     * Expects in scope: `cell` (vec2 cell UV), `cellPx` (vec2 cell size in pixels),
     * uniforms `uLedHalfSize` and `uCornerRadius`. Defines `led` (0..1 coverage
     * with ~1.5 px smoothstep antialiasing).
     */
    const val ROUNDED_BOX_SDF = """
    vec2 p          = (cell - 0.5) * cellPx;
    vec2 halfSizePx = uLedHalfSize * cellPx;
    float r         = uCornerRadius * min(cellPx.x, cellPx.y);
    vec2 q          = abs(p) - halfSizePx + r;
    float sd        = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    float led       = 1.0 - smoothstep(-0.75, 0.75, sd);      // ~1.5 px antialiasing
"""

    /**
     * Exponential halo falloff radius in pixels, shared by both raster shaders.
     *
     * The halo term is `exp(-max(sd, 0.0) / GLOW_FALLOFF_RADIUS_PX)`: at one
     * radius distance ~37% strength remains, at three radii ~5%.
     */
    const val GLOW_FALLOFF_RADIUS_PX = 8f
}
