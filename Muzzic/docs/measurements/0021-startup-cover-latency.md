# Messung 0021 – Cover-Latenz beim Kaltstart

Zerlegung der Zeit vom Prozessstart bis zum ersten sichtbaren Album-Cover auf dem
`PlayerScreen` (Startziel der App).

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-07 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37, 1080×2424, 420 dpi, Debug-Build |
| App | `de.carsten.android.muzzic` 0.12.4 |
| Coil | 3.6.3, `DebugLogger` (Tag `RealImageLoader`), Memory-Cache leer nach `am force-stop` |
| Bibliothek | lokale MP3s mit eingebettetem Cover (kein Disk-Cache für `albumart://`), 12 Songs in der Queue |
| Start | `am start -W` nach `am force-stop`, drei Läufe |
| Basis | Marken relativ zum Prozess-Fork (`Process.getStartUptimeMillis()`) |

Die Cover liegen als eingebettete Bilder in den MP3s. `albumart://` schreibt keinen
Disk-Cache (`AlbumArtFetcher` liefert ein `SourceImageSource`, Coil setzt `diskCacheKey`
nur für `FileImageSource`), jeder `source=DISK`-Marker ist also ein echter Decode.

## Instrumentierung

Alle Marken laufen über `logging/StartupTrace.kt` (`mark` / `markOnce`, Ausgabe relativ
zum Prozessstart). Der Coil-Listener ist auf Debug-Builds beschränkt.

| Marke | Ort |
| --- | --- |
| `activity.create` | `MainActivity.onCreate` |
| `first.frame` | `MainActivity.logFirstFrame` (`ViewTreeObserver.OnDrawListener`) |
| `playerVm.init` | `PlayerViewModel.init` |
| `service.create` | `MusicPlayerService.onCreate` |
| `browser.connected` | `PlayerViewModel.setupBrowserObservation` |
| `song.available` | erster nicht-null `currentMediaItem` (Browser oder `onMediaItemTransition`) |
| `cover.<variante>.start/size/decodeEnd/success` | Coil `EventListener` in `UiModule` (Variante: `display`, `palette`, `blur`, `notification`) |
| `palette.ready` | `rememberPaletteState` nach `Palette.generate()` |
| `cover.firstFrame` | `AlbumArtControl` nach erstem Frame mit geladenem Bitmap, gesetzt in `PlayerScreenContext` |

`cover.firstFrame` ist der Beginn des 500-ms-Fade-ins (`ALBUM_ART_FADE_IN_OUT`), nicht die
volle Deckkraft. Vollständig sichtbar ist das Cover also erst
`cover.firstFrame + 500 ms`.

## Ergebnisse

Marken in ms relativ zum Prozess-Fork, Median aus drei Läufen:

| Marke | Lauf 1 | Lauf 2 | Lauf 3 | Median |
| --- | ---: | ---: | ---: | ---: |
| `activity.create` | 624 | 480 | 489 | 489 |
| `playerVm.init` (1) | 968 | 823 | 827 | 827 |
| `playerVm.init` (2) | 1086 | 939 | 954 | 954 |
| `first.frame` | 1366 | 1204 | 1222 | 1222 |
| `service.create` | 1571 | 1392 | 1383 | 1392 |
| `cover.notification.start` | 1759 | 1694 | 1566 | 1694 |
| `browser.connected` | 1899 | 1934 | 1734 | 1899 |
| `song.available` | 1899 | 1935 | 1736 | 1899 |
| `cover.blur.start` | 2019 | 2055 | 1829 | 2019 |
| `cover.display.start` | 2044 | 2067 | 1841 | 2044 |
| `cover.palette.start` | 2136 | 2208 | 1955 | 2136 |
| `cover.palette.success` | 2287 | 2250 | 2081 | 2250 |
| `palette.ready` | 2411 | 2301 | 2128 | 2301 |
| `cover.notification.success` | 2421 | 2449 | 2442 | 2442 |
| `cover.display.success` | 2485 | 2571 | 2347 | 2485 |
| `cover.firstFrame` | 2598 | 2682 | 2590 | 2598 |
| Cover voll sichtbar (`+500 ms`) | 3098 | 3182 | 3090 | 3098 |

`am start -W` `TotalTime`: 1643 / 1500 / 1479 ms (Median 1500). Der Unterschied zu den
Marken kommt vom Fork-Zeitpunkt: der Prozess startet ca. 250 ms nach dem Start-Intent.

