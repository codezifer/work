# Konzept: LED-Balken-Spektrumanalyzer (GLES 3.0) für Android

Implementierungsspezifikation zur Übergabe an eine Coding-KI. Ziel ist eine flüssige Musikvisualisierung im Stil der LED-Pegelanzeigen von 80er-Jahre-Kassettenrecordern und
HiFi-Anlagen.

---

## 0. Arbeitsanweisungen an die implementierende KI

Bitte zuerst lesen und durchgehend beachten:

1. **Arbeite phasenweise** (Abschnitt 12). Beende jede Phase mit lauffähigem Code und grünen Tests, bevor du die nächste beginnst.
2. **Erfinde keine APIs.** Wo dieses Dokument auf Bibliothekssignaturen hinweist (besonders Media3), prüfe sie gegen die im Projekt verwendete Version. Passe an, wenn sie
   abweichen, und notiere die Abweichung im Code-Kommentar.
3. **Halte dich an die Projektkonventionen** (Paketstruktur, DI, Logging, Formatierung, Versionskatalog `libs.versions.toml`). Die Paketnamen unten sind Vorschläge.
4. **Keine Allokationen in Hot Paths.** Das betrifft `queueInput`, die FFT/Analyse und `onDrawFrame`. Alle Puffer werden vorab angelegt.
5. **Konstanten aus diesem Dokument sind Startwerte.** Sie sind in `VisualizerConfig` bzw. `AnalysisConfig` zentral und änderbar zu halten, nicht im Code zu verstreuen.
6. **Schreibe Tests** für alle rein logischen Komponenten (FFT, Band-Mapping, Auto-Gain, Smoother, Bus). GL-Code wird nicht unit-getestet, sondern über die Debug-Quelle (Abschnitt
   9) visuell geprüft.
7. **Bei echten Unklarheiten:** Treffe die konservativste Annahme, dokumentiere sie in `docs/visualizer-decisions.md` und arbeite weiter. Blockiere nicht.
8. **Liefere am Ende** eine kurze Zusammenfassung: erstellte Dateien, getroffene Annahmen, offene Punkte, und wie man die Debug-Quelle aktiviert.

---

## 1. Ziel und Nicht-Ziele

### Ziel

- Echtzeit-Spektrumanalyse des tatsächlich abgespielten Audiosignals.
- Darstellung als **~32 vertikale LED-Balken**, jeweils aus ~20 einzelnen LED-Segmenten mit farbigen Zonen (grün → gelb → rot), **Peak-Hold-Marker** pro Balken, leicht sichtbare "
  ausgeschaltete" LEDs.
- Flüssig (Display-Refresh-Rate, mindestens 60 fps) bei minimaler CPU-/Akkulast.
- Sauber synchron zum Gehörten.
- Optional: Glow/Bloom, Themes, Fallback ohne OpenGL.

### Nicht-Ziele

- Keine Audioaufnahme, kein Mikrofonzugriff, keine `RECORD_AUDIO`-Berechtigung.
- Keine Nutzung der `android.media.audiofx.Visualizer`-API.
- Keine Änderung der Audioausgabe (der Processor ist ein reiner Passthrough).
- Kein Waveform-/Oszilloskop-Modus in dieser Version (siehe Erweiterungen).

---

## 2. Annahmen und Rahmenbedingungen

Diese Annahmen gelten, sofern das Projekt nichts anderes vorgibt. Weicht das Projekt ab, passe die Umsetzung an und dokumentiere es.

| Thema            | Annahme                                                                                                                         |
|------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Sprache/UI       | Kotlin, Jetpack Compose                                                                                                         |
| Player           | Media3 / ExoPlayer, ggf. in einem `MediaSessionService`                                                                         |
| minSdk           | ≥ 24, GLES 3.0 ist auf praktisch allen Geräten dieser Klasse vorhanden, ein Fallback bleibt trotzdem Pflicht                    |
| Prozess          | Player und UI laufen im selben Prozess                                                                                          |
| FFT              | Eigene, allokationsfreie Radix-2-Implementierung (keine Zusatzdependency)                                                       |
| Threading        | Audio-Thread (Producer), GL-Thread (Consumer), Main-Thread (Konfiguration/Lifecycle)                                            |
| Vorhandener Code | Falls bereits eine Visualisierung existiert: hinter einem Interface austauschbar machen (Feature-Flag), nicht ersatzlos löschen |

---

## 3. Architekturüberblick

