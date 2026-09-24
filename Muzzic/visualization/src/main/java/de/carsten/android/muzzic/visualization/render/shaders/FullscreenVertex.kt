package de.carsten.android.muzzic.visualization.render.shaders

/**
 * Single-triangle full-screen quad vertex shader (no VBO needed).
 *
 * Generates a full-screen triangle covering normalized device coordinates (-1..1).
 */
object FullscreenVertex {

    const val SOURCE = """#version 300 es
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
"""
}
