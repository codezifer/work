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
 */
class ProjectMGLSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val renderer = ProjectMRenderer(context)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ProjectMNativeBridge.release()
    }

    private class ProjectMRenderer(private val context: Context) : Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            val presetDir = PresetManager.ensurePresetsExtracted(context)
            ProjectMNativeBridge.init(presetDir)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            ProjectMNativeBridge.resize(width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            ProjectMNativeBridge.render()
        }
    }
}