```
ExoPlayer ── DefaultAudioSink ── [SpectrumProcessor]  (Audio-Thread)
                                       │  PCM mono, Ringpuffer 2048, Hop 512
                                       │  Hann-Fenster → FFT → Band-Mapping
                                       │  → dB → Tilt → Auto-Gain → 0..1
                                       ▼
                                 SpectrumBus  (lock-freier SPSC-Ringpuffer, 64 Frames, Zeitstempel)
                                       │
                                       ▼
        Compose: SpectrumVisualizer ── GLSurfaceView (GL-Thread)
                                       │  LedBarSmoother (Attack/Decay/Peak-Hold, dt-basiert)
                                       │  LedBarRenderer → 1 Draw Call, Fragment-Shader
                                       │  optional BloomPipeline (FBO, Blur, Composite)
                                       ▼
                                    Display
```

### Paket-/Dateistruktur (Vorschlag)

```
visualizer/
  VisualizerConfig.kt          // Konfig-Dataclasses + Defaults
  VisualizerTheme.kt           // Farben, Zonen
  audio/
    SpectrumProcessor.kt       // Media3 AudioProcessor
    Fft.kt                     // Radix-2, in-place
    BandMapper.kt              // Bin→Band, Frequenzkanten, Interpolation
    AutoGain.kt                // dB-Normalisierung + adaptive Verstärkung
  bus/
    SpectrumBus.kt             // SPSC-Ringpuffer + Seqlock
  render/
    LedBarSmoother.kt          // pure Kotlin, testbar
    LedBarRenderer.kt          // GLSurfaceView.Renderer
    GlUtil.kt                  // compile/link/checkGlError
    Shaders.kt                 // GLSL als Konstanten
    BloomPipeline.kt           // Phase 4
  ui/
    SpectrumVisualizer.kt      // Composable
    RenderDriver.kt            // Choreographer-getriebenes requestRender
    CanvasFallbackVisualizer.kt// Compose-Canvas-Fallback
  debug/
    SyntheticSpectrumSource.kt // Testsignale ohne Wiedergabe
docs/visualizer-decisions.md
```

---

## 4. Audio-Pfad im Detail

### 4.1 `SpectrumProcessor` (erbt von `BaseAudioProcessor`)

**Aufgabe:** PCM-Daten unverändert durchreichen und dabei mitlesen.

Verhalten:

- `onConfigure(inputAudioFormat)`: Unterstützt `C.ENCODING_PCM_16BIT` und `C.ENCODING_PCM_FLOAT`. Bei anderen Encodings **nicht** `UnhandledAudioFormatException` werfen, sondern
  das Format durchreichen und die Analyse deaktivieren (Bänder bleiben 0). Zweck: Die Wiedergabe darf nie an der Visualisierung scheitern. Beim Konfigurieren die Sample Rate,
  Kanalzahl und alle davon abhängigen Tabellen neu berechnen (Band-Kanten, Bin-Bereiche).
- `queueInput(ByteBuffer)`:
    1. Eingabepuffer **duplizieren** (`duplicate()`, Byte-Order `LITTLE_ENDIAN`), damit Position/Limit des Originals unberührt bleiben.
    2. Frames lesen, auf **Mono downmixen** (Mittelwert aller Kanäle), auf −1..1 normalisieren (`/32768f` bei 16 Bit).
    3. In Ringpuffer (2048 Samples) schreiben.
    4. Alle `hop = 512` neuen Samples `analyze()` aufrufen.
    5. Originalen Buffer unverändert in den Output kopieren: `replaceOutputBuffer(input.remaining()).put(input).flip()`.
- `onFlush` / `onReset` (Signatur gegen Media3-Version prüfen): Ringpuffer und Zähler zurücksetzen, **einen Null-Frame** in den Bus schreiben. So entstehen nach einem Seek keine
  Geister-Balken.
- `enabled: AtomicBoolean` (von außen gesetzt, solange die Visualisierung sichtbar ist). Bei `false` nur durchreichen, keine Analyse (spart CPU).
- Keine Allokationen, keine Locks, keine Logs in `queueInput`.

Einhängen in den Player (Signatur der `buildAudioSink`-Methode gegen die verwendete Media3-Version prüfen):

```kotlin
val renderersFactory = object : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(arrayOf(spectrumProcessor))
        .build()
}
val player = ExoPlayer.Builder(context, renderersFactory).build()
```

Hinweise:

- Die Analyse arbeitet **vor** der Lautstärkeregelung des `AudioTrack`. Die Visualisierung ist damit lautstärkeunabhängig, das ist gewünscht.
- Bei Offload, Tunneling oder komprimiertem Passthrough laufen keine Audioprozessoren. Dann bleibt die Anzeige leer. Das ist zu akzeptieren und im Code zu kommentieren, sichtbare
  Fehler sind nicht nötig.
