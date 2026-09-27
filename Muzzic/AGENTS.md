# Gemini Ruleset: Android Development with Kotlin

> Google Best Practices — Complete Ruleset

---

## 🧭 Core Philosophy
**Intent**: Establish a unified, forward-looking development standard based on Google's Modern Android Development (MAD) recommendations.
**Optimization Purpose**: Reduce technical debt, improve code readability for human and AI collaborators, and leverage the latest compiler and framework optimizations.

- Always prefer **idiomatic Kotlin** over Java-style patterns.
- Follow **Modern Android Development (MAD)** principles at all times.
- Prioritize **Jetpack Compose** for new UI; use Views only if explicitly required.
- Default to **MVVM + Clean Architecture** unless the scope clearly doesn't warrant it.
- Favor **coroutines and Flow** over callbacks, RxJava, or threads.
- Apply **unidirectional data flow (UDF)** in all UI state management.

---

## 📁 Project Structure
**Intent**: Enforce a strict separation of concerns via Clean Architecture to make the codebase modular and testable.
**Optimization Purpose**: Minimize build times through decoupled modules and prevent circular dependencies.

```
app/
├── data/
│   ├── local/          # Room DAOs, entities, local data sources
│   ├── remote/         # Retrofit APIs, DTOs, remote data sources
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Pure domain models (no Android dependencies)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Use cases / interactors
├── ui/
│   ├── component/      # Reusable Compose components
│   ├── screen/         # Screen-level composables + ViewModels
│   └── theme/          # MaterialTheme, Typography, Color, Shape
└── utils/              # Helper functions and extensions
```

- One feature = one package. Never mix features.
- `domain/` must have **zero Android framework dependencies**.
- Keep `data/` and `ui/` strictly separated via `domain/` interfaces.
- Koin modules should be located within the relevant package (e.g., `data/local/databaseModule.kt`).

### File Granularity
**Intent**: Keep files navigable and diffs reviewable as the model grows.
**Optimization Purpose**: Reduce merge conflicts and let human and AI collaborators locate a type by file name without searching.

- **One frame = one file**: each model type lives in its own file named after the primary type (e.g., `ApicFrame.kt` for `ApicFrame`).
- Helper types belonging to exactly one primary type (its enum, value object, or entry class — e.g., `PictureType` belongs to `ApicFrame`) live in the same file.
- Shared base types (interfaces, headers, sealed hierarchies whose subtypes share the file) stay in the parent package.
- Maximum: one primary public type per file; group files that grow beyond ~10 types into focused sub-packages instead.

---

## 🔤 Kotlin Language Rules
**Intent**: Ensure code consistency and safety using Kotlin's expressive features.
**Optimization Purpose**: Maximize null safety and performance by using appropriate language constructs and reducing boilerplate.

### Naming Conventions

```kotlin
// Classes, objects, enums: PascalCase
class UserRepository
object AppConfig
enum class LoadingState { IDLE, LOADING, SUCCESS, ERROR }

// Functions, variables: camelCase
fun fetchUserData(): Flow<User>
val currentUser: User?

// Constants: SCREAMING_SNAKE_CASE in companion objects
companion object {
    const val MAX_RETRY_COUNT = 3
    const val BASE_URL = "https://api.example.com/"
}

// Private fields: no underscore prefix (Kotlin convention)
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Flow-returning functions: get{Model}Stream() — plural model name for lists
fun getUserStream(id: String): Flow<User>
fun getUsersStream(): Flow<List<User>>

// Interface implementations: meaningful name, or Default{Interface} if none fits
// Fakes always get the Fake prefix (used by tests, not production code)
class OfflineFirstUserRepository : UserRepository
class DefaultUserRepository : UserRepository
class FakeUserRepository : UserRepository
```

### Null Safety

```kotlin
// ✅ Use safe calls and Elvis operator
val name = user?.profile?.displayName ?: "Anonymous"

// ✅ Use let/run for null-safe blocks
user?.let { validUser ->
    processUser(validUser)
}

// ❌ Never use !! unless you can guarantee non-null with a comment
val name = user!!.name // Forbidden without explicit justification
```

### Immutability

