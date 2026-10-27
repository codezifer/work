# FastScroller: Vorschau-Bubble folgt dem Handle vertikal

## Ziel
Die große Buchstaben-Bubble (Thumb) hing bisher fest in der vertikalen Mitte der Bar. Sie soll beim Scrollen/Draggen zusammen mit dem Handle hoch- und runterwandern.

## Kontext / Root Cause
- Bubble: `.align(Alignment.CenterStart)` + nur horizontales `offset(x = -INDICATOR_SLIDER_PREVIEW_PADDING)` → y blieb immer die Mitte der Bar.
- Handle: eigener `animateDpAsState`-Offset, berechnet aus `letterIndex / (n-1)` → bewegte sich bereits korrekt.
- Zwei getrennte Positionsberechnungen → Bubble und Handle konnten nicht synchron laufen.

## Proposed Changes

### [MODIFY] [FastScroller.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/controls/FastScroller.kt)
- Eine gemeinsame Quelle: `centerOffsetDp` (Mitte des aktuellen Buchstaben) + `animateDpAsState(label = "fastScrollerCenter")` → `animatedCenter`.
- Handle: `offset(y = animatedCenter - ICON_SIZE_MEDIUM / 2)`.
- Bubble: `Alignment.TopStart`, `offset(x = …, y = animatedCenter - ICON_SIZE_FAST_SCROLL_THUMB / 2)`.
- Clamping der Bubble auf `0 .. columnHeight - ICON_SIZE_FAST_SCROLL_THUMB`, damit sie an erstem/letztem Buchstaben nicht aus der Bar läuft.
- Thumb größer (50 → 64dp) und als Sprechblase: `IndicatorShape(tailSize = 10dp, cornerRadius = 32dp, direction = Right)` — die Spitze zeigt auf die Buchstaben-Zeile, das Letter-Textfeld sitzt zentriert im runden Körper.
- `wrapContentSize(Alignment.TopStart, unbounded = true)`: die 40dp breite Bar würde die breitere Bubble sonst auf 40dp kürzen bzw. zentrieren (Kind breiter als Parent).
- Kommentare/KDocs der Datei auf 1–2 Zeilen gekürzt (neue Kommentar-Regel in `AGENTS.md`).

### [MODIFY] [Constants.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/Constants.kt)
- `ICON_SIZE_FAST_SCROLL_THUMB` 50 → 64dp, neu `FAST_SCROLL_THUMB_TAIL_SIZE = 10dp`, `FAST_SCROLL_THUMB_TAIL_OVERLAP = 3dp`.

### [MODIFY] [IndicatorShape.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/shape/IndicatorShape.kt)
- Neuer Parameter `tailOverlap` (Default `0.dp` → IndicatorSlider unverändert).
- Körper und Spitze werden als zwei Subpaths gebaut und per `Path.op(..., PathOperation.Union)` zu **einer** Kontur verschmolzen.
- Root Cause der Naht: zwei überlappende Subpaths mit gegenläufiger Windung → NonZero-Fill löscht die Überlappung aus (Loch in exakter Overlap-Breite, auf dem Gerät als dunkler Balken sichtbar). Union statt überlappender Subpaths behebt das unabhängig von der Windung.

### [MODIFY] [AGENTS.md](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/AGENTS.md)
- Neuer Abschnitt `## ✍️ Comments` (kurz, knapp, „why not what", Trim on touch).
- Pre-Commit-Checkliste: KDoc- und Kommentar-Punkt entsprechend geschärft.

## Erwartetes Verhalten
- Beim Drag liegt die Bubble-Mitte auf der Handle-Mitte; beide laufen animiert mit.
- Die Bubble-Spitze zeigt auf die aktuelle Buchstaben-Zeile, ohne die Bar zu überdecken.
- Am oberen/unteren Ende bleibt die Bubble vollständig innerhalb der Bar.
- Ohne Drag (nur Listen-Scroll) erscheint weiterhin nur der Handle.

## Verification Plan

### Automated
- `./gradlew ktlintFormat` ✅ (2026-10-27)
- `./gradlew :app:testDebugUnitTest` ✅ (2026-10-27)

### On-Device (Emulator Pixel_10, 1080×2424, `android` CLI, 2026-10-27) ✅
- Drag Mitte: Bubble „D" / „K" jeweils deckungsgleich mit dem Handle.
- Drag nach oben: Bubble „6" oben in der Bar, kein Überlauf.
- Drag nach unten: Bubble „Z" unten in der Bar, Clamping greift.
- Thumb 64dp mit Sprechblasen-Spitze: Spitze endet ~2dp links der Bar auf Höhe der Buchstaben-Zeile („F", „8" geprüft), Buchstabe zentriert im runden Körper.
- Naht geprüft (Pixel-Analyse des Screenshots): Körper und Spitze gehen ohne dunkle Linie ineinander über, Spitze endet bei x≈935, Bar beginnt bei x≈943.