- Der `SpectrumProcessor` und der `SpectrumBus` sind **Singletons im Player-Scope** (z. B. per DI oder im Service). Die UI bekommt nur den Bus.

### 4.2 Analyseparameter (`AnalysisConfig`)

| Parameter           | Startwert                        | Bemerkung                                   |
|---------------------|----------------------------------|---------------------------------------------|
| `fftSize`           | 2048                             | Zweierpotenz                                |
| `hopSize`           | 512                              | ≈ 86 Analysen/s bei 44,1 kHz                |
| Fenster             | Hann (symmetrisch), vorberechnet | `w[i] = 0.5 − 0.5·cos(2π·i/(N−1))`          |
| `bandCount`         | 32                               | 8..64                                       |
| `fMinHz`            | 40                               |                                             |
| `fMaxHz`            | `min(16000, 0.45 · sampleRate)`  |                                             |
| `floorDb`           | −54                              | unterer Anzeigewert                         |
| `topDb`             | −6                               | oberer Anzeigewert                          |
| `tiltDbPerOctave`   | +3.0                             | Ausgleich des Abfalls (Pink-Noise-Referenz) |
| `agTargetTopDb`     | −6                               | Ziel für den adaptiven Pegel                |
| `agMaxGainDb`       | 18                               | maximale adaptive Verstärkung               |
| `agReleaseDbPerSec` | 2.0                              | Zeit, in der der Referenzpegel absinkt      |

### 4.3 Analysekette (`analyze()`)

1. Letzte 2048 Samples in Reihenfolge aus dem Ring kopieren und mit dem Fenster multiplizieren (Arbeitspuffer `re`, `im` mit `im = 0`).
2. FFT in-place (`Fft.kt`, Radix-2, vorberechnete Twiddle-Tabellen und Bit-Reversal-Tabelle).
3. Magnituden für die Bins `0..N/2`: `mag[k] = sqrt(re² + im²)`.
4. Amplituden-Normierung: `amp[k] = 2 · mag[k] / sumWindow` (`sumWindow` = Summe der Fensterwerte). Ein Vollaussteuerungs-Sinus liefert damit ≈ 1,0 (0 dBFS).
5. **Band-Mapping** (4.4) → `bandAmp[b]`.
6. `db[b] = 20 · log10(max(bandAmp[b], 1e-6f))`.
7. **Tilt**: `db[b] += tiltDbPerOctave · log2(fCenter[b] / 1000)`.
8. **Auto-Gain** (4.5) → normalisierter Wert `value[b] ∈ [0,1]`.
9. Frame mit Zeitstempel `System.nanoTime()` in den `SpectrumBus` schreiben.

### 4.4 Band-Mapping (`BandMapper`)

- Logarithmische Kanten: `edge[i] = fMin · (fMax/fMin)^(i / bandCount)`, `i = 0..bandCount`.
- Mittenfrequenz: geometrisches Mittel `fCenter[b] = sqrt(edge[b] · edge[b+1])`.
- Binauflösung `df = sampleRate / fftSize` (≈ 21,5 Hz bei 44,1 kHz).
- Pro Band: `lo = ceil(edge[b]/df)`, `hi = floor(edge[b+1]/df)`.
    - Wenn `hi ≥ lo`: Energiemittel `bandAmp = sqrt(mean(amp[k]²))` über `k = lo..hi`.
    - Wenn `hi < lo` (schmale Bassbänder): **lineare Interpolation** der Amplitude an der fraktionalen Binposition `fCenter/df` zwischen den zwei Nachbarbins.
- Tabellen (`lo`, `hi`, Interpolationsgewichte, `fCenter`) werden bei `onConfigure` bzw. Bandzahländerung vorberechnet, nicht pro Frame.
- Akzeptiertes Verhalten: Benachbarte Bassbänder können ähnliche Werte zeigen (Auflösungsgrenze bei fftSize 2048).

### 4.5 Normalisierung und Auto-Gain (`AutoGain`)

Ziel: Leise und laute Tracks sollen ähnlich lebendig aussehen, ohne dass Rauschen in Stille aufgeblasen wird.

