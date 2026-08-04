# Folio Android

Native Android client for Folio — private research, grounded answers. Sources, notes, and citations in one private archive.

**Package:** `com.nus.folio` | **Screens:** Login (start) → Spaces → Home (per space); Sign Up, Reset Password, Account (nav route from Spaces)

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
app/src/
├── main/java/com/nus/folio/
│   ├── components/       # Shared UI: AnimatedModalSheet, BouncingDotsIndicator, FolioToast, ItemOptionsBottomSheet
│   ├── data/             # DataSource, RepositoryImpl (shared)
│   ├── domain/           # Models, Repository interfaces, UseCases
│   ├── di/               # AppContainer, LocalAppContainer
│   ├── presentation/
│   │   ├── login/
│   │   ├── signup/
│   │   ├── resetpassword/
│   │   ├── space/        # SpaceScreen, SpaceViewModel, AddSpaceBottomSheet
│   │   ├── home/         # HomeScreen + panes, header, bottom nav, bottom sheets
│   │   ├── account/      # AccountSettingsScreen (nav route from Spaces)
│   │   └── navigation/   # FolioNavHost, FolioDestination
│   └── ui/theme/         # Color, Type, Theme
├── debug/                # AuthDataSource + AuthCapabilities (mock auth)
├── release/              # AuthDataSource + AuthCapabilities (backend unavailable)
├── test/                 # Shared unit tests + testing/ fakes
├── testDebug/            # Auth tests against debug AuthDataSource
└── testRelease/          # Auth tests against release AuthDataSource
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
- `AppContainer.isAuthAvailable` mirrors `AuthCapabilities.isBackendAvailable` (true in debug, false in release until a real backend is wired)
- `AppContainer.defaultLoginEmail` / `defaultLoginPassword` mirror `AuthCapabilities` (prefilled in debug, empty in release)

### Auth build variants

- `AuthDataSource` and `AuthCapabilities` are **not** in `main` — they live in `debug/` and `release/`
- Debug: credential-gated mock auth for UI development; exposes `defaultLoginEmail` / `defaultLoginPassword` for prefilled login fields
- Release: auth APIs throw / are unavailable; login UI shows unavailable state via `isAuthAvailable`
- Put auth implementation tests in `testDebug` / `testRelease`; keep use-case and ViewModel tests in shared `test/` with fakes

### ViewModel pattern

- One `*UiState` data class per screen (e.g. `HomeUiState`, `LoginUiState`)
- `MutableStateFlow` + `asStateFlow()` for state
- Screens use `collectAsStateWithLifecycle()`
- Access dependencies from `LocalAppContainer.current`

```kotlin
@Composable
fun HomeScreen(
    spaceId: String,
    spaceTitle: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            getSourcesUseCase = LocalAppContainer.current.getSourcesUseCase,
            getAskTopicsUseCase = LocalAppContainer.current.getAskTopicsUseCase,
            getNotesUseCase = LocalAppContainer.current.getNotesUseCase,
        ),
    ),
) { ... }
```

### Use cases

- Single responsibility; typically `operator fun invoke(...): Result<T>` or `suspend operator fun invoke(...): Result<T>`
- Sync session helpers may return a plain value (e.g. `GetCurrentSessionUseCase` → `AuthSession?`, `ClearAuthSessionUseCase` → `Unit`)
- Home data use cases are **space-scoped**: `getSourcesUseCase(spaceId)`, `getAskTopicsUseCase(spaceId)`, `getNotesUseCase(spaceId)`
- Registered in `AppContainer` as lazy properties

### Screens

- Public `*Screen` composable + private `*Content` for UI/preview
- User-facing strings in `res/values/strings.xml`
- `rememberSaveable` for form fields
- `Modifier` parameter with default, passed to root layout
- Previews wrap content in `FolioAndroidTheme`
- Home is tabbed (`HomeTab`: Sources, Ask, Notes, Notebook); Account is a separate nav route from Spaces, not a Home tab
- Shared UI components live in `components/` (modal sheets, toasts, loading indicators, item options)

### Navigation

- Routes in `FolioDestination` (`FolioNavHost.kt`): `LOGIN`, `SIGN_UP`, `RESET_PASSWORD`, `SPACES`, `ACCOUNT`, `HOME`
- Start destination: waits for async session restore, then `SPACES` if signed in else `LOGIN`
- Post-auth flow: Login / Sign Up → `SPACES` → `HOME/{spaceId}?title={title}`
- `FolioDestination.home(spaceId, spaceTitle)` builds the Home route; `resetPassword(email)` builds Reset Password with optional email arg
- `AccountSettingsScreen` is a top-level route (`ACCOUNT`), opened from Spaces; sign-out navigates back to `LOGIN`
- Register new composables in `FolioNavHost`