### Phasenzerlegung (Median)

| Phase | Dauer |
| --- | ---: |
| Fork → `activity.create` | 489 ms |
| `activity.create` → `playerVm.init` | 338 ms |
| `playerVm.init` → `first.frame` | 395 ms |
| `first.frame` → `service.create` | 170 ms |
| `service.create` → `browser.connected` / `song.available` | 507 ms |
| `song.available` → `cover.display.start` | 145 ms |
| `cover.display.start` → `cover.display.success` (Decode) | 441 ms |
| `display.success` → `cover.firstFrame` | 113 ms |
| Fade-in | 500 ms |
| **Summe bis voll sichtbar** | **3098 ms** |

### Decode-Dauern (Start → Success)

| Variante | Lauf 1 | Lauf 2 | Lauf 3 | Median |
| --- | ---: | ---: | ---: | ---: |
| `display` (912×912) | 441 | 504 | 506 | 504 ms |
| `blur` (256×256) | 345 | 509 | 514 | 509 ms |
| `palette` (96×96) | 151 | 42 | 126 | 126 ms |
| `notification` (Originalgröße) | 662 | 755 | 876 | 755 ms |

## Befunde

1. **Der Request startet erst bei `song.available`** (+1899 ms). `currentSong` kommt
   ausschließlich aus dem verbundenen `MediaBrowser`; davor gibt es keinen `albumArtPath`.
   Die Kette Service-`onCreate` → ExoPlayer/Session → Queue-Query → `setMediaItems` →
   `onMediaItemTransition` kostet allein 507 ms.
2. **Der Main-Thread blockiert den ersten Frame**: `Davey! duration=748–766 ms`,
   `Skipped 42 frames`, in allen Läufen. `first.frame` liegt erst bei +1222 ms.
3. **Vier parallele Decodes desselben JPEGs**: Notification, Blur, Display und Palette
   starten innerhalb von 442 ms. Der Notification-Request startet zuerst und ist mit
   755 ms der langsamste, weil er in Originalgröße dekodiert; der Display-Decode wartet
   dadurch 504 ms statt der ~150 ms der kleinen Palette.
4. **Der Blur-Request startet vor dem Display-Request** (2019 vs. 2044 ms) und ist
   ähnlich teuer – der Hintergrund ist damit nicht aus dem Anzeige-Bitmap abgeleitet.
5. **Der 500-ms-Fade-in addiert sich voll**: `cover.firstFrame` +2598 ms, voll sichtbar
   +3098 ms. Er läuft auch bei warmem Memory-Cache.
6. **`PlayerViewModel` wird zweimal erzeugt** (zwei `playerVm.init` und zwei
   `browser.connected` pro Lauf): einmal in `MusicPlayerApp` (Activity-Scope), einmal in
   `PlayerScreen` (NavBackStackEntry-Scope). Folgen: doppeltes `scanLibrary()`
   (WorkManager dedupliziert die Arbeit, die Enqueues bleiben), zwei Listener am
   `MediaBrowser`, zwei Progress-Loops.
7. **Der Vollscan läuft im selben Prozess während des Cover-Decodes**: Start bei
   +1586 ms, Ende nach ~5,9 s. Er teilt sich CPU und SAF-IO mit den vier Bild-Requests.

## Reproduktion

```bash
./gradlew :app:assembleDebug
android install --apks=app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop de.carsten.android.muzzic
adb shell am start -n de.carsten.android.muzzic/.MainActivity   # Aufwärmlauf
adb shell am force-stop de.carsten.android.muzzic
adb logcat -c
adb shell am start -W -n de.carsten.android.muzzic/.MainActivity
sleep 10
adb logcat -d -v threadtime | grep -E "StartupTrace|Displayed|Davey|Skipped"
```

Die Marken sind reine Log-Ausgaben und bleiben in Release-Builds stumm; der Coil-Listener
wird nur bei `BuildConfig.DEBUG` registriert.

## Nachher – P1/P2 umgesetzt

Umgesetzt:

- **Doppelter `PlayerViewModel` entfernt**: `MusicPlayerApp` reicht seine activity-scoped
  Instanz über `AppNavHost` an `PlayerScreen` weiter. Pro Lauf jetzt genau ein
  `playerVm.init` und ein `browser.connected` (9/9 Läufen).
