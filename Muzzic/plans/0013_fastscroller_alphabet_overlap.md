# FastScroller: Alphabet-Buchstaben zu klein bzw. überlappend

## Ziel
Bei vollem Alphabet (A-Z, 26 Buchstaben) sollen sich die Buchstaben im FastScroller nicht mehr überlappen bzw. zu klein dargestellt werden, ohne dass die Bar auf die volle Bildschirmhöhe erweitert werden muss.

## Kontext / Root Cause
- `step`-Berechnung in [FastScroller.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/controls/FastScroller.kt) nutzt `floor` (`(alphabet.size.toFloat() / maxVisibleItems).toInt()`).
- Bei 26 Buchstaben und typischer Bar-Höhe (z. B. 400dp) ergibt das `maxVisibleItems = 400/16 = 25` und `step = floor(26/25) = 1` → alle 26 Buchstaben werden angezeigt, je Slot nur ~15dp (Schrift ist 12sp) → Überlappung.
- Die vorhandene Dots-Kompression (`isDotPosition`, Zeile ~233) greift erst bei `step >= 2`, also erst bei < ~200dp Bar-Höhe → kommt bei vollem Alphabet praktisch nie zum Einsatz.
- Option "volle Bildschirmhöhe" wäre ein Refactor über 5 Screens + 3 Grid-/List-Komponenten und überlagert z. B. in LibraryScreen den Settings-Button – verworfen.

## Proposed Changes

### [MODIFY] [Constants.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/Constants.kt)
- Neue Konstante `FAST_SCROLL_ITEM_MIN_HEIGHT = 22.dp` (im Bereich "Component Specific Dimensions").

### [MODIFY] [FastScroller.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/controls/FastScroller.kt)
- `import kotlin.math.ceil` hinzufügen.
- `step`-Berechnung (Zeilen ~107-117) ersetzen:
  - `16.dp` → `FAST_SCROLL_ITEM_MIN_HEIGHT`
  - `(alphabet.size.toFloat() / maxVisibleItems).toInt()` → `ceil(alphabet.size.toFloat() / maxVisibleItems).toInt()`
  - Kommentar anpassen.

## Erwartetes Verhalten (26 Buchstaben)
- Bar ≥ ~572dp (22dp × 26): alle 26 Buchstaben sichtbar, je ≥ 22dp → lesbar, kein Überlappen.
- Bar < ~572dp: automatische Kompression auf jeden 2. Buchstaben + `•` dazwischen (Rendering existiert bereits), sichtbare Buchstaben mit ~30dp+.
- Tap-/Drag-Mapping bleibt korrekt, da es auf `alphabet[index]` mit voller Länge basiert, unabhängig vom Anzeige-`step`.

## Verification Plan

### Automated Tests
- `./gradlew ktlintFormat`
- `./gradlew testDebugUnitTest`

### Manual Verification
- App starten, zur Bibliothek (Artists/Albums/Songs) navigieren.
- Prüfen, dass bei vollem Alphabet (26 Buchstaben) keine Buchstaben mehr überlappen.
- Prüfen, dass Tippen/Ziehen auf Punkte-Positionen weiterhin den korrekten Buchstaben ansteuert.
