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
 * All lighting terms are white-mixes or zone-multiplies, hence hue-agnostic: the
 * same strengths look identical for every theme. Each effect strength is 0 to
 * disable; `uGlow = 0` reproduces the pre-glow behavior exactly.
 */
object LedFragment {

    val SOURCE = """#version 300 es
precision highp float;
precision highp int;

uniform vec2  uRes;             // Viewport dimensions in pixels
uniform int   uBandCount;       // Number of spectrum bars (e.g. 32)
uniform float uSegments;        // Number of LED segments per bar (e.g. 20.0)
uniform vec4  uBands[16];       // Packed current bar values 0..1 (16 * vec4 = 64 values)
uniform vec4  uPeaks[16];       // Packed peak-hold values 0..1 (16 * vec4 = 64 values)
uniform vec4  uTrail[16];       // Packed trail memory 0..1 (lags bars, feeds afterglow ghosts)
uniform vec3  uColLow;          // Low zone LED color (e.g. green)
uniform vec3  uColMid;          // Mid zone LED color (e.g. yellow)
uniform vec3  uColHigh;         // High zone LED color (e.g. red)
uniform vec2  uZoneStart;       // Zone thresholds: (midStart, highStart), e.g. (0.60, 0.85)
uniform vec2  uLedHalfSize;     // LED segment half-size in cell fraction, e.g. (0.40, 0.34)
uniform float uCornerRadius;    // Relative corner radius, e.g. 0.25
uniform float uOffIntensity;    // Brightness for inactive LEDs, e.g. 0.06
uniform vec3  uBackground;      // Background RGB color
uniform float uGlow;            // Outer halo strength around lit LEDs, 0 disables
uniform float uShimmer;         // Shimmer strength on lit LEDs, 0 disables
uniform float uHotCore;         // White-hot center mix on lit LEDs, 0 disables
uniform float uSpecular;        // Traveling dome sheen on lit LEDs, 0 disables
uniform float uBleed;           // Spill above the frontier + column wash, 0 disables
uniform float uGlowGrade;       // Level grading: lower segments dimmer, 0 disables
uniform float uFade;            // Soft frontier segment fading out, 0 disables
uniform float uTrailStrength;   // Afterglow ghost intensity, 0 disables
uniform float uTime;            // Elapsed render time in seconds

out vec4 fragColor;

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

    // Shimmer only affects lit LEDs so the background stays calm.
    float shimmer = 1.0 + uShimmer * sin(uTime * 3.0 + float(band) * 0.7 + seg * 0.35);
    zone *= mix(1.0, shimmer, on);

    // Level grading: spatial ramp (calm base, hot top, flicker-free) times
    // bar energy (loud bars radiate more). Multiplies the whole light kit and
    // stays hue-agnostic; 0 disables grading (uniform glow as before).
    float grade = mix(0.15, 1.0, pow(t, 2.2));
    float energy = 0.5 + 0.5 * value;
    float glowScale = mix(1.0, grade * energy, uGlowGrade);

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
    float sweep = (cell.x - cell.y) + 0.15 * sin(uTime * 1.7 + float(band) * 0.9 + seg * 0.12);
    float spec = exp(-sweep * sweep * 18.0) * led * on;
    litCol += vec3(1.0) * spec * uSpecular * glowScale;

    // Halo plus wide aura: the tight rim stays zone-colored and overbright, the
    // aura is the same light spread thin for the glow-in-the-dark feel.
    vec3 haloCol = mix(zone * 1.6, zone, clamp(max(sd, 0.0) / 1.0, 0.0, 1.0));
    float halo = exp(-max(sd, 0.0) / ${ShaderSnippets.GLOW_FALLOFF_RADIUS_PX}) * uGlow * on * glowScale;
    float aura = exp(-max(sd, 0.0) / (${ShaderSnippets.GLOW_FALLOFF_RADIUS_PX} * 3.0)) * uGlow * on * glowScale;

    // Bleed: light spilling into the dark segments just above the lit frontier,
    // plus a faint wash along the whole column. Gated on real signal so silence
    // stays dark.
    float above = sy - front;
    float bleedBand = exp(-max(above, 0.0) * 1.4) * step(0.0, above) * step(above, 3.0);
    float colMask = 1.0 - abs(cell.x - 0.5) * 2.0;
    vec3 spill = zone * bleedBand * colMask * uBleed * hasSignal * glowScale;
    vec3 wash = zone * value * colMask * colMask * uBleed * 0.35 * hasSignal * glowScale;

    // Off-state rim: unlit LED plastic catching ambient light at its edge.
    float rim = smoothstep(0.0, 1.5, sd) * (1.0 - smoothstep(1.5, 3.5, sd)) * (1.0 - on);
    vec3 rimCol = zone * rim * 0.10;

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
