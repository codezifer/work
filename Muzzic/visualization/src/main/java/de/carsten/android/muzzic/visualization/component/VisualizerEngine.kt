package de.carsten.android.muzzic.visualization.component

/**
 * Supported rendering engines for music visualization.
 */
enum class VisualizerEngine {
    /** Mirrored segmented bars rendered via GLES 3.0 (classic look, modern pipeline). */
    BARS,

    /** 3D OpenGL Milkdrop visualizer powered by libprojectM */
    PROJECT_M,

    /** 80s HiFi LED Bar Spectrum Analyzer powered by GLES 3.0 */
    LED_SPECTRUM,
}
