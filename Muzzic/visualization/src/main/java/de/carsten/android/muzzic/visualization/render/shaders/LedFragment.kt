package de.carsten.android.muzzic.visualization.render.shaders

/**
 * LED raster fragment shader using pixel-space Signed Distance Field (SDF) for rounded boxes,
 * smoothstep anti-aliasing, packed `vec4` uniform arrays, three color zones, off-state LEDs,
 * and an analytic LED light kit: white-hot core with saturated mid-ring (`uHotCore`),
 * traveling dome sheen (`uSpecular`), light spill above the lit frontier plus column
 * wash (`uBleed`), level grading from dim base to hot top (`uGlowGrade`), soft
 * frontier fade-out inertia (`uFade`), lagging trail memory painted as
 * afterglow ghosts (`uTrail` / `uTrailStrength`), and a
 * two-layer halo plus wide aura (`uGlow`).
 *
 * Free hue rotation (`uHueShift`, in full turns, `0` disables) spins the zone
 * colors around the color wheel independently of the theme base color; applied
 * to `zone` before the light kit so core, halo, bleed and rim rotate together.
 *
 * All lighting terms are white-mixes or zone-multiplies, hence hue-agnostic: the
 * same strengths look identical for every theme. Each effect strength is 0 to
 * disable; `uGlow = 0` switches off halo and aura exactly (off-state rim and
 * frost stay visible by design, independent of glow).
 */
object LedFragment {

    val SOURCE = """#version 300 es
precision highp float;
precision highp int;

${ShaderSnippets.COMMON_UNIFORMS}
uniform vec3  uColLow;          // Low zone LED color (e.g. green)
uniform vec3  uColMid;          // Mid zone LED color (e.g. yellow)
uniform vec3  uColHigh;         // High zone LED color (e.g. red)
uniform vec2  uZoneStart;       // Zone thresholds: (midStart, highStart), e.g. (0.60, 0.85)

out vec4 fragColor;

${ShaderSnippets.HUE_ROTATION}
void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    float bx = uv.x * float(uBandCount);
    int band = clamp(int(floor(bx)), 0, uBandCount - 1);
    float sy = uv.y * uSegments;
    float seg = floor(sy);
    vec2 cell = vec2(fract(bx), fract(sy));

    float value = uBands[band >> 2][band & 3];
    float peak  = uPeaks[band >> 2][band & 3];
    float trailVal = uTrail[band >> 2][band & 3];

    // Pixel-space SDF for non-distorted rounded rectangle LEDs
    vec2 cellPx = uRes / vec2(float(uBandCount), uSegments);
${ShaderSnippets.ROUNDED_BOX_SDF}
    float t      = (seg + 0.5) / uSegments;
    float front  = value * uSegments;
    float hasSignal = step(0.5 / uSegments, value);
    // Soft tip: the frontier segment dims proportionally as the value decays
    // through it instead of popping off, giving blocks a fade-out inertia.
    // uFade = 0 keeps the previous binary on/off behavior exactly.
    float litHard = step(t, value);
    float litSoft = clamp(front - sy + 1.0, 0.0, 1.0) * hasSignal;
    float lit    = mix(litHard, litSoft, uFade);
    float peakSg = min(floor(peak * uSegments), uSegments - 1.0);
    float isPeak = step(abs(seg - peakSg), 0.5) * step(0.02, peak);
    float on     = max(lit, isPeak);

    vec3 zone = t < uZoneStart.x ? uColLow : (t < uZoneStart.y ? uColMid : uColHigh);

    // Free hue rotation around the color wheel; 0 keeps the theme colors exactly.
    zone = shiftHue(zone, uHueShift);

    // Shimmer only affects lit LEDs so the background stays calm.
    float shimmer = 1.0 + uShimmer * sin(uTime * 3.0 + float(band) * 0.7 + seg * 0.35);
    zone *= mix(1.0, shimmer, on);

    // Level grading: spatial ramp (calm base, hot top, flicker-free) times
    // bar energy (loud bars radiate more). Multiplies the whole light kit and
    // stays hue-agnostic; 0 disables grading (uniform glow as before).
    float grade = mix(0.15, 1.0, pow(t, 2.2));
    float energy = 0.5 + 0.5 * value;
    float glowScale = mix(1.0, grade * energy, uGlowGrade);

${ShaderSnippets.LIGHT_KIT_CORE}
${ShaderSnippets.LIGHT_KIT_GLOW}
    // Light trail: the lagging trail frontier paints fading ghosts above the
    // live frontier, so blocks glimmer out one after another. Masked to pixels
    // the live bar no longer covers; brightness follows the lag.
    float trailFront = trailVal * uSegments;
    float lag = clamp(trailVal - value, 0.0, 1.0);
    float ghost = clamp(trailFront - sy + 1.0, 0.0, 1.0) * (1.0 - lit) * step(0.001, lag);
    vec3 ghostCol = zone * ghost * (0.35 + 0.65 * lag) * uTrailStrength;

    vec3 c = litCol * mix(uOffIntensity, 1.0, on) + haloCol * (halo + aura * 0.30) + spill + wash + rimCol + ghostCol;

    // Coverage follows all light terms so glow stays visible on translucent
    // surfaces (uGlow 0 reproduces the previous behavior exactly).
    float glowCover = (bleedBand * colMask + value * colMask * colMask * 0.35) * uBleed * hasSignal * glowScale;
    float cover = clamp(led + (halo + aura * 0.30 + glowCover * 0.5) * glowScale + rim * 0.5 + ghost * uTrailStrength * 0.5, 0.0, 1.0);

    // Alpha follows LED coverage so unlit areas stay transparent on
    // translucent surfaces (ignored on opaque surfaces).
    fragColor = vec4(mix(uBackground, c, cover), cover);
}
"""
}
