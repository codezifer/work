# 1 — Architektur

## Gesamt-Blockschaltbild

```mermaid
flowchart LR
    subgraph C1["1 · Erfassung & Analyse"]
        direction TB
        TEE["TeeAudioProcessor\n(ExoPlayer)"]
        VS["VisualizerSink\n(service)"]
        SP["SpectrumProcessor"]
        FFT["Fft"]
        BM["BandMapper"]
        AG["AutoGain"]
        TEE -->|"PCM"| VS --> SP --> FFT --> BM --> AG
    end
    subgraph C2["2 · GLES-Rendering"]
        direction TB
        SB[("SpectrumBus")]
        MV["MusicVisualization"]
        SV["SpectrumVisualizer"]
        RD["RenderDriver"]
        LBR["LedBarRenderer"]
        SM["LedBarSmoother"]
        CF["CanvasFallback"]
        SB --> SV --> RD --> LBR --> SM
        MV -->|"BARS / LED"| SV
        SV -.->|"ohne GLES 3.0"| CF
    end
    subgraph C3["3 · ProjectM (nativ)"]
        direction TB
        GLS["ProjectMGLSurfaceView"]
        JNI["ProjectMNativeBridge"]
        CPP["projectm_bridge.cpp"]
        LIB[("libprojectM-4.so")]
        PRE[("Presets (.milk)")]
        GLS --> JNI --> CPP --> LIB
        PRE --> CPP
    end

    AG -->|"Bänder 0..1"| SB
    VS -->|"PCM-Float"| JNI
```

Lesart: Es gibt **einen Eingang** (`VisualizerSink` am ExoPlayer) und **zwei
Ausgänge** (GLES-Renderer und ProjectM). Die GLES-Engines lesen aufbereitete
Bänder aus dem `SpectrumBus`; ProjectM bekommt Roh-PCM und macht alles selbst.

## Paket-Übersicht (Zuständigkeiten)

| Paket | Inhalt | Darf kennen |
|---|---|---|
| `service` | `VisualizerSink` — einziger Einstiegspunkt, verteilt PCM an beide Pfade | `audio`, `bus`, `projectm` |
| `audio` | `SpectrumProcessor`, `Fft`, `BandMapper`, `AutoGain` — reine Signalverarbeitung, **kein Android**, JVM-testbar | nur `bus`, `Constants` |
| `bus` | `SpectrumBus` — Thread-Brücke, **kein Android**, JVM-testbar | nur `Constants` |
| `ui` | `SpectrumVisualizer`, `RenderDriver`, `CanvasFallbackVisualizer` — Compose-Hülle + Frame-Steuerung | `bus`, `render`, `audio` (nur Typ `SpectrumProcessor`) |
| `component` | `MusicVisualization` (Engine-Weiche), `ProjectMGLSurfaceView` (GL-Hülle) | `ui`, `projectm` |
| `render` | `LedBarRenderer`, `LedBarSmoother`, `EglManager`, `GlUtil`, `shaders.*` — OpenGL-Zeichnung + Glättung | `bus` |
| `projectm` | `ProjectMNativeBridge`, `PresetManager` — JNI-Fassade + Asset-Verwaltung | nichts (nur Android-SDK) |
| `debug` | `SyntheticSpectrumSource` — künstliche Test-Signale ohne Audio | `bus` |
| Wurzel | `VisualizerConfig`, `VisualizerTheme`, `Constants` — reine Daten/Konstanten | nichts |

Regel: `audio` und `bus` enthalten **keine Android-Imports** — deshalb laufen
ihre Unit-Tests auf der JVM. Alles mit `android.opengl`, `GLSurfaceView` oder
`TextureView` liegt in `ui`/`render`/`component` und ist nur auf Gerät/Emulator
lauffähig.

## Thread-Modell

```mermaid
flowchart LR
    subgraph AT["Audio-Thread (ExoPlayer)"]
        A1["VisualizerSink.handleBuffer"]
        A2["SpectrumProcessor.processAudio/analyze"]
        A3["SpectrumBus.write"]
        A1 --> A2 --> A3
    end
    subgraph MT["Main-Thread (Compose)"]
        M1["SpectrumVisualizer\n(TextureView-Callbacks)"]
        M2["RenderDriver\n(Choreographer-Frames)"]
        M1 <--> M2
    end
    subgraph RT["GL-Thread"]
        R1["LedBarRenderer.onDrawFrame\n(EGL-Kontext, Main-Thread*)"]
        R2["ProjectMGLSurfaceView.Renderer\n(eigener GLSurfaceView-Thread)"]
    end
    A3 -.->|"Seqlock, lock-frei"| R1
    A3 -.->|"addPcm (Mutex in C++)"| R2
```

\* Hinweis: Der GLES-Pfad nutzt bewusst **keinen** `GLSurfaceView`-Thread.
`EglManager` erzeugt den EGL-Kontext auf dem Main-Thread, und `RenderDriver`
triggert Frames per `Choreographer` ebenfalls dort — Surface-Callbacks und
Frames „treffen sich" auf demselben Thread. Nur ProjectM (echte
`GLSurfaceView`) besitzt einen separaten GL-Thread; dort schützt ein
C++-`std::mutex` die native Instanz.

Synchronisations-Prinzipien:

- **GLES-Pfad:** komplett lock-frei (`SpectrumBus` mit Seqlock, atomare
  `enabled`-Flags, `@Volatile isIdle`). Der Audio-Thread blockiert nie.
- **ProjectM-Pfad:** `ProjectMNativeBridge.addPcm` ist ein reiner
  Vorwärts-Kanal (Feuer-und-Vergessen, wirft nie); Lesen/Schreiben der nativen
  Instanz serialisiert `g_mutex` in C++.
- **Lebenszyklus:** `ON_PAUSE`/`Surface-Destroy` stoppen Analyse + Frames, damit
  CPU/GPU auf ~0 % fallen (Details in Kapitel 4).

Weiter: [02 — Audio-Pipeline](02-audio-pipeline.md).
