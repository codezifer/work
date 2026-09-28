package de.carsten.android.muzzic.visualization.render

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.util.Log

private const val TAG = "EglManager"

/**
 * Minimal EGL14 context manager for rendering into a [TextureView] surface.
 *
 * Unlike `GLSurfaceView`, a `TextureView` composites inside the app window, so
 * transparency reveals the app UI beneath and Compose clip/alpha apply. All
 * methods must be called on the same thread (the dedicated render thread in our
 * setup: `SurfaceTextureListener` callbacks post their work there and the frame
 * driver runs there as well). Cross-thread use is logged and ignored where safe.
 */
class EglManager {

    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var surface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null
    private var ownerThread: Thread? = null

    /** Whether a current EGL context with a live surface exists. */
    val isReady: Boolean
        get() = display !== EGL14.EGL_NO_DISPLAY &&
            context !== EGL14.EGL_NO_CONTEXT &&
            surface !== EGL14.EGL_NO_SURFACE

    /**
     * Creates an OpenGL ES 3.0 context bound to [surfaceTexture].
     *
     * @return `true` on success; on failure a warning is logged and [release] state holds.
     */
    fun init(surfaceTexture: SurfaceTexture, width: Int, height: Int): Boolean {
        release()

        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display === EGL14.EGL_NO_DISPLAY) {
            Log.w(TAG, "eglGetDisplay failed")
            release()
            return false
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            Log.w(TAG, "eglInitialize failed")
            release()
            return false
        }

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
            Log.w(TAG, "eglChooseConfig found no RGBA8888 ES3 config")
            release()
            return false
        }

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (context === EGL14.EGL_NO_CONTEXT) {
            Log.w(TAG, "eglCreateContext failed")
            release()
            return false
        }

        surfaceTexture.setDefaultBufferSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
        surface = EGL14.eglCreateWindowSurface(display, configs[0], surfaceTexture, intArrayOf(EGL14.EGL_NONE), 0)
        if (surface === EGL14.EGL_NO_SURFACE) {
            Log.w(TAG, "eglCreateWindowSurface failed")
            release()
            return false
        }

        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            Log.w(TAG, "eglMakeCurrent failed")
            release()
            return false
        }
        this.surfaceTexture = surfaceTexture
        ownerThread = Thread.currentThread()
        return true
    }

    /**
     * Updates the backing buffer size after the view resized.
     *
     * Must be called on the same thread as [init]; the EGL window surface picks
     * up the new size on the next [swapBuffers].
     */
    fun setBufferSize(width: Int, height: Int) {
        if (!checkThread("setBufferSize")) return
        surfaceTexture?.setDefaultBufferSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
    }

    /**
     * Presents the current framebuffer. Returns `false` when no surface is live.
     */
    fun swapBuffers(): Boolean {
        if (!isReady) return false
        if (!checkThread("swapBuffers")) return false
        return EGL14.eglSwapBuffers(display, surface)
    }

    /**
     * Destroys surface, context, and display connection. Safe to call repeatedly
     * and from any thread (only the owner thread destroys live resources).
     */
    fun release() {
        if (!checkThread("release")) return
        if (display !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        }
        if (surface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, surface)
            surface = EGL14.EGL_NO_SURFACE
        }
        if (context !== EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
            context = EGL14.EGL_NO_CONTEXT
        }
        if (display !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        surfaceTexture = null
        ownerThread = null
    }

    private fun checkThread(op: String): Boolean {
        val owner = ownerThread
        if (owner != null && Thread.currentThread() !== owner) {
            Log.w(TAG, "$op called on ${Thread.currentThread().name} but EGL owner is ${owner.name}; ignoring")
            return false
        }
        return true
    }
}
