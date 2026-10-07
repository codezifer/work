---
name: android-code-review
description: Performs an expert, automated Android code review focusing on code maintainability, bug prevention, resource leak minimization, performance optimizations, and compliance with Modern Android Development (MAD) standards and project architecture guidelines (AGENTS.md). Use this skill when reviewing pull requests, inspecting newly added or modified Kotlin files, evaluating Compose components, Room entities/DAOs, Coroutines/Flow usage, or conducting pre-commit audits.
---

# Android Code Review Guidelines & Standard

This skill provides a systematic framework for reviewing Android codebase contributions. The primary objectives are:
1. **Maintainability & Clean Architecture**: Ensure code readability, modularity, consistency with `AGENTS.md`, and proper layer boundaries.
2. **Bug & Crash Prevention**: Detect edge cases, improper null handling, lifecycle mismatches, and race conditions.
3. **Resource & Memory Optimization**: Prevent `Context` leaks, unreleased hardware/player resources, uncancelled coroutines, and memory footprint bloat.
4. **Performance & Recomposition**: Minimize unnecessary Compose recompositions, heavy main-thread work, and inefficient collection operations.
5. **Modern Android Development (MAD)**: Enforce Jetpack Compose, Kotlin Coroutines/Flow, Room DB conventions, Koin DI, and testability.

---

## 🔄 Scope Determination & Git Strategy

When reviewing modified or uncommitted code across the codebase, determine the review scope using Git:

1. **Identify Current Branch**:
   Check the active branch name using `git rev-parse --abbrev-ref HEAD`.

2. **Branch Diff Strategy**:
   - **Directly on `feature/muzzic` (Main Development Branch)**:
     Compare uncommitted working tree and staged changes against the latest commit using:
     ```bash
     git diff HEAD
     git status --porcelain
     ```
   - **On Feature / Topic Branch (Branch != `feature/muzzic`)**:
     Compare all branch changes introduced relative to `feature/muzzic` using:
     ```bash
     git diff feature/muzzic...HEAD
     ```

3. **File Inspection**:
   Extract the list of modified `.kt`, `.xml`, or database files from the diff and inspect their full context using `read_file` to run the inspection checklist.

---

## 🔍 Code Review Inspection Checklist

### 1. Architecture & File Structure
- [ ] **Clean Architecture Layer Isolation**:
  - `domain/` layer contains **zero Android framework dependencies**.
  - `data/` and `ui/` are decoupled using `domain/` interfaces and use cases.
- [ ] **Feature Isolation & Package Organization**:
  - Related types live within feature packages or dedicated component modules.
- [ ] **File Granularity**:
  - One primary public type per file, matching the file name (e.g., `ApicFrame.kt` for `ApicFrame`).
  - Helper types belonging strictly to one primary type (e.g., enums, value objects) live in the same file.
  - Sub-packages are created when a package exceeds ~10 types.
- [ ] **KDoc Documentation**:
  - All public classes, interfaces, methods, and properties have concise, meaningful KDoc descriptions including parameter, return, and exception details.

### 2. Resource Management & Leak Prevention
- [ ] **Context & Activity Reference Leaks**:
  - ViewModels, Repositories, and Singletons **never** hold references to `Context`, `Activity`, `View`, or `Resources`.
  - Application Context (`androidContext()`) is used only when strictly necessary in DI/Repositories.
- [ ] **Heavy Resource Lifecycle Handling**:
  - Media players (`ExoPlayer`), Bitmaps, database connections, and hardware resources call `.release()` or `.close()` in appropriate lifecycle hooks (`onStop`, `onDestroy`, `onDispose`).
- [ ] **Callback & Listener Unregistration**:
  - Callbacks registered in `callbackFlow` are properly cleaned up in `awaitClose { ... }`.
  - System or lifecycle observers registered in Composables use `DisposableEffect` with `onDispose { ... }` or `LifecycleStartEffect`.
- [ ] **Structured Concurrency**:
  - Coroutines run in structured scopes (`viewModelScope`, `lifecycleScope`, custom `CoroutineScope` with supervisor jobs).
  - No usage of `GlobalScope`.
  - IO/CPU bound operations explicitly specify `Dispatchers.IO` or `Dispatchers.Default` (injected via constructor in repositories/use-cases).

### 3. Bug Prevention, Null Safety & Error Handling
- [ ] **Null Safety Enforcement**:
  - **No `!!` (not-null assertion operator)** unless accompanied by explicit mathematical/guaranteed proof in comments. Safe calls (`?.`) and Elvis operator (`?:`) are preferred.
- [ ] **Exception Handling**:
  - Asynchronous flows and coroutine jobs handle exceptions gracefully using `catch`, `CoroutineExceptionHandler`, or safe wrappers (`safeApiCall`).
  - No silent suppression of exceptions without logging or user notification.