- `frameMaxDb = max(db[b])` (nach Tilt).
- `trackDb`: schneller Anstieg (sofort), langsamer Abfall mit `agReleaseDbPerSec` (pro Frame `dt = hop/sampleRate`).
- `gainDb = clamp(agTargetTopDb − trackDb, 0, agMaxGainDb)`.
- `value[b] = clamp((db[b] + gainDb − floorDb) / (topDb − floorDb), 0, 1)`.
- Ist `frameMaxDb < −80 dB` (digitale Stille), `gainDb = 0` setzen. Damit bleibt alles 0.
- Zustand (`trackDb`) beim Flush/Track-Wechsel weich zurücksetzen (z. B. auf `agTargetTopDb`), nicht hart auf −∞.
- Debug-Overlay (nur Debug-Build): `trackDb`, `gainDb`, fps, Latenz.

Alle Werte sind **Startwerte** und werden mit der Debug-Quelle und mindestens drei echten Tracks (leise Akustik, laute Elektronik, Klassik) visuell nachjustiert.

---

## 5. `SpectrumBus` (Producer/Consumer)

Anforderungen:

- **Single Producer** (Audio-Thread), **Single Consumer** (GL-Thread), lock-frei, allokationsfrei im Betrieb.
- 64 vorab angelegte Slots, je `{ seq: Int, timestampNanos: Long, bandCount: Int, values: FloatArray(64) }`.
- Schreiben: `seq` auf ungerade setzen → Daten schreiben → `seq` auf gerade setzen (**Seqlock**). Danach `writeIndex` (volatile/`AtomicLong`) erhöhen.
- Lesen: `readAtOrBefore(targetNanos, out: FloatArray): Boolean`. Suche rückwärts ab `writeIndex` den neuesten Slot mit `timestamp ≤ targetNanos`, kopiere in `out`, verifiziere
  `seq` vor und nach dem Lesen (bei Abweichung: Slot verwerfen und den nächstälteren nehmen).
- Zusätzlich `newestTimestampNanos()`.

Puffertiefe: 64 Slots × ~11,6 ms ≈ 740 ms, mehr als genug für die Latenzkompensation (Abschnitt 6).

---

## 6. Synchronisation (Latenzkompensation)

Der Processor sieht Audio **früher**, als es hörbar wird (Puffer im `AudioTrack`, typisch 100–250 ms).

- Der Consumer zeigt zum Renderzeitpunkt `now` den Frame mit `timestamp ≤ now − visualLatencyNanos`.
- `visualLatencyMs`: Default **150 ms**, in `VisualizerConfig` konfigurierbar, im Debug-Overlay per Slider justierbar. (Spätere Erweiterung: aus `AudioTrack`-Timestamps ableiten.)
- **Stale-Erkennung:** Ist der neueste Frame älter als `now − visualLatency − 120 ms` (Pause, Buffering, Track-Ende), dann ist das Ziel ein Nullvektor. Die Balken fallen dadurch
  über den normalen Decay ab, statt einzufrieren.

---

## 7. Rendering

### 7.1 Voraussetzungen und Setup

- `GLSurfaceView` mit `setEGLContextClientVersion(3)`, `setEGLConfigChooser(8,8,8,8,0,0)` (kein Depth/Stencil).
- `renderMode = RENDERMODE_WHEN_DIRTY`. Das Rendern wird von `RenderDriver` (7.6) gesteuert.
- `setPreserveEGLContextOnPause(true)`.
- **Kontextverlust:** Alle GL-Ressourcen (Programme, FBOs, Texturen) werden in `onSurfaceCreated` neu erzeugt, der Renderer hält keine Annahmen über frühere Handles.
- Vor der Initialisierung `GLES30`-Verfügbarkeit prüfen. Wenn nicht verfügbar oder Programmkompilierung fehlschlägt: **Canvas-Fallback** (7.7).

### 7.2 Geometrie

Ein einziges Dreieck über den ganzen Bildschirm, **ohne Vertex-Buffer** (`glDrawArrays(GL_TRIANGLES, 0, 3)`).

Vertex-Shader:

```glsl
#version 300 es
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}
```

### 7.3 Fragment-Shader (LED-Raster)

Alle Positionsberechnungen mit **`highp`**. Die Bandwerte sind in `vec4`-Arrays gepackt (16 × vec4 = 64 Werte), das schont Uniform-Slots.

