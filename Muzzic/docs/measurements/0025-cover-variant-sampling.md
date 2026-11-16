# Messung 0025 – Hardware-Allocator blockiert das Sampling der Cover-Varianten

Nachtrag zu 0024. Dort kosteten die drei großen Cover-Varianten je ~290 ms Decode,
obwohl ihre Zielgrößen (256/512 px) weit unter der Quelle (1208×912) liegen. Nur die
96-px-Palette war mit 26–39 ms billig – und sie war die einzige Variante mit
`allowHardware(false)`.

## Aufbau

| Feld | Wert |
| --- | --- |
| Datum | 2026-11-10 |
| Gerät | `Pixel_10` (`emulator-5554`), API 37, 4 vCPUs, SwiftShader |
| App | `de.carsten.android.muzzic` 0.12.4, Debug-Build |
| Start | `am start -W` nach `am force-stop`, 1 Warmup verworfen |
| Läufe | `b5-gate` 4 (vorher), `b7-blursw` 4 (Blur), `b8-notifsw` 5 (Blur + Notification) |
| Trace | `coldstart-4` (vorher), `coldstart-5` (nachher) |

## Eingriff

Zwei Zeilen: `allowHardware(false)` am Blur-Request (`rememberBlurImageRequest`) und
am Notification-Request (`CoilBitmapLoader`).

Ursache: `StaticImageDecoder` setzt `ImageDecoder.setTargetSize(...)` **und** danach
`allocator = ALLOCATOR_HARDWARE`. Mit dem Hardware-Allocator dekodiert der native
Decoder die volle Quellgröße und skaliert anschließend auf der GPU; mit dem
Software-Allocator greift das Sampling. Deshalb kostete 256 px so viel wie 912 px.

## Ergebnisse

Decode-Phase je Variante (Median, ms):

| Variante | Ziel | vorher | nachher | Δ |
| --- | --- | ---: | ---: | ---: |
| Blur | 256×256 | 323 | **42** | −281 |
| Notification | 512×512 | 290 | **84** | −206 |
| Palette | 96×96 | 39 | 39 | 0 (war schon Software) |
| Display | 912×912 | 305 | 357 | im Rauschen |

Main-Thread zur Laufzeit (`thread_state` > 150 ms):

| Trace | längster Block im Cover-Fenster | `RenderProxy#copyHWBitmapInto readback` |
| --- | ---: | --- |
| `coldstart-4` | 443 ms | vorhanden |
| `coldstart-5` | 352 ms | **nicht mehr vorhanden** |

`cover.display.start` → `cover.firstFrame`: 860 → 839 (nur Blur) → 805 (Blur +
Notification). Das liegt innerhalb der Lauf-zu-Lauf-Streuung (412–1178 ms) und ist
**kein belegbarer Latenzgewinn**. `am start -W TotalTime` driftet über die
Messsession (2373 → 2523 ms) und ist für diesen Vergleich unbrauchbar.

## Bewertung

1. **Der Decode der Varianten war ein Konfigurationsfehler, kein Rechenproblem.**
   ~500 ms CPU pro kaltem Cover entfielen, ohne dass sich am Ergebnisbild etwas
   ändert (Screenshot: Player-Hintergrund und Notification-Artwork korrekt).
2. **Der 626–768 ms Readback auf dem Main-Thread verschwindet.** Er blockierte den
   Main-Thread in `S` (Warten auf die GPU), weshalb `firstFrame` unverändert bleibt –
   der Thread hatte in dem Moment nichts anderes zu tun.
3. **Damit ist der große Umbau „ein Decode, abgeleitete Varianten" nicht mehr
   gerechtfertigt.** Er würde nur noch die Palette (39 ms) und die Notification
   (84 ms) einsparen; der Display-Decode (305 ms, volle JPEG-Auflösung) bleibt
   unverändert und dominiert den Pfad.

## Konsequenz

Beide `allowHardware(false)` bleiben drin: halbierter Decode-Aufwand, kein
GPU-Readback im Kaltstart, keine gemessene Verschlechterung. Der Umbau auf einen
einzigen Decode wird verworfen.

Restpfad des Display-Covers (Median 860 ms ab `cover.display.start`): 283 ms Gate
(Disk-/I/O-Konkurrenz, kein App-Code), 305 ms voller JPEG-Decode (irreduzibel),
147 ms bis `onSuccess`, Rest Zeichnen. Kein weiterer Hebel im Cover-Pfad selbst.
