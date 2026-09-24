# Technical Decisions Log: LED Bar Spectrum Visualizer

This document records conservative design decisions and technical trade-offs made during the implementation of the GLES 3.0 LED Bar Spectrum Visualizer for Muzzic.

---

## Decision Log

### 1. Zero-Allocation Hot Path Design
- **Decision**: No allocations permitted inside `processAudio`, `analyze`, `map`, `process`, or `onDrawFrame`.
- **Rationale**: Audio callbacks and GL render frames run at ~86 Hz and 60–120 FPS. Any garbage collection churn in these callbacks causes micro-stutter/jank during audio playback.
- **Implementation**: Pre-allocated primitive arrays (`FloatArray`, `IntArray`) and reusable buffer fields in `Fft`, `BandMapper`, `AutoGain`, `LedBarSmoother`, and `LedBarRenderer`.

### 2. SPSC Lock-Free SpectrumBus with Seqlocks
- **Decision**: Use a 64-slot lock-free Single-Producer Single-Consumer (SPSC) ring buffer with JVM-safe Seqlocks (`AtomicInteger` sequence counters and `@Volatile` fields).
- **Rationale**: The audio thread must never be blocked by UI or GL rendering locks. Seqlocks allow thread-safe lock-free reads and writes.
- **JVM Concurrency Note**: `AtomicInteger.lazySet()` and `AtomicInteger.get()` ensure memory barriers and prevent instruction reordering on ARM64 architectures.

### 3. Logarithmic Band Mapping with Bass Interpolation
- **Decision**: Map 2048-point FFT bins to 32 logarithmic bands (40 Hz – 16 kHz) with linear interpolation for narrow sub-bass bands.
- **Rationale**: Linear FFT bin spacing groups sub-bass into only 1 or 2 bins. Logarithmic mapping matches human pitch perception, and interpolation avoids empty bass bars.

### 4. Spectral Tilt (+3.0 dB/octave) & Auto-Gain
- **Decision**: Apply a +3.0 dB/octave tilt compensation above 1 kHz and adaptive gain tracking with `agReleaseDbPerSec = 2.0 dB/s`.
- **Rationale**: Musical audio spectra naturally roll off according to pink noise power distribution. Spectral tilt prevents dull high-frequency bars, while auto-gain normalizes track volumes without inflating digital silence (< -80 dB).

### 5. Pixel-Space SDF Rounded Box Shader
- **Decision**: Calculate Signed Distance Fields in screen pixel coordinates inside the GLES 3.0 fragment shader.
- **Rationale**: Normalizing SDF in UV space causes oval/distorted corners when aspect ratios are non-square. Pixel-space SDF guarantees perfect circular corner radii with 1.5px `smoothstep` antialiasing.

### 6. Choreographer-Driven RenderDriver with Idle Auto-Stop
- **Decision**: Control rendering using `Choreographer.postFrameCallback` and pause frame requests when audio is stopped and bars have settled (`renderer.isIdle`).
- **Rationale**: Prevents GPU battery drain during playback pauses. CPU and GPU utilization drops to ~0% when idle.

### 7. Seamless GLES 3.0 Fallback to Compose Canvas
- **Decision**: Check `reqGlEsVersion >= 0x00030000` at runtime and automatically render `CanvasFallbackVisualizer` if GLES 3.0 is missing or shader linking fails.
- **Rationale**: Guarantees backwards compatibility across all Android devices while offering high-performance GLES 3.0 visuals on supported hardware.
- **Project note**: The app targets `minSdk = 36`, so GLES 3.0 is effectively guaranteed. The fallback remains as a safety net for shader-link failures and emulator images without GPU acceleration.

### 8. TeeAudioProcessor Tap Instead of BaseAudioProcessor
- **Decision**: The spectrum feed runs as a `TeeAudioProcessor.AudioBufferSink` inside the existing `VisualizerSink`, not as a standalone `BaseAudioProcessor` with `replaceOutputBuffer` passthrough (as sketched in the concept).
- **Rationale**: A tap cannot corrupt or drop audio output; a passthrough bug would silence playback. The existing `PlaybackManager` wiring (`DefaultRenderersFactory.buildAudioSink` with `enableAudioOutputPlaybackParams`, Media3 1.11.1) stays untouched.
- **Consequence**: The concept's byte-equality passthrough test does not apply. Equivalent coverage: `SpectrumProcessorTest` (16-bit stereo + float mono sine peaks, `flush` zero-frame, disabled-writes-nothing) plus `AutoGainTest`.

### 9. Encoding Is Passed Through from flush()
- **Decision**: `VisualizerSink` stores the `encoding` from `flush()` and calls `SpectrumProcessor.processAudio(buffer, isFloat)` accordingly. Only 16-bit and float PCM are analyzed; other encodings (e.g. offloaded/tunneled passthrough, where no processor runs) leave the display empty by design — no visible error.
- **Rationale**: The previous code hardcoded `isFloat = false`, misinterpreting float output as 16-bit. The projectM feed honors the same flag.

