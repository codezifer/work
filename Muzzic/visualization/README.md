# Muzzic Audio Visualization Module (`:visualization`)

This Android Library module encapsulates all audio visualization components for Muzzic, including:
- **2D Segmented Bars Visualizer** (Pure Jetpack Compose Canvas)
- **3D Milkdrop Visualizer** (Powered by `libprojectM` C++ engine via OpenGL ES 2.0 / 3.0 and JNI, using the public **C API** `projectM-4/projectM.h`)

> **Important:** The prebuilt `libprojectM-4.so` only exports the public C API
> (`projectm_*` functions). The C++ class API (`libprojectM::ProjectM`) has
> hidden visibility — the JNI bridge must only call the C API, otherwise the
> native link fails with `undefined symbol` errors.

> Ausführliche Architektur- und Klassendokumentation (deutsch, mit
> UML-/Block-Diagrammen, als PDF baubar): [`docs/`](docs/README.md)
> (`cd visualization/docs && ./generate_pdf.sh`).

---

## Directory Structure

```
visualization/
├── docs/                            # Moduldokumentation (Markdown + Mermaid, PDF via generate_pdf.sh)
├── src/main/
│   ├── java/de/carsten/android/muzzic/visualization/
│   │   ├── component/
│   │   │   ├── MusicVisualization.kt    # Haupt-Composable (BARS, LED_SPECTRUM, PROJECT_M)
│   │   │   └── ProjectMGLSurfaceView.kt # OpenGL Surface & Renderer für Milkdrop-Presets
│   │   ├── projectm/
│   │   │   ├── ProjectMNativeBridge.kt  # JNI-Bridge Kotlin-Interface
│   │   │   └── PresetManager.kt         # Asset-Preset-Extraktor (.milk-Dateien)
│   │   ├── service/
│   │   │   └── VisualizerSink.kt        # ExoPlayer TeeAudioProcessor-Sink (FFT- + PCM-Pfad)
│   │   ├── audio/
│   │   │   ├── SpectrumProcessor.kt     # Analyse-Dirigent (Fenster → FFT → Bänder → Auto-Gain → Bus)
│   │   │   ├── Fft.kt                   # Allokationsfreie Radix-2-FFT
│   │   │   ├── BandMapper.kt            # Log. Band-Mapping mit RMS + Bass-Interpolation
│   │   │   └── AutoGain.kt              # dB-Skalierung, Tilt-Kompensation, adaptiver Gain
│   │   ├── bus/
│   │   │   └── SpectrumBus.kt           # Lock-freier SPSC-Ringbuffer (Audio- → Render-Thread)
│   │   ├── ui/
│   │   │   ├── SpectrumVisualizer.kt    # TextureView-Hülle mit GLES-Check + Lifecycle
│   │   │   ├── RenderDriver.kt          # Choreographer-Frame-Steuerung mit Auto-Stop
│   │   │   └── CanvasFallbackVisualizer.kt # Compose-Canvas-Fallback ohne GLES 3.0
│   │   ├── render/
│   │   │   ├── LedBarRenderer.kt        # GLES-3.0-Renderer (LED-Türme + gespiegelte Balken)
│   │   │   ├── LedBarSmoother.kt        # Attack/Decay/Peak-Hold-Glättung (JVM-testbar)
│   │   │   ├── EglManager.kt            # Minimaler EGL14-Kontext für TextureView
│   │   │   ├── GlUtil.kt                # Shader-Compile-/Link-Helfer
│   │   │   └── shaders/                 # FullscreenVertex, LedFragment, BarsFragment, ShaderSnippets
│   │   ├── debug/
│   │   │   └── SyntheticSpectrumSource.kt # Künstliche Test-Signale ohne Audio
│   │   ├── VisualizerConfig.kt          # VisualizerConfig / AnalysisConfig / SmootherConfig
│   │   ├── VisualizerTheme.kt           # Farb-Themes + Zonen
│   │   └── Constants.kt                 # Zentrale Konstanten (Bänder, Zeiten, dB, Schwellen)
│   ├── assets/
│   │   └── presets/                     # Default Milkdrop .milk preset files
 │   ├── cpp/
 │   │   ├── CMakeLists.txt               # CMake configuration linking libprojectM.so & projectm_bridge.cpp
 │   │   ├── projectm_bridge.cpp          # C++ JNI bridge implementation (C API only, see note above)
 │   │   └── include/projectM-4/          # Public C API headers (projectM.h, audio.h, core.h, ...) +
 │   │                                     # generated version.h / projectM_export.h
│   ├── jniLibs/                         # Pre-compiled libprojectM.so binaries (projectM 4.2.0)
│   │   ├── arm64-v8a/
│   │   ├── x86_64/
│   │   ├── armeabi-v7a/
│   │   └── x86/
│   └── build_projectm.sh                # Automation script to precompile libprojectM locally
```