```glsl
#version 300 es
precision highp float;
precision highp int;

uniform vec2  uRes;             // Viewport in Pixeln
uniform int   uBandCount;       // 8..64
uniform float uSegments;        // z. B. 20.0
uniform vec4  uBands[16];       // gepackte Bandwerte 0..1
uniform vec4  uPeaks[16];       // gepackte Peak-Hold-Werte 0..1
uniform vec3  uColLow;          // grüne Zone
uniform vec3  uColMid;          // gelbe Zone
uniform vec3  uColHigh;         // rote Zone
uniform vec2  uZoneStart;       // (Start Gelb, Start Rot), 0..1
uniform vec2  uLedHalfSize;     // Halbgröße der LED in Zellanteilen, z. B. (0.40, 0.34)
uniform float uCornerRadius;    // relativ zur kleineren Zellseite, z. B. 0.25
uniform float uOffIntensity;    // Helligkeit ausgeschalteter LEDs, z. B. 0.06
uniform vec3  uBackground;
out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    float bx = uv.x * float(uBandCount);
    int band = clamp(int(floor(bx)), 0, uBandCount - 1);
    float sy = uv.y * uSegments;
    float seg = floor(sy);
    vec2 cell = vec2(fract(bx), fract(sy));

    float value = uBands[band >> 2][band & 3];
    float peak  = uPeaks[band >> 2][band & 3];

    // Rounded-Box-SDF im Pixelraum (korrekte Rundung trotz nicht quadratischer Zellen)
    vec2 cellPx = uRes / vec2(float(uBandCount), uSegments);
    vec2 p      = (cell - 0.5) * cellPx;
    vec2 halfPx = uLedHalfSize * cellPx;
    float r     = uCornerRadius * min(cellPx.x, cellPx.y);
    vec2 q      = abs(p) - halfPx + r;
    float sd    = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    float led   = 1.0 - smoothstep(-0.75, 0.75, sd);      // ~1,5 px Antialiasing

    float t      = (seg + 0.5) / uSegments;
    float lit    = step(t, value);
    float peakSg = min(floor(peak * uSegments), uSegments - 1.0);
    float isPeak = step(abs(seg - peakSg), 0.5) * step(0.02, peak);
    float on     = max(lit, isPeak);

    vec3 zone = t < uZoneStart.x ? uColLow : (t < uZoneStart.y ? uColMid : uColHigh);
    vec3 c    = zone * mix(uOffIntensity, 1.0, on);

    fragColor = vec4(mix(uBackground, c, led), 1.0);
}
```

### 7.4 `LedBarSmoother` (pure Kotlin, ohne GL, unit-testbar)

```kotlin
class LedBarSmoother(private val maxBands: Int = 64) {
    val bands = FloatArray(maxBands)
    val peaks = FloatArray(maxBands)
    private val hold = FloatArray(maxBands)

    fun update(target: FloatArray, bandCount: Int, dt: Float, cfg: SmootherConfig)
}
```

Regeln pro Band (mit `dt` in Sekunden, auf ≤ 0,05 s begrenzt):

- **Attack:** sofort: `if (target > bar) bar = target`.
- **Decay:** linear: `bar = max(target, bar − cfg.fallPerSec · dt)`, Startwert `fallPerSec = 1.6`.
- **Peak-Hold:** `if (bar ≥ peak) { peak = bar; hold = cfg.holdSec }`, sonst `if (hold > 0) hold −= dt else peak = max(bar, peak − cfg.peakFallPerSec · dt)`. Startwerte
  `holdSec = 0.35`, `peakFallPerSec = 0.5`.
- Alle Werte auf [0,1] klemmen.
- Nach `bandCount`-Wechsel Zustand über die neue Bandzahl hinaus nullen.

### 7.5 `LedBarRenderer`

- Pro Frame: `dt` aus `System.nanoTime()`, Zielvektor aus `bus.readAtOrBefore(now − latency, out)` (bzw. Nullvektor bei Stale/Fehler), `smoother.update(...)`, Uniforms setzen (
  `glUniform4fv` für gepackte Arrays, Anzahl = `ceil(bandCount/4)`), Draw.
- Uniform-Locations einmalig in `onSurfaceCreated` abfragen.
- `onSurfaceChanged`: Viewport und `uRes` setzen, ggf. FBOs neu anlegen.
- `isIdle: Boolean` (volatile): `true`, wenn kein aktueller Frame vorliegt **und** alle `bands` und `peaks` < 0,01. Wird vom `RenderDriver` gelesen.
- `GlUtil.checkGlError` nur im Debug-Build.

### 7.6 `RenderDriver` (Frame-Steuerung)

Ziel: Rendern nur, wenn es etwas zu zeigen gibt.

- Läuft nur, wenn `isPlaying || !renderer.isIdle` **und** die View sichtbar/resumed ist.
- Nutzt `Choreographer.postFrameCallback`. Pro Callback `glSurfaceView.requestRender()`, dann erneut posten. So folgt die Framerate der Displayrate (60/90/120 Hz). Optionales
  fps-Limit in `VisualizerConfig.maxFps`.
