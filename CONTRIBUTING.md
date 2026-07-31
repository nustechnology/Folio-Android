# Contributing to Folio Android

Thank you for your interest in contributing! This document covers how to get set up, submit changes, and follow project conventions.

## Getting started

1. Fork the repository and clone your fork.
2. Open the project in Android Studio (JDK 17, Android SDK 35).
3. Build and run tests before making changes:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

4. Debug builds use mock auth — see [README.md](README.md#debug-login) for credentials.

## How to contribute

### Reporting bugs

Open a [bug report](.github/ISSUE_TEMPLATE/bug_report.yml) with:

- Steps to reproduce
- Expected vs actual behavior
- Device/emulator info (Android version, screen size if relevant)
- Screenshots or logs if available

### Suggesting features

Open a [feature request](.github/ISSUE_TEMPLATE/feature_request.yml) describing the problem and proposed solution.

### Pull requests

1. Create a branch from `main` with a descriptive name (e.g. `fix/login-error-state`, `feat/space-search`).
2. Keep changes focused — one concern per PR.
3. Follow existing patterns in the codebase (see [CLAUDE.md](CLAUDE.md)).
4. Add or update unit tests for behavior changes.
5. Ensure CI passes:

```bash
./gradlew testDebugUnitTest lintDebug
```

6. Fill out the PR template completely.

## Code conventions

- **Architecture:** Clean Architecture — `presentation → domain ← data`. Never import `data` from `presentation` or `domain`.
- **Strings:** User-facing text goes in `res/values/strings.xml`, not hardcoded in composables.
- **State:** One `*UiState` per screen; ViewModels use `StateFlow`.
- **DI:** Wire dependencies through `AppContainer`; ViewModels receive use cases via `ViewModel.Factory`.
- **Auth:** Backend-specific auth code stays in `debug/` and `release/` source sets only.
- **Scope:** Minimize diff size; avoid unrelated refactors or new dependencies without discussion.

See [CLAUDE.md](CLAUDE.md) for detailed architecture, navigation, design tokens, and reference files.

## Adding a feature

1. **Domain** — model → repository interface → use case
2. **Data** — data source → repository impl
3. **DI** — register in `AppContainer`
4. **Presentation** — UiState → ViewModel → Screen → navigation route (or Home tab/pane)

## Code of conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By participating, you agree to uphold it.

## Questions

Open a [GitHub Discussion](https://github.com/nustechnology/Folio-Android/discussions) or issue if you're unsure about an approach before starting large changes.
