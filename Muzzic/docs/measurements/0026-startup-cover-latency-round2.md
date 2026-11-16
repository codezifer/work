# Messung 0026 – Wo die verbleibende Cover-Latenz herkommt

Zweite Messrunde zum Kaltstart. Nach 0021–0025 war der Delay weiterhin spürbar,
also wurde neu vermessen statt weiter zu optimieren. Ziel: die verbleibenden
Deltas zwischen „PlayerScreen sichtbar" und „Album-Art sichtbar" attribuieren.

Ergebnis vorweg: **keine Code-Änderung hat die Messung überlebt.** Drei
Hypothesen sind widerlegt, eine Änderung war nicht reproduzierbar, und der
größte Hebel liegt außerhalb des App-Codes.

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-16 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37, 4 vCPUs, SwiftShader |
| App | `de.carsten.android.muzzic` 0.12.4, Debug-Build, `09e190e` |
| Start | `am start -W` nach `am force-stop`, 1 Warmup verworfen |
| Läufe | je 5 Kaltstarts pro Variante |
| Trace | `b10`, `b11` (Perfetto, 12 s, atrace + `sched` + SurfaceFlinger) |

Das Messwerkzeug aus 0021–0025 lag nicht mehr vor und wurde neu gebaut
(`measure.sh`, `record-coldstart.sh`, `phases.py`, `window.py`, `markers.py`,
`kpi.py`). Es liegt bewusst nur unter `/tmp/opencode` und wird nicht committet.

## Mess-Hook (behalten)

`StartupTrace.mark()` emittiert zusätzlich einen kurzen atrace-Slice `ST:<name>`:

```kotlin
fun mark(name: String) {
    Trace.beginSection("ST:$name")
    Trace.endSection()
    logger.info("$name @ +${elapsedMs()}ms")
}
```

Damit liegen App-Marker und Systemereignisse erstmals in **einer** Zeitbasis.
Vorher mussten Logcat-Zeitstempel gegen den Trace ausgerichtet werden, was
Fehler in der Größenordnung der gesuchten Deltas erzeugt. Ohne aktives Tracing
kostet `Trace.beginSection` nur einen Flag-Check.

Nebenbefund: `slice.ts` in PerfettoSQL ist **Nanosekunden**, nicht Mikrosekunden.

## Ergebnisse

Median je Variante, ms. `req` = `first.frame → cover.display.start`,
`cover` = `cover.display.start → cover.firstFrame`.

| Lauf | ART | Änderung | `first.frame` | `cover.firstFrame` | req | Gate | Fetch | Decode | Tail | Draw | **Summe** |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `b10` | JIT | — | 1220 | 2184 | 406 | 212 | 44 | 184 | 44 | 31 | **956** |
| `b11` | JIT | ohne Emoji | 1271 | 2347 | 425 | 102 | 70 | 211 | 70 | 59 | **1006** |
| `b12` | AOT | ohne Emoji | 715 | 1688 | 356 | 212 | 27 | 210 | 63 | 30 | **941** |
| `b13` | AOT | — | 668 | 1774 | 381 | 377 | 39 | 175 | 12 | 45 | **1091** |
| `b14` | AOT | Display Software | 692 | 1529 | 363 | 248 | 27 | 71 | 62 | 10 | **808** |
| `b15` | AOT | Display Software, Regime B | 910 | 3536 | 2111 | 281 | 25 | 93 | 20 | 57 | **2595** |
| `b16` | AOT | — , Regime B | 735 | 3154 | 1861 | 288 | 33 | 98 | 63 | 75 | **2457** |

`Gate` = `mapEnd → fetchStart`, `Tail` = `decodeEnd → success`, `Draw` = `success → firstFrame`.

## Befunde

### 1. Der 500-ms-Fade liegt nicht auf dem kritischen Pfad (widerlegt)

Drei Bildschirmaufnahmen, Cover-Region als Graustufen-Mittelwert pro Frame:

```
1.14–2.03  Platzhalter (Disc), statisch
2.04–2.28  Rampe  → das ist der Disc, der einfadet (AnimatedContent)
2.69–2.81  Sprung  → das Cover ersetzt die Disc in ≤ 70 ms
```

Das Cover erscheint **hart**, ohne Einblendung. Die 500 ms aus
`ALBUM_ART_FADE_IN_OUT` laufen ab, während der Platzhalter sichtbar ist und die
Bitmap noch nicht existiert. Eine Kürzung des Fades bringt für den Kaltstart
nichts.

### 2. Der Visualizer ist beim Kaltstart inaktiv (widerlegt)

