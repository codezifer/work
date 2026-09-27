package de.carsten.android.muzzic.visualization.render

/**
 * Minimal lifecycle contract for a frame renderer.
 *
 * The host (e.g. a `TextureView` with an EGL context) owns the surface and calls
 * the callbacks on a single thread with the GL context current. Implementations
 * stay platform-agnostic and never touch Android views directly.
 */
interface FrameRenderer {
    /** Whether the renderer has settled to silence and can pause frame scheduling. */
    val isIdle: Boolean

    /** Allocates GL programs and caches uniform locations. */
    fun onSurfaceCreated()

    /**
     * Updates the viewport after surface creation or size changes.
     *
     * @param width Surface width in pixels.
     * @param height Surface height in pixels.
     */
    fun onSurfaceChanged(width: Int, height: Int)

    /** Renders a single frame with the GL context current. */
    fun onDrawFrame()

    /** Releases GL resources when the surface is destroyed. */
    fun release()
}