---

## Pre-compiling `libprojectM` for Android

To keep the main build fast and avoid downloading/compiling the entire C++ `projectM` repository on every Gradle invocation, `libprojectM` is pre-compiled locally and stored in `src/main/jniLibs/`.

### Prerequisites

1. **Android NDK**:
   - Ensure the NDK is installed via Android Studio SDK Manager (e.g., NDK 30.0.16248370, see `ndk` in `gradle/libs.versions.toml`).
   - Set `$ANDROID_HOME` or `$ANDROID_NDK_HOME` environment variables:
     ```bash
     export ANDROID_HOME="$HOME/Android/Sdk"
     export ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/30.0.16248370"
     ```

2. **CMake & Build Tools**:
   - CMake 3.22.1+ and standard C++ build utilities (`git`, `make`/`ninja`).

---

### Step-by-Step Compilation Guide

You can run the included automation script `build_projectm.sh` or execute the commands manually:

#### Automated Execution:
```bash
cd visualization
chmod +x build_projectm.sh
./build_projectm.sh
```

#### Manual Execution Steps:

1. **Clone projectM source repository (master branch with submodules)**:
   ```bash
   git clone --recurse-submodules --depth 1 https://github.com/projectM-visualizer/projectm.git /tmp/projectm_src
   ```

2. **Cross-compile for each target ABI**:
   For `arm64-v8a`, `x86_64`, `armeabi-v7a` and `x86`:
   ```bash
   NDK_TOOLCHAIN="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake"

   for ABI in arm64-v8a x86_64 armeabi-v7a x86; do
       cmake -B "/tmp/build-${ABI}" -S "/tmp/projectm_src" \
           -DCMAKE_TOOLCHAIN_FILE="${NDK_TOOLCHAIN}" \
           -DANDROID_ABI="${ABI}" \
           -DANDROID_PLATFORM=android-21 \
           -DENABLE_GLES=ON \
           -DENABLE_SDL=OFF \
           -DENABLE_QT=OFF \
           -DBUILD_TESTING=OFF \
           -DCMAKE_BUILD_TYPE=Release

       cmake --build "/tmp/build-${ABI}" --target projectM -j8

       # Copy output library into jniLibs
       mkdir -p "src/main/jniLibs/${ABI}"
       cp "/tmp/build-${ABI}/src/libprojectM/libprojectM.so" "src/main/jniLibs/${ABI}/"
   done
   ```

3. **Copy the public C API headers** (only these are needed — the internal
   C++ headers of `src/libprojectM` are intentionally NOT vendored, since the
   prebuilt library only exports the C API):
   ```bash
   mkdir -p src/main/cpp/include/projectM-4
   cp /tmp/projectm_src/src/api/include/projectM-4/*.h src/main/cpp/include/projectM-4/
   cp /tmp/build-arm64-v8a/src/api/include/projectM-4/version.h \
      /tmp/build-arm64-v8a/**/projectM_export.h src/main/cpp/include/projectM-4/
   ```
   If the upstream revision changed, update `PROJECTM_VERSION_*` in
   `src/main/cpp/include/projectM-4/version.h` to match the prebuilt binaries.

---

## How It Works in Muzzic

1. **Audio Capture**: ExoPlayer passes audio buffers to `VisualizerSink` (`TeeAudioProcessor.AudioBufferSink`).
2. **GLES Spectrum Path**: `VisualizerSink` → `SpectrumProcessor` (Hann window → `Fft` → `BandMapper` → `AutoGain`) → normalized bands (0..1) into `SpectrumBus`; `LedBarRenderer` reads them with latency compensation (`visualLatencyMs`), smooths via `LedBarSmoother`, and draws a fullscreen shader (`LedFragment` / `BarsFragment`), driven by `RenderDriver` (Choreographer, auto-stop when idle). Without GLES 3.0, `CanvasFallbackVisualizer` draws the same state in Compose Canvas.
3. **PCM Stream**: `VisualizerSink` feeds raw PCM float samples to `ProjectMNativeBridge.addPcm()` → `projectm_pcm_add_float()` (count is per channel).
4. **OpenGL Rendering**: `ProjectMGLSurfaceView` triggers `projectm_opengl_render_frame()` on the EGL rendering thread. The instance is created with `projectm_create()` in `onSurfaceCreated` (a current GL context is required).
4. **Preset Extraction**: `PresetManager` copies bundled `.milk` files from `assets/presets/` to `context.filesDir/projectm/presets/` on launch. The native bridge scans this directory at init, loads the first preset immediately and cycles through the rest via next/previous with smooth transitions.