```kotlin
// ✅ Prefer val over var
val items: List<String> = listOf("a", "b")

// ✅ Prefer immutable collections
val map: Map<String, Int> = mapOf("key" to 1)

// ✅ Use data classes with copy() for mutations
val updatedUser = user.copy(name = "New Name")
```

### Functions

```kotlin
// ✅ Use expression bodies for single-expression functions
fun fullName(first: String, last: String): String = "$first $last"

// ✅ Use default parameters instead of overloads
fun createRequest(url: String, timeout: Int = 30, retries: Int = 3): Request

// ✅ Use named arguments for clarity
createRequest(url = "https://api.example.com", retries = 5)

// ✅ Use extension functions to extend existing types cleanly
fun String.isValidEmail(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
```

### Documentation (KDoc)

```kotlin
/**
 * Short description of the function's goal.
 *
 * Longer explanation if necessary, detailing complex logic or side effects.
 *
 * @param parameterName Description of what this parameter represents.
 * @return Description of the result produced by this function.
 * @throws ExceptionType Description of when this exception might be thrown.
 */
fun processData(input: String): Result { ... }
```

- All **public methods and classes** MUST have KDoc.
- Keep descriptions concise but informative.
- Document all parameters, return values, and potential exceptions.

---

## ♻️ Reusability & Generalization
**Intent**: Promote DRY (Don't Repeat Yourself) through highly generalized and decoupled UI/Logic components.
**Optimization Purpose**: Reduce binary size and maintainability overhead by consolidating shared patterns into atomic components.

- **Favor Generalized Components**: Avoid screen-specific implementations for common UI patterns (e.g., Drag-and-Drop, Loading states, Error handling).
- **DRY Principle**: Logic or UI patterns appearing more than once, or complex enough to be isolated, MUST be moved to `ui/component/` or `utils/`.
- **Composition over Inheritance**: Provide flexible Slot-based APIs (`content: @Composable () -> Unit`) to make components versatile.
- **Stateless Components**: Keep shared components as stateless as possible by hoisting state to the caller.
- **Custom UI Shapes**: Avoid inline definitions of `GenericShape` with manual path drawing for common geometric patterns. Instead, use idiomatic `Shape` implementations (`RoundedCornerShape`, `CutCornerShape`) with `CornerSize` (percentage or absolute). Centralize reusable shapes in `ui/theme/Shapes.kt` to ensure design consistency.
- **Avoid Magic Numbers**: Never use hardcoded "magic numbers" for dimensions, ratios, or colors. Define reusable constants with descriptive names. For proportional rounding (percentage-based `CornerSize`), document the base dimensions used for the calculation to ensure maintainability.
- **Byte Operations & Low-level Logic**: Always document byte-level operations, bit-shifting, and manual buffer parsing. These operations are hard to read and require explicit comments explaining the structure being parsed (e.g., "Extracts the synchsafe integer from bytes 6-9 representing the tag size").

---

### Data Classes & Sealed Classes
**Intent**: Represent state and domain models explicitly and securely using Kotlin's type system.
**Optimization Purpose**: Enable exhaustive `when` expressions for state management and reduce runtime errors.

```kotlin
// ✅ Domain models as data classes
data class User(
    val id: String,
    val name: String,
    val email: String
)

// ✅ Use sealed classes / interfaces for state and results
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}
```

### Scope Functions
**Intent**: Improve code readability and chainability by using idiomatic scope functions.
**Optimization Purpose**: Reduce the need for temporary variables and clearly define the scope of operations.

```kotlin
// let  → nullable transformation / side effect block
user?.let { processUser(it) }

// apply → builder-style object configuration
val intent = Intent(context, MainActivity::class.java).apply {
    putExtra("key", "value")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}

// with → operate on a non-null receiver without return
with(binding) {
    titleTextView.text = "Hello"
    subtitleTextView.visibility = View.GONE
}

// run → scoped computation that returns a value
val result = user.run { "${name}: ${email}" }

// also → side effects without altering the receiver
val list = mutableListOf(1, 2, 3).also { log("List created: $it") }
```

---

## ⚡ Coroutines & Flow
**Intent**: Handle asynchronous operations and reactive data streams using structured concurrency.
**Optimization Purpose**: Prevent memory leaks and ensure UI responsiveness by offloading heavy work to appropriate background dispatchers.

### Coroutine Scopes

```kotlin
// ✅ ViewModels always use viewModelScope
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    fun loadUser(id: String) {
        viewModelScope.launch {
            // suspend work here
        }
    }
}

// ✅ Lifecycle-aware scopes in fragments/activities
lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}
```

### Dispatchers

```kotlin
// IO-bound work
withContext(Dispatchers.IO) { database.userDao().getAll() }

// CPU-intensive work
withContext(Dispatchers.Default) { computeHeavyResult() }

// Never hardcode Dispatchers in repositories — inject via constructor
class UserRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
)
```

### Flow Best Practices

```kotlin
// ✅ Use StateFlow for UI state
private val _uiState = MutableStateFlow(UiState.Initial)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ⚠️ SharedFlow for one-off events (navigation, snackbars) is an OLDER pattern.
// Google's current architecture guidance explicitly recommends AGAINST sending
// events from the ViewModel to the UI: process the event immediately in the
// ViewModel and let it produce a state update instead.
// See: https://developer.android.com/topic/architecture/recommendations#ui-layer
// Model the "event" as part of uiState instead:
data class UiState(
    val oneOffMessage: String? = null, // consumed + cleared by the UI, not streamed
    // ...
)
// The UI reads it, shows it, then calls viewModel.onMessageShown() to clear it.
// Only fall back to SharedFlow for events that are genuinely fire-and-forget
// and have no sensible state representation (rare) — document why if you do.

// ✅ Use callbackFlow to wrap callback-based APIs
fun observeNetworkState(): Flow<Boolean> = callbackFlow {
    val callback = object : ConnectivityCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }
        override fun onLost(network: Network) {
            trySend(false)
        }
    }
    connectivityManager.registerCallback(callback)
    awaitClose { connectivityManager.unregisterCallback(callback) }
}

// ✅ Prefer cold flows from Room/Retrofit — don't collect unnecessarily
val users: Flow<List<User>> = userDao.observeAll()
```

---

## 🏗️ Architecture: MVVM + Clean Architecture
**Intent**: Structure the application into distinct layers to isolate business logic from UI and data concerns.
**Optimization Purpose**: Facilitate unit testing, enable parallel development, and simplify maintenance as the project grows.

### ViewModel

- Use ViewModels **only at screen level** (screen-level composables or navigation destinations) — never inject a ViewModel into a reusable, non-screen component. Reusable components take a plain state holder or hoisted state/callbacks instead.
- Never pass `Context`, `Activity`, or `Resources` into a ViewModel constructor. If something needs one, that logic belongs in the UI or data layer, not the ViewModel.

```kotlin
class UserViewModel(
    private val getUsersUseCase: GetUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<User>>> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            getUsersUseCase()
                .onStart { _uiState.value = UiState.Loading }
                .catch { e -> _uiState.value = UiState.Error(e.message ?: "Unknown error") }
                .collect { users -> _uiState.value = UiState.Success(users) }
        }
    }
}
```

### Use Cases

```kotlin
// ✅ Single-responsibility use cases
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    operator fun invoke(): Flow<List<User>> = userRepository
        .getUsers()
        .flowOn(ioDispatcher)
}
```

### Repository Pattern

```kotlin
interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun getUserById(id: String): User?
    suspend fun saveUser(user: User)
}

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {
    override fun getUsers(): Flow<List<User>> = localDataSource.observeUsers()
        .onEmpty { fetchAndCacheUsers() }
}
```

---

## 🎨 Jetpack Compose
**Intent**: Build intuitive, high-performance UIs using a declarative programming model.
**Optimization Purpose**: Reduce recompositions through proper state management and optimized Composable design.

### Composable Rules

```kotlin
// ✅ Composables are PascalCase
@Composable
fun UserProfileScreen(viewModel: UserViewModel = koinViewModel()) {
}

// ✅ Use state hoisting — lift state up to the caller
@Composable
fun NameInput(value: String, onValueChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onValueChange)
}

// ✅ Stateless composables for reusability and testability
@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Text(text = user.name)
    }
}

// ✅ Self-contained screens: wrap screen-level composables in AppTheme
@Composable
fun UserProfileScreen(viewModel: UserViewModel = koinViewModel()) {
    AppTheme {
        UserProfileContent(...)
    }
}
```

### State in Compose

```kotlin
// ✅ Collect StateFlow safely in Compose
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// ✅ Use remember for local state
var expanded by remember { mutableStateOf(false) }

// ✅ Use rememberSaveable to survive config changes
var inputText by rememberSaveable { mutableStateOf("") }
```

### Previews

```kotlin
// ✅ Use AppTheme and showBackground for consistent previews
// ✅ ALWAYS implement both Light and Dark mode previews for UI components
@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Dark Mode")
fun UserProfilePreview() {
    AppTheme {
        UserProfileContent(...)
    }
}
```

- All **UI components** MUST include a preview for both Light Mode and Dark Mode.
- Use `AppTheme` and `showBackground = true` for consistent previews.
- Keep descriptions concise but informative.

### Performance

```kotlin
// ✅ Use keys in LazyColumn to maintain identity
LazyColumn {
    items(users, key = { it.id }) { user ->
        UserCard(user = user)
    }
}

// ✅ Avoid heavy computation in composition — use derivedStateOf
val sortedItems by remember {
    derivedStateOf { items.sortedBy { it.name } }
}

// ✅ Minimize recomposition scope — pass lambdas not state objects
UserList(
    onItemClick = { id -> viewModel.onUserSelected(id) }
)
```

### Side Effects
**Intent**: Bridge the gap between the declarative UI and imperative lifecycle/system events.
**Optimization Purpose**: Ensure cleanup of resources and prevent redundant executions of side-logic.

```kotlin
// ✅ LaunchedEffect: for coroutines triggered by key changes.
// ALWAYS document the purpose of the effect with a concise comment.
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}

// DisposableEffect: for cleanup on leave.
// ALWAYS document the purpose of the effect with a concise comment.
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}

// ✅ Prefer LifecycleStartEffect / LifecycleResumeEffect over a manual
// LifecycleEventObserver for simple start/stop or resume/pause bound work —
// less boilerplate, same lifecycle-awareness.
LifecycleStartEffect(Unit) {
    val listener = /* e.g. register a system callback */ TODO()
    onStopOrDispose { /* unregister */ }
}

// SideEffect: sync Compose state to non-Compose systems.
// ALWAYS document the purpose of the effect with a concise comment.
SideEffect {
    systemUiController.setStatusBarColor(color = MaterialTheme.colorScheme.primary)
}
```

---

## 🗄️ Room Database
**Intent**: Provide a reliable, reactive local storage solution using Room.
**Optimization Purpose**: Minimize disk I/O on the main thread and ensure data consistency through schema migrations.

```kotlin
// ✅ Entities use data classes
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val email: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ✅ DAOs return Flow for reactive queries
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY display_name ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Delete
    suspend fun delete(user: UserEntity)
}

// ✅ Database schema changes
// When changing the database schema (Entities, DAOs), ALWAYS increment the version
// in the Database class and provide a migration if necessary.
@Database(entities = [UserEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

// ✅ Database identifiers are snake_case; Kotlin properties stay camelCase
// Table, column, view and index names MUST be snake_case. Map them explicitly
// with @ColumnInfo(name = "...") or SQL aliases (e.g. `genre AS genre_key`);
// Kotlin properties, POJO fields and DAO parameters stay camelCase.
// Pre-existing camelCase columns are grandfathered — renaming them requires a
// table rebuild migration, so the rule applies to new identifiers only.
@DatabaseView(viewName = "songs_enriched")
data class SongsEnriched(
    @Embedded val song: Song,
    @ColumnInfo(name = "normalized_genre") val normalizedGenre: String,
    @ColumnInfo(name = "sort_artist") val sortArtist: String
)

// ✅ Use TypeConverters for complex types
class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun toList(value: String): List<String> = Gson().fromJson(value, Array<String>::class.java).toList()
}
```

---

## 🎵 Media3 / ExoPlayer
**Intent**: Integrate playback cleanly into the layered architecture instead of letting the UI talk to the player directly.
**Optimization Purpose**: Avoid leaks, keep playback state testable, and get system integration (notification, Bluetooth, Android Auto) for free.

```kotlin
// ✅ Background playback via MediaSessionService, not a bare Service
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}

// ✅ UI/ViewModel never touches ExoPlayer directly — go through a repository
// that wraps a MediaController, consistent with the Repository Pattern above.
interface PlaybackRepository {
    fun getPlaybackStateStream(): Flow<PlaybackState>
    suspend fun play(mediaItem: MediaItem)
    suspend fun pause()
}

class MediaControllerPlaybackRepository(
    private val controllerFuture: ListenableFuture<MediaController>
) : PlaybackRepository {
    override fun getPlaybackStateStream(): Flow<PlaybackState> = callbackFlow {
        val controller = controllerFuture.await()
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                trySend(controller.toPlaybackState())
            }
        }
        controller.addListener(listener)
        trySend(controller.toPlaybackState())
        awaitClose { controller.removeListener(listener) }
    }

    override suspend fun play(mediaItem: MediaItem) {
        controllerFuture.await().apply { setMediaItem(mediaItem); prepare(); play() }
    }

    override suspend fun pause() {
        controllerFuture.await().pause()
    }
}
```

- Never create a long-lived `ExoPlayer` directly inside a Composable. Bind it to a lifecycle owner or a `MediaSessionService`, and always call `release()` in `onStop`/`onDispose`/`onDestroy`.
- `Player.Listener` callbacks get turned into a `Flow`/`StateFlow` (via `callbackFlow`, same pattern as other callback-based APIs above) — never held as raw mutable Compose state read from a listener directly.
- Test playback logic with the fakes from `media3-test-utils` (`FakeExoPlayer`/`FakeClock` etc.) instead of instantiating a real `ExoPlayer` in unit tests.

---

## 🌐 Networking with Retrofit
**Intent**: Implement type-safe HTTP clients for remote data fetching.
**Optimization Purpose**: Centralize API handling and enable easy integration of interceptors for logging, auth, and caching.

```kotlin
// ✅ Suspend functions in service interfaces
interface UserApiService {
    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserDto>

    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserDto>
}

// ✅ Map DTOs to domain models — never leak DTOs into domain/UI
// ✅ Place mapping logic (e.g., DTO to MediaItem, Entity to DTO) as extension functions
//    within the DTO or Entity file. This ensures related logic stays close together.
fun UserDto.toDomain(): User = User(
    id = id,
    name = displayName,
    email = email
)

// ✅ Media3 Conversion Pattern: MediaItem to DTO
// Place conversion logic as extension functions within the DTO file
fun MediaItem.toArtistDto(): ArtistDto {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: Bundle.EMPTY
    return ArtistDto(
        artistName = metadata.title?.toString() ?: "",
        albumCount = extras.getInt("album_count"),
        songCount = extras.getInt("song_count")
    )
}

// ✅ Wrap network calls in safe result wrappers
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Result.Success(response.body()!!)
        } else {
            Result.Error(HttpException(response))
        }
    } catch (e: IOException) {
        Result.Error(e)
    }
}
```

---

## 💉 Dependency Injection with Koin
**Intent**: Decouple component creation from usage to enhance modularity and testability.
**Optimization Purpose**: Reduce memory overhead by using appropriate scopes (single, factory, viewModel).

```kotlin
// ✅ Module per concern
val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

// ✅ Register ViewModels using viewModel or viewModelOf
val viewModelModule = module {
    viewModelOf(::UserViewModel)

    // Explicit constructor injection if needed
    viewModel {
        LibraryViewModel(musicRepository = get(), artistRepository = get())
    }
}

// ✅ Register Repositories and Data Sources
val repoModule = module {
    single<UserRepository> { UserRepositoryImpl(get()) }
}

// ✅ Koin Scopes and Definitions
// single  → singleton definition, lives as long as the Koin container
// factory → provides a new instance each time
// viewModel → specific definition for Android ViewModels
```

---

## 🧪 Testing
**Intent**: Verify application logic and UI behavior through automated test suites.
**Optimization Purpose**: Catch regressions early and document expected behavior through executable specifications.

```kotlin
// ✅ Unit test ViewModels with TestCoroutineDispatcher
@ExperimentalCoroutinesApi
class UserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getUsersUseCase: GetUsersUseCase = mockk()
    private lateinit var viewModel: UserViewModel

    @Before
    fun setup() {
        viewModel = UserViewModel(getUsersUseCase)
    }

    @Test
    fun `loadUsers emits Success state`() = runTest {
        val users = listOf(User("1", "Alice", "alice@example.com"))
        every { getUsersUseCase() } returns flowOf(users)

        viewModel.uiState.test {
            assertEquals(UiState.Success(users), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

// ✅ Use Turbine for Flow testing
// ✅ Use Robolectric for Android unit tests without emulator
// ✅ Use Compose Testing APIs for UI tests
```

- **Prefer fakes over mocks** for repositories and data sources (Google's current strong recommendation). A `FakeUserRepository` backed by an in-memory list is easier to reason about and reuse than a MockK stub repeated per test. Reserve MockK for collaborators that are impractical to fake (e.g. platform callbacks with complex contracts).
- When testing a `StateFlow`, assert on the `.value` property directly where possible, and account for `WhileSubscribed(...)` timing when the flow is built with `stateIn`.

---

## 🔐 Security & Privacy
**Intent**: Protect user data and application integrity against common threats.
**Optimization Purpose**: Maintain user trust and comply with privacy regulations by minimizing data exposure.

- Never log sensitive data (tokens, passwords, PII).
- Store secrets in `local.properties` or encrypted SharedPreferences — never in code.
- Use `EncryptedSharedPreferences` for sensitive user data.
- Always validate SSL certificates — never use custom TrustManagers in production.
- Request only the permissions you actually need.
- Use `FLAG_SECURE` for screens containing sensitive content.
- Obfuscate with ProGuard/R8 in release builds.

```kotlin
// ✅ Encrypted preferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## ♿ Accessibility
**Intent**: Ensure the application is usable by everyone, including people with disabilities.
**Optimization Purpose**: Improve user experience and reach a wider audience by following Material Design accessibility guidelines.

```kotlin
// ✅ Always provide content descriptions for images
Image(
    painter = painterResource(R.drawable.user_avatar),
    contentDescription = stringResource(R.string.user_avatar_description)
)

// ✅ Use semantics for custom components
Box(
    modifier = Modifier.semantics {
        contentDescription = "Close dialog"
        role = Role.Button
    }
)

// ✅ Ensure touch targets are at least 48dp
IconButton(
    modifier = Modifier.size(48.dp),
    onClick = { }
) { Icon(Icons.Default.Close, contentDescription = "Close") }
```

---

## 🚀 Performance
**Intent**: Optimize the application's speed, efficiency, and resource consumption.
**Optimization Purpose**: Provide a smooth user experience (60/120 FPS) and minimize battery drain.

- Use `R8` full mode and enable shrinking in release builds.
- Avoid memory leaks: never hold Activity/Context references in long-lived objects.
- Profile with Android Studio Profiler before optimizing.
- Prefer `LazyColumn`/`LazyRow` over `Column`/`Row` for long lists.
- Use `Coil` or `Glide` for image loading — never load bitmaps on the main thread.
- Avoid `GlobalScope` — always use structured concurrency.
- Use `WorkManager` for deferrable background tasks.

```kotlin
// ✅ Coil for image loading in Compose
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.avatarUrl)
        .crossfade(true)
        .build(),
    contentDescription = user.name,
    contentScale = ContentScale.Crop,
    modifier = Modifier.clip(CircleShape)
)
```

---

## 🔧 Build & Tooling
**Intent**: Maintain a modern and efficient build system using Gradle and Kotlin DSL.
**Optimization Purpose**: Ensure consistent dependency management and faster incremental builds.

```kotlin
// ✅ Use Version Catalog (libs.versions.toml)
[versions]
kotlin = "2.2.0"
compose = "1.7.0"
koin = "4.1.1"