- Hält an, sobald `isPlaying == false` und der Renderer idle ist (Balken sind abgefallen), und startet bei erneutem Play. Nach dem Stoppen fallen CPU und GPU auf ~0.
- Setzt `spectrumProcessor.enabled` passend zur Sichtbarkeit.

### 7.7 Canvas-Fallback

`CanvasFallbackVisualizer` zeichnet dieselben LEDs mit `Canvas.drawRoundRect` in Compose (gleicher `LedBarSmoother`, gleiche `VisualizerConfig`, gleiches Theme). Wird gewählt, wenn
GLES 3.0 nicht verfügbar ist oder die Shader-Initialisierung scheitert. Er darf einfacher sein (kein Bloom), muss aber visuell erkennbar dasselbe darstellen.

---

## 8. Compose-Integration (`SpectrumVisualizer`)

```kotlin
@Composable
fun SpectrumVisualizer(
    bus: SpectrumBus,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    config: VisualizerConfig = VisualizerConfig(),
    theme: VisualizerTheme = VisualizerTheme.ClassicGreen,
)
```

Anforderungen:

- `AndroidView` mit `GLSurfaceView`. Der Renderer und der `RenderDriver` werden per `remember` gehalten.
- **Lifecycle:** `LifecycleEventObserver` ruft `onResume`/`onPause` der View auf und startet/stoppt den `RenderDriver`. Im Hintergrund dürfen keine GL-Aufrufe und keine
  Choreographer-Callbacks laufen.
- Konfigurationsänderungen (Rotation, Größe) dürfen weder Absturz noch sichtbares Flackern verursachen.
- `onRelease` der `AndroidView`: Driver stoppen, Renderer-Referenzen lösen, Choreographer-Callbacks entfernen.
- **Bekanntes Risiko:** `GLSurfaceView` ist eine `SurfaceView` und kann bei Animationen, Scrolling oder Überlagerungen in Compose Z-Order-Probleme machen. Die Komponente soll daher
  an **einer festen Stelle** (z. B. im Now-Playing-Screen) eingesetzt werden. Falls Probleme auftreten, dokumentiere sie und bevorzuge den Canvas-Fallback für diesen Kontext, statt
  zu improvisieren.
- Automatische Bandanzahl (Phase 4, optional): `bandCount = clamp(widthDp / minBarWidthDp, 12, 64)` mit `minBarWidthDp ≈ 9`.

---

## 9. Debug-Quelle (`SyntheticSpectrumSource`)

Zweck: Visualisierung und Tuning **ohne Wiedergabe**.

- Schreibt periodisch (~86 Hz) synthetische Frames in den `SpectrumBus`, gleiche Zeitstempel-Semantik wie der Processor.
- Modi: `Sweep` (wandernder Peak über alle Bänder), `Pink` (Rauschen mit Abfall), `Beat` (Bass-Puls 2 Hz + Höhen), `Silence`, `FullScale` (alle Bänder 1,0).
- Nur im Debug-Build erreichbar (z. B. Debug-Menü oder Preview-Screen).
- Zusätzlich ein Debug-Overlay (fps, `trackDb`, `gainDb`, `visualLatencyMs`-Slider, Bandanzahl).

---

## 10. Phase 4: Bloom/Glow (optional, per `VisualizerConfig.bloomEnabled`)

Pipeline:

1. Szene in **FBO A** (RGBA8, volle Auflösung) rendern statt direkt in den Backbuffer.
2. Herunterskalieren auf **FBO B** (halbe Auflösung, `GL_LINEAR`).
3. Separabler Gauß-Blur: horizontal B → C, vertikal C → B (5-Tap mit Linear-Sampling).
4. Composite in den Backbuffer: `scene + bloom · uBloomStrength`, Startwert 0,8, Ausgabe klemmen.

Blur-Fragmentshader (Kern):

```glsl
// Offsets/Gewichte für 9-Tap-Gauß per 5 Linear-Samples
const float OFFS[3] = float[](0.0, 1.3846153846, 3.2307692308);
const float WGT[3]  = float[](0.2270270270, 0.3162162162, 0.0702702703);
vec3 acc = texture(uTex, uv).rgb * WGT[0];
for (int i = 1; i < 3; i++) {
    vec2 o = uDir * OFFS[i] / uTexSize;   // uDir = (1,0) oder (0,1)
    acc += texture(uTex, uv + o).rgb * WGT[i];
    acc += texture(uTex, uv - o).rgb * WGT[i];
}
fragColor = vec4(acc, 1.0);
```

