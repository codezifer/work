package de.carsten.android.muzzic.visualization.render.shaders

/**
 * LED raster fragment shader using pixel-space Signed Distance Field (SDF) for rounded boxes,
 * smoothstep anti-aliasing, packed `vec4` uniform arrays, three color zones, and off-state LEDs.
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
uniform vec3  uColLow;          // Low zone LED color (e.g. green)
uniform vec3  uColMid;          // Mid zone LED color (e.g. yellow)
uniform vec3  uColHigh;         // High zone LED color (e.g. red)
uniform vec2  uZoneStart;       // Zone thresholds: (midStart, highStart), e.g. (0.60, 0.85)
uniform vec2  uLedHalfSize;     // LED segment half-size in cell fraction, e.g. (0.40, 0.34)
uniform float uCornerRadius;    // Relative corner radius, e.g. 0.25
uniform float uOffIntensity;    // Brightness for inactive LEDs, e.g. 0.06
uniform vec3  uBackground;      // Background RGB color

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

    // Pixel-space SDF for non-distorted rounded rectangle LEDs
    vec2 cellPx = uRes / vec2(float(uBandCount), uSegments);
${ShaderSnippets.ROUNDED_BOX_SDF}
    float t      = (seg + 0.5) / uSegments;
    float lit    = step(t, value);
    float peakSg = min(floor(peak * uSegments), uSegments - 1.0);
    float isPeak = step(abs(seg - peakSg), 0.5) * step(0.02, peak);
    float on     = max(lit, isPeak);

    vec3 zone = t < uZoneStart.x ? uColLow : (t < uZoneStart.y ? uColMid : uColHigh);
    vec3 c    = zone * mix(uOffIntensity, 1.0, on);

    fragColor = vec4(mix(uBackground, c, led), 1.0);
}
"""
}
