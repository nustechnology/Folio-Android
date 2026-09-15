# Folio Android

[![CI](https://github.com/nustechnology/Folio-Android/actions/workflows/ci.yml/badge.svg)](https://github.com/nustechnology/Folio-Android/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Native Android client for Folio — private research, grounded answers. Sources, notes, and citations in one private archive.

## App flow

```text
Login → Spaces → Home (per space)
         ↓
      Account
```

| Screen | Description |
|--------|-------------|
| **Login** | Sign in, sign up |
| **Spaces** | Browse and manage research spaces |
| **Home** | Tabbed workspace per space: Sources, Ask, Notes, Notebook |
| **Account** | Profile and sign-out (navigated from Spaces) |

## Tech stack

- **Kotlin 2.0** · **Jetpack Compose** · **Material 3**
- **Clean Architecture** — `presentation → domain ← data`
- **Navigation Compose** · **Coroutines** · **StateFlow**
- Manual DI via `AppContainer` + `CompositionLocal`
- Min SDK 24 · Target/Compile SDK 35 · Java 17

## Getting started

### Requirements

- Android Studio (latest stable recommended)
- JDK 17
- Android SDK 35

### Build & run

```bash
git clone https://github.com/nustechnology/Folio-Android.git
cd Folio-Android
./gradlew assembleDebug
```

Open the project in Android Studio and run the `app` configuration on an emulator or device.

### Debug login

Debug builds use mock auth with prefilled credentials:

| Field | Value |
|-------|-------|
| Email | `researcher@folio.app` |
| Password | `folio-debug` |

Release builds have no backend wired yet — the login screen shows an unavailable state.

## Build & test

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew testDebugUnitTest      # Unit tests (test/ + testDebug/)
./gradlew lintDebug              # Lint
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator)
```

CI (GitHub Actions on `main`): unit tests + lint.

## Project structure

```text
app/src/
├── main/java/com/nus/folio/
│   ├── components/       # Shared UI (sheets, toasts, loading indicators)
│   ├── data/             # DataSources, RepositoryImpl
│   ├── domain/           # Models, repository interfaces, use cases
│   ├── di/               # AppContainer, LocalAppContainer
│   ├── presentation/
│   │   ├── login/ signup/
│   │   ├── space/        # Space list & management
│   │   ├── home/         # Tabbed workspace (Sources, Ask, Notes, Notebook)
│   │   ├── account/      # Account settings
│   │   └── navigation/   # FolioNavHost, FolioDestination
│   └── ui/theme/         # Color, Type, Theme
├── debug/                # Mock AuthDataSource + AuthCapabilities
├── release/              # Auth stubs (backend unavailable)
├── test/                 # Shared unit tests + fakes
├── testDebug/            # Auth tests (debug variant)
└── testRelease/          # Auth tests (release variant)
```

## Architecture

Dependency flows inward — `presentation` and `data` both depend on `domain`:

| Layer | Depends on | Must not depend on |
|-------|------------|-------------------|
| `presentation` | `domain` | `data` |
| `domain` | — | `data`, `presentation`, Android |
| `data` | `domain` | `presentation` |

Key conventions:

- One `*UiState` + `*ViewModel` per screen; state via `StateFlow`
- Use cases registered in `AppContainer`; ViewModels receive them through `ViewModel.Factory`
- Home data is **space-scoped** (`getSourcesUseCase(spaceId)`, etc.)
- User-facing strings in `res/values/strings.xml`
- Auth backend code lives in `debug/` and `release/` source sets only

## Adding a feature

1. **Domain** — model → repository interface → use case
2. **Data** — data source → repository impl
3. **DI** — wire in `AppContainer`
4. **Presentation** — UiState → ViewModel → Screen → nav route (or Home tab/pane)

See [CLAUDE.md](CLAUDE.md) for detailed conventions, reference files, and design system tokens.

## Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

- [Report a bug](.github/ISSUE_TEMPLATE/bug_report.yml)
- [Request a feature](.github/ISSUE_TEMPLATE/feature_request.yml)
- [Security policy](SECURITY.md) — report vulnerabilities privately, not via public issues

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2026 NUS Technology