Im Fenster `first.frame → cover.firstFrame` existiert kein `VisualizerGL`-Thread
und kein GL-Slice. Die ~226 ms CPU aus einer früheren Beobachtung stammen aus
Läufen mit laufender Wiedergabe. Für den Kaltstart irrelevant.

### 3. EmojiCompat: Mechanismus bestätigt, Wirkung null (zurückgebaut)

`androidx.emoji2.text.EmojiCompatInitializer` läuft beim Kaltstart
(`EmojiCompat.MetadataRepo.create` 158 ms, Font-Request 170 ms, eigener Thread).
Entfernen über `tools:node="remove"` beseitigt Thread und CPU sauber —
aber ohne Wall-Clock-Gewinn (`b11` 1006 ms vs `b10` 956 ms, überlappende
Streuung). Eine Compose-App rendert Emoji über die Plattform-Schrift, der
Initializer ist also nutzlos, aber er ist nicht der Engpass. Nach der Regel
„nur messbar Gewonnenes bleibt" zurückgebaut.

### 4. ART-Zustand ist der größte Hebel (≈ 500 ms), aber kein App-Code

`adb shell cmd package compile -m speed -f` vor der Messung:

| | JIT (`b10`) | AOT (`b13`) |
| --- | ---: | ---: |
| `activity.create` | 512 | **124** |
| `first.frame` | 1220 | **668** |
| `cover.firstFrame` | 2184 | **1774** |
| `am start -W` TotalTime | 1423 | **~950** |

Der Cover-Pfad selbst (`first.frame → cover.firstFrame`) bleibt dabei konstant
bei ~950–1100 ms. Das heißt: der Anteil **vor** der Cover-Anfrage skaliert mit
dem Dexopt-Zustand, der Anteil **nach** der Anfrage nicht.

Das erklärt auch die Drift, die 0021 als „`am start -W` ist unbrauchbar"
vermerkt hat: nicht Zeitdrift, sondern wechselnder ART-Zustand.

### 5. Das Gate ist Klassenladen, kein App-Code

Fenster `mapEnd → fetchStart` (111 ms, `b10`):

| Thread | Slice | ms |
| --- | --- | ---: |
| main | `VerifyClass com.google.common.collect.Lists` | 20.8 |
| main | `Lock contention on ClassLinker classes lock` | 19.3 |
| main | `Lcom/google/common/collect/Lists$TwoPlusArrayList;` | 19.3 |
| main | `AndroidOwner:draw` | 14.5 |
| DefaultDispatcher | `monitor contention ... ZipFile$Source.initCEN` | 82.8 |
| Jit thread pool | `Compiling baseline` (373×) | 90.3 |

Main-Thread: `Running` 63 von 111 ms. Das Gate ist also nicht Warten auf I/O,
sondern Erstbenutzung des Coil/okio-Pfads plus `DiskLruCache.initialize()`.

### 6. Der Main-Thread ist der Engpass vor der Anfrage

Fenster `playerVm.init → song.prefetched` (1048 ms, `b10`):

| Thread | ms |
| --- | ---: |
| main (`Running`) | **873** |
| Jit thread pool | 440 (2312 Kompilierungen) |
| EmojiCompatInit | 170 |
| RenderThread | ~350 |

Der Main-Thread ist zu 83 % beschäftigt: Service-Start (ExoPlayer-Konstruktion,
`VerifyClass` der media3-Klassen), erste Komposition, Zeichnen, Scan-Callbacks.
`preloadCurrentSong()` liest die Queue zwar lokal, kann das Ergebnis aber erst
zustellen, wenn der Main-Thread frei ist.

### 7. Display-Decode mit Software-Bitmap: nicht reproduzierbar (zurückgebaut)

`allowHardware(false)` am Display-Request (`rememberCoverImageRequest`), analog
zu Blur/Notification aus 0025:

- Regime A (`b14`): Decode 175 → **71 ms**, Summe 1091 → **808 ms**
- Regime B (`b15` vs `b16`): Decode 93 vs 98 ms, Summe 2595 vs **2457 ms** (schlechter)

Die Ursache ist plausibel (Hardware-Allocator deaktiviert Sampling, plus
`allocateHardwareBitmap` 120 ms und `uploadHardwareBitmap` 112 ms im Trace),
aber der Gesamteffekt war nicht reproduzierbar. Zurückgebaut, weil die Änderung
alle Cover-Ansichten betrifft (Galerie, Playlists) und Speicher von GPU nach
Heap verschiebt.

## Regimewechsel (Messfehler, dokumentiert)