- FBOs in `onSurfaceChanged` neu anlegen, in `onSurfaceCreated` ebenfalls (Kontextverlust).
- Bloom ist abschaltbar. Ohne Bloom rendert die Pipeline direkt in den Backbuffer (kein FBO-Overhead).
- Performance-Ziel bleibt: GL-Thread-Frame < 4 ms auf Mittelklassegerät.

---

## 11. Theme und Konfiguration

`VisualizerTheme` (Datenklasse): `colLow`, `colMid`, `colHigh`, `zoneStart`, `background`, `offIntensity`.

Mitgelieferte Themes:

- **ClassicGreen:** Grün/Gelb/Rot, Zonen (0,60 / 0,85).
- **Amber:** durchgehend Bernstein, hellere Spitzen.
- **IceBlue:** Cyan → Weiß.

`VisualizerConfig` (Auszug): `bandCount = 32`, `segmentCount = 20`, `ledHalfSize = (0.40, 0.34)`, `cornerRadius = 0.25`, `visualLatencyMs = 150`, `maxFps = null`,
`bloomEnabled = false`, `bloomStrength = 0.8`, `smoother = SmootherConfig(fallPerSec = 1.6, holdSec = 0.35, peakFallPerSec = 0.5)`.

Hinweis: Themefarben **nicht** an Material-Theme koppeln, wenn sie dadurch semantisch (grün/gelb/rot) verfälscht würden. Optional darf ein Theme aus dem Album-Cover abgeleitet
werden (Erweiterung).

---

## 12. Phasen und Meilensteine

**Phase 1: Analysekern (ohne UI)**

- `Fft`, `BandMapper`, `AutoGain`, `SpectrumBus`, `SpectrumProcessor`, `LedBarSmoother`.
- Unit-Tests (Abschnitt 13). Processor an den Player angebunden, Passthrough-Test grün.
- *Ergebnis:* Werte fließen nachweislich in den Bus (Logging nur temporär).

**Phase 2: GL-Renderer**

- `GlUtil`, `Shaders`, `LedBarRenderer`, einfache Test-Activity oder Preview mit `SyntheticSpectrumSource`.
- *Ergebnis:* LED-Balken laufen flüssig mit Testsignalen.

**Phase 3: Compose- und Player-Integration**

- `SpectrumVisualizer`, `RenderDriver`, Lifecycle, Latenzkompensation, Stale-Erkennung, Canvas-Fallback.
- *Ergebnis:* Echte Musik → synchrone Balken; Pause lässt Balken abfallen und stoppt die Render-Schleife.

**Phase 4: Politur**

- Bloom-Pipeline, Themes, optionale automatische Bandanzahl, Debug-Overlay, Performance-Feintuning.

**Phase 5: Abnahme**

- Alle Kriterien aus Abschnitt 14 auf mindestens einem Mittelklasse- und einem älteren Gerät prüfen.

---

## 13. Tests

**Unit-Tests (JVM):**

- `Fft`: Vergleich gegen naive DFT für N = 8, 64, 2048 (Toleranz 1e-3). Reiner Sinus auf Bin-Mitte ergibt Maximum bei erwartetem Bin.
- Fenster: `sumWindow` und Symmetrie korrekt.
- `BandMapper`: Kanten streng monoton, erstes/letztes Band an `fMin`/`fMax`; Sinus 1 kHz bei −6 dBFS → Maximum im Band, das 1 kHz enthält, für 44,1 / 48 / 96 kHz; Bassband mit
  `hi < lo` liefert Interpolation statt 0.
- `AutoGain`: Stille → alle Werte 0; leiser Sinus (−40 dBFS) wird angehoben, aber `gainDb ≤ agMaxGainDb`; lauter Sinus (−3 dBFS) → `gainDb = 0`.
- Processor: Synthetischer `ByteBuffer` (16 Bit stereo und Float mono) → **Ausgabe bytegleich zur Eingabe**; Analyse liefert erwartete Bänder; `onFlush` erzeugt Null-Frame; kein
  Crash bei unbekanntem Encoding.
- `SpectrumBus`: Ordnung, `readAtOrBefore`-Semantik, Überlauf (mehr als 64 Frames), Seqlock-Verhalten bei simuliertem Schreiben während des Lesens.
- `LedBarSmoother`: Attack sofort; Decay-Geschwindigkeit stimmt mit `dt` überein (dt-Unabhängigkeit: 1×0,032 s ≈ 2×0,016 s); Peak-Hold-Dauer; Klemmung.

