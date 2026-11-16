# Messung 0024 – Fetch-vs-Decode im Cover-Pfad des Kaltstarts

Zerlegung der 638 ms, die in Messung 0021 zwischen `cover.display.start` und
`cover.firstFrame` lagen. Frage: dominiert der Fetch (Disk-I/O) oder der Decode?
Davon hängt ab, ob eine "Preview first"-Strategie (kleines Cover vor dem vollen
Decode) überhaupt etwas bringt.

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-10 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37, 4 vCPUs, SwiftShader (Software-GL) |
| App | `de.carsten.android.muzzic` 0.12.4, Debug-Build |
| Bibliothek | 1693 Songs, Queue mit 12 Songs, MP3s mit eingebettetem Cover |
| Start | `am start -W` nach `am force-stop`, 1 Warmup verworfen |
| Läufe | `b5-gate`: 4 Läufe + 1 Perfetto-Trace (`coldstart-4`) |
| Basis | Marken relativ zum Prozess-Fork (`Process.getStartUptimeMillis()`) |

Vorher wurde der Mess-Hook korrigiert (siehe unten), sonst wäre jede Zahl um
~100 ms zu spät.

## Mess-Hook (separater Fix, behalten)

`AlbumArtControl` markierte `cover.firstFrame` über `onSuccess` → `LaunchedEffect` →
`withFrameNanos`. Das meldet den Frame **nach** dem Zeichnen, nicht den, der das
Bitmap zeigt. Ersetzt durch `Modifier.drawWithContent` mit zwei `AtomicBoolean`
(kein Recompose, kein State):

```kotlin
Modifier.drawWithContent {
    drawContent()
    if (bitmapLoaded.get() && drawn.compareAndSet(false, true)) onCoverDrawn?.invoke()
}
```

| Marke (Median, 5 Läufe) | HEAD | mit Hook-Fix | Δ |
| --- | ---: | ---: | ---: |
| `cover.firstFrame` | 3684 | 3586 | **−98** |
| `am start -W` TotalTime | 2381 | 2425 | +44 |

Der Gewinn ist reine Messkorrektur, keine App-Änderung: die alte Marke kam einen
Frame zu spät. `TotalTime` bleibt im Rauschen.

## Instrumentierung

`StartupImageListener` (nur `BuildConfig.DEBUG`) markiert jetzt zusätzlich
`sizeStart`, `mapStart/End`, `keyStart/End`, `fetchStart/End`, `decodeStart/End`.
Damit ist der Pfad in seine Coil-Phasen zerlegt.

## Ergebnisse – Phasen der Display-Variante (Median, 4 Läufe)

| Phase | ms | Anteil |
| --- | ---: | ---: |
| `start` → `size` (Size-Resolver) | 22 | 3 % |
| `mapEnd` → `fetchStart` (**Gate**) | 256 | 30 % |
| `fetchStart` → `fetchEnd` (Disk-Cache) | 46 | 5 % |
| `fetchEnd` → `decodeEnd` (Decode) | 307 | 36 % |
| `decodeEnd` → `success` | 147 | 17 % |
| `success` → `firstFrame` (Zeichnen) | 31 | 4 % |
| **Summe `start` → `firstFrame`** | **860** | |

Streuung: Gate 224–297 ms, Decode 70–349 ms (ein Ausreißer-Lauf mit 70 ms).

## Trace-Befund (Perfetto, `coldstart-4`)

1. **Der Fetch ist irrelevant.** 46 ms Median, Quelle immer `DISK`. Der Pfad ist
   nicht I/O-gebunden im Sinne von "die Bilddatei muss erst gelesen werden".
2. **Alle vier Cover-Varianten dekodieren dasselbe JPEG in voller Auflösung.**
   `ImageDecoder_nDecodeBitmap` im selben Zeitfenster auf `DefaultDispatcher`-Threads:

   | Variante | Ziel | Decode |
   | --- | --- | ---: |
   | Notification | 512×512 | 292 ms |
   | Blur | 256×256 | 292 ms |
   | Palette | 96×96 | 26 ms |
   | Display | 912×912 | 273 ms |

   Nur die 96-px-Variante wird herunterskaliert; 256, 512 und 912 kosten alle
   ~280 ms, weil `ImageDecoder.setTargetSize` auf diesem Gerät nicht sampled.
   Quelle ist 1208×912. Auf 4 vCPUs laufen die drei großen Decodes parallel und
   teilen sich die Kerne – der Display-Decode braucht deshalb ~280 ms statt ~90 ms.
3. **Hardware-Bitmaps kosten zusätzlich.** `allocateHardwareBitmap` ~259 ms und
   `uploadHardwareBitmap` ~247 ms (inkl. `Creating EGLContext` 157 ms auf
   `GrallocUploadTh`) laufen auf dem Coil-Worker, also nicht auf dem Main-Thread.
4. **Das Gate ist kein App-Code.** Im Fenster `mapEnd` → `fetchStart` gibt es
   keinen einzigen App-Slice und keine App-CPU-Zeit; die vier Requests laufen alle
   zum selben Zeitpunkt in `fetchStart`, obwohl sie bis zu 300 ms versetzt starten.
   Zeitgleich läuft `EmojiCompatInit` (348 ms, 205 ms CPU, lädt die Emoji-Font von
   Disk), dazu Room-Schreibzugriffe des laufenden Scans und ExoPlayer-Reads. Das
   Disk-Cache-Journal wird beim ersten Zugriff lazy geöffnet
   (`DiskLruCache.initialize()` unter globalem Lock) und serialisiert alle vier
   Requests dahinter.

## Bewertung

1. **Preview first wird verworfen.** Eine kleine Vorab-Anfrage würde einen
   *fünften* voll aufgelösten Decode in dasselbe CPU-Fenster legen und den
   Display-Decode weiter verlangsamen. Der Gewinn wäre höchstens die Differenz
   zwischen 96-px- und 912-px-Decode (26 vs. 273 ms) – bezahlt mit mehr
   Konkurrenz für genau den Decode, der zählt.
2. **Der echte Hebel ist die Anzahl der Decodes.** Vier Varianten × voller Decode
   sind ~900 ms CPU. Ein einziger Decode mit abgeleiteten Varianten würde drei
   davon einsparen und dem Display-Decode die Kerne freimachen.
3. **Das Gate ist umgebungsabhängig.** Emoji-Font-Init, Scan-Schreibzugriffe und
   Emulator-Disk treffen in einem 300-ms-Fenster zusammen; auf einem Gerät mit
   echtem Flash und ohne parallele Font-Initialisierung ist es deutlich kleiner.
   Es ist jedenfalls nicht durch Coil-Konfiguration zu beheben.

## Konsequenz

Keine Verhaltensänderung am Cover-Pfad. Behalten werden nur die
DEBUG-Instrumentierung und der Hook-Fix. Der in 0024 noch empfohlene Umbau auf
einen einzigen Decode wurde nach Messung 0025 verworfen: die Decode-Kosten der
Varianten waren ein Allocator-Fehlverhalten, kein Rechenproblem.
