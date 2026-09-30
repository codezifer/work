package de.carsten.android.muzzic.visualization.render.shaders

/**
 * Mirrored segmented-bars fragment shader: the classic BARS look rendered via GLES.
 *
 * Visual columns are mirrored horizontally (bass in the center) and folded
 * vertically (bidirectional bars growing from the vertical center). Colors
 * form a base-to-target gradient from center to edge with amplitude-driven
 * white glow, time-based shimmer on lit LEDs, a tip highlight, and the full
 * LED light kit ported from [LedFragment]: white-hot core with saturated
 * mid-ring (`uHotCore`), traveling dome sheen (`uSpecular`), light spill above
 * the lit frontier plus column wash (`uBleed`), level grading (`uGlowGrade`),
 * soft frontier fade-out inertia (`uFade`), lagging trail memory painted as
 * afterglow ghosts (`uTrail` / `uTrailStrength`), and a two-layer halo plus
 * wide aura (`uGlow`).
 *
 * Geometry note: `sy`/`seg`/`t` are already folded around the vertical center
 * (`abs(uv.y - 0.5)`), so all light terms work on the folded coordinates with
 * no extra mirroring. `cellPx` and the shared SDF block stay on the full
 * `uSegments`; only frontier logic (`front`, `hasSignal`, `litSoft`) uses
 * `halfSegs`.
 *
 * Alpha semantics: `fragColor.a = cover * uAlpha * vis` — `uAlpha` comes from
 * `theme.colLow.alpha`, `vis` keeps unlit areas transparent while
 * `uOffIntensity` is zero.
 *
 * Each effect strength is 0 to disable; `uGlow = 0`, `uHotCore = 0`,
 * `uSpecular = 0`, `uBleed = 0`, `uGlowGrade = 0`, `uFade = 0` and
 * `uTrailStrength = 0` each switch off exactly their own term. Free hue
 * rotation (`uHueShift`, in full turns, `0` disables) spins the base-to-target
 * gradient around the color wheel independently of the theme base color;
 * applied to `zone` before the light kit so core, halo, bleed and rim rotate
 * together.
 *
 * Note: `half` is a reserved word in GLSL ES 3.00 and must never be used as an
 * identifier here (see ShaderReservedWordsTest).
 */
object BarsFragment {