[libraries]
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "compose" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }

// ✅ Kotlin DSL for build scripts (build.gradle.kts)
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

---

## ❌ Anti-Patterns to Avoid
**Intent**: Prevent common mistakes and outdated patterns that hinder quality and performance.
**Optimization Purpose**: Guide developers towards safer, more maintainable alternatives.

| Anti-Pattern                            | Correct Alternative                   |
|-----------------------------------------|---------------------------------------|
| `AsyncTask`                             | `viewModelScope.launch` + coroutines  |
| `LiveData` in new code                  | `StateFlow` / `SharedFlow`            |
| `startActivity` in ViewModel            | Model destination/one-off message as part of `uiState`, consumed by the UI |
| Hardcoded strings                       | `strings.xml` resources               |
| `!!` (non-null assertion)               | Safe calls + Elvis operator           |
| Static context references               | Koin injection / `androidContext()`   |
| Blocking main thread                    | `withContext(Dispatchers.IO)`         |
| Custom `Application.instance` singleton | Koin `single`                         |
| Mutable public state in ViewModel       | Private `_state` + public `state`     |
| Business logic in Composables           | ViewModel + Use Cases                 |

---

## 📚 Documentation
**Intent**: Keep human-readable docs in sync with the code so architecture, flows, and diagrams stay trustworthy.
**Optimization Purpose**: Prevent doc rot by making doc updates part of the change itself instead of a separate cleanup task.

