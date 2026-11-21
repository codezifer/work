# Plan 0018: IndicatorScreen als Alternative zum FastScroller (Artist, Album, Genre, Song)

## Ziel
Konfigurierbare Schnell-Navigation: In den Einstellungen (`SettingsScreen`) wählt der Nutzer, ob die Übersichten
**Artist**, **Album**, **Genre** und **Song** die klassische seitliche `FastScroller`-Leiste oder den neuen
`IndicatorScreen` (3-Spalten-Grid der vorhandenen Anfangsbuchstaben) verwenden.

Der Modus wird **explizit** durchgereicht (kein `CompositionLocal`); Quelle ist das `SettingsViewModel`.
Damit bleibt das Muster identisch zu `colorSource`.

---

## Entscheidungen

| Punkt | Entscheidung |
| --- | --- |
| Modus-Verteilung | Explizit: `MusicPlayerAppUI` → `AppNavHost` → Screens → Grids → `FastScrollBox` |
| Quelle des Werts | **Dedizierte** `SettingsViewModel.fastScrollMode: StateFlow` (nicht das `uiState`-combine) |
| Settings-Widget | Dropdown (`ExposedDropdownMenuBox`), gleiches Muster wie `VisualizerEngine` |
| Songs | Ja, `SongList` nutzt die Einstellung mit |
| Alphabet-Reihenfolge | Begegnungsreihenfolge der bereits sortierten Liste (kein `.sorted()`) |
| Buchstabe | `getLabel` ist die einzige Quelle; kein zweites `.take(...)` in `FastScrollBox` |
| `getFastScrollMode()` | entfällt – wird nirgends gebraucht |
| Repository-Test | entfällt; nur `SettingsViewModelTest` erweitern |
| `IndicatorScreen`-Ablage | `ui/screens/` (einheitlich mit allen `*Screen`-Composables; kein Navigationsziel, nur Baustein von `FastScrollBox`) |
| Name `FastScrollBox` | bleibt (Umbenennen wäre reine Kosmetik und würde 4 Call-Sites + Historie berühren) |
| Coil-Prefetch | **kein** Prefetch der gefilterten Liste (bewusst entfernt, siehe Messung 0020) |

---

## Architektur

```
MusicPlayerAppUI
│  collect: settingsViewModel.fastScrollMode      (distinkte StateFlow)
│
├── SettingsViewModel ──── observeFastScrollMode() ──── AppSettingsRepository
│
└── AppNavHost(fastScrollMode)
    │
    ├── LibraryScreen ────────┬── ArtistGrid ─────┐
    │                         ├── AlbumGrid ──────┤
    │                         ├── GenreGrid ──────┤
    │                         └── SongList ───────┤
    │                                             │
    ├── ArtistAlbumsScreen ─── AlbumGrid ─────────┤
    ├── GenresScreen ───────── GenreGrid ─────────┤
    └── GenreArtistsScreen ─── ArtistGrid ────────┤
                                                  │
                                                  ▼
                                                  FastScrollBox(fastScrollMode)
                                                  ├── FAST_SCROLLER    → content(items) + FastScroller
                                                  ├── INDICATOR_SCREEN → IndicatorScreen(alphabet, counts)
                                                  └── Buchstabe        → Kopfzeile + content(gefiltert)
```