    val SOURCE = """#version 300 es
precision highp float;
precision highp int;

uniform vec2  uRes;             // Viewport dimensions in pixels
uniform int   uColumns;         // Number of visual columns (e.g. 64)
uniform int   uBandCount;       // Number of spectrum bands in uBands (e.g. 32)
uniform float uSegments;        // Total LED rows, split in halves (e.g. 16.0)
uniform vec4  uBands[16];       // Packed band values 0..1 (16 * vec4 = 64 values)
uniform vec4  uPeaks[16];       // Packed peak-hold values 0..1 (16 * vec4 = 64 values)
uniform vec4  uTrail[16];       // Packed trail memory 0..1 (lags bars, feeds afterglow ghosts)
uniform vec3  uColBase;         // Gradient color at the vertical center
uniform vec3  uColTarget;       // Gradient color at the outer edges
uniform float uShimmer;         // Shimmer strength on lit LEDs, 0 disables
uniform float uTipGlow;         // White highlight on tip segments, 0 disables
uniform float uTime;            // Elapsed render time in seconds
uniform float uAlpha;           // Global alpha multiplier
uniform vec2  uLedHalfSize;     // LED segment half-size in cell fraction
uniform float uCornerRadius;    // Relative corner radius
uniform float uOffIntensity;    // Brightness for inactive LEDs, 0 hides them
uniform vec3  uBackground;      // Background RGB color
uniform float uGlow;            // Outer halo strength around lit LEDs, 0 disables
uniform float uHotCore;         // White-hot center mix on lit LEDs, 0 disables
uniform float uSpecular;        // Traveling dome sheen on lit LEDs, 0 disables
uniform float uBleed;           // Spill above the frontier + column wash, 0 disables
uniform float uGlowGrade;       // Level grading: center dimmer, edges full, 0 disables
uniform float uFade;            // Soft frontier segment fading out, 0 disables
uniform float uTrailStrength;   // Afterglow ghost intensity, 0 disables
uniform float uHueShift;        // Free hue rotation in full turns, 0 disables

out vec4 fragColor;

${ShaderSnippets.HUE_ROTATION}
void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    float fx = uv.x * float(uColumns);
    int col = clamp(int(floor(fx)), 0, uColumns - 1);

    // Horizontal mirror: bass frequencies sit in the center.
    int halfCols = uColumns / 2;
    int m = col < halfCols ? (halfCols - 1 - col) : (col - halfCols);
    int band = clamp(m, 0, uBandCount - 1);

    // Vertical fold: bars grow bidirectionally from the center line.
    // halfSegs is the per-half segment count; sy/seg/t are folded already.
    float halfSegs = uSegments * 0.5;
    float sy = abs(uv.y - 0.5) * 2.0 * halfSegs;
    float seg = floor(sy);
    vec2 cell = vec2(fract(fx), fract(sy));

    // Same packed-band indexing as LedFragment; band comes from the mirror above.
    float value = uBands[band >> 2][band & 3];
    float peak = uPeaks[band >> 2][band & 3];
    float trailVal = uTrail[band >> 2][band & 3];

    // Pixel-space SDF for non-distorted rounded rectangle LEDs.
    // Invariant: cellPx stays on the FULL uSegments so LED size never changes;
    // only frontier logic below uses halfSegs.
    vec2 cellPx = uRes / vec2(float(uColumns), uSegments);
${ShaderSnippets.ROUNDED_BOX_SDF}
    // t runs 0 at the center to 1 at the outer edges on both halves.
    float t   = (seg + 0.5) / halfSegs;
    float front = value * halfSegs;
    float hasSignal = step(0.5 / halfSegs, value);
    // Soft tip: the frontier segment dims proportionally as the value decays
    // through it instead of popping off. uFade = 0 keeps binary on/off exactly.
    float litHard = step(t, value);
    float litSoft = clamp(front - sy + 1.0, 0.0, 1.0) * hasSignal;
    float lit = mix(litHard, litSoft, uFade);
    // True peak-hold marker from the smoother (holds, then falls slowly).
    float peakSg = min(floor(peak * halfSegs), halfSegs - 1.0);
    float isPeak = step(abs(seg - peakSg), 0.5) * step(0.02, peak);
    float on  = max(lit, isPeak);

    vec3 zone = mix(uColBase, uColTarget, clamp(t, 0.0, 1.0));

    // Free hue rotation around the color wheel; 0 keeps the gradient exactly.
    zone = shiftHue(zone, uHueShift);

    // Amplitude-driven white glow, matching the legacy Canvas bars (0.35 max).
    zone = mix(zone, vec3(1.0), clamp(value * 0.35, 0.0, 1.0));

    // Shimmer only affects lit LEDs so the background stays calm.
    float shimmer = 1.0 + uShimmer * sin(uTime * 3.0 + float(band) * 0.7 + seg * 0.35);
    zone *= mix(1.0, shimmer, on);

    // Level grading: spatial ramp (calm center, hot edges, flicker-free) times
    // bar energy (loud bars radiate more). Hue-agnostic; 0 disables grading.
    float grade = mix(0.15, 1.0, pow(t, 2.2));
    float energy = 0.5 + 0.5 * value;
    float glowScale = mix(1.0, grade * energy, uGlowGrade);

    // Hot core: a scaled copy of the rounded-box SDF burns toward white in the
    // center while the ring around it stays overbright and saturated.
    // uHotCore = 0 reproduces the previous flat look exactly.
    vec2 qCore = abs(p) - halfSizePx * 0.45 + r * 0.45;
    float sdCore = length(max(qCore, 0.0)) + min(max(qCore.x, qCore.y), 0.0) - r * 0.45;
    float core = (1.0 - smoothstep(-0.75, 0.75, sdCore)) * on;
    float ring = clamp(led - core, 0.0, 1.0);
    vec3 litCol = zone * (1.0 + 0.35 * ring * uHotCore * glowScale);
    litCol = mix(litCol, vec3(1.0), core * uHotCore * glowScale);

    // Dome specular: diagonal sheen sweeping slowly across the LED face.
    // cell.y is fract(sy) from the FOLDED coordinate, so top/bottom halves
    // stay mirror-symmetric.
    float sweep = (cell.x - cell.y) + 0.15 * sin(uTime * 1.7 + float(band) * 0.9 + seg * 0.12);
    float spec = exp(-sweep * sweep * 18.0) * led * on;
    litCol += vec3(1.0) * spec * uSpecular * glowScale;

    // Tip highlight on the outermost lit segment of each column, applied on top
    // of the light kit (fast amplitude-driven mix plus structural hot core).
    float litCount = floor(value * halfSegs);
    float isTip = step(abs(seg - (litCount - 0.5)), 0.5) * step(0.5, litCount) * on;
    litCol = mix(litCol, vec3(1.0), uTipGlow * isTip);

    // Halo plus wide aura: the tight rim stays gradient-colored and overbright,
    // the aura is the same light spread thin for the glow-in-the-dark feel.
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
    vec3 rimCol = zone * rim * 0.22;

    // Light trail: the lagging trail frontier paints fading ghosts above the
    // live frontier, so blocks glimmer out one after another. Masked to pixels
    // the live bar no longer covers; brightness follows the lag.
    float trailFront = trailVal * halfSegs;
    float lag = clamp(trailVal - value, 0.0, 1.0);
    float ghost = clamp(trailFront - sy + 1.0, 0.0, 1.0) * (1.0 - lit) * step(0.001, lag);
    vec3 ghostCol = zone * ghost * (0.35 + 0.65 * lag) * uTrailStrength;

    vec3 c = mix(zone * uOffIntensity, litCol, on) + haloCol * (halo + aura * 0.30) + spill + wash + rimCol + ghostCol;

    // Coverage follows all light terms so glow stays visible on translucent
    // surfaces. uAlpha/visGate preserved: alpha stays transparent where nothing
    // is lit and uOffIntensity is zero.
    float glowCover = (bleedBand * colMask + value * colMask * colMask * 0.35) * uBleed * hasSignal * glowScale;
    float cover = clamp(led + (halo + aura * 0.30 + glowCover * 0.5) * glowScale + rim * 0.5 + ghost * uTrailStrength * 0.5, 0.0, 1.0);

    float vis = max(on, step(0.001, uOffIntensity));
    fragColor = vec4(mix(uBackground, c, cover), cover * uAlpha * vis);
}
"""
}
