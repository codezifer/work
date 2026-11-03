# Messung 0020 – Cover-Latenz nach einem FastScroller-Sprung

Vergleich von `HEAD` (Crossfade + `ImagePrefetcher`) gegen den Arbeitsstand aus Issue 0020
(kein Crossfade, kein Prefetcher).

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-03 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37.1, 1080×2424, 420 dpi, Debug-Build |
| Coil | 3.6.3, `DebugLogger` (Tag `RealImageLoader`), Memory-Cache leer nach `am force-stop` |
| Bibliothek | 100 Songs, 32 Alben (lokale Album-Art aus den MP3s, kein Disk-Cache) |
| Vorher | `c507973` (HEAD: 200 ms Crossfade, Prefetcher mit 512 px) |
| Nachher | Arbeitsstand 0020 (kein Crossfade, kein Prefetcher) |

Die Cover liegen als eingebettete Bilder in den MP3s, nicht im Coil-Disk-Cache. Jeder
`Successful (DISK)`-Eintrag ist damit ein echter Decode aus der Datei, `Successful
(MEMORY_CACHE)` ein Treffer im RAM-Cache.

## Methode

`screenrecord` liefert auf diesem Emulator nur 1–2 Frames pro Lauf (SwiftShader ohne
GPU-Komposition), deshalb wird die Latenz aus den Coil-Logs gemessen und die Geste über einen
Gerätezeit-Anker synchronisiert:

```bash
adb shell am force-stop de.carsten.android.muzzic && adb logcat -c
android run --apks=app/build/outputs/apk/debug/app-debug.apk
# Bibliothek -> Alben, danach Logcat leeren
adb shell 'input swipe 540 1300 540 950 120; sleep 0.1; echo "T0=$(date +%s%3N)"; \
           input tap 995 1160; echo "T1=$(date +%s%3N)"'
adb logcat -d -v threadtime -s RealImageLoader
```

Der Swipe macht die Buchstabenleiste sichtbar (sie ist im Ruhezustand ausgeblendet), der Tap
auf einen Buchstaben löst den Sprung aus. `T0`/`T1` liegen in derselben Uhr wie die
Logcat-Zeitstempel; alle Zeiten unten sind Millisekunden nach `T0`. Sprungziel ist in jedem
Lauf derselbe Buchstabe. Für das Scroll-Fenster ersetzen zwei langsame Swipes den Tap.

Gemessen wird die Fertigstellung des Decodes. Ohne Crossfade ist das Deckungsgleich mit dem
ersten Frame, in dem das Cover sichtbar ist (plus ≤ 2 Frames Bildaufbau). Mit Crossfade
beginnt an diesem Punkt erst die 200-ms-Blende von Alpha 0 auf 1.

## Ergebnis – Sprung (Tap auf die Buchstabenleiste)

| Lauf | Requests nach Tap | davon `DISK` | erster Decode | letzter Decode |
| --- | --- | --- | --- | --- |
| vorher 1 | 34 | 32 | 240 ms | 580 ms |
| vorher 2 | 32 | 32 | 206 ms | 484 ms |
| vorher 3 | 32 | 32 | 200 ms | 446 ms |
| nachher 1 | 14 | 14 | 208 ms | 259 ms |
| nachher 2 | 14 | 14 | 225 ms | 259 ms |
| nachher 3 | 12 | 12 | 196 ms | 228 ms |

Alle Zahlen ohne die zwei Decodes der vorherigen Seite (vor `T0`). Der erste Batch landet in
beiden Varianten bei ~200–240 ms: das ist der Decode der sichtbaren Kacheln. Der Unterschied
steckt im Rest: vorher laufen danach noch 20 weitere Prefetch-Decodes nach (Wellen bei 300,
340, 400, 450, 480, 580 ms), nachher ist nach dem ersten Batch Schluss.

Bis das Cover **vollständig sichtbar** ist, ergibt sich daraus:

| | vorher | nachher |
| --- | --- | --- |
| erstes Cover sichtbar | ~400–440 ms (Decode + 200 ms Fade) | ~200–260 ms |
| letztes Cover vollständig | ~650–780 ms | ~230–260 ms |

## Ergebnis – stetiges Scrollen (zwei langsame Swipes)

| Lauf | Requests | `DISK` | `MEMORY_CACHE` |
| --- | --- | --- | --- |
| vorher 1 | 36 | 11 | 25 |
| vorher 2 | 36 | 11 | 25 |
| nachher 1 | 12 | 12 | 0 |
| nachher 2 | 16 | 16 | 0 |

Die Zahl der echten Decodes bleibt gleich (11 gegen 12–16 bei leicht unterschiedlich weit
gescrollten Fenstern). Der Prefetcher hat die Decodes nur vorverlegt: die Anzeige-Requests
trafen danach den RAM-Cache. Ein Zeitgewinn beim Scrollen ist darin nicht messbar, weil
Compose in Scrollrichtung ohnehin ein Item vor-komponiert und der Request dort startet.

Auf diesem Gerät ist die Kartenbreite ~500 px, also ≤ 512 px: die Prefetch-Größe war hier
zufällig groß genug, und der Prefetch griff (25 `MEMORY_CACHE`-Treffer). Auf dichteren Geräten
(Kartenbreite ~670 px) verwirft Coil diese Einträge, wie in Issue 0020 beschrieben – dort war
der Prefetcher zusätzlich wirkungslos.

## Einordnung

- **Sprung:** Der sichtbare Gewinn kommt aus zwei Teilen: 200 ms Crossfade (deterministisch) und
  dem Wegfall von 20 Prefetch-Decodes, die nach dem Landepunkt mit den sichtbaren Kacheln um
  CPU- und IO-Zeit konkurrieren. Zusammen sinkt „Loslassen bis letztes Cover“ von ~480–680 ms
  auf ~230–260 ms.
- **Scrollen:** Keine Regression. Die Anzeige dekodiert dieselben Bilder, nur eben genau dann,
  wenn sie gebraucht werden.
- **Grenzen:** Ein Gerät, ein Emulator, 32 Alben. Gemessen wird die Decode-Fertigstellung, nicht
  die tatsächliche Präsentation (kein brauchbares `screenrecord` auf diesem Emulator). Der
  Crossfade-Anteil ist aus dem Coil-Code belegt, nicht aus Frames.
