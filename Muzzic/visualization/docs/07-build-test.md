# 7 — Build & Test

## 7.1 Gradle-Modul (`visualization/build.gradle.kts`)

- Android-Library (`namespace de.carsten.android.muzzic.visualization`),
  `minSdk`/`targetSdk`/NDK-Version aus dem Versionskatalog, Compose aktiviert,
  `externalNativeBuild` mit `src/main/cpp/CMakeLists.txt` (CMake 3.22.1,
  C++17).
- Abhängigkeiten: `:logging`, Coroutines, Compose (BOM), Media3-ExoPlayer
  (für `TeeAudioProcessor`), Koin; Tests: JUnit, AssertJ, MockK.
- Workflow-Regeln (AGENTS.md) gelten auch hier: nach Kotlin-Änderungen
  `./gradlew ktlintFormat`, vor Abschluss `:visualization:testDebugUnitTest`.

## 7.2 Nativ-Build-Kette

```
build_projectm.sh → jniLibs/<ABI>/*.so → Gradle → libprojectm_bridge.so
        │                ▲ C-Header → cpp/include/projectM-4/
        └── Upstream-Clone (/tmp, nicht eingecheckt)
```

- Normaler App-Build compiliert **nur** die kleine `projectm_bridge.cpp`
  gegen die eingecheckten Prebuilts — kein Upstream-Download, schneller Build.
- Nur wer die Engine aktualisiert, führt `./build_projectm.sh` aus
  (Voraussetzungen: NDK, CMake 3.22.1+, `ANDROID_HOME`/`ANDROID_NDK_HOME`).
  Details + Glad-/VAO-Patches: Kapitel 5.5.
- `version.h`/`projectM_export.h` in `cpp/include` sind generiert — bei
  Upstream-Wechsel mit den Prebuilts aktualisieren (`PROJECTM_VERSION_*`).

## 7.3 Unit-Tests (`src/test/.../visualization/`)

Alle JVM-Tests (kein Emulator nötig — möglich, weil `audio`/`bus` kein
Android enthalten; `VisualizerFactoryTest` braucht nur Compose-`Color`-Daten):

| Test | Sichert ab |
|---|---|
| `FftTest` | Impuls → flach, Sinus → Peak am richtigen Bin |
| `BandMapperTest` | Log-Kanten, RMS-Mittel, Bass-Interpolation |
| `AutoGainTest` | dB/Tilt-Normierung, Silence-Sperre, Release-Verhalten |
| `SpectrumProcessorTest` | Ende-zu-Ende: PCM → normierte Bus-Frames |
| `SpectrumBusTest` | Seqlock-Konsistenz, `readAtOrBefore`-Semantik |
| `LedBarSmootherTest` | Attack/Decay/Peak-Hold-Zeiten |
| `MirroredBarsTest` | Center-Mirror-Abbildung (`mirroredBandIndex`) |
| `BarsThemeTest` | HSL-Ableitung (`barsThemeFrom`) |
| `LedThemeTest` | LED-Zonen-Ableitung aus der Akzentfarbe (`ledThemeFrom`) |
| `VisualizerFactoryTest` | Engine→Definition-Auflösung (`VisualizerFactory`), Params-Mismatch-Abweisung |
| `ConfigDefaultsTest` | Sinnvolle Defaults (fängt versehentliche Änderungen) |
| `ShaderReservedWordsTest` | Kein `half` o. a. reservierte Wörter im GLSL |

Ausführen: `./gradlew :visualization:testDebugUnitTest`. Faustregel: Wer
`audio`/`bus`/`render`-Logik anfasst, erweitert den passenden Test (Fakes
statt Mocks für Repositories/Datenquellen; reine Logik ist direkt testbar).

Weiter: [08 — Glossar](08-glossar.md).
