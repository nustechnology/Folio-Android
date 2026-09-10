# Folio Android

Native Android client for Folio — private research, grounded answers. Sources, notes, and citations in one private archive.

**Package:** `com.nus.folio` | **Screens:** Login (start) → Spaces → Home (per space) → Source Detail; Sign Up, Reset Password, Account (nav route from Spaces)

## Tech Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture — dependency flow: **presentation → domain ← data**
- **DI:** Manual `AppContainer` + `CompositionLocal` (no Hilt/Koin yet)
- **Navigation:** Navigation Compose
- **Async:** Kotlin Coroutines + `StateFlow`
- **Persistence:** Encrypted DataStore session (`EncryptedAuthSessionStore`); local notebook content via `NotebookStore` (DataStore)
- **Min SDK:** 24 | **Target/Compile SDK:** 35 | **Java:** 17
- **Dependencies:** `gradle/libs.versions.toml`

## Project Structure

```text
app/src/
├── main/java/com/nus/folio/
│   ├── components/       # Shared UI: sheets, toast, skeleton, search, empty state, bouncing dots
│   ├── data/
│   │   ├── auth/         # AuthSessionStore (+ cipher); AuthCapabilities is variant-only
│   │   ├── datasource/   # Shared sample data; Auth/Space/Source/Note/Ask/Notebook live in backend/
│   │   ├── network/      # UnauthorizedException (shared); API clients live in backend/
│   │   ├── notebook/     # NotebookStore (local DataStore persistence)
│   │   ├── repository/   # *RepositoryImpl
│   │   └── util/         # MIME types, file bytes reader, original-file writer, path helpers
│   ├── domain/           # Models, Repository interfaces, UseCases, input-rule utils
│   ├── di/               # AppContainer, LocalAppContainer
│   ├── presentation/
│   │   ├── login/
│   │   ├── signup/
│   │   ├── resetpassword/
│   │   ├── space/        # SpaceScreen, SpaceViewModel, Add/EditSpaceBottomSheet, AccountListBottomSheet
│   │   ├── home/         # HomeScreen, HomeViewModel, Home*Delegate, HomeTokens, CitedAnswerContent
│   │   │   ├── pane/     # SourcesPane, AskPane, NotesPane, NotebookPane
│   │   │   ├── notebook/ # Editor, toolbar, print/export/clipboard side effects
│   │   │   └── bottomsheet/  # Add/Edit/Sort/Processing/Ask/Note/Export sheets, etc.
│   │   ├── sourcedetail/ # SourceDetailScreen + WebView HTML preview / original file
│   │   ├── account/      # AccountSettingsScreen (nav route from Spaces)
│   │   └── navigation/   # FolioNavHost, FolioDestination
│   └── ui/theme/         # Color, Type, Theme
├── debug/                # AuthCapabilities (backend on) + debug resources (`Folio Debug`)
├── release/              # AuthCapabilities (backend on)
├── backend/              # Shared HTTP API clients + real DataSources (debug + release)
├── test/                 # Shared unit tests + testing/ fakes
├── testDebug/            # Variant tests against debug DataSources / network clients
├── testRelease/          # Variant tests against release DataSources / production base URL
└── androidTest/          # Instrumented UI / accessibility tests
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
- `AppContainer.isAuthAvailable` mirrors `AuthCapabilities.isBackendAvailable` (true in debug and release)
- On init, `AppContainer` restores the encrypted session off the main thread, refreshes the access token if present, then sets `isSessionRestored`
- Token-aware data sources (Space / Source / Note / Ask / Notebook) receive `accessTokenProvider` + `refreshAccessToken` from `AppContainer`

### Build variants (debug vs release)

These types are **not** in `main` — they live in `debug/` / `release/` (`AuthCapabilities`) or shared `backend/` (HTTP + real data sources):

| Type | Debug | Release |
|------|-------|---------|
| `AuthCapabilities` | `isBackendAvailable = true` | `true` |
| API base URL | `https://folio.nustechnology.com` | `https://folio.nustechnology.com` |
| `AuthDataSource` | Real auth API via `AuthApiClient` (sign-up / sign-in / refresh / logout / getUser); Apple + password-reset stay local mocks | Same as debug |
| `SpaceDataSource` | Real spaces API via `SpacesApiClient` (401 → refresh once + retry) | Same as debug |
| `SourceDataSource` | Real sources API via `SourcesApiClient` for list/create/detail/retry/delete; update + some processing still local | Same as debug |
| `NoteDataSource` | Real notes API via `NotesApiClient` (list/create/detail/update/delete/convert; 401 → refresh once + retry) | Same as debug |
| `AskDataSource` | Real ask SSE + suggestions via `AskApiClient` (`POST .../ask`, `GET .../ask/suggestions`); 401 → refresh once + retry | Same as debug |
| `NotebookDataSource` | Real notebook GET/PUT via `NotebookApiClient` (`GET/PUT .../notebook`); HTML↔markdown for the editor; dirty local DataStore is source of truth for unsynced edits (`writeIfNotDirty` / `markClean`); falls back to cache on transport failures and 404; 401 → refresh once + retry | Same as debug |
| Network stack | `FolioHttp`, `FolioApiPaths`, `*ApiClient`, `HttpDebugLogger` | Same clients; `FOLIO_API_BASE_URL` from `BuildConfig` |