- **Docs follow code**: Every change that affects documented behavior or structure MUST update the corresponding docs in the same change — e.g. new/renamed/removed public types, changed data flows, new modules or engines, changed lifecycle/threading, outdated diagrams (PlantUML in `docs/`, mermaid in module `docs/`).
- **Docs live with the code**: App-level docs go in `docs/` (e.g. `docs/class_diagram.puml`); module docs go in the module's `docs/` folder (e.g. `visualization/docs/` with `README.md` as index). KDoc on public APIs stays the source of truth for API details; markdown docs cover architecture, flows, and cross-file relationships.
- **Missing docs get created**: If the touched area has no doc yet, create one following the existing structure (index entry in the folder's `README.md` where one exists, same chapter schema and diagram style as neighboring docs).
- **Keep it proportional**: Fix the facts and diagrams the change invalidates; do not rewrite unrelated chapters in the same change.

---

## ✅ Pre-Commit Checklist
**Intent**: Ensure final code quality and adherence to rules before merging.
**Optimization Purpose**: Reduce review overhead and prevent common issues from entering the main branch.

- [ ] No `!!` without justification
- [ ] No hardcoded strings — use `strings.xml`
- [ ] No business logic in UI layer
- [ ] All suspend functions tested with `runTest`
- [ ] ViewModels use `viewModelScope` only
- [ ] Flows collected with `repeatOnLifecycle`
- [ ] All public APIs documented with KDoc (short but detailed, including goal, parameters, errors, and result types)
- [ ] Affected docs updated or created (`docs/`, module `docs/`, diagrams)
- [ ] New database identifiers (tables, columns, views, indexes) are snake_case; Kotlin properties stay camelCase
- [ ] ProGuard rules updated for new dependencies
- [ ] Accessibility: content descriptions on interactive elements
- [ ] No sensitive data in logs

---

## 🛠️ Workflow Rules
**Intent**: Enforce automated quality checks and consistency across the project.
**Optimization Purpose**: Reduce manual review effort and ensure a stable, well-formatted codebase.

- **Automated Formatting**: After making any changes to Kotlin files, ALWAYS run the `./gradlew ktlintFormat` task to ensure the code adheres to the project's style guide.
- **Verification**: Before concluding a task, ALWAYS verify that all unit tests pass by running the `./gradlew test` task (or the specific task for the module, e.g., `:app:testDebugUnitTest`). No task is considered complete if there are failing tests.
- **Continuous Improvement**: If you encounter recurring issues or patterns that could be automated, propose an update to this ruleset.

---

*Based on: Google Android Developer Guidelines, Kotlin Coding Conventions, MAD Scorecard, Jetpack Documentation — 2025*
