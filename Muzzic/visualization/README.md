# Muzzic Audio Visualization Module (`:visualization`)

This Android Library module encapsulates all audio visualization components for Muzzic, including:
- **2D Segmented Bars Visualizer** (Pure Jetpack Compose Canvas)
- **3D Milkdrop Visualizer** (Powered by `libprojectM` C++ engine via OpenGL ES 2.0 / 3.0 and JNI, using the public **C API** `projectM-4/projectM.h`)

> **Important:** The prebuilt `libprojectM-4.so` only exports the public C API
> (`projectm_*` functions). The C++ class API (`libprojectM::ProjectM`) has
> hidden visibility — the JNI bridge must only call the C API, otherwise the
> native link fails with `undefined symbol` errors.

---

## Directory Structure

```
visualization/
├── src/main/
│   ├── java/de/carsten/android/muzzic/visualization/
│   │   ├── component/
│   │   │   ├── MusicVisualization.kt    # Main Composable (supports BARS and PROJECT_M engines)
│   │   │   └── ProjectMGLSurfaceView.kt # OpenGL Surface & Renderer for Milkdrop presets
│   │   ├── projectm/
│   │   │   ├── ProjectMNativeBridge.kt  # JNI Bridge Kotlin interface
│   │   │   └── PresetManager.kt         # Asset preset extractor (.milk files)
│   │   ├── service/
│   │   │   └── VisualizerSink.kt        # ExoPlayer TeeAudioProcessor Sink (FFT & PCM sample stream)
│   │   └── state/
│   │       └── MusicVisualizerState.kt  # State holder & math calculations for 2D visualizer
│   ├── assets/
│   │   └── presets/                     # Default Milkdrop .milk preset files
│   ├── cpp/
│   │   ├── CMakeLists.txt               # CMake configuration linking libprojectM.so & projectm_bridge.cpp
│   │   ├── projectm_bridge.cpp          # C++ JNI bridge implementation (C API only, see note above)
│   │   └── include/                     # C++ header files (ProjectM.hpp, etc.)
│   │       └── projectM-4/              # Public C API headers (projectM.h, audio.h, core.h, ...) +
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

3. **Copy C++ Header Files**:
   ```bash
   mkdir -p src/main/cpp/include
   cp -r /tmp/projectm_src/src/libprojectM/*.h src/main/cpp/include/
   # Plus the public C API headers and the generated build headers:
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
2. **PCM Stream**: `VisualizerSink` feeds raw PCM float samples to `ProjectMNativeBridge.addPcm()` → `projectm_pcm_add_float()` (count is per channel).
3. **OpenGL Rendering**: `ProjectMGLSurfaceView` triggers `projectm_opengl_render_frame()` on the EGL rendering thread. The instance is created with `projectm_create()` in `onSurfaceCreated` (a current GL context is required).
4. **Preset Extraction**: `PresetManager` copies bundled `.milk` files from `assets/presets/` to `context.filesDir/projectm/presets/` on launch. The native bridge scans this directory at init, loads the first preset immediately and cycles through the rest via next/previous with smooth transitions.
