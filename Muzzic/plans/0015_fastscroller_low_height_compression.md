# FastScroller: Buchstaben bei geringer Bar-Höhe teilweise verdeckt

## Ziel
Bei geringer Screen-/Bar-Höhe sind die Buchstaben des FastScrollers teilweise verdeckt/überlappt. Jeder Buchstabe soll bei jeder Höhe vollständig sichtbar sein — erst durch weniger Slots (Buchstaben + Dots), im Notfall durch verkleinerten Font. Follow-up zu Issue 0013.

## Kontext / Root Cause
- Die `step`-Kompression aus 0013 dekoriert nur Slots (Buchstabe/`•`/`""`), **reduziert aber die Slot-Anzahl nicht**: `alphabet.forEachIndexed` rendert weiterhin alle 26 Slots mit `weight(1f)` → Slot = `usableHeight / alphabet.size`, unabhängig von `step`.
- Zusätzlich erzwang `isFirst || isLast || isStepMatch` das letzte Buchstaben-Zeichen außerhalb des Step-Rhythmus → garantierte Nachbar-Überlappung am unteren Ende (z. B. `step = 2`, gerades Alphabet: idx 24 + 25).
- Zweite Ursache: `Text` nutzte den Default-`lineHeight` des Styles (~20sp) bei `fontSize = 12sp` → Zeilenbox größer als der Slot → Glyphen überlaufen in Nachbarslots.

## Proposed Changes

### [MODIFY] [FastScroller.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/controls/FastScroller.kt)
- Neues privates Display-Modell `FastScrollerDisplayItem` (sealed interface, `Letter(value)` / `Dot`, gleiche Datei gemäß File-Granularity-Regel).
- Neuer Builder `buildFastScrollerDisplayItems(alphabet, maxVisibleItems)`:
  - Passt Maximum: alle Buchstaben, je Slot ≥ `FAST_SCROLL_ITEM_MIN_HEIGHT` (22dp).
  - Komprimiert: gleichmäßig verteilte Buchstaben (erstes und letztes immer dabei, Abstand via `round(slot * (n-1) / (L-1))`) + `•` zwischen jeder Buchstaben-Paarung; Slot-Anzahl (2L−1) bleibt ≤ `maxVisibleItems` mit `L = (maxVisibleItems + 1) / 2`.
- Die Column rendert nur noch `displayItems` (keine leeren Slots) → jeder Slot tatsächlich ≥ 22dp.
- Pointer-Mapping (`letterIndexAt`) bleibt proportional zum vollen Alphabet (`fraction * (n-1)`), jetzt übrigens korrekt auf den nutzbaren Bereich (innerhalb des vertikalen Paddings) appliziert → alle Zwischen-Buchstaben bleiben wählbar, auch über Dot-Slots.
- Handle sitzt proportional (`letterIndex / (n-1)`) auf die Zeiger-Mapping-Position statt auf Slot-Zentren.
- `FastScrollerLetter`: neuer Parameter `fontSize: TextUnit`; `lineHeight = fontSize`, damit die Zeilenbox nie größer als der Slot ist.
- Dynamischer Font als Fallback: `fontSize = min(FONT_SIZE_CAPTION, slotHeight.toSp())` — `dp.toSp()` kompensiert auch `fontScale`, unterhalb des Caps wird der Font exakt auf die Slot-Höhe angepasst; so bleibt er voll lesbar.
- Neue Previews: `FastScrollerShortPreview` (A–Z bei 300dp, Light + Dark).

## Erwartetes Verhalten (26 Buchstaben)
- Bar ≥ ~572dp: wie bisher, alle 26 Buchstaben bei 12sp.
- Bar < ~572dp: gleichmäßig verteilte Buchstaben inkl. A/Z mit `•` dazwischen, jeder Slot ≥ 22dp; Font bleibt 12sp.
- Extrem kurze Bar: Font verkleinert sich exakt auf die Slot-Höhe — nie abgeschnitten/überlappt.
- Tap/Drag: durchgehend feingranulares Mapping auf alle Alphabet-Buchstaben (auch die nicht gerenderten).

## Verification Plan

### Automated
- `./gradlew ktlintFormat :app:testDebugUnitTest` ✅ (2026-10-18)

### Manual
- Compose-Preview `FastScrollerShortPreview` (300dp, A–Z): Buchstaben + Dots vollständig sichtbar, kein Überlappen.
- Auf Gerät/Emulator: kurze Bar scrubben — Handle folgt Finger, Zwischen-Buchstaben im Bubble sichtbar.
- Optional: Verifikation auf kleinem Emulator via `android` CLI (`android emulator list/start`).
