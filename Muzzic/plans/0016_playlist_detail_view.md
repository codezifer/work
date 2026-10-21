# Playlist-Detail-View mit Songliste und Play-Button

## Ziel
Ein Klick auf eine Playlist öffnet eine neue Detail-View mit allen Songs der Playlist in Playlist-Reihenfolge (Issue 0016). Playlist-Karten und Listeneinträge erhalten einen Play-Button mit derselben Funktion wie bei Artists/Alben: Queue ersetzen und Playlist abspielen.

## Kontext
- Bisher lud ein Klick auf eine Playlist die Songs in die Queue und navigierte zum Queue-Screen (`LibraryViewModel.setIntoPlayingQueue`).
- `PlaylistCard` hatte keinen Play-Button, `PlaylistListItem` bereits einen (`onPlayClick`).
- Detail-Screens für Artist/Alben/Genres liegen in der `LIBRARY_GRAPH`; die Playlists waren eine einzelne Top-Level-Route ohne Detail-Ziel.

## Entscheidungen
1. **Tab-Highlight:** Neue `PLAYLISTS_GRAPH` mit `PLAYLISTS` als Startziel und `PLAYLIST_SONGS` als Kind → der Playlists-Tab bleibt auf der Detail-View markiert (auch beim Einstieg über den Library-Filter).
2. **Song-Klick:** ersetzt die Queue und startet an der gewählten Position (`AbstractViewModel.enqueue(songs, startIndex)`), damit der Queue-Screen die Playlist widerspiegelt.
3. **Play-Button im Grid:** nutzt `onPlayPlaylist` (Queue ersetzen, Player öffnen) – identisch zu `PlaylistListItem`.
4. **Header:** Song-Anzahl + Gesamtdauer im Format `HHh:MMmin` (immer gleich, z. B. `00h:50min`, `01h:30min`); Minuten werden abgerundet, Untertitel erst ab `songs.isNotEmpty()`.

## Proposed Changes

### Data Layer
- `PlaylistDao`: `observePlaylist(playlistId): Flow<Playlist?>` und `observeSongsInPlaylist(playlistId): Flow<List<Song>>` (Sortierung `ps.position`); bestehende suspend-Variante bleibt für Play/Queue.
- `PlaylistRepository`: `observePlaylistName(playlistId): Flow<String?>` und `observeSongsInPlaylist(playlistId): Flow<List<SongDto>>` mit `mapIndexed` auf `SongDto`.
- Kein Schema-Change → keine Migration.

### ViewModel / State
- Neu `ui/state/PlaylistSongsUiState.kt`: `playlistId`, `playlistName`, `songs`, `totalDurationMs`.
- Neu `viewmodel/PlaylistSongsViewModel.kt`: `combine` aus Name und Songs, `asUiState(...)`, `playSong(song, allSongs)` → `enqueue(allSongs, index)`.
- `AbstractViewModel.enqueue(songs, startIndex = 0)` mit `coerceIn` auf gültigen Index.
- Koin: `viewModelOf(::PlaylistSongsViewModel)`.
- `LibraryViewModel.setIntoPlayingQueue` entfernt (ungenutzt).

### UI
- Neu `ui/screens/PlaylistSongsScreen.kt`: stateful Wrapper (`koinViewModel`, Selection) + stateless `PlaylistSongsScreenContent` (Preview-fähig), Header mit Back-Button, Playlist-Name, `"{count} Songs · {duration}"`, `SongList`.
- `PlaylistCard`: `onPlayClick` → `MuzzicCard` (`PlayButton` im Cover).
- `PlaylistGrid`: `onPlaylistPlayClick` durchreichen.
- `LibraryScreen`: `onPlaylistPlayClick` → `PlaylistGrid`.
- `formatDurationLong(durationMs)` in `ui/utils/Helper.kt` für `HHh:MMmin`.
- String `playlist_songs_count` in `values` und `values-de`.

### Navigation
- `AppDestinations`: `PLAYLISTS_GRAPH`, `PLAYLIST_ID_ARGUMENT`, `PLAYLIST_SONGS`, `playlistSongs(id)`.
- `NavigationEvent.ToPlaylistSongs(playlistId)`.
- `AppNavigator`: Route + `ToPlaylists` → Graph.
- `MusicAppStateMachine`: Event + Route-Mapping; `AppUiState.Playlists(selectionActive)` (für die Selection-Toolbar).
- `AppNavHost`: Graph um die Playlists-Route, neues Composable mit `navArgument`, `onPlaylistClick` navigiert nur noch.
- `BottomNavigationBar`: `BottomNavItem.Playlists` zeigt auf die Graph-Route.

### Tests / Doku
- `PlaylistSongsViewModelTest` (Turbine, Koin-frei, MockK): State-Kombination, `playSong`-Startindex, leere Playlist.
- `DurationFormatTest` für `formatDurationLong`.
- `docs/class_diagram.puml`: neuer Screen/ViewModel, DAO-/Repository-Methoden und Relationen.

## Erwartetes Verhalten
- Klick auf Playlist (Grid oder Liste) → Songliste der Playlist, Playlists-Tab markiert, Back führt zur Ausgangsseite.
- Song-Klick → Queue wird durch die Playlist ersetzt, Start beim gewählten Song; Long-Press → Selection inkl. Toolbar.
- Play-Button auf Karte/ListItem → Playlist startet (Player öffnet), unabhängig vom Klick auf die Karte.
- Leere Playlist: Header ohne Untertitel, leere Liste, kein Crash, kein Abspielen.

## Verification Plan

### Automated
- `./gradlew ktlintFormat`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

### Manual / On-Device
- Playlists-Tab → Playlist öffnen, Songs prüfen, Play-Button auf Karte nutzen, Back.
- Library-Filter Playlists → Playlist öffnen, Tab-Highlight prüfen.
- Song antippen → Player/Queue prüfen; Long-Press → Selection-Toolbar.
- Previews `PlaylistSongsScreenPreview` (Light/Dark) visuell prüfen.
