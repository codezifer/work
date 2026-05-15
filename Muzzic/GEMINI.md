# Gemini Ruleset: Android Development with Kotlin

> Google Best Practices — Complete Ruleset

---

## 🧭 Core Philosophy

- Always prefer **idiomatic Kotlin** over Java-style patterns.
- Follow **Modern Android Development (MAD)** principles at all times.
- Prioritize **Jetpack Compose** for new UI; use Views only if explicitly required.
- Default to **MVVM + Clean Architecture** unless the scope clearly doesn't warrant it.
- Favor **coroutines and Flow** over callbacks, RxJava, or threads.
- Apply **unidirectional data flow (UDF)** in all UI state management.

---

## 📁 Project Structure

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

---

## 🔤 Kotlin Language Rules

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

- **Favor Generalized Components**: Avoid screen-specific implementations for common UI patterns (e.g., Drag-and-Drop, Loading states, Error handling).
- **DRY Principle**: Logic or UI patterns appearing more than once, or complex enough to be isolated, MUST be moved to `ui/component/` or `utils/`.
- **Composition over Inheritance**: Provide flexible Slot-based APIs (`content: @Composable () -> Unit`) to make components versatile.
- **Stateless Components**: Keep shared components as stateless as possible by hoisting state to the caller.

---

### Data Classes & Sealed Classes

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

// ✅ Use SharedFlow for one-time events (navigation, snackbars)
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

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

### ViewModel

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

```kotlin
// LaunchedEffect: for coroutines triggered by key changes
LaunchedEffect(userId) {
    viewModel.loadUser(userId)
}

// DisposableEffect: for cleanup on leave
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}

// SideEffect: sync Compose state to non-Compose systems
SideEffect {
    systemUiController.setStatusBarColor(color = MaterialTheme.colorScheme.primary)
}
```

---

## 🗄️ Room Database

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

// ✅ Use TypeConverters for complex types
class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun toList(value: String): List<String> = Gson().fromJson(value, Array<String>::class.java).toList()
}
```

---

## 🌐 Networking with Retrofit

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
// ✅ Use MockK for mocking
// ✅ Use Robolectric for Android unit tests without emulator
// ✅ Use Compose Testing APIs for UI tests
```

---

## 🔐 Security & Privacy

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

| Anti-Pattern                            | Correct Alternative                   |
|-----------------------------------------|---------------------------------------|
| `AsyncTask`                             | `viewModelScope.launch` + coroutines  |
| `LiveData` in new code                  | `StateFlow` / `SharedFlow`            |
| `startActivity` in ViewModel            | Navigation events via `SharedFlow`    |
| Hardcoded strings                       | `strings.xml` resources               |
| `!!` (non-null assertion)               | Safe calls + Elvis operator           |
| Static context references               | Koin injection / `androidContext()`   |
| Blocking main thread                    | `withContext(Dispatchers.IO)`         |
| Custom `Application.instance` singleton | Koin `single`                         |
| Mutable public state in ViewModel       | Private `_state` + public `state`     |
| Business logic in Composables           | ViewModel + Use Cases                 |

---

## ✅ Pre-Commit Checklist

- [ ] No `!!` without justification
- [ ] No hardcoded strings — use `strings.xml`
- [ ] No business logic in UI layer
- [ ] All suspend functions tested with `runTest`
- [ ] ViewModels use `viewModelScope` only
- [ ] Flows collected with `repeatOnLifecycle`
- [ ] All public APIs documented with KDoc (short but detailed, including goal, parameters, errors, and result types)
- [ ] ProGuard rules updated for new dependencies
- [ ] Accessibility: content descriptions on interactive elements
- [ ] No sensitive data in logs

---

*Based on: Google Android Developer Guidelines, Kotlin Coding Conventions, MAD Scorecard, Jetpack Documentation — 2025*
