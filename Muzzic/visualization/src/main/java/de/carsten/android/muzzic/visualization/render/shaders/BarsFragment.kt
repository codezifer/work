package de.carsten.android.muzzic.visualization.render.shaders

/**
 * Mirrored segmented-bars fragment shader: the classic BARS look rendered via GLES.
 *
 * Visual columns are mirrored horizontally (bass in the center) and folded
 * vertically (bidirectional bars growing from the vertical center). Colors
 * form a base-to-target gradient from center to edge with amplitude-driven
 * white glow, time-based shimmer on lit LEDs, a tip highlight, and an outer
 * halo around lit segments (`uGlow`).
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

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    float fx = uv.x * float(uColumns);
    int col = clamp(int(floor(fx)), 0, uColumns - 1);

    // Horizontal mirror: bass frequencies sit in the center.
    int halfCols = uColumns / 2;
    int m = col < halfCols ? (halfCols - 1 - col) : (col - halfCols);
    int band = clamp(m, 0, uBandCount - 1);

    // Vertical fold: bars grow bidirectionally from the center line.
    float halfSegs = uSegments * 0.5;
    float sy = abs(uv.y - 0.5) * 2.0 * halfSegs;
    float seg = floor(sy);
    vec2 cell = vec2(fract(fx), fract(sy));

    float value = uBands[band >> 2][band & 3];

    // Pixel-space SDF for non-distorted rounded rectangle LEDs
    vec2 cellPx = uRes / vec2(float(uColumns), uSegments);
${ShaderSnippets.ROUNDED_BOX_SDF}
    // t runs 0 at the center to 1 at the outer edges on both halves.
    float t   = (seg + 0.5) / halfSegs;
    float lit = step(t, value);
    float on  = lit;

    vec3 grad = mix(uColBase, uColTarget, clamp(t, 0.0, 1.0));

    // Amplitude-driven white glow, matching the legacy Canvas bars (0.35 max).
    vec3 onCol = mix(grad, vec3(1.0), clamp(value * 0.35, 0.0, 1.0));

    // Shimmer only affects lit LEDs so the background stays calm.
    float shimmer = 1.0 + uShimmer * sin(uTime * 3.0 + float(band) * 0.7 + seg * 0.35);
    onCol *= mix(1.0, shimmer, on);

    // Tip highlight on the outermost lit segment of each column.
    float litCount = floor(value * halfSegs);
    float isTip = step(abs(seg - (litCount - 0.5)), 0.5) * step(0.5, litCount) * on;
    onCol = mix(onCol, vec3(1.0), uTipGlow * isTip);

    // Outer halo: exponential falloff with pixel-space SDF distance (see GLOW_FALLOFF_RADIUS_PX).
    // Zone-colored halo with an overbright rim at the block edge (no white, so
    // the gradient hues stay dominant); the tips stay hot through the
    // amplitude-driven white mix in onCol above.
    float halo = exp(-max(sd, 0.0) / ${ShaderSnippets.GLOW_FALLOFF_RADIUS_PX}) * uGlow * on;
    vec3 haloCol = mix(grad * 1.4, grad, clamp(max(sd, 0.0) / 1.0, 0.0, 1.0));
    vec3 c = mix(grad * uOffIntensity, onCol, on) + haloCol * halo;
    float cover = clamp(led + halo, 0.0, 1.0);

    float vis = max(on, step(0.001, uOffIntensity));
    fragColor = vec4(mix(uBackground, c, cover), cover * uAlpha * vis);
}
"""
}
