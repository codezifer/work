# Star-Rating des aktuellen Songs im Player

## Ziel
Im Player soll der aktuell abgespielte Song über einen Stern-Button bewertet werden können. Der Klick öffnet ein Popup mit einer Sterne-Reihe (0 = miserabel, 5 = sehr gut) in 0,5-Schritten. Im Popup werden die Sterne per horizontalem Ziehen auf- und abgefüllt; das erneute Setzen desselben Werts löscht die Bewertung (= 0). Die Bewertung wird sofort in Room persistiert und sowohl im Player als auch in der Bibliothek/Statistik korrekt dargestellt.

## Kontext / Ist-Zustand
- Persistenz existiert bereits: `Song.rating` (WMP 0-255, `Int`, Default 0), `SongDao.updateRating(songId, rating)`, `MusicRepository.updateSongRating(songId, rating)` – bisher aber von keiner UI genutzt.
- [StarRating.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/cards/StarRating.kt) ist ein reines Anzeige-Widget (nur ganze Sterne, `Int`), dessen `onRatingChanged`-Callbacks in `SongListItem`/`SongStatsCard` No-ops sind.
- Einheiten-Bug: das Widget interpretiert `rating` als Sternzahl (0-5), die Aufrufer übergeben aber `song.rating` als WMP-Wert (0-255) → faktisch immer 5 Sterne bei bewerteten Songs.
- Solider Nenner: [Helper.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/utils/Helper.kt) hat bereits `getStarRating` (WMP→Media3 `StarRating`) und `getWmpRating` (→ WMP), allerdings mit überlappenden Grenzbereichen.

## Proposed Changes

### [MODIFY] [Helper.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/utils/Helper.kt)
- WMP-Ankerwerte `0, 25, 51, 75, 102, 128, 153, 178, 204, 225, 255` für die 0,5-Stern-Schritte als private Liste.
- `getStars(rating: Int?): Float` (WMP → 0f..5f, auf 0,5 gerundet) ergänzen.
- `getStarRating(rating: Int?)` nutzt `getStars`; `getWmpRating(starRating: StarRating)` und neue Überladung `getWmpRating(stars: Float)` mappen verlustfrei auf die Anker.

### [MODIFY] [Constants.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/Constants.kt)
- `STAR_RATING_ICON_SPACING = 2.dp` als benannter Token (keine Magic Number).

### [MOVE + MODIFY] `ui/screens/cards/StarRating.kt` → [ui/component/StarRating.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/component/StarRating.kt)
- Signatur auf `rating: Float` (0f..5f) umstellen.
- Halbsterne über `Icons.Filled.StarHalf` (Star gefüllt ≥ 1, StarHalf ≥ 0,5, sonst StarBorder).
- Optionaler `editable`-Modus: horizontales Ziehen/Tippen setzt den Wert über die x-Position (`round(fraction*10)/2`); Tippen exakt auf den aktuellen Wert ruft `onRatingCleared`.
- Light/Dark-Previews.

### [NEW] [ui/component/RatingDialog.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/component/RatingDialog.kt)
- `AlertDialog` mit editierbarem `StarRating` und numerischer Anzeige (`%1$.1f / 5`); `onRatingChanged` persistiert sofort, OK/außerhalb schließt.
- Light/Dark-Previews.

### [MODIFY] [SongListItem.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/cards/SongListItem.kt) / [SongStatsCard.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/cards/SongStatsCard.kt)
- Import auf `ui.component.StarRating` umstellen und `rating = getStars(song.rating)` übergeben (behebt den Einheiten-Bug).

### [MODIFY] [PlayerViewModel.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/viewmodel/PlayerViewModel.kt)
- `onRatingChanged(stars: Float)`: WMP-Wert via `getWmpRating`, optimistisches `_uiState.currentSong.copy(rating = ...)`, dann `repository.updateSongRating(id, wmp)` in `viewModelScope`.

### [MODIFY] [PlayerScreen.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/PlayerScreen.kt) / [PlayerScreenContext.kt](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/screens/PlayerScreenContext.kt)
- `ratingStars: Float` + `onRatingChanged: (Float) -> Unit` durchreichen.
- Stern-Button im Song-Info-Block; lokaler `remember`-State steuert das Popup (`RatingDialog`).

### [MODIFY] [strings.xml](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/res/values/strings.xml)
- `rate_song`, `rating_dialog_title`, `rating_value_format`, `rating_cleared` (Accessibility).

## Tests
- [NEW] `ui/utils/RatingConversionTest.kt`: Round-Trip für alle 0,5-Schritte (0/…/5 ↔ WMP) sowie Nachbarwerte.
- [MODIFY] `PlayerViewModelTest.kt`: `onRatingChanged` persistiert den korrekten WMP-Wert und aktualisiert `uiState.currentSong`.

## Verification Plan
### Automated
- `./gradlew ktlintFormat`
- `./gradlew testDebugUnitTest`
### Manual
- Song abspielen, Stern-Button tippen, im Popup ziehen → Sterne füllen sich in 0,5-Schritten, Wert wird sofort gespeichert.
- Denselben Wert erneut antippen → Bewertung wird 0.
- Bibliotheksliste/Statistik zeigen die Bewertung korrekt (Halbsterne).
