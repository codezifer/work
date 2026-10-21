package de.carsten.android.muzzic.visualization.render.shaders

/**
 * Shared GLSL snippets composed into fragment shaders via string templates.
 *
 * GLES has no `#include` mechanism, so sharing happens at the Kotlin level.
 * Every snippet documents the variables it expects in scope.
 */
object ShaderSnippets {

    /**
     * Exponential halo falloff radius in pixels, shared by both raster shaders.
     *
     * The halo term is `exp(-max(sd, 0.0) / GLOW_FALLOFF_RADIUS_PX)`: at one
     * radius distance ~37% strength remains, at three radii ~5%.
     *
     * Declared before the snippets below because they interpolate its value.
     */
    const val GLOW_FALLOFF_RADIUS_PX = 8f

    /**
     * Uniform declarations shared by the LED and mirrored-BARS fragment shaders.
     *
     * Keeping them here prevents silent drift between [LedFragment] and
     * [BarsFragment]: a name declared twice fails to compile, a missing one
     * leaves the whole program at 0.
     */
    const val COMMON_UNIFORMS = """
uniform vec2  uRes;             // Viewport dimensions in pixels
uniform int   uBandCount;       // Number of spectrum bands in uBands (e.g. 32)
uniform float uSegments;        // LED segments per bar (LED) / total rows (BARS)
uniform vec4  uBands[16];       // Packed current values 0..1 (16 * vec4 = 64 values)
uniform vec4  uPeaks[16];       // Packed peak-hold values 0..1 (16 * vec4 = 64 values)
uniform vec4  uTrail[16];       // Packed trail memory 0..1 (lags bars, feeds afterglow ghosts)
uniform vec2  uLedHalfSize;     // LED segment half-size in cell fraction, e.g. (0.40, 0.34)
uniform float uCornerRadius;    // Relative corner radius, e.g. 0.25
uniform float uOffIntensity;    // Brightness for inactive LEDs, 0 hides them
uniform vec3  uBackground;      // Background RGB color
uniform float uGlow;            // Outer halo strength around lit LEDs, 0 disables
uniform float uShimmer;         // Shimmer strength on lit LEDs, 0 disables
uniform float uHotCore;         // White-hot center mix on lit LEDs, 0 disables
uniform float uSpecular;        // Traveling dome sheen on lit LEDs, 0 disables
uniform float uBleed;           // Spill above the frontier + column wash, 0 disables
uniform float uGlowGrade;       // Level grading: lower segments dimmer, 0 disables
uniform float uFade;            // Soft frontier segment fading out, 0 disables
uniform float uTrailStrength;   // Afterglow ghost intensity, 0 disables
uniform float uHueShift;        // Free hue rotation in full turns, 0 disables
uniform float uTime;            // Elapsed render time in seconds
"""

    /**
     * Hot core and dome specular of the analytic LED light kit.
     *
     * Expects in scope: `p`/`halfSizePx`/`r`/`led` (from [ROUNDED_BOX_SDF]),
     * `on`, `zone`, `glowScale`, `cell`, `band`, `seg`, `uTime`, `uHotCore`,
     * `uSpecular`. Defines `litCol`.
     */
    const val LIGHT_KIT_CORE = """
    // Hot core: a scaled copy of the rounded-box SDF burns toward white in the
    // center while the ring around it stays overbright and saturated. Both terms
    // scale with uHotCore, so 0 reproduces the previous flat look exactly.
    vec2 qCore = abs(p) - halfSizePx * 0.45 + r * 0.45;
    float sdCore = length(max(qCore, 0.0)) + min(max(qCore.x, qCore.y), 0.0) - r * 0.45;
    float core = (1.0 - smoothstep(-0.75, 0.75, sdCore)) * on;
    float ring = clamp(led - core, 0.0, 1.0);
    vec3 litCol = zone * (1.0 + 0.35 * ring * uHotCore * glowScale);
    litCol = mix(litCol, vec3(1.0), core * uHotCore * glowScale);

    // Dome specular: diagonal sheen sweeping slowly across the LED face.
    // cell.y is the (possibly folded) cell coordinate, so mirrored halves stay
    // symmetric.
    float sweep = (cell.x - cell.y) + 0.15 * sin(uTime * 1.7 + float(band) * 0.9 + seg * 0.12);
    float spec = exp(-sweep * sweep * 18.0) * led * on;
    litCol += vec3(1.0) * spec * uSpecular * glowScale;
"""

    /**
     * Halo, aura, bleed/wash and off-state rim of the analytic LED light kit.
     *
     * Expects in scope: `sd`/`led` (from [ROUNDED_BOX_SDF]), `zone`, `on`,
     * `glowScale`, `sy`, `front`, `cell`, `hasSignal`, `uGlow`, `uBleed`.
     * Defines `haloCol`, `halo`, `aura`, `bleedBand`, `colMask`, `spill`,
     * `wash`, `rim`, `rimCol`.
     */
    const val LIGHT_KIT_GLOW = """
    // Halo plus wide aura: the tight rim stays zone-colored and overbright, the
    // aura is the same light spread thin for the glow-in-the-dark feel.
    vec3 haloCol = mix(zone * 1.6, zone, clamp(max(sd, 0.0) / 1.0, 0.0, 1.0));
    float halo = exp(-max(sd, 0.0) / $GLOW_FALLOFF_RADIUS_PX) * uGlow * on * glowScale;
    float aura = exp(-max(sd, 0.0) / ($GLOW_FALLOFF_RADIUS_PX * 3.0)) * uGlow * on * glowScale;

    // Bleed: light spilling into the dark segments just above the lit frontier,
    // plus a faint wash along the whole column. Gated on real signal so silence
    // stays dark.
    float above = sy - front;
    float bleedBand = exp(-max(above, 0.0) * 1.4) * step(0.0, above) * step(above, 3.0);
    float colMask = 1.0 - abs(cell.x - 0.5) * 2.0;
    vec3 spill = zone * bleedBand * colMask * uBleed * hasSignal * glowScale;
    vec3 wash = zone * value * colMask * colMask * uBleed * 0.35 * hasSignal * glowScale;

    // Off-state rim: unlit LED plastic catching ambient light at its edge.
    // Frost look: 0.22 makes unlit segments readable as matte acrylic glass.
    float rim = smoothstep(0.0, 1.5, sd) * (1.0 - smoothstep(1.5, 3.5, sd)) * (1.0 - on);
    vec3 rimCol = zone * rim * 0.22;
"""

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
