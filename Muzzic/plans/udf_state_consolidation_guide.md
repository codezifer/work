# Guide: Manuelle Konsolidierung des UI-States (UDF)

Dieser Guide unterstützt dich bei der manuellen Umsetzung der UDF-Architektur (Unidirectional Data Flow) in der Muzzic-App. Ziel ist es, die vielen einzelnen `StateFlows` in den ViewModels durch eine einzige `uiState`-Datenklasse zu ersetzen.

---

## 1. Schritt: PlayingQueue UDF Refactoring

### A. UI State definieren
Erstelle die Datei `app/src/main/java/de/carsten/android/muzzic/ui/state/PlayingQueueUiState.kt`:

```kotlin
package de.carsten.android.muzzic.ui.state

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.ui.PLAYING_QUEUE
import de.carsten.android.muzzic.ui.model.PlayingQueueDto

data class PlayingQueueUiState(
    val queue: List<PlayingQueueDto> = emptyList(),
    val currentSong: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val name: String = PLAYING_QUEUE
)
```

### B. ViewModel anpassen (`PlayingQueueViewModel.kt`)
Ersetze die einzelnen Flows durch einen zentralen `MutableStateFlow`:

1.  **Entferne**: `currentPlayingQueue`, `_currentSong`, `_isPlaying`, `_currentPosition`, `_duration`, `_currentName` und deren öffentliche Pendants.
2.  **Füge hinzu**:
    ```kotlin
    private val _uiState = MutableStateFlow(PlayingQueueUiState())
    val uiState: StateFlow<PlayingQueueUiState> = _uiState.asStateFlow()
    ```
3.  **DB-Flow einbinden**: Nutze `onEach` im `init`-Block, um den `repository.observePlayingQueue()` in den `_uiState` zu mergen:
    ```kotlin
    init {
        repository.observePlayingQueue()
            .onEach { queue -> _uiState.update { it.copy(queue = queue) } }
            .launchIn(viewModelScope)
        // ... restliche Initialisierung
    }
    ```
4.  **Listener & Updater**: Aktualisiere die Werte über `_uiState.update { it.copy(...) }`.

### C. Screen anpassen (`PlayingQueueScreen.kt`)
1.  **Entferne** die vielen einzelnen `by collectAsStateWithLifecycle()` Aufrufe.
2.  **Füge hinzu**:
    ```kotlin
    val uiState by playingQueueViewModel.uiState.collectAsStateWithLifecycle()
    ```
3.  Übergib die Werte aus `uiState` an `PlayingQueueContent`.

---

## 2. Schritt: Library UDF Refactoring

### A. LibraryUiState vervollständigen
Stelle sicher, dass [`LibraryUiState.kt`](file:///home/carsten/Besenkammer/Gits/Codezifer/Work/Muzzic/app/src/main/java/de/carsten/android/muzzic/ui/state/LibraryUiState.kt) auch `playlists` enthält:

```kotlin
data class LibraryUiState(
    val artists: List<ArtistDto> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
    val songs: List<SongDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList() // Hinzufügen!
)
```

### B. Mapping-Funktion erstellen
Erstelle eine Extension-Funktion für das Mapping von Room-Entitäten zu DTOs (z.B. in `SongDto.kt` oder `Extensions.kt`):

```kotlin
fun Song.toDto(): SongDto = SongDto(
    title = title,
    trackNumber = trackNumber,
    // ... alle Felder mappen
)
```

### C. ViewModel anpassen (`LibraryViewModel.kt`)
Implementiere die `combine`-Logik wie im Plan beschrieben. Entferne alle alten `StateFlows` (`songs`, `artists`, etc.).

### D. Screen anpassen (`LibraryScreen.kt`)
Stelle die `LibraryScreen`-Funktion so um, dass sie nur noch den `uiState` sammelt und die Unter-Komponenten daraus bedient.

---

## 3. Schritt: Verifizierung
1.  Führe `./gradlew ktlintFormat` aus.
2.  Starte die App und prüfe, ob die Wiedergabe und die Navigation in der Bibliothek flüssig funktionieren.
3.  Checke Logcat auf eventuelle Fehler bei der Flow-Initialisierung.