- `NotebookStore` remains in **main** (local DataStore cache per space)
- Put variant implementation + network tests in `testDebug` / `testRelease`; keep use-case and ViewModel tests in shared `test/` with fakes
- Keep backend-specific HTTP clients in `backend/` (compiled into debug and release) — not in `main`
- Launcher label: debug overrides `app_name` to **Folio Debug**; release/main uses **Folio** (`debug/res/values/strings.xml`)

### ViewModel pattern

- One `*UiState` data class per screen (e.g. `HomeUiState`, `LoginUiState`, `SourceDetailUiState`)
- `MutableStateFlow` + `asStateFlow()` for state
- Screens use `collectAsStateWithLifecycle()`
- Access dependencies from `LocalAppContainer.current`
- Factory constructors take the use cases / helpers the screen needs (Home wires many; Login/Space are smaller)
- Home splits feature logic into `HomeSourcesDelegate`, `HomeAskDelegate`, `HomeNotesDelegate`, `HomeNotebookDelegate`

```kotlin
@Composable
fun SpaceScreen(
    onOpenSpace: (Space) -> Unit,
    onOpenAccount: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SpaceViewModel = viewModel(
        factory = SpaceViewModel.Factory(
            getSpacesUseCase = LocalAppContainer.current.getSpacesUseCase,
            createSpaceUseCase = LocalAppContainer.current.createSpaceUseCase,
            updateSpaceUseCase = LocalAppContainer.current.updateSpaceUseCase,
            deleteSpaceUseCase = LocalAppContainer.current.deleteSpaceUseCase,
            getCurrentSessionUseCase = LocalAppContainer.current.getCurrentSessionUseCase,
            syncCurrentUserUseCase = LocalAppContainer.current.syncCurrentUserUseCase,
            refreshAuthSessionUseCase = LocalAppContainer.current.refreshAuthSessionUseCase,
        ),
    ),
) { ... }
```

### Use cases

- Single responsibility; typically `operator fun invoke(...): Result<T>` or `suspend operator fun invoke(...): Result<T>`
- Sync session helpers may return a plain value (e.g. `GetCurrentSessionUseCase` → `AuthSession?`, `ClearAuthSessionUseCase` → `Unit`)
- Streaming / Flow use cases exist where needed (e.g. `StreamAskAnswerUseCase`, `ObserveSourceProcessingUseCase`)
- Home data use cases are **space-scoped**: `getSourcesUseCase(spaceId)`, `getAskSuggestionsUseCase(spaceId, …)`, `getNotesUseCase(spaceId)`, `getNotebookUseCase(spaceId)`
- Registered in `AppContainer` as lazy properties

### Screens

- Public `*Screen` composable + private `*Content` for UI/preview
- User-facing strings in `res/values/strings.xml`
- `rememberSaveable` for form fields
- `Modifier` parameter with default, passed to root layout
- Previews wrap content in `FolioAndroidTheme`
- Home is tabbed (`HomeTab`: Sources, Ask, Notes, Notebook); Account is a separate nav route from Spaces, not a Home tab
- Source Detail is a separate nav route from Home (optional citation highlight + “ask about this source” result back to Home Ask)
- Shared UI components live in `components/` (modal sheets, toasts, skeletons, search field, empty state, loading indicators, item options)
- Domain input / formatting helpers live in `domain/util/` (e.g. `AuthInputRules`, `AddSourceInputRules`, `NoteInputRules`, `NotebookInputRules`)

### Navigation

- Routes in `FolioDestination` (`FolioNavHost.kt`): `LOGIN`, `SIGN_UP`, `RESET_PASSWORD`, `SPACES`, `ACCOUNT`, `HOME`, `SOURCE_DETAIL`
- Saved-state keys for Home ↔ Source Detail: `HOME_TAB_RESULT`, `HOME_ASK_SOURCE_RESULT`, `HOME_REFRESH_SOURCES_RESULT`
- Start destination: waits for `isSessionRestored`, then `SPACES` if signed in else `LOGIN`
- Post-auth flow: Login / Sign Up → `SPACES` → `HOME/{spaceId}?title={title}` → optional `SOURCE_DETAIL/{sourceId}?spaceId=&highlight=`
- `FolioDestination.home(...)`, `sourceDetail(...)`, `resetPassword(email)` build typed routes
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

