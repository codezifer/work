# Messung 0019 – Doppelte In-Flight-Requests beim Laden von Covern

Vergleich der Coil-Requests vor und nach der Umsetzung von Issue 0019 (ein gemeinsamer
`AsyncImagePainter` in `GradientBlurredBackground`, sichtbare Indizes aus dem Prefetch-Fenster
ausgeschlossen).

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-10-25 |
| Gerät | `Pixel_10` (`emulator-5554`), Debug-Build |
| Coil | 3.6.3, `DebugLogger` (Tag `RealImageLoader`) |
| Bibliothek | Mock-Seeder, im Messfenster 18 Song-Dateien / 7 Content-Keys |
| Stand vorher | `8e31721` (Issue 0017 umgesetzt) |
| Stand nachher | Arbeitsstand mit den 0019-Änderungen |

Ein `Successful (DISK)`-Logeintrag bedeutet: Bytes geholt und Bitmap neu dekodiert.
`Successful (MEMORY_CACHE)` bedeutet: Treffer im RAM-Cache. Lokale SAF-Cover schreiben keinen
Disk-Cache, daher ist `DISK` hier gleichbedeutend mit „echter Decode“.

## Messfenster

Jede Messung startet mit `am force-stop` (leerer Memory-Cache) und leerem Logcat. Das
Öffnen-Fenster umfasst nur den Wechsel auf den Songs-Tab, das Scroll-Fenster die zwei
anschließenden Swipes nach unten. Beide Läufe besuchen dieselben Zeilen.

```bash
adb shell am force-stop de.carsten.android.muzzic && adb logcat -c
android run --apks=app/build/outputs/apk/debug/app-debug.apk
# Player -> Bibliothek, dann Logcat leeren
adb logcat -c
# Songs öffnen, 7 s warten
adb logcat -d -s RealImageLoader > after-songs.log
# Logcat leeren, zweimal nach unten scrollen, 4 s warten
adb logcat -d -s RealImageLoader > after-scroll.log
```

Auswertung: pro Data-String zählen (Duplikate = derselbe Request mehrfach) und pro Content-Key
(`hashCode` + `size` + `mimeType`) gruppieren.

## Ergebnis

| Fenster | Requests | DISK (Decodes) | MEMORY_CACHE | Keys mit >1 Decode | Max. Decodes pro Data-String |
| --- | --- | --- | --- | --- | --- |
| Songs öffnen, vorher | 21 | 10 | 11 | 3 von 5 | 2 |
| Songs öffnen, nachher | **10** | **7** | 3 | 2 von 5 | **1** |
| Scrollen, vorher | 33 | 7 | 26 | 1 von 5 | 3 |
| Scrollen, nachher | **20** | **5** | 15 | 1 von 4 | **2** |

Decodes pro Content-Key:

| Fenster | vorher | nachher |
| --- | --- | --- |
| Songs öffnen | 4, 2, 2, 1, 1 (5 Keys, 10 Decodes) | 2, 2, 1, 1, 1 (5 Keys, 7 Decodes) |
| Scrollen | 3, 1, 1, 1, 1 (5 Keys, 7 Decodes) | 2, 1, 1, 1 (4 Keys, 5 Decodes) |

## Einordnung

- **Ziel erreicht:** Kein Data-String wird im Öffnen-Fenster mehr als einmal dekodiert. Vorher
  waren es vier Data-Strings mit je zwei Decodes (Blur-Layer + Klar-Layer), im Scroll-Fenster
  einer mit drei Decodes (Prefetch + zwei Layer).
- **Requests halbiert:** 21 → 10 beim Öffnen, 33 → 20 beim Scrollen. Die Prefetch-Requests für
  bereits sichtbare Zeilen entfallen vollständig; pro Zeile bleibt genau ein Display-Request.
- **Rest-Duplikate pro Content-Key:** Die verbleibenden 2er-Gruppen sind keine Doppel-Requests
  derselben Zeile mehr, sondern mehrere gleichzeitig sichtbare Songs desselben Albums (z. B. drei
  sichtbare Tracks von „Sharp Dressed Men“). Diese Zeilen starten im selben Frame, verfehlen
  gemeinsam den Memory-Cache und dekodieren denselben Content-Key je einmal. Ein zusätzlicher
  In-Flight-Deduplicator (Änderung 3 aus Issue 0019) könnte das auf einen Decode pro Content-Key
  zusammenführen; der Gewinn wäre in diesem Fenster 7 → 5 Decodes.
- **Grenzen:** Kleine Bibliothek auf dem Emulator, keine Messung über die gesamte Bibliothek.
  Zwischen Vorher- und Nachher-Lauf wurde der Emulator neu gestartet; beide Läufe begannen aber
  mit geleertem Speicher und identischer Navigation. Die Rohlogs lagen nur temporär vor und sind
  nicht eingecheckt.

## Verwendung

Diese Zahlen sind die Vergleichsbasis für ein mögliches Folge-Issue zum In-Flight-Deduplicator
(Änderung 3 aus 0019). Ein solches Ticket sollte auf dieses Dokument und auf Issue 0019 verweisen
und dieselbe Messung erneut durchführen.

## Hinweis (2026-11-03)

Issue 0020 hat `ImagePrefetcher` vollständig entfernt und den globalen Crossfade abgeschaltet.
Die Aussagen zu Prefetch-Fenstern in diesem Dokument beschreiben daher den Stand vom 25.10.2026;
die Messmethode bleibt gültig. Die A/B-Messung dazu (Sprung-Latenz, stetiges Scrollen) steht in
`0020-cover-latency-fastscroller.md`.
