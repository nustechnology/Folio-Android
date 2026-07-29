# Folio Android

Native Android client for Folio — private research, grounded answers. Sources, notes, and citations in one private archive.

**Package:** `com.nus.folio` | **Screens:** Login (start), Home

## Tech Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture — dependency flow: **presentation → domain ← data**
- **DI:** Manual `AppContainer` + `CompositionLocal` (no Hilt/Koin yet)
- **Navigation:** Navigation Compose
- **Async:** Kotlin Coroutines + `StateFlow`
- **Min SDK:** 24 | **Target/Compile SDK:** 35 | **Java:** 17
- **Dependencies:** `gradle/libs.versions.toml`

## Project Structure

```text
app/src/main/java/com/nus/folio/
├── data/           # DataSource, RepositoryImpl
├── domain/         # Models, Repository interfaces, UseCases
├── di/             # AppContainer, LocalAppContainer
├── presentation/   # Screens, ViewModels, UiState, navigation
└── ui/theme/       # Color, Type, Theme
```

**Dependency flow:** presentation → domain ← data (outer layers depend inward; domain is the center).

| Layer | May depend on | Must not depend on |
|-------|---------------|-------------------|
| `presentation` | `domain` | `data` |
| `domain` | nothing in `data` or `presentation` | `data`, `presentation`, Android/framework |
| `data` | `domain` | `presentation` |

Repository **interfaces** live in `domain`; **implementations** live in `data`.

## Architecture Conventions

### DI

- `FolioApplication` creates `AppContainer`
- `MainActivity` provides it via `CompositionLocalProvider(LocalAppContainer)`
- ViewModels receive use cases through `ViewModel.Factory`

### ViewModel pattern

- One `*UiState` data class per screen (e.g. `HomeUiState`)
- `MutableStateFlow` + `asStateFlow()` for state
- Screens use `collectAsStateWithLifecycle()`
- Access dependencies from `LocalAppContainer.current`

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalAppContainer.current.getGreetingUseCase),
    ),
) { ... }
```

### Use cases

- Single responsibility, `suspend operator fun invoke(): Result<T>`
- Registered in `AppContainer` as lazy properties

### Screens

- Public `*Screen` composable + private `*Content` for UI/preview
- User-facing strings in `res/values/strings.xml`
- `rememberSaveable` for form fields
- `Modifier` parameter with default, passed to root layout
- Previews wrap content in `FolioAndroidTheme`

### Navigation

- Routes in `FolioDestination` (`FolioNavHost.kt`); start destination: `LOGIN`
- Register new composables in `FolioNavHost`

## Design System

- **Theme:** `FolioAndroidTheme` in `ui/theme/Theme.kt`
- **Brand font:** `CormorantGaramond` (`Type.kt`)
- **Login palette:** `LoginBackground`, `LoginPrimary`, `LoginCopper`, etc. (`Color.kt`)
- Strings in `res/values/strings.xml`; assets in `res/drawable/`; fonts in `res/font/`
- Prefer theme colors over hardcoded values; add new tokens to `Color.kt`

## Build, Test & CI

```bash
./gradlew assembleDebug          # Build APK
./gradlew testDebugUnitTest      # Unit tests
./gradlew lintDebug              # Lint
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator)
```

CI (GitHub Actions on `main`): unit tests + lint only.

## Guidelines

- Minimize scope — match existing patterns, no unrelated changes
- No over-engineering — avoid abstractions for one-off use
- No new dependencies without discussion
- Respect dependency flow: presentation → domain ← data; never import `data` from `presentation` or `domain` from `data`
- Do not add Hilt/Koin without explicit request
- Do not hardcode user-facing strings in composables
- Do not commit `.idea/` churn or secrets
- Only create git commits when explicitly asked

## Adding a New Feature

Follow dependency direction: define contracts in `domain` first, implement in `data`, consume from `presentation`.

1. **Domain:** model → repository interface → use case
2. **Data:** data source → repository impl (implements domain interface)
3. **DI:** wire in `AppContainer` (data impl → use case → ViewModel factory)
4. **Presentation:** UiState → ViewModel (+ Factory) → Screen → navigation route

## Reference Files

| Purpose | File |
|---------|------|
| Screen + ViewModel | `presentation/home/HomeScreen.kt`, `HomeViewModel.kt` |
| Use case | `domain/usecase/GetGreetingUseCase.kt` |
| Repository | `domain/repository/`, `data/repository/` |
| DI wiring | `di/AppContainer.kt` |
| Navigation | `presentation/navigation/FolioNavHost.kt` |
| Design tokens | `ui/theme/Color.kt`, `Type.kt` |
| Polished UI | `presentation/login/LoginScreen.kt` |
