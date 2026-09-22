package de.carsten.android.muzzic.visualization.projectm

import android.util.Log

/**
 * JNI bridge singleton interfacing with the native C++ projectM library.
 *
 * Loads the prebuilt `libprojectM-4.so` (public C API) first and then the
 * module's own `libprojectm_bridge.so` JNI layer on top of it. If either
 * library is missing (e.g. an ABI slice without prebuilt), all calls become
 * safe no-ops and the GL surface falls back to a static background color.
 */
object ProjectMNativeBridge {

    private const val TAG = "ProjectMNativeBridge"

    private var isNativeLibraryLoaded = false

    init {
        try {
            // Load order matters: the bridge links against libprojectM-4.so.
            System.loadLibrary("projectM-4")
            System.loadLibrary("projectm_bridge")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Successfully loaded libprojectM-4.so and libprojectm_bridge.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Could not load native projectM libraries, visualization disabled", e)
            isNativeLibraryLoaded = false
        }
    }

    /**
     * Creates the native projectM instance for the current GL context.
     *
     * Must be called on the GL thread with a current EGL context, typically
     * from `GLSurfaceView.Renderer.onSurfaceCreated`.
     *
     * @param presetDirPath Absolute path of the directory holding `.milk` preset files.
     */
    fun init(presetDirPath: String) {
        if (isNativeLibraryLoaded) {
            nativeInit(presetDirPath)
        }
    }

    /**
     * Updates the native viewport size.
     *
     * @param width New viewport width in pixels.
     * @param height New viewport height in pixels.
     */
    fun resize(width: Int, height: Int) {
        if (isNativeLibraryLoaded) {
            nativeResize(width, height)
        }
    }

    /**
     * Renders a single projectM frame into the current framebuffer.
     *
     * Must be called on the GL thread, typically from `onDrawFrame`.
     */
    fun render() {
        if (isNativeLibraryLoaded) {
            nativeRender()
        }
    }

    /**
     * Feeds interleaved PCM float samples (range -1 to 1) to projectM.
     *
     * @param pcmData Interleaved samples in LRLR order for stereo input.
     * @param channels Channel count of [pcmData]: 1 for mono, 2 for stereo.
     */
    fun addPcm(pcmData: FloatArray, channels: Int) {
        if (isNativeLibraryLoaded) {
            nativeAddPcm(pcmData, channels)
        }
    }

    /** Loads the next preset from the preset directory with a smooth transition. */
    fun nextPreset() {
        if (isNativeLibraryLoaded) {
            nativeNextPreset()
        }
    }

    /** Loads the previous preset from the preset directory with a smooth transition. */
    fun previousPreset() {
        if (isNativeLibraryLoaded) {
            nativePreviousPreset()
        }
    }

    /** Destroys the native projectM instance and frees its resources. */
    fun release() {
        if (isNativeLibraryLoaded) {
            nativeRelease()
        }
    }

    private external fun nativeInit(presetDirPath: String)
    private external fun nativeResize(width: Int, height: Int)
    private external fun nativeRender()
    private external fun nativeAddPcm(pcmData: FloatArray, channels: Int)
    private external fun nativeNextPreset()
    private external fun nativePreviousPreset()
    private external fun nativeRelease()
}