`SettingsViewModel` ist app-weit instanziiert. Wichtig: **nicht** das komplette `SettingsUiState` in der Shell
sammeln, sondern nur den dedizierten, distinkten Mode-Flow (siehe Abschnitt „Performance").

---

## Zustandsmaschine `FastScrollBox`

Einstieg je nach Modus und Datenlage:

```
  FAST_SCROLLER      → Bar-Modus
  INDICATOR_SCREEN   → Buchstaben-Grid   (alphabet nicht leer)
  alphabet leer      → Inhalt ohne Navigation
```

Übergänge:

```
                    ┌───────────────────────┐
                    │      Bar-Modus        │
                    │  content(items)       │
                    │  + FastScroller       │
                    └───────────────────────┘

                    ┌───────────────────────┐
                    │   Buchstaben-Grid     │
                    │   IndicatorScreen     │
                    │   (alphabet, counts)  │
                    └───────────┬───────────┘
                                │ Buchstabe getippt
                                │ selectedLetter = X
                                │ scrollToItem(0)
                                ▼
                    ┌───────────────────────┐
                    │   Gefiltertes Grid    │
                    │   Kopfzeile (Chip X)  │
                    │   content(items mit   │
                    │   Initiale X)         │
                    └───────────┬───────────┘
                                │ Chip X / System-Back
                                ▼
                      zurück zum Buchstaben-Grid

  Buchstaben-Grid ── Liste geleert ──────────────► Inhalt ohne Navigation
  Gefiltertes Grid ─ letzter Buchstabe entfernt ─► Inhalt ohne Navigation
```

---

## Interaktion (Sequenz)

```
Nutzer ──"M"──► IndicatorScreen ──onLetterClick("M")──► FastScrollBox
                                                            │
                                                            ├─ selectedLetter = "M"
                                                            ├─ scrollToItem(0)      (scope.launch;
                                                            │                        funktioniert auch ohne
                                                            │                        komponiertes Grid)
                                                            └─ content(visibleItems = gefiltert)
                                                            │
Nutzer ◄────────────────────────────────────────────────────┘
        nur "M"-Einträge, Liste startet oben

Nutzer ──System-Back──► FastScrollBox ──BackHandler(enabled = effectiveLetter != null)
                                             │
                                             └─ selectedLetter = null
Nutzer ◄─────────────────────────────────────┘
        IndicatorScreen (Buchstaben-Grid)
```

---

## 1. Data & Settings

### `ui/model/FastScrollMode.kt` (NEW)
```kotlin
enum class FastScrollMode {
    FAST_SCROLLER,
    INDICATOR_SCREEN,
}
```
Ablage in `ui.model` ist bewusst: der Präzedenzfall `visualization.component` zeigt, dass
`AppSettingsRepository` bereits Feature-Enums importiert.

### `persistence/repo/AppSettingsRepository.kt` (MODIFY)
- `const val KEY_FAST_SCROLL_MODE = "fast_scroll_mode"` im Companion.
- `observeFastScrollMode(): Flow<FastScrollMode>` – Default `FAST_SCROLLER`, unbekannte Werte über
  `runCatching { valueOf(...) }.getOrNull()` abfangen (Muster `observeVisualizerEngine`, Zeile 63-67).
- `saveFastScrollMode(mode: FastScrollMode)` – `insertOrUpdate(GenericSetting(KEY, mode.name))`.
- **Kein** `getFastScrollMode()` (kein Aufrufer, würde totes API erzeugen).

### `viewmodel/SettingsViewModel.kt` (MODIFY)
**Keine** Erweiterung des bestehenden `combine` (Begründung: Performance, siehe R1). Stattdessen eine
dedizierte, distinkte StateFlow:

```kotlin
val fastScrollMode: StateFlow<FastScrollMode> =
    appSettingsRepository.observeFastScrollMode()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
            FastScrollMode.FAST_SCROLLER,
        )

fun updateFastScrollMode(mode: FastScrollMode) {
    viewModelScope.launch { appSettingsRepository.saveFastScrollMode(mode) }
}
```

`SettingsUiState` bleibt **unverändert**. Der SettingsScreen sammelt diesen Flow zusätzlich zu `uiState`.

### `ui/screens/SettingsScreen.kt` (MODIFY)
- `val fastScrollMode by settingsViewModel.fastScrollMode.collectAsStateWithLifecycle()`
- `SettingsScreenContent` bekommt `fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER` und
  `onFastScrollModeSelected: (FastScrollMode) -> Unit = {}`.
- Neue private Composable `FastScrollModeSetting` im Stil von `VisualizerSetting` (Dropdown mit
  `FastScrollMode.entries`, Anzeige über `fastScrollModeLabel(...)`).
- Platzierung: in `SettingsSection(stringResource(R.string.library_config))`, **nach** den beiden Verzeichnissen
  (die Einstellung betrifft die Bibliotheksansicht, nicht die Visualisierung).
- Preview bleibt unverändert (Default-Parameter).

---

## 2. UI Component Layer

### `ui/screens/IndicatorScreen.kt` (NEW)
```kotlin
@Composable
fun IndicatorScreen(
    letters: List<String>,
    itemCounts: Map<String, Int> = emptyMap(),
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
)
```
- `LazyVerticalGrid(columns = GridCells.Fixed(AppConfig.Ui.NUM_OF_INDICATOR_COLUMNS))`
- Padding/Spacing wie die Item-Grids: `GRID_HORIZONTAL_PADDING` / `GRID_SPACING`.
- Karte: `Surface`/`Card` mit `MaterialTheme.shapes.medium`, quadratisch (`aspectRatio(1f)`), Akzentrahmen
  `colorSource.accentColor`, Buchstabe zentriert in `FONT_SIZE_HUGE_TITLE`/`FONT_SIZE_LARGE_TITLE`.
- Textfarbe über `colorSource.accentColor.contrastColor()` (wie `FastScroller.kt:327`), nie fest verdrahtet.
- Optionaler Zähler unter dem Buchstaben: `pluralStringResource(R.plurals.indicator_item_count, count, count)`.
- Accessibility: `Modifier.semantics { contentDescription = "<Buchstabe>, <Anzahl> Einträge" }` auf der Karte;
  Touch-Ziel ≥ 48dp.
- Previews: Light + Dark (`@Preview(showBackground = true)` und `PREVIEW_DARK_MODE`), mit/ohne Zähler.

### `ui/component/fastscroll/FastScrollBox.kt` (MODIFY)
Neue Signatur:
```kotlin
@Composable
fun <T> FastScrollBox(
    items: List<T>,
    getLabel: (T) -> String,
    firstVisibleItemIndex: () -> Int,
    isScrollInProgress: () -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
    colorSource: ColorSource = composableColorSource(),
    content: @Composable (visibleItems: List<T>) -> Unit,
)
```

Zustandsableitung (eine Wahrheit, keine zweite Buchstaben-Definition, alles remembered):
```kotlin
val labels        = remember(items) { items.map { getLabel(it).uppercase(Locale.ROOT) } }
val letterToIndex = remember(labels) { buildMap { labels.forEachIndexed { i, l -> putIfAbsent(l, i) } } }
val alphabet      = remember(letterToIndex) { letterToIndex.keys.toList() }   // Begegnungsreihenfolge

// Nur im Indicator-Modus zählen (Default-Pfad bleibt unbelastet)
val itemCounts = remember(labels, fastScrollMode) {
    if (fastScrollMode == FastScrollMode.INDICATOR_SCREEN) labels.groupingBy { it }.eachCount() else emptyMap()
}

var selectedLetter by rememberSaveable { mutableStateOf<String?>(null) }
val effectiveLetter = selectedLetter?.takeIf { fastScrollMode == FastScrollMode.INDICATOR_SCREEN && it in itemCounts }

// O(n) genau einmal pro Buchstabenwahl, nicht pro Recomposition
val visibleItems = remember(items, effectiveLetter) {
    if (effectiveLetter == null) items else items.filterIndexed { i, _ -> labels[i] == effectiveLetter }
}
```

Rendering:
```kotlin
Column(modifier.fillMaxSize()) {
    if (effectiveLetter != null) {
        // Kopfzeile: FilterChip(selected = true, label = Buchstabe, trailingIcon = Close,
        // contentDescription = R.string.back); Klick setzt selectedLetter = null
    }
    Box(Modifier.weight(1f)) {
        when {
            fastScrollMode == INDICATOR_SCREEN && alphabet.isNotEmpty() && effectiveLetter == null ->
                IndicatorScreen(alphabet, itemCounts, onLetterClick = { letter ->
                    selectedLetter = letter
                    scope.launch { scrollToItem(0) }   // gefilterte Liste startet oben
                })
            else -> content(visibleItems)
        }
        if (fastScrollMode == FAST_SCROLLER && alphabet.isNotEmpty()) FastScroller(...)  // wie bisher
    }
}
```

- `BackHandler(enabled = effectiveLetter != null) { selectedLetter = null }` – `enabled` hängt am
  effektiven Buchstaben, damit der Handler nie das Verlassen eines Screens schluckt.
- `LaunchedEffect(alphabet) { if (selectedLetter !in alphabet) selectedLetter = null }` – Datenänderung
  (Scan, Filter) darf keinen leeren Screen hinterlassen.
- Leeres Alphabet → `content(items)` (kein leerer Kachel-Screen).
- **Layout-Falle:** `content` muss in `Box(Modifier.weight(1f))` liegen, weil die Item-Grids selbst
  `fillMaxSize()` mitbringen und sonst die Kopfzeile aus dem Bild schieben.

### `AppConfig.kt` (MODIFY)
`const val NUM_OF_INDICATOR_COLUMNS = 3` in `object Ui` (keine Magic Number im Grid).

---

## 3. Integration (explizite Kette)

1. `MusicPlayerAppUI`: `settingsViewModel: SettingsViewModel = koinViewModel()`,
   `val fastScrollMode by settingsViewModel.fastScrollMode.collectAsStateWithLifecycle()`,
   `AppNavHost(fastScrollMode = fastScrollMode, …)`.
   **Nicht** `settingsViewModel.uiState` sammeln (siehe R1/R2).
2. `AppNavHost`: neuer Parameter `fastScrollMode: FastScrollMode` (ohne Default, wie `colorSource`);
   weiterreichen an `LibraryScreen`, `ArtistAlbumsScreen`, `GenresScreen`, `GenreArtistsScreen`.
3. `LibraryScreen` + `LibraryScreenContent`: Parameter mit Default, weiter an `ArtistGrid`, `AlbumGrid`,
   `GenreGrid`, `SongList`.
4. `ArtistAlbumsScreen`: Parameter mit Default, weiter an `AlbumGrid`.
5. `GenresScreen` + `GenresScreenContent`: Parameter mit Default (hat heute gar kein `colorSource`),
   weiter an `GenreGrid`.
6. `GenreArtistsScreen`: Parameter mit Default, weiter an `ArtistGrid`.
7. `ArtistGrid`, `AlbumGrid`, `GenreGrid`, `SongList`: Parameter mit Default
   `FastScrollMode.FAST_SCROLLER`; `content`-Lambda auf `{ visible -> … }` umstellen und `visible`
   als `items`-Quelle verwenden. In `SongList` explizit `{ _ -> … }`, damit klar ist, dass dort
   (noch) nicht gefiltert wird.
8. `PlaylistGrid` bleibt unverändert (nutzt `FastScrollBox` nicht).

Alle Defaults halten Previews und bestehende Aufrufer unverändert.

---

## 4. Performance (verbindliche Vorgaben)

### R1 – Kein app-weites `SettingsUiState` (kritisch)
`SettingsViewModel.uiState` berechnet `availablePresets` **im** `combine`-Transform:
`PresetManager.getAvailablePresets` (`PresetManager.kt:109`) ruft `ensurePresetsExtracted` (`:47`) →
`assetManager.list()`, `File.exists()` pro Preset, zwei `listFiles()`, ggf. Kopieren aus den Assets.
Der Transform läuft auf `Main.immediate` (`stateIn` mit `viewModelScope`).

Würde `MusicPlayerAppUI` das `uiState` sammeln, liefe diese Datei-I/O:
- beim App-Start (erster Frame, inkl. potenziellem Preset-Copy),
- dauerhaft bei **jeder** Settings-Änderung (z. B. Hue-Slider pro Tick).

**Vorgabe:** Die Shell sammelt ausschließlich `fastScrollMode` (dedizierte, distinkte `StateFlow`).
`uiState` bleibt screen-scoped wie heute.

### R2 – Shell-Recomposition nur bei echtem Mode-Wechsel
Die dedizierte `StateFlow` emittiert per `StateFlow`-Semantik nur bei Wertänderung; damit recomposed
`MusicPlayerAppUI` (Scaffold, SnackbarHost, `PlayerBackground`, `BottomNavigationBar`, `AppNavHost`)
nur beim Umschalten in den Einstellungen – nicht bei Slider/Toggle-Änderungen.

### R3 – Filterliste remembern
`visibleItems` ist `remember(items, effectiveLetter)`; ohne `remember` liefe `filterIndexed` bei jeder
Recomposition über bis zu 10k Einträge auf dem Main-Thread.

### M1 – Zählung nur im Indicator-Modus
`labels.groupingBy { }.eachCount()` läuft ausschließlich, wenn `fastScrollMode == INDICATOR_SCREEN`;
im Default-Pfad `emptyMap()`.

### M2 – Stabile Identitäten für `IndicatorScreen`
`letters`/`itemCounts` werden remembered übergeben, damit der Composable skip-fähig bleibt
(`List`/`Map` sind für Compose unstable).

### Recomposition-Scopes (Soll-Zustand)

```
Scrollen (Bar-Modus)
  gridState.firstVisibleItemIndex ── derivedStateOf ──> nur FastScroller
                                                        FastScrollBox: kein Recompose

Hue-Slider in Settings
  SettingsUiState (nur SettingsScreen) ──> SettingsScreenContent
  MusicPlayerAppUI: kein Recompose  (liest nur fastScrollMode)

Mode-Wechsel (Dropdown)
  fastScrollMode ──> MusicPlayerAppUI ──> AppNavHost ──> aktuelle Destination ──> Grid ──> FastScrollBox

Buchstabenwahl
  selectedLetter ──> FastScrollBox ──> IndicatorScreen raus, Grid rein
```

### Bewusst nicht optimiert
- `FastScroller` im Indicator-Modus komponieren und ausblenden → nein (kein Scroll-Listener, keine Recomposition pro Frame).
- Indikator-Grid durch `FlowRow` ersetzen → Lazy-Grid bleibt bei großer Schrift/Tablet scrollbar.
- `labels`/`alphabet` in einen Hintergrund-Thread → < 1 ms für 10k Einträge, Thread-Wechsel teurer.
- Filter per `withContext` verschieben → O(n) pro Tap, Dispatcher-Wechsel kostet mehr.
- `labels` auf `Char` umbauen → spart Allokationen, verzweigt aber Filter- und Alphabet-Logik. Erst bei Messbedarf.

---

## 5. Strings & Ressourcen

`values/strings.xml` und `values-de/strings.xml`:
- `fast_scroll_mode` – „Schnell-Navigation" / „Quick navigation"
- `fast_scroll_mode_bar` – „Seitenleiste (FastScroller)" / „Side bar (FastScroller)"
- `fast_scroll_mode_indicator` – „Buchstaben-Kacheln" / „Letter tiles"
  (bewusst kein „IndicatorScreen" – Entwicklerjargon)
- `plurals/indicator_item_count` – one: „%d Eintrag", other: „%d Einträge"

`R.string.back` wird für die Kopfzeile wiederverwendet (existiert bereits, `SettingsScreen.kt:159`) –
kein neues `back_to_indicator`.

---

## 6. Bewusst nicht geändert

- `FastScroller.kt` bleibt unangetastet.
- Keine Datenbank-Migration (nur ein `GenericSetting`-Eintrag).
- Keine neuen Architektur-Docs: `FastScrollBox`-KDoc beschreibt künftig beide Modi; die Messungen
  `docs/measurements/0019`/`0020` beschreiben weiterhin gültig den `FAST_SCROLLER`-Pfad.
- `availablePresets` im `combine` bleibt bestehen (bestehender Main-Thread-I/O beim Slider-Drag);
  durch R1 wird er nicht verschlimmert. Separater Folgeschritt, falls gewünscht.

---

## 7. Verification Plan

### Automatisiert
1. `./gradlew ktlintFormat`
2. `./gradlew test`

`SettingsViewModelTest`:
- `fastScrollModeFlow = MutableStateFlow(FastScrollMode.FAST_SCROLLER)` und
  `every { appSettingsRepository.observeFastScrollMode() } returns fastScrollModeFlow` im `@Before`
  (**Pflicht**: die dedizierte `stateIn` wird beim VM-Konstruktor aufgebaut; ohne Stub wirft MockK).
- Neuer Test: Flow-Wert ändern → `fastScrollMode` folgt; `updateFastScrollMode(...)` →
  `coVerify { saveFastScrollMode(...) }`.
- Bestehende `uiState`-Tests bleiben unverändert (kein vierter `combine`-Zweig).

Kein `AppSettingsRepositoryTest` (existiert für andere Settings ebenfalls nicht; reiner DAO-Wrapper).

### Manuell (Gerät/Emulator)
1. `SettingsScreen` → „Schnell-Navigation" von „Seitenleiste" auf „Buchstaben-Kacheln" stellen.
2. Artists, Albums, Genres und Songs prüfen: 3-Spalten-Grid nur mit vorhandenen Buchstaben.
3. Buchstabe wählen (z. B. „M") → nur „M"-Einträge, Liste startet oben.
4. Rückkehr: FilterChip ✕ und System-Back führen beide zum Buchstaben-Grid.
5. Rotation bei aktivem Filter → Filter bleibt erhalten (`rememberSaveable`).
6. Modus auf „Seitenleiste" zurückstellen → Overlay-Verhalten unverändert, Songs ebenso.
7. Edge Cases: leere Bibliothek (kein leerer Kachel-Screen), Bibliothek mit nur einem Buchstaben,
   Ziffern/Umlaute als Anfangszeichen (Kachel zeigt das Zeichen, „A-Z" ist keine Zusicherung).
8. Tab-Wechsel in der Library bei aktivem Filter → Filter resettet (erwartetes Verhalten, kein
   `SaveableStateHolder` pro Tab).
9. `ArtistAlbumsScreen` öffnen, Filter setzen, System-Back → erst Filter weg, dann Screen verlassen.

### Performance-Nachweis
1. **Cold Start**: Layout Inspector / `adb shell dumpsys gfxinfo` – kein Preset-Datei-I/O im ersten Frame
   (vorher/nachher vergleichen, da die Shell das `SettingsViewModel` jetzt früher erzeugt).
2. **Hue-Slider** im SettingsScreen ziehen → im Layout Inspector darf `MusicPlayerAppUI` nicht
   recomposen (nur `SettingsScreenContent`).
3. **Buchstabenwahl** bei großer Bibliothek (z. B. 5k Songs): Tippen → erster Frame ≤ 1 Frame;
   `filterIndexed` läuft einmal (Logpunkt/Compose-Counter), nicht pro Recomposition.
4. **Bar-Modus** unverändert: Scrollen recomposed weiterhin nur `FastScroller`.
