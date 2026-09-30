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

    /**
     * Free hue-rotation helpers shared by both raster shaders.
     *
     * Defines `hueRgbToHsv`, `hueHsvToRgb` and `shiftHue(c, shift)`; `shift` is
     * in full turns (`0.0..1.0`, `fract`-wrapped), so the CPU uploads
     * `renderTime * speed` directly. `shiftHue` returns `c` unchanged for
     * `shift == 0.0`, keeping `hueRotationSpeed = 0` bit-exact with the
     * previous rendering.
     */
    const val HUE_ROTATION = """
    vec3 hueRgbToHsv(vec3 c) {
        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }
    vec3 hueHsvToRgb(vec3 c) {
        vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
        return c.z * mix(vec3(1.0), rgb, c.y);
    }
    vec3 shiftHue(vec3 c, float shift) {
        if (shift == 0.0) return c;
        vec3 hsv = hueRgbToHsv(c);
        hsv.x = fract(hsv.x + shift);
        return hueHsvToRgb(hsv);
    }
"""
}
