package de.carsten.android.muzzic.visualization.render

import android.opengl.GLES30
import android.util.Log

/**
 * OpenGL ES 3.0 utility functions for compiling shaders, linking programs, and checking GL errors.
 */
object GlUtil {
    private const val TAG = "GlUtil"

    /**
     * Checks if GLES 3.0 report any error and logs a warning if found.
     *
     * @param op Description of the OpenGL operation being performed.
     * @return `true` if an error occurred.
     */
    fun checkGlError(op: String): Boolean {
        var error = GLES30.glGetError()
        var hasError = false
        while (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "GL Error in $op: 0x${Integer.toHexString(error)}")
            hasError = true
            error = GLES30.glGetError()
        }
        return hasError
    }

    /**
     * Compiles a shader of the specified [type] from GLSL source string.
     */
    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Could not create GL shader of type $type")
            return 0
        }

        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val infoLog = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "Could not compile shader type $type: $infoLog")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    /**
     * Links a vertex shader and a fragment shader into an executable OpenGL ES program.
     */
    fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        if (vertexShader == 0) return 0

        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader)
            return 0
        }

        val program = GLES30.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create GL program")
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }

        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val infoLog = GLES30.glGetProgramInfoLog(program)
            Log.e(TAG, "Could not link GL program: $infoLog")
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }

        // Clean up attached shader objects
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return program
    }
}
