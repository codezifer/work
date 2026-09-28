package de.carsten.android.muzzic.visualization.component

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import de.carsten.android.muzzic.visualization.projectm.PresetManager
import de.carsten.android.muzzic.visualization.projectm.ProjectMNativeBridge
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Custom GLSurfaceView rendering the 3D Milkdrop ProjectM visualizer on an EGL context.
 *
 * Holds only the application context (never an activity) and pauses its render
 * thread via [isPlaying], so an inaudible/invisible visualizer burns no GPU.
 * Preset extraction runs on a background thread ([PresetManager]); the GL thread
 * never performs file I/O.
 */
class ProjectMGLSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val appContext = context.applicationContext
    private val renderer = ProjectMRenderer()

    var presetName: String? = null
        set(value) {
            if (field != value) {
                field = value
                if (!value.isNullOrEmpty()) {
                    queueEvent {
                        ProjectMNativeBridge.selectPreset(value)
                    }
                }
            }
        }

    /**
     * Playback gate. The GL thread intentionally keeps running while paused:
     * projectM eases out on decaying audio and then idles calmly instead of
     * freezing hard. On pause only stale queued PCM is dropped so the decay
     * starts clean; resume needs nothing (rendering never stopped).
     * Must be set from the UI thread.
     */
    var isPlaying: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    ProjectMNativeBridge.clearPcm()
                }
            }
        }

    init {
        setEGLContextClientVersion(3)
        // ProjectM renders textured fullscreen quads; no depth or stencil buffer needed.
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setZOrderMediaOverlay(true)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        // Extract presets early on a background thread. The GL thread only calls
        // init() (which rescans the directory), so first frames never block on I/O.
        // The re-init after extraction completes only rescans when the instance exists.
        PresetManager.ensurePresetsExtractedAsync(appContext) { path ->
            queueEvent {
                ProjectMNativeBridge.init(path)
            }
        }
    }

    override fun onDetachedFromWindow() {
        // Release on the GL thread before teardown drains the event queue.
        try {
            queueEvent {
                ProjectMNativeBridge.release()
            }
        } catch (_: RuntimeException) {
            // Renderer thread already gone; nothing live to release.
        }
        super.onDetachedFromWindow()
    }

    private inner class ProjectMRenderer : Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // Fresh instance for the new GL context: a surviving instance would
            // hold handles of a dead context and crash the renderer. No I/O here
            // beyond ensuring the directory exists; extraction runs async.
            val presetDir = PresetManager.presetDirPath(appContext)
            ProjectMNativeBridge.recreate(presetDir)
            presetName?.let { name ->
                if (name.isNotEmpty()) {
                    ProjectMNativeBridge.selectPreset(name)
                }
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            ProjectMNativeBridge.resize(width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            ProjectMNativeBridge.render()
        }
    }
}