Unit tests use fakes under `app/src/test/java/com/nus/folio/testing/` (e.g. `FakeAuthRepository`, `FakeSourceRepository`, `FakeSpaceRepository`, `FakeAskRepository`, `FakeNoteRepository`, `FakeNotebookRepository`, `MainDispatcherRule`).

## Guidelines

- Minimize scope — match existing patterns, no unrelated changes
- No over-engineering — avoid abstractions for one-off use
- No new dependencies without discussion
- Respect dependency flow: presentation → domain ← data; never import `data` from `presentation` or `domain`
- Do not add Hilt/Koin without explicit request
- Do not hardcode user-facing strings in composables
- Do not commit `.idea/` churn or secrets
- Only create git commits when explicitly asked
- Keep HTTP backend code in `backend/` (compiled into debug and release), not `main`
- API base URLs live in `BuildConfig.FOLIO_API_BASE_URL` (`https://folio.nustechnology.com` for debug and release)

## Adding a New Feature

Follow dependency direction: define contracts in `domain` first, implement in `data`, consume from `presentation`.

1. **Domain:** model → repository interface → use case
2. **Data:** data source → repository impl (implements domain interface); if HTTP is needed, add/extend debug (and later release) API clients under `data/network/`
3. **DI:** wire in `AppContainer` (data impl → use case → ViewModel factory)
4. **Presentation:** UiState → ViewModel (+ Factory) → Screen → navigation route (or Home tab/pane/sheet if it belongs on Home)

## Reference Files

| Purpose | File |
|---------|------|
| Screen + ViewModel | `presentation/home/HomeScreen.kt`, `HomeViewModel.kt` |
| Home delegates | `HomeSourcesDelegate.kt`, `HomeAskDelegate.kt`, `HomeNotesDelegate.kt`, `HomeNotebookDelegate.kt` |
| Space screen | `presentation/space/SpaceScreen.kt`, `SpaceViewModel.kt` |
| Home panes | `presentation/home/pane/SourcesPane.kt`, `AskPane.kt`, `NotesPane.kt`, `NotebookPane.kt` |
| Notebook editor | `presentation/home/notebook/` (editor, toolbar, `NotebookSideEffects`) |
| Home sheets | `presentation/home/bottomsheet/` (e.g. `AddSourceBottomSheet.kt`) |
| Source detail | `presentation/sourcedetail/SourceDetailScreen.kt`, `SourceDetailViewModel.kt` |
| Account (nav route) | `presentation/account/AccountSettingsScreen.kt` |
| Shared components | `components/` (`FolioToast`, `AnimatedModalSheet`, `FolioSkeleton`, `FolioSearchField`, …) |
| Auth use cases | `domain/usecase/SignInUseCase.kt`, `SignUpUseCase.kt`, `RefreshAuthSessionUseCase.kt`, `GetCurrentSessionUseCase.kt` |
| Home data use cases | `GetSourcesUseCase`, `CreateSourceUseCase`, `GetAskSuggestionsUseCase`, `StreamAskAnswerUseCase`, `GetNotesUseCase`, `GetNotebookUseCase`, `SaveNotebookUseCase`, … |
| Space use cases | `domain/usecase/GetSpacesUseCase.kt`, `CreateSpaceUseCase.kt`, `UpdateSpaceUseCase.kt`, `DeleteSpaceUseCase.kt` |
| Domain utils | `domain/util/AuthInputRules.kt`, `AddSourceInputRules.kt`, `NoteInputRules.kt`, `NotebookInputRules.kt` |
| Repository | `domain/repository/`, `data/repository/` |
| Session store | `data/auth/AuthSessionStore.kt` |
| Notebook store | `data/notebook/NotebookStore.kt` |
| HTTP | `backend/.../network/FolioHttp.kt`, `FolioApiPaths.kt`, `AuthApiClient.kt`, `SpacesApiClient.kt`, `SourcesApiClient.kt`, `NotesApiClient.kt`, `AskApiClient.kt`, `NotebookApiClient.kt` |
| Auth / Space / Source / Note / Ask / Notebook | `backend/.../*DataSource.kt`; `debug\|release/.../AuthCapabilities.kt` |
| DI wiring | `di/AppContainer.kt` |
| Navigation | `presentation/navigation/FolioNavHost.kt` |
| Test fakes | `test/.../testing/FakeAuthRepository.kt`, `FakeSpaceRepository.kt`, `FakeSourceRepository.kt`, `FakeNotebookRepository.kt`, … |
| Design tokens | `ui/theme/Color.kt`, `Type.kt`, `presentation/home/HomeTokens.kt`, `presentation/space/SpaceTokens.kt` |
| Polished UI | `presentation/login/LoginScreen.kt` |
