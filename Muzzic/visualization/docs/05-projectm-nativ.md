# 5 — ProjectM / Nativ (`component`, `projectm`, `cpp`)

Der ProjectM-Pfad rendert 3D-Milkdrop-Visuals in nativem C++ (`libprojectM`)
auf einer eigenen `GLSurfaceView`. Kotlin besitzt **keine** Rendering-Logik —
es verwaltet nur Lebenszyklus, Presets und PCM-Nachschub.

```mermaid
flowchart LR
    subgraph Kotlin["Kotlin-Seite"]
        GLSV["ProjectMGLSurfaceView\n(GLSurfaceView, ES 3)"]
        R["ProjectMRenderer\nonSurfaceCreated/Changed/DrawFrame"]
        BR["ProjectMNativeBridge\n(Singleton)"]
        PMgr["PresetManager"]
    end
    subgraph JNI["JNI-Grenze"]
        J1["nativeInit / nativeResize\nnativeRender / nativeAddPcm\nnativeNext/Previous/SelectPreset\nnativeIsActive / nativeRelease"]
    end
    subgraph Native["C++-Seite (projectm_bridge.cpp)"]
        G["g_projectM-Handle\n+ g_mutex + Presetliste"]
        CAPI["public C-API\n(projectm_*)"]
    end
    subgraph Lib["Vorcompiliert"]
        SO[("libprojectM-4.so\n(jniLibs/<ABI>)")]
    end
    GLSV --> R --> BR --> J1 --> G --> CAPI --> SO
    PMgr -.->|"Preset-Verzeichnis\nfilesDir/projectm/presets"| G
```

## 5.1 `ProjectMGLSurfaceView` (`component/ProjectMGLSurfaceView.kt`)

**Aufgabe:** Dünne GL-Hülle mit festem Setup:

- EGL-Kontext **3.0**, RGBA8888 + 16-Bit-Tiefe, `ZOrderMediaOverlay`,
  `RENDERMODE_CONTINUOUSLY`.
- `presetName`-Property: bei Änderung via `queueEvent` (also auf dem
  GL-Thread!) an `selectPreset` weitergereicht.
- Innerer `ProjectMRenderer`:
  - `onSurfaceCreated` → `PresetManager.ensurePresetsExtracted()` →
    `ProjectMNativeBridge.init(presetDir)` (+ optionales `selectPreset`).
    **Reihenfolge wichtig:** `init` braucht einen aktuellen GL-Kontext.
  - `onSurfaceChanged` → `resize(w, h)`.
  - `onDrawFrame` → `render()`.
- `onDetachedFromWindow` → `release()` (native Instanz zerstören, sonst
  GL-Speicherleck).

## 5.2 `ProjectMNativeBridge` (`projectm/ProjectMNativeBridge.kt`)

**Aufgabe:** Absturzsichere JNI-Fassade als `object`-Singleton.

- **Ladereihenfolge** im `init`: erst `projectM-4`, dann `projectm_bridge`
  (die Bridge linkt gegen die Lib). Schlägt eines fehl
  (`UnsatisfiedLinkError`), werden **alle** Aufrufe zu sicheren No-Ops —
  die GL-Fläche zeigt dann nur die Stub-Hintergrundfarbe, die App läuft weiter.
- Öffentliche Methoden (`init`, `resize`, `render`, `addPcm`, `nextPreset`,
  `previousPreset`, `selectPreset`, `release`, `isActive`) prüfen
  `isNativeLibraryLoaded` und delegieren an `private external fun`
  (`native*`).
- `addPcm(pcmData: FloatArray, channels)`: interleavtes Float-PCM (−1..1,
  LRLR bei Stereo). `isActive` (= Lib geladen **und** native Instanz existent)
  steuert, ob `VisualizerSink` überhaupt Samples schickt.

## 5.3 `projectm_bridge.cpp` (`src/main/cpp/`)

**Aufgabe:** Einzige Stelle mit nativem projectM-Kontakt. Nutzt
**ausschließlich die öffentliche C-API** (`projectM-4/projectM.h`,
`projectm_*`-Funktionen) — die C++-Klassen-API hat in der vorcompilierten Lib
*hidden visibility*; jeder Griff darauf endet im Linkfehler
(`undefined symbol`).

Zustand (alles unter `g_mutex`):

- `g_projectM` (opakes Handle), `g_presetDir`, sortierte `g_presets` +
  `g_presetIndex`, Viewport-Größe.
