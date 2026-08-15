# Android App SDD (03)

> **Status:** ACTIVE
> **Scope:** Documents the existing Android architecture for reference in multi-platform alignment.

## 1. Module Architecture

The Android app follows a strict modular structure utilizing Koin for dependency injection.

```
:apps:finance:app          (Shell, MainActivity, Navigation, Koin)
:apps:finance:data         (Repositories, Room DAOs, Retrofit API)
:apps:finance:feature:*    (Independent feature modules e.g. networth, expenses, calculator)
:libs:settings             (EncryptedDataStore for secure preferences)
:libs:core                 (Design system, FeatureHost, Base components)
```

**Dependency Rule:** Feature modules depend on `:libs:core` and `:data` interfaces. Feature modules **never** depend on other feature modules.

## 2. Data Layer

- **Remote**: Retrofit with Moshi for JSON parsing, interceptors for Supabase JWT injection.
- **Local**: Room DB for calculator history and currency rate caching.
- **State**: Repositories expose `Flow<T>` or suspend functions to ViewModels.

## 3. UI & State Management

- **Pattern**: MVVM with `StateFlow`.
- **Navigation**: Custom `NavigationDispatcher` and sealed `NavTarget` classes. No Androidx Navigation graph. Single Activity with `HorizontalPager` for root tabs.
- **Components**: Strict usage of `:libs:core` components (`BentoCard`, `MoneyText`, etc.) following the Design Standard.

## 4. Security Model (8-Layer)

1. **Authentication**: Google Credential Manager (ID Token).
2. **Transport**: Certificate Pinning via OkHttp (ISRG Root X1/X2).
3. **App Lock**: BiometricPrompt Class 3 with fallback (R3 phase).
4. **Obfuscation**: R8 and ProGuard in release builds.
5. **Storage**: EncryptedDataStore for tokens.
6. **Screen Security**: `FLAG_SECURE` on sensitive routes (R3 phase).
7. **Privacy Mode**: Visual masking of financial values.
8. **Integrity**: Play Integrity API (warn-only).

## 5. Build & CI Pipeline

- Managed via GitHub Actions (`ci.yml`).
- **Gates**: Spotless (format), Detekt (lint), Unit Tests (JaCoCo), Release Build.
- **Artifacts**: Auto-versioning via commit history resulting in an APK attached to a GitHub Release.
