# Visualisierungsmodul (`:visualization`) — Dokumentation

Zielgruppe: Entwickler ohne Vorwissen im Projekt, die schnell und umfassend in das
Visualisierungsmodul einsteigen wollen.

## Lese-Reihenfolge

| # | Kapitel                                   | Inhalt                                                                                                          |
|---|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| 0 | [Überblick](00-ueberblick.md)             | Was das Modul leistet, die drei Engines, Begriffe                                                               |
| 1 | [Architektur](01-architektur.md)          | Blockschaltbild, Komponenten, Thread-Modell                                                                     |
| 2 | [Audio-Pipeline](02-audio-pipeline.md)    | `VisualizerSink`, `SpectrumProcessor`, `Fft`, `BandMapper`, `AutoGain`                                          |
| 3 | [SpectrumBus](03-spectrum-bus.md)         | Lock-freier Ringbuffer zwischen Audio- und Render-Thread                                                        |
| 4 | [GLES-Rendering](04-rendering-gles.md)    | `SpectrumVisualizer`, `EglManager`, `RenderDriver`, `FrameRenderer`, `LedBarRenderer`, `LedBarSmoother`, Shader, Canvas-Fallback |
| 5 | [ProjectM / Nativ](05-projectm-nativ.md)  | `ProjectMGLSurfaceView`, `ProjectMNativeBridge`, `PresetManager`, `projectm_bridge.cpp`, Build                  |
| 6 | [Konfiguration & UI](06-konfiguration.md) | `MusicVisualization`, `VisualizerEngine`, `VisualizerParams`, `VisualizerDefinition`/`VisualizerFactory`, `VisualizerConfig`, `VisualizerTheme`, `Constants`, `SyntheticSpectrumSource` |
| 7 | [Build & Test](07-build-test.md)          | NDK/CMake/ABIs, `build_projectm.sh`, Unit-Tests                                                                 |
| 8 | [Glossar](08-glossar.md)                  | Fachbegriffe kurz erklärt                                                                                       |

Jedes Klassen-Kapitel folgt dem Schema **Aufgabe → Zuständigkeit/Abgrenzung →
wichtigste API → Ablauf → Zusammenspiel → Fallstricke**.

## Diagramme

Alle UML-/Block-Diagramme sind als `mermaid`-Codeblöcke direkt in den Kapiteln
enthalten: sie werden auf GitHub/GitLab gerendert und beim PDF-Build automatisch
zu PNG-Bildern umgewandelt (siehe unten). Es gibt bewusst keine separaten
Bilddateien zu pflegen.

## PDF erzeugen (Toolchain A: pandoc + mermaid-cli)

Voraussetzungen (alle bereits vorhanden bzw. per `npx` automatisch bezogen):

- `pandoc` + `xelatex` (TeX Live)
- `node`/`npx` (zieht `@mermaid-js/mermaid-cli` beim ersten Lauf)
- `chromium` als Headless-Browser für mermaid-cli

```bash
cd visualization/docs
./generate_pdf.sh
# Ergebnis: visualization/docs/visualization-doku.pdf
```

Optional:

```bash
./generate_pdf.sh --no-mermaid   # Diagramme als Codeblöcke behalten (ohne Rendering)
./generate_pdf.sh -o mein.pdf    # anderer Dateiname
make pdf                         # Kurzform, ruft das Skript auf
```

Das Skript extrahiert alle `mermaid`-Blöcke, rendert sie per `mmdc` nach PNG
(`.gen/`-Zwischenverzeichnis, wird nicht eingecheckt) und baut danach mit pandoc
ein PDF mit Inhaltsverzeichnis. Zwei Helfer sichern die Druckqualität:

- `table-colwidth.lua` (Lua-Filter): gibt Tabellen proportionale,
  umbrechende Spaltenbreiten — ohne ihn liefe langer Zellentext über den
  Seitenrand (pandoc erzeugt sonst starre `l`-Spalten).
- `header-includes` im Skript: begrenzt Bilder auf
  `\linewidth × 0.85\textheight`, damit kein Diagramm je aus der Seite läuft.
