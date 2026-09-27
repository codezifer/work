# 0 — Überblick

Das Modul `:visualization` (Paket `de.carsten.android.muzzic.visualization`)
kapselt **alle Audio-Visualisierungen** der Muzzic-App. Es ist eine reine
Android-Library ohne eigene Activities: Die App bettet genau **eine**
Composable-Funktion ein — `MusicVisualization` — und wählt damit eine von drei
Rendering-Engines.

## Die drei Engines

| Engine (`VisualizerEngine`) | Optik | Technik | Audio-Quelle |
|---|---|---|---|
| `BARS` | Gespiegelte Segment-Balken (Bass in der Mitte, wächst beidseitig von der Bildmitte) | Eigenes GLES-3.0-Fullscreen-Shader-Programm | `SpectrumBus` |
| `LED_SPECTRUM` | 80er-HiFi-LED-Türme (Zonenfarben aus der Album-Art-Farbe + Peak-Hold-Marker + Halo) | Eigenes GLES-3.0-Fullscreen-Shader-Programm | `SpectrumBus` |
| `PROJECT_M` | 3D-Milkdrop-Effekte (flüssig, psychedelisch) | `libprojectM` (C++-Engine) via JNI + OpenGL | PCM-Float direkt |

Die beiden GLES-Engines (`BARS`, `LED_SPECTRUM`) teilen sich die **komplette
Audio-Analyse-Pipeline**: ExoPlayer → `VisualizerSink` → `SpectrumProcessor`
(FFT → Band-Mapping → Auto-Gain) → `SpectrumBus` → Renderer. Nur die
Shader-Programme und das Farbschema unterscheiden sich. `PROJECT_M` läuft
dagegen auf einem **eigenen Pfad**: Es bekommt rohe PCM-Samples direkt aus dem
`VisualizerSink` und rendert alles selbst in nativem C++-Code. Die Auswahl
erfolgt über die Generalisierungsschicht `VisualizerEngine` → `VisualizerParams`
→ `VisualizerFactory` → `VisualizerDefinition` (Details in Kapitel 6); beide
GLES-Stile teilen sich dabei die eine `FrameRenderer`-Implementierung
`LedBarRenderer`.

## Warum zwei Pfade?

- **GLES-Pfad:** leichtgewichtig, voll testbar (reines Kotlin, JVM-Unit-Tests),
  deterministisch, geringer Akku-Verbrauch (Render-Loop stoppt bei Stille
  automatisch). Ideal als Standard-Visualisierung.
- **ProjectM-Pfad:** maximale visuelle Vielfalt durch hunderte Community-Presets
  (`.milk`-Dateien), aber schwere native Abhängigkeit (`libprojectM-4.so` pro
  ABI vorcompiliert in `jniLibs/`), eigener GL-Kontext, nicht unit-testbar.

## Zentrale Begriffe (Vokabular für alle Kapitel)

- **PCM:** rohe Audio-Samples (16-Bit-Int oder 32-Bit-Float), Eingang beider Pfade.
- **FFT:** Fast Fourier Transform — zerlegt ein Zeitfenster (~2048 Samples) in
  Frequenzanteile („Bins").
- **Band:** logarithmisch zusammengefasster Frequenzbereich (Standard: 32 Bänder
  von 40 Hz bis ~16 kHz). Was der Renderer pro Balken sieht, ist genau ein Band.
- **SpectrumBus:** Ringbuffer, der normalisierte Band-Werte (0.0–1.0) vom
  Audio-Thread zum Render-Thread transportiert.
- **Preset (`.milk`):** Textdatei mit Formeln, die ein Milkdrop-Bild beschreiben.
- **JNI-Bridge:** Klebeschicht zwischen Kotlin und C++ (`ProjectMNativeBridge`
  ↔ `projectm_bridge.cpp`).
- **Smoother:** Zeitliche Glättung (Angriff/Abfall/Peak-Hold), damit Balken
  nicht flackern.

Weiter: [01 — Architektur](01-architektur.md).