- [ ] **Lifecycle-Aware Flow Collection**:
  - Flows in Composables are collected using `collectAsStateWithLifecycle()`.
  - Flows in Views/Fragments are collected inside `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- [ ] **Unidirectional Data Flow (UDF)**:
  - UI State is exposed as immutable `StateFlow<UiState>`, with backing `MutableStateFlow` kept `private`.
  - One-off events (e.g., snackbars, navigation) are represented as consumable state within `UiState` rather than `SharedFlow` channels, unless fire-and-forget is explicitly justified.

### 4. Jetpack Compose & Recomposition
- [ ] **State Hoisting & Stateless Components**:
  - Reusable UI components in `ui/component/` are stateless, hoisting state and callbacks to callers.
  - ViewModels are consumed only at the screen level (e.g., `UserProfileScreen`), never inside reusable leaf composables.
- [ ] **Recomposition Efficiency**:
  - `LazyColumn` / `LazyRow` items specify stable, unique `key` parameter functions.
  - Derived values from rapidly changing state use `derivedStateOf`.
  - Heavy computations stay outside composition blocks.
- [ ] **Side Effects**:
  - `LaunchedEffect` keys are correctly specified to avoid missing re-triggers or infinite execution loops. Every side effect includes a comment explaining its purpose.
- [ ] **Previews**:
  - UI components provide `@Preview` annotations for **both Light Mode and Dark Mode**, using `AppTheme` and `showBackground = true`.
- [ ] **Shapes & Magic Numbers**:
  - Custom shapes use standard `Shape` implementations (`RoundedCornerShape`, `CutCornerShape`) with `CornerSize` defined in `ui/theme/Shapes.kt`.
  - Dimensions, colors, and thresholds use centralized constants rather than hardcoded "magic numbers".

### 5. Room Database & Persistence
- [ ] **Database Naming Conventions**:
  - Database tables, columns, views, and indexes use explicit `snake_case` mapped via `@ColumnInfo(name = "...")` or `@DatabaseView(viewName = "...")`.
  - Kotlin properties remain `camelCase`.
- [ ] **DAOs & Reactive Queries**:
  - DAOs return `Flow<T>` for observable queries.
  - Schema modifications increment the `@Database` version number and provide migrations when necessary.

### 6. Testing & Code Quality
- [ ] **Testing Standards**:
  - Repositories and DataSources use Fakes (`FakeUserRepository`) rather than heavy Mocks where practical.
  - Assertions use AssertJ (`assertThat(...)`), not JUnit assertions.
  - `StateFlow` tests directly evaluate `.value` or use Turbine.
- [ ] **Code Formatting**:
  - Code complies with standard formatting (`./gradlew ktlintFormat`).

---

## 📋 Standard Review Output Format

When executing a code review with this skill, present findings using the following structured report:

### 📊 Executive Summary
- **Status**: [✅ APPROVED / ⚠️ NEEDS REVISION / 🔴 BLOCKED]
- **Summary**: Concise overview of the reviewed changes and overall code health.
- **Key Metrics**:
  - 🔴 Critical Issues: X
  - 🟡 Warnings / Best Practice Violations: Y
  - 🟢 Clean Code & Refactoring Suggestions: Z

---

### 🔴 Critical Issues (Must Fix Before Merge)
*Severe bugs, memory/context leaks, unsafe null assertions (`!!`), main-thread blocking, missing resource cleanup, or build-breaking flaws.*

- **Location**: `[FileName.kt:LineNumber](file:///path/to/FileName.kt#L123)`
  - **Issue**: Explanation of the critical defect.
  - **Impact**: Potential runtime crash, memory leak, or performance degradation.
  - **Remediation**:
    ```kotlin
    // Proposed fixed code snippet
    ```

---

### 🟡 Warnings & MAD Compliance
*Violations of Modern Android Development guidelines, missing previews, improper Flow collection, missing keys in LazyColumn, missing KDoc, or hardcoded values.*

- **Location**: `[FileName.kt:LineNumber](file:///path/to/FileName.kt#L123)`
  - **Issue**: Explanation of the warning.
  - **Remediation**:
    ```kotlin
    // Proposed fix or improvement
    ```

---

### 🟢 Suggestions & Refactoring
*Clean code polish, micro-optimizations, improved naming, or better component reusability.*

- **Location**: `[FileName.kt:LineNumber](file:///path/to/FileName.kt#L123)`
  - **Suggestion**: Details on how to enhance readability or modularity.

---

### 🧪 Verification Steps
Recommended tests or validation steps (e.g., `./gradlew test`, UI preview rendering, leak detection) to verify the fixes.