Nach einem Emulator-Absturz und Neustart war `first.frame → service.create`
**1754 ms** statt 175 ms (5/5 Läufe), bei `load average` 0.24 und 392 % idle.
Ursache nicht abschließend geklärt (Speicherdruck: 3,67 von 4 GB belegt,
622 MB Swap; möglicher Rescan). Die Läufe `b14b`/`b14c` sind deshalb verworfen.
**Konsequenz für künftige Runden:** ART-Zustand pinnen
(`cmd package compile -m speed -f`), System nach Boot ~5 min beruhigen lassen,
und A/B nur innerhalb desselben Regimes vergleichen.

## Nachtrag: Was der 1,4-s-Block `first.frame → service.create` ist

Der Block ist im aktuellen Regime der größte Posten und war der einzige offene
Punkt. Trace `b17` (3 Läufe, `first.frame → service.create` 1348/1454/1380 ms):

| Beobachtung | Wert |
| --- | --- |
| Main-Thread in **einem** `Choreographer#doFrame` | 80.9 → 2042 ms (1961 ms) |
| `draw-VRI` endet | 765.1 ms |
| Main-Thread danach `S`, geweckt von RenderThread | **1275.9 ms** |
| RenderThread `DrawFrames 85965` | 764.8 → 2041.1 ms (1276.3 ms) |
| davon `flush commands` | 948.2 ms |
| `eglSwapBuffersWithDamageKHR` | 134.0 ms |
| `shader_compile`, ganzer Start, 18 Programme | **1197.5 ms** |
| RenderThread-Zustand im Fenster | `Running` 1034 von 1084 ms |

Die Kinder von `shader_compile` sind winzig: `driver_compile_shader` ~1 ms,
`driver_link_program` 1–6 ms, `cache_miss` 4–8 ms. Der GL-Treiber ist also
~50 ms von 1197 ms — der Rest ist Skias CPU-seitiger Programmbau. Kein Lock
(RenderThread läuft durch), kein CPU-Mangel (alle 4 vCPUs ~96–100 %).

**Der Vergleich entscheidet:** derselbe Code, dieselbe Anzahl Programme,
zwei Boots.

| Trace | `shader_compile` | Programme |
| --- | ---: | ---: |
| `b10` | **81.3 ms** | 18 |
| `b17` | **1197.5 ms** | 18 |

Faktor 14,7 bei identischem Umfang. Ein Shader kostet hier 40–150 ms statt
2–10 ms. Damit ist der Block **kein App-Code und keine feste Kostenstelle**,
sondern eine Eigenschaft dieses Emulator-Boots (SwiftShader-Software-GL bei
209 MB freiem Speicher und aktiver Swap-Nutzung). Warum genau, ließ sich ohne
Kernel-Symbole nicht aufklären; ausgeschlossen sind Treiberkosten,
RenderThread-Verhungerung und Host-Last (12 Kerne, load 3,7).

Ein Shader-Disk-Cache existiert nicht (`run-as … ls cache/` ist leer), deshalb
zahlt **jeder** Kaltstart die volle Summe — konsistent mit 5/5 langsamen Läufen
und mit `b18` (1447 ms, nach dem System-Beruhigen unverändert).

Strukturell wichtig: der Main-Thread hängt 80 → 2042 ms in **einem** Frame.
Alles Folgende steht in der Warteschlange dahinter, nicht in einer kausalen
Abhängigkeit — der Service-Bind wird bereits bei `playerVm.init` (~330 ms)
angestoßen, `MusicPlayerService.onCreate` läuft aber erst bei 2107 ms, danach
`prefetched` 2323 ms und die Cover-Anfrage 2496 ms. P1 ist also eine **Queue**,
kein Serialisierungsschritt, und app-seitig nicht zu verkürzen: ExoPlayer muss
auf einem Looper-Thread gebaut werden.

## Konsequenzen

1. Kein Code-Stand aus dieser Runde wird übernommen — außer dem `ST:`-Marker in
   `StartupTrace`.
2. Der nächste sinnvolle Schritt ist **kein** App-Code, sondern ein
   Baseline-Profile (das Projekt hat keins) bzw. die Messung des Release-Builds.
   `-m speed` zeigt die Obergrenze: ~500 ms.
3. Der einzige verbliebene App-seitige Block ist der Service-Start auf dem
   Main-Thread (175 ms vor dem Neustart, 1754 ms danach). Er ist der Grund, warum
   die Cover-Anfrage nicht früher entstehen kann — `preloadCurrentSong()` liefert
   die Daten, aber der Main-Thread stellt sie erst nach Service-Start zu.
   Laut Nachtrag oben ist die Ursache dieser Wartezeit der erste Frame
   (Shader-Kompilierung im Software-GL), nicht der Service-Start selbst.