**Manuell/instrumentiert:**

- Debug-Quelle mit allen Modi.
- Click-Track (Metronom) zur subjektiven Synchronprüfung, `visualLatencyMs` justieren.
- Lifecycle: Home, Rückkehr, Rotation, Bildschirm aus/an, Kopfhörer aus/an.

---

## 14. Abnahmekriterien

1. **Flüssigkeit:** 60 fps stabil (bzw. Displayrate) auf Mittelklassegerät. GL-Thread-Framezeit p95 < 8 ms ohne Bloom, < 12 ms mit Bloom. Keine Jank-Frames durch die Visualisierung
   in der UI.
2. **Keine Allokationen** in `queueInput`, `analyze`, `onDrawFrame` (per Allocation-Profiler bestätigt).
3. **Audio unverändert:** Processor-Ausgabe bytegleich zur Eingabe. Kein hörbarer Effekt, kein Underrun durch die Analyse.
4. **Korrektheit:** Sinus-Tests aus Abschnitt 13 bestehen. 1 kHz-Ton bewegt genau die erwarteten Balken.
5. **Synchronität:** Sichtbarer Beat-Einsatz innerhalb ca. ±40 ms zum Hörbaren nach Kalibrierung.
6. **Pause/Stille:** Balken fallen in ≤ 1 s auf 0, die Render-Schleife hält innerhalb 1,5 s an, danach ~0 % CPU/GPU-Last durch die Komponente.
7. **Seek/Trackwechsel:** Keine Geister-Peaks, kein Einfrieren.
8. **Lifecycle:** Kein GL-/Choreographer-Betrieb im Hintergrund. Rückkehr und Rotation ohne Absturz oder leeren Bildschirm. Kontextverlust wird verkraftet.
9. **Formate:** 44,1 / 48 / 96 kHz, Mono/Stereo, 16 Bit und Float funktionieren. Nicht unterstützte Formate lassen die Wiedergabe unberührt (Anzeige leer).
10. **Fallback:** Ohne GLES 3.0 oder bei Shader-Fehler erscheint die Canvas-Variante.
11. **Look:** LEDs mit sauberen abgerundeten Kanten und Antialiasing, klar abgegrenzte Farbzonen, sichtbarer Peak-Hold, dezent sichtbare "aus"-LEDs. Mit Bloom: weiches Leuchten
    ohne Überstrahlen der Nachbarbalken.

---

## 15. Bekannte Fallstricke (bitte beachten)

- **`mediump` in Fragment-Shadern** kann bei `gl_FragCoord`/`fract` auf manchen GPUs sichtbare Artefakte erzeugen. Deshalb durchgehend `highp` (in GLES 3.0 im Fragment-Shader
  garantiert).
- **Uniform-Arrays dynamisch indizieren** ist im Fragment-Shader erst ab GLES 3.0 sinnvoll. Deshalb die Anforderung an 3.0 und das `vec4`-Packing.
- **Audio-Thread schützen:** keine Locks, keine Allokationen, kein Logging, keine Exceptions aus der Analyse nach außen. Analysefehler fangen und die Analyse für die aktuelle
  Konfiguration deaktivieren.
- **Media3-Signaturen** (`buildAudioSink`, `onFlush`) ändern sich zwischen Versionen. Gegen die tatsächliche Version prüfen.
- **Sample-Rate-Wechsel** (gapless mit unterschiedlichen Formaten): `onConfigure` wird erneut aufgerufen, alle Tabellen müssen dann neu entstehen.
- **`GLSurfaceView` + Compose:** Z-Order/Animationsprobleme (siehe Abschnitt 8).
- **Doppelte Wahrheit vermeiden:** Smoothing nur im `LedBarSmoother` (Renderer-Seite), **nicht** zusätzlich im Processor.

---

## 16. Mögliche Erweiterungen (nicht Teil dieser Umsetzung)

- Höhere Bassauflösung durch zusätzliche 4096er-FFT nur für < 200 Hz.
- Latenz automatisch aus `AudioTrack.getTimestamp()` ableiten.
- Themefarben aus Album-Cover.
- Stereo-Modus (linker/rechter Kanal getrennt, gespiegelt).
- Oszilloskop-/Waveform-Modus.
- Horizontale Ausrichtung, Mirror-Modus.
- Nutzerdefinierbare Einstellungen (Bandanzahl, Theme, Bloom) im Settings-Screen.
