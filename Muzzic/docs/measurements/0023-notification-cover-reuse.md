# Messung 0023 – Notification-Cover aus dem Player-Cover bedienen

Prüfung der These aus Messung 0021: Die Media3-Notification fragt das Album-Cover
früher an als der `PlayerScreen` es dekodiert hat. Weil beide denselben Memory-Key
benutzen und Coil bei `Precision.INEXACT` ein größeres gecachtes Bitmap für eine
kleinere Anfrage akzeptiert, müsste ein Warten auf den Player-Cover den zweiten Decode
einsparen.

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-10 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37, SwiftShader (Software-GL) |
| App | `de.carsten.android.muzzic` 0.12.4, Debug-Build |
| Bibliothek | 1693 Songs, Queue mit 12 Songs, MP3s mit eingebettetem Cover |
| Start | `am start -W` nach `am force-stop`, 1 Warmup verworfen, 5 Läufe je Seite |
| Basis | Marken relativ zum Prozess-Fork (`Process.getStartUptimeMillis()`) |

Beide Seiten wurden **in derselben Emulator-Session** gemessen (before → after → before),
damit die Wirtslast identisch ist.

## Eingriff (nur für die Messung)

`CoilBitmapLoader.loadBitmap` wartet vor dem Request, bis der Cover-Key im Coil-Memory-Cache
liegt (Poll alle 20 ms, Timeout 3000 ms), statt selbst ein 512-px-Bitmap zu dekodieren.

```kotlin
private suspend fun awaitCachedCover(key: String, timeoutMs: Long): Boolean {
    val start = SystemClock.uptimeMillis()
    while (SystemClock.uptimeMillis() - start < timeoutMs) {
        if (imageLoader.memoryCache?.keys?.any { it.key == key } == true) return true
        delay(20)
    }
    return false
}
```

## Ergebnisse (Median aus 5 Läufen)

| Marke | vorher | nachher | Δ |
| --- | ---: | ---: | ---: |
| `activity.create` | 671 | 713 | +42 |
| `first.frame` | 1434 | 1548 | +114 |
| `song.prefetched` | 3328 | 3462 | +134 |
| `cover.display.start` | 3550 | 3676 | +126 |
| `cover.display.decodeEnd` | 4112 | 4495 | +383 |
| `cover.display.success` | 4239 | 4590 | +351 |
| `cover.notification.start` | 3691 | 4661 | +970 |
| `cover.notification.decodeEnd` | 4117 | – | entfällt |
| `cover.notification.success` | 4390 | 4661 | +271 |
| `cover.firstFrame` | **4581** | **4894** | **+313** |
| `am start -W` TotalTime | 3028 | 3313 | +285 |

Nachher-src der Treffer: `cover.notification.success … source=MEMORY_CACHE` in **5 von 5**
Läufen. Der zweite Decode der Notification entfällt also tatsächlich. Wartezeit:
348 / 779 / 810 / 813 / 846 ms.

Lauf 1 der Nachher-Seite war ein Ausreißer (`first.frame` 4171 ms, `song.prefetched`
7276 ms, Emulator-Hänger). Ohne diesen Lauf liegt `cover.firstFrame` bei 4709 ms statt
4587 ms (+122 ms) und TotalTime bei 3201 ms statt 3022 ms (+179 ms) – also weiterhin
keine Verbesserung.

## Bewertung

1. **Der Decode der Notification war nie auf dem kritischen Pfad.** Er lief parallel zum
   Display-Decode und verlangsamte diesen nicht. Ein gespartes Decode bringt dem
   Kaltstart also nichts.
2. **Die Notification bekommt ihr Cover jetzt später** (+970 ms bis `notification.start`),
   weil sie auf das Player-Cover warten muss. Das ist eine echte Verschlechterung für
   die Media-Notification, auch wenn das Cover selbst korrekt erscheint (Screenshot
   bestätigt: Artwork wird angezeigt, kein Hardware-Bitmap-Fehler).
3. **Der Player-Screen wird nicht schneller.** `cover.firstFrame` ist im Median später,
   innerhalb der Streuung aber nicht signifikant anders – gemessen ist kein Gewinn.

## Konsequenz

Der Eingriff wird **vollständig zurückgenommen** (`CoilBitmapLoader` unverändert). Der
Notification-Decode bleibt als paralleler Request bestehen; er stört den Kaltstart nicht.

Offen bleibt der eigentliche Gate aus der Vorabmessung: der Main-Thread-Draw des
Startbildschirms (1,5 s), vor dem der Cover-Request überhaupt startet, und der 500-ms-Fade
(C3).