## Design System

- **Theme:** `FolioAndroidTheme` in `ui/theme/Theme.kt`
- **Brand font:** `CormorantGaramond` (`Type.kt`)
- **Login palette:** `LoginBackground`, `LoginPrimary`, `LoginCopper`, etc. (`Color.kt`)
- **Home palette:** `HomeBackground`, `HomeHeader`, `HomeCardBackground`, status/sheet tokens (`Color.kt`)
- **Home shapes / local tokens:** `presentation/home/HomeTokens.kt`
- **Space shapes / local tokens:** `presentation/space/SpaceTokens.kt`
- Strings in `res/values/strings.xml`; assets in `res/drawable/`; fonts in `res/font/`
- Prefer theme colors over hardcoded values; add new tokens to `Color.kt` (or `HomeTokens.kt` / `SpaceTokens.kt` for screen-local shapes)

## Build, Test & CI

```bash
./gradlew assembleDebug          # Build APK
./gradlew testDebugUnitTest      # Unit tests (includes test/ + testDebug/)
./gradlew lintDebug              # Lint
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator)
```

CI (GitHub Actions on `main`): unit tests + lint only.

Unit tests use fakes under `app/src/test/java/com/nus/folio/testing/` (e.g. `FakeAuthRepository`, `FakeSourceRepository`, `FakeSpaceRepository`, `FakeGreetingRepository`).

## Guidelines

- Minimize scope — match existing patterns, no unrelated changes
- No over-engineering — avoid abstractions for one-off use
- No new dependencies without discussion
- Respect dependency flow: presentation → domain ← data; never import `data` from `presentation` or `domain`
- Do not add Hilt/Koin without explicit request
- Do not hardcode user-facing strings in composables
- Do not commit `.idea/` churn or secrets
- Only create git commits when explicitly asked
- Keep auth backend-specific code in `debug`/`release` source sets, not `main`

## Adding a New Feature

Follow dependency direction: define contracts in `domain` first, implement in `data`, consume from `presentation`.

1. **Domain:** model → repository interface → use case
2. **Data:** data source → repository impl (implements domain interface)
3. **DI:** wire in `AppContainer` (data impl → use case → ViewModel factory)
4. **Presentation:** UiState → ViewModel (+ Factory) → Screen → navigation route (or Home tab/pane if it belongs on Home)

## Reference Files

| Purpose | File |
|---------|------|
| Screen + ViewModel | `presentation/home/HomeScreen.kt`, `HomeViewModel.kt` |
| Space screen | `presentation/space/SpaceScreen.kt`, `SpaceViewModel.kt` |
| Home panes / sheets | `SourcesPane.kt`, `AskPane.kt`, `NotesPane.kt`, `NotebookPane.kt`, `AddSourceBottomSheet.kt` |
| Account (nav route) | `presentation/account/AccountSettingsScreen.kt` |
| Shared components | `components/FolioToast.kt`, `AnimatedModalSheet.kt`, `ItemOptionsBottomSheet.kt`, `BouncingDotsIndicator.kt` |
| Auth use cases | `domain/usecase/SignInUseCase.kt`, `SignInWithAppleUseCase.kt`, `GetCurrentSessionUseCase.kt` |
| Home data use cases | `domain/usecase/GetSourcesUseCase.kt`, `GetAskTopicsUseCase.kt`, `GetNotesUseCase.kt` |
| Space use cases | `domain/usecase/GetSpacesUseCase.kt`, `GetGreetingUseCase.kt` |
| Repository | `domain/repository/`, `data/repository/` |
| Auth variants | `debug\|release/.../AuthDataSource.kt`, `AuthCapabilities.kt` |
| DI wiring | `di/AppContainer.kt` |
| Navigation | `presentation/navigation/FolioNavHost.kt` |
| Test fakes | `test/.../testing/FakeAuthRepository.kt`, `FakeSpaceRepository.kt` |
| Design tokens | `ui/theme/Color.kt`, `Type.kt`, `presentation/home/HomeTokens.kt`, `presentation/space/SpaceTokens.kt` |
| Polished UI | `presentation/login/LoginScreen.kt` |