- `android_gl_load_proc`: GL-Funktionslader — erst `eglGetProcAddress`, dann
  Fallback per `dlsym` auf `libGLESv3.so`/`libGLESv2.so` (Android liefert
  Core-Funktionen nicht über `eglGetProcAddress`).
- `nativeInit`: Preset-Verzeichnis scannen (`.milk`-Suffix, sortiert) →
  GL-Info loggen → Log-Callback setzen → `projectm_create_with_opengl_load_proc`
  (muss auf dem GL-Thread mit aktuellem Kontext laufen) → Fenstergröße +
  Textur-Suchpfad setzen → **erstes Preset ohne Übergang** laden
  (Folgewechsel immer mit Übergang = `smooth`).
- `nativeResize`: `glViewport` + `projectm_set_window_size`.
- `nativeRender`: `projectm_opengl_render_frame`; ohne Instanz nur
  dunkelblaues Clear (Stub-Modus, z. B. wenn per CMake ohne Prebuilt gebaut).
- `nativeAddPcm`: Float-Array aus JNI holen, durch
  `projectm_pcm_add_float(handle, samples, count, STEREO|MONO)` schieben
  (`count` = Samples **pro Kanal**), per `JNI_ABORT` freigeben (kein
  Rückkopieren nötig).
- `nativeNext/Previous/SelectPreset`: Index-Arithmetik (Previous mit
  `+ size − 1` statt Modulo-negativ) bzw. Pfad aus Verzeichnis + Name laden
  und Index nachführen.
- `nativeIsActive`/`nativeRelease`: Handle-Check bzw. `projectm_destroy` +
  Listen leeren.

## 5.4 `PresetManager` (`projectm/PresetManager.kt`)

**Aufgabe:** Macht gebündelte Presets zur Laufzeit auffindbar.

- `ensurePresetsExtracted(context)`: kopiert `assets/presets/*.milk` nach
  `filesDir/projectm/presets/` — nur fehlende Dateien (idempotent), gibt den
  absoluten Pfad zurück (→ `nativeInit`).
- `getAvailablePresets` (sortierte `.milk`-Namen) und `getPresetFilePath`
  für Preset-Auswahl-UIs.
- Mitgeliefert: 8 eigene `Muzzic -*`-Presets in `src/main/assets/presets/`.

## 5.5 Nativ-Build (`CMakeLists.txt`, `jniLibs/`, `build_projectm.sh`)

- `src/main/cpp/CMakeLists.txt`: sucht `libprojectM*.so/.a` unter
  `jniLibs/${ANDROID_ABI}` → als `IMPORTED`-Lib einbinden + `HAVE_PROJECTM=1`
  definieren. **Ohne** Prebuilt wird nur der Stub (`projectm_bridge` ohne
  projectM) gebaut — App bleibt lauffähig, ProjectM zeigt Hintergrundfarbe.
  Gelinkt wird gegen `GLESv2`, `EGL`, `log`.
- `src/main/jniLibs/<arm64-v8a|x86_64|armeabi-v7a|x86>/libprojectM-4.so`:
  vorcompilierte projectM-4.2.0-Binaries (Hauptbuild lädt sie, compiliert aber
  kein C++-Upstream neu → schneller Build).
- `build_projectm.sh` (im `visualization/`-Verzeichnis ausführen): klont
  projectM (master + Submodule), patcht `GladLoader.cpp` auf GLES 3.0
  (Upstream verlangt 3.2) und `VertexArray.hpp` (redundante Binds raus),
  baut pro ABI mit NDK-Toolchain (`ENABLE_GLES=ON`, SDL/QT/Tests aus),
  strippt, kopiert `.so` nach `jniLibs/` sowie öffentliche C-Header
  (`*.h`, **kein** C++-`hpp`) + generierte `version.h`/`projectM_export.h`
  nach `cpp/include/projectM-4/`.

**Die wichtigste Regel dieses Kapitels:** JNI-Bridge **nur** gegen
`projectM-4/projectM.h` (C-API). Wer interne `libprojectM`-C++-Header
vendored oder C++-Symbole aufruft, bekommt `undefined symbol`-Linkfehler —
das ist dokumentiertes Verhalten der Prebuilts, kein Toolchain-Bug.

Weiter: [06 — Konfiguration & UI](06-konfiguration.md).