### 10. Legacy 64-Bar FFT Runs Only for the BARS Engine
- **Decision**: `VisualizerSink.legacyBarsEnabled` (volatile, set by `PlayerViewModel` from the active `VisualizerEngine`) gates the old FFT path with its per-buffer allocations. Spectrum analysis and the projectM feed always run.
- **Rationale**: Avoids double FFT work and audio-thread allocations when the LED or projectM engine is active. The legacy path additionally only handles 16-bit input (as before); float input is visualized via the spectrum path.

### 11. RenderDriver Disables Analysis on Idle Auto-Stop
- **Decision**: When the Choreographer loop stops because playback is paused and bars have settled (`renderer.isIdle`), the driver also clears `processor.enabled`. Any `start()` (play/resume) re-enables it.
- **Rationale**: Otherwise the FFT would keep running during long pauses. Stale detection in `LedBarRenderer` (newest frame older than latency + 120 ms) turns the target into a zero vector, so bars decay normally before the stop.

### 12. GL Resource Release on the GL Thread
- **Decision**: `SpectrumVisualizer.onRelease` deletes the GL program via `GLSurfaceView.queueEvent { renderer.release() }` instead of on the Compose UI thread.
- **Rationale**: `glDeleteProgram` outside the GL thread is undefined behavior and can leak GPU resources.

### 13. Bloom Removed (Supersedes Earlier Bloom Notes)
- **Decision**: The bloom/glow pipeline was removed entirely by product decision: `BloomPipeline`, the blur/composite shaders, `VisualizerConfig.bloomEnabled`/`bloomStrength`, the `bloom_enabled` setting with its `SettingsScreen` switch, and all renderer bloom branches are deleted. Both render styles draw directly to the backbuffer.
- **Rationale**: Shimmer and tip glow deliver most of the luminous look at ~0 % extra cost; the FBO multi-pass overhead was not justified. (Earlier entries covering bloom hardening, the toggle, and BARS bloom are superseded by this removal.)

### 14. CanvasFallback Coroutine Cancellation
- **Decision**: The fallback's `LaunchedEffect` frame loop uses `while (isActive)` so it ends with composition instead of leaking a `withFrameNanos` loop.
- **Rationale**: The previous `while (true)` survived `isPlaying` toggles until recomposition replaced the effect.

### 15. Classic BARS Rendered via GLES (No Legacy Path)
- **Decision**: The `BARS` engine is a second `RenderStyle` (`MIRRORED_BARS`) in `LedBarRenderer`, fed by the shared 32-band `SpectrumBus`. A dedicated `BARS_FRAGMENT_SHADER` mirrors 64 visual columns horizontally (bass center) and folds rows vertically (bidirectional), reusing the pixel-space SDF LED raster. The legacy Canvas bars, the `VisualizerSink` 64-bin FFT with its audio-thread allocations, the `amplitudes` StateFlow, and `MusicVisualizerState` were removed.
- **Rationale**: One DSP path (log bands, dB/tilt/auto-gain, dt-based smoothing) for all bar engines; single-draw-call GLES rendering at display rate instead of per-segment Canvas draws driven by audio-rate StateFlow emissions.

### 16. Shimmer and Tip Glow as Separate Settings
- **Decision**: Two persisted toggles (`bars_shimmer_enabled`, `bars_tip_glow_enabled`, default true) map to `VisualizerConfig.shimmerStrength` / `tipGlowStrength` (`0` disables the effect without shader branching). Shimmer modulates only lit LEDs via `uTime` so the loop can still idle-stop; the tip segment gets a white mix. Both switches live in `SettingsScreen` and are only shown for the `BARS` engine (same conditional pattern as the projectM preset selector); the flags flow through `PlayerUiState` to `MusicVisualization`.
- **Rationale**: Shimmer-only-on-lit keeps background calm and preserves the ~0 % idle behavior; separate switches allow glow without glitter and vice versa.

### 17. Translucent Surface for Mirrored Bars
- **Decision**: `SpectrumVisualizer` uses a translucent holder format (`PixelFormat.TRANSLUCENT`, RGBA8888) for `MIRRORED_BARS` and the BARS shader outputs non-premultiplied alpha with `SRC_ALPHA/ONE_MINUS_SRC_ALPHA` blending (off-intensity `0` hides unlit LEDs exactly like the legacy Canvas `continue`).
- **Rationale**: The BARS strip sits over app UI (player controls); an opaque black clear color would box it in. The LED tower keeps its opaque surface. Theme derivation (`barsThemeFrom`) uses pure-Kotlin HSL math instead of `ColorUtils` so it stays JVM-testable without Robolectric.