- **P1 – Cover-Request vom `MediaBrowser` entkoppelt**: `PlayerViewModel.preloadCurrentSong`
  liest die erste Song-Zeile der gespeicherten Queue direkt aus Room und setzt
  `currentSong`, bevor der Service verbunden ist.
- **P2 – Notification-Decode begrenzt**: `CoilBitmapLoader` requestet
  `NOTIFICATION_ARTWORK_PX` (512 px) mit `Precision.INEXACT`, nutzt den Content-Key des
  Covers und `CachePolicy.READ_ONLY`, kann also ein größeres Display-Bitmap
  wiederverwenden, es aber nie durch das kleinere Icon überschreiben.

Messung: neun Kaltstarts, gleiches Gerät/Setup wie oben.

| Marke | Vorher (Median) | Nachher (Median) | Δ |
| --- | ---: | ---: | ---: |
| `activity.create` | 489 | 548 | +59 |
| `playerVm.init` (1. Instanz) | 827 | 908 | +81 |
| `first.frame` | 1222 | 1262 | +40 |
| `service.create` | 1392 | 1426 | +34 |
| `browser.connected` | 1899 | 2003 | +104 |
| `song.prefetched` | – | 1543 | – |
| `cover.display.start` | 2044 | 1665 | **−379** |
| `cover.display.success` | 2485 | 2255 | −230 |
| `cover.notification.start` | 1694 | 1800 | +106 |
| `cover.notification.success` | 2442 | 2338 | −104 |
| `cover.firstFrame` | 2598 | 2453 | −145 |
| Cover voll sichtbar (`+500 ms`) | 3098 | 2953 | **−145** |
| `am start -W` `TotalTime` | 1500 | 1504 | +4 |

Decode-Dauern (Median der Läufe, `start` → `success`): `display` 521 ms (vorher 504),
`notification` 518 ms (vorher 755), `blur` 545 ms (vorher 509), `palette` 135 ms (vorher 126).

### Segmentvergleich (Median der Läufe)

Die Summe der Mediane ist irreführend, weil die frühen Phasen (Prozessstart bis
`service.create`) in den Nachher-Läufen zufällig ~100 ms langsamer waren. Aussagekräftig
ist das Segment, das die Änderung tatsächlich beeinflusst:

| Segment | Vorher | Nachher | Δ |
| --- | ---: | ---: | ---: |
| `service.create` → `cover.display.start` | 473 | 231 | **−242** |
| `display.start` → `display.success` (Decode) | 504 | 521 | +17 |
| `display.success` → `cover.firstFrame` (Draw) | 113 | 178 | +65 |
| **`service.create` → `cover.firstFrame`** | **1207** | **951** | **−256** |

P1 verschiebt den Request also um ~240 ms nach vorn, und der Gewinn kommt bis zum ersten
Frame durch. Der Draw-Abschnitt verliert 65 ms an den parallel laufenden Scan. In der
Gesamtbilanz (`fork` → `cover.firstFrame`) bleiben davon 145 ms übrig, weil der
unkontrollierte Teil davor in den Nachher-Läufen ~100 ms langsamer war.

### Befunde nachher

1. **P1 wirkt auf den Start des Requests**: `song.prefetched` liegt bei +1543 ms und damit
   ~460 ms vor dem früheren `song.available` (+1899 ms). Der Display-Request startet 379 ms
   früher, das Segment `service.create` → Request-Start schrumpft um 242 ms.
2. **Der Decode wird kaum teurer** (+17 ms), aber der Draw-Abschnitt verliert 65 ms an den
   parallel laufenden Scan. Auf dem kontrollierten Abschnitt bleibt netto −256 ms.
3. **Die Notification ist deutlich entlastet** (518 statt 755 ms), bleibt aber ein vierter
   Decode desselben JPEGs.
4. **Der SAF-Scan dominiert die Streuung**: Läufe mit laufendem Vollscan sind 300–900 ms
   langsamer. Der einzige Lauf ohne Scan (`after2-run1`) hatte `cover.firstFrame` +2065 ms
   gegenüber +2468 ms Median mit Scan. `am start -W` schwankt entsprechend zwischen 1356
   und 1799 ms.
5. **Verbleibende Kandidaten**: Scan beim Start zurückstellen (P5), 500-ms-Fade entfernen
   (P4), Blur/Palette aus demselben Decode ableiten. Die Zusammenführung von Blur und
   Palette braucht einen größeren Umbau des geteilten ImageLoaders und wurde hier
   bewusst nicht angefasst.

