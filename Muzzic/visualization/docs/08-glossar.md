# 8 — Glossar

| Begriff | Bedeutung im Modul |
|---|---|
| ABI | CPU-Binärschnittstelle (`arm64-v8a`, `x86_64`, `armeabi-v7a`, `x86`); pro ABI liegt eine `libprojectM-4.so` in `jniLibs/` |
| Attack/Decay | Balken steigt sofort, fällt linear (`fallPerSec`) — siehe `LedBarSmoother` |
| Auto-Gain | Adaptive Verstärkung, die leise Tracks anhebt ohne Stille zu boosten (`AutoGain`) |
| Band | Ein logarithmischer Frequenzbereich; ein Balken = ein Band (Standard: 32) |
| Bin | Ein linearer FFT-Frequenzanteil (bei 2048er-FFT: 1025 nutzbare Bins) |
| Choreographer | Android-Vsync-Taktgeber; `RenderDriver` bezieht darüber Frames in Display-Rate |
| dB-Skala | `20·log10(Amp)`; Anzeige-Fenster −54…−6 dB (`floorDb`/`topDb`) |
| EGL | Schnittstelle zwischen OpenGL und Fenster-Surface (`EglManager`, `GLSurfaceView`) |
| Fullscreen-Dreieck | Ein Dreieck, das den Screen füllt; alle LED-Pixel entstehen im Fragment-Shader |
| GLES 3.0 | OpenGL ES 3.0; Voraussetzung für `LedBarRenderer` (sonst Canvas-Fallback) |
| Hann-Fenster | Einblendfunktion vor der FFT gegen Spektral-Leckage |
| Hop-Size | Samples zwischen zwei Analysen (512 ≈ 86 Hz bei 44,1 kHz) |
| JNI | Java Native Interface; Brücke Kotlin ↔ C++ (`ProjectMNativeBridge` ↔ `projectm_bridge.cpp`) |
| Latenzkompensation | `visualLatencyMs` (150 ms): Renderer zeigt den Frame, der zum Ton passt |
| Milkdrop/Preset | `.milk`-Textdatei mit Formeln für ein ProjectM-Bild |
| Nyquist-Frequenz | Halbe Sample-Rate; Analyse bleibt per Marge (0,45×) darunter |
| PCM | Rohe Samples (16-Bit-Int / 32-Bit-Float), Eingang beider Pfade |
| Peak-Hold | Marker, der den Höchststand kurz hält, dann absinkt |
| Seqlock | Lock-freie Lese-Schreib-Koordination im `SpectrumBus` (ungerade = schreibt) |
| SDF | Signed Distance Field; Kantengleichung für runde LEDs im Shader |
| SPSC | Single Producer, Single Consumer — das `SpectrumBus`-Modell |
| Stale-Frame | Bus-Frame älter als Latenz + 120 ms → Quelle versiegt → Balken auf null |
| TeeAudioProcessor | ExoPlayer-Baustein, der Audio-Buffer an `VisualizerSink` abzweigt |
| Tilt (+3 dB/Okt.) | Höhen-Anhebung ab 1 kHz gegen natürlichen Abfall |
| Vsync | Display-Takt (60/120 Hz), über den `RenderDriver` Frames anfordert |

Zurück: [README](README.md).
