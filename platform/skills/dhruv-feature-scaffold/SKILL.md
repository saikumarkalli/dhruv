---
name: dhruv-feature-scaffold
description: Scaffold a new Finance feature module in the Dhruv monorepo. Use whenever the user asks to "add a feature module", "create a new feature", "scaffold a calculator/loans/currency/... feature", or any request to add a new Gradle module under apps/finance/feature/. Composes dhruv-compose-screen, dhruv-room-entity, and dhruv-module-audit. Always read this skill BEFORE creating a finance feature module.
---

# Dhruv Finance Feature Scaffold

Creates a complete, platform-compliant feature module under `apps/finance/feature/<name>/`.
DI is **Koin** (not Hilt). Theme access is `MaterialTheme.colorScheme` (not `DhruvTheme.colors`).
Crash recording uses `crashReporter.recordException(e)` (not `crashReporter.report(e)`).

> **Stale skill warning**: `dhruv-compose-screen` SKILL.md still references Hilt/`hiltViewModel`/
> `DhruvTheme.colors`/`crashReporter.report`. Those are wrong for this codebase. Use the patterns
> in THIS skill instead.

---

## 1. Directory layout

```
apps/finance/feature/<name>/
├── build.gradle.kts
├── consumer-rules.pro          (empty — required by dhruv.android.library convention plugin)
└── src/
    └── main/
        ├── AndroidManifest.xml
        └── java/com/dhruv/finance/<name>/
            ├── <Name>NavGraph.kt
            ├── <Name>Screen.kt
            ├── <Name>ViewModel.kt
            ├── <Name>UiState.kt
            └── di/
                └── <Name>Module.kt
```

---

## 2. build.gradle.kts

```kotlin
plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
    // add alias(libs.plugins.google.devtools.ksp) only if this module owns Room entities
}

android {
    namespace = "com.dhruv.finance.<name>"
}

dependencies {
    implementation(project(":apps:finance:data"))
    implementation(project(":libs:core"))
    implementation(project(":libs:settings"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Lifecycle / Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
```

Do NOT apply `id("dhruv.hilt")`. The Hilt plugin is incompatible with AGP 9 and the app is fully on Koin.

---

## 3. AndroidManifest.xml (minimal)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

The `time` module is the only exception — it adds `<receiver>`, `<service>`, and `<activity>` entries for alarm components that manifest-merge into the app.

---

## 4. NavGraph extension

```kotlin
package com.dhruv.finance.<name>

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

const val <NAME>_ROUTE = "<name>"

fun NavGraphBuilder.<name>Graph(navController: NavHostController) {
    composable(<NAME>_ROUTE) {
        <Name>Screen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

---

## 5. UiState

```kotlin
package com.dhruv.finance.<name>

sealed interface <Name>UiState {
    data object Loading : <Name>UiState
    data class Success(/* feature-specific data */) : <Name>UiState
    data class Error(val message: String) : <Name>UiState
}
```

---

## 6. ViewModel

```kotlin
package com.dhruv.finance.<name>

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class <Name>ViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
    // inject repositories from :apps:finance:data
) : ViewModel() {

    private val _uiState = MutableStateFlow<<Name>UiState>(<Name>UiState.Loading)
    val uiState: StateFlow<<Name>UiState> = _uiState.asStateFlow()

    // Expose uncaught feature errors so FeatureHost can render FeatureErrorCard
    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        crashReporter.recordException(throwable)
        _featureError.value = throwable
    }

    init {
        crashReporter.setModule("<name>")
        load()
    }

    private fun load() {
        viewModelScope.launch(errorHandler) {
            performanceTracer.trace("<name>_load") {
                // load data from repository
                _uiState.value = <Name>UiState.Success(/* ... */)
            }
        }
    }
}
```

---

## 7. Screen

```kotlin
package com.dhruv.finance.<name>

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun <Name>Screen(
    viewModel: <Name>ViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    <Name>Content(
        state = uiState,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun <Name>Content(
    state: <Name>UiState,
    onNavigateBack: () -> Unit,
) {
    // Use MaterialTheme.colorScheme (NOT DhruvTheme.colors — that API does not exist)
    // Use MaterialTheme.typography for text styles
    when (state) {
        is <Name>UiState.Loading -> { /* loading indicator */ }
        is <Name>UiState.Error   -> { /* error card */ }
        is <Name>UiState.Success -> { /* main content */ }
    }
}
```

---

## 8. Koin module

```kotlin
package com.dhruv.finance.<name>.di

import com.dhruv.finance.<name>.<Name>ViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val <name>Module = module {
    viewModel { <Name>ViewModel(get(), get() /*, get() for repos */) }
}
```

Wire this module in `CalculatorApplication.startKoin { modules(..., <name>Module) }`.

---

## 9. Feature flag entry

Add to `platform/feature-flags/dhruv-finance.json`:

```json
"<name>": { "enabled": true, "minVersion": "1.0.0" }
```

Disabled features (e.g. `date`, `time`, `assistant`) use `"enabled": false`.
Features requiring user consent (e.g. `assistant`) add `"requiresConsent": true`.

---

## 10. FeatureHost wrapping (in the app shell)

Every route rendered in `MainActivity`/`NavHost` **must** be wrapped:

```kotlin
FeatureHost(
    featureKey = "<name>",
    isEnabled = resolver.isEnabled("<name>"),
    featureError = viewModel.featureError.collectAsStateWithLifecycle().value,
    crashReporter = crashReporter,
) {
    <Name>Screen(...)
}
```

`FeatureHost`, `FeatureErrorCard`, and `FeatureDisabledCard` live in `:libs:core`.

---

## 11. Register the module in settings.gradle.kts

```kotlin
include(":apps:finance:feature:<name>")
```

---

## 12. Checklist

- [ ] `build.gradle.kts` uses `dhruv.android.library` + `dhruv.android.compose`; no `dhruv.hilt`
- [ ] `namespace = "com.dhruv.finance.<name>"`
- [ ] `consumer-rules.pro` file present (may be empty)
- [ ] `AndroidManifest.xml` present (minimal `<manifest />`)
- [ ] Included in `settings.gradle.kts`
- [ ] Per-module Koin `module {}` registered in Application
- [ ] ViewModel `init` calls `crashReporter.setModule("<name>")`
- [ ] At least one `performanceTracer.trace("<name>_...")` call
- [ ] `featureError: StateFlow<Throwable?>` exposed on ViewModel
- [ ] Route wrapped in `FeatureHost` in the app shell
- [ ] Feature flag entry in `dhruv-finance.json`
- [ ] `./gradlew :apps:finance:feature:<name>:assembleDebug` passes
- [ ] Pre-merge: run `platform/skills/dhruv-module-audit/SKILL.md` checklist

---

## Composed skills

| Task | Skill |
|------|-------|
| Adding Room persistence to the feature | `dhruv-room-entity` |
| Creating screen + ViewModel bodies | `dhruv-compose-screen` (use Koin patterns from THIS skill, not Hilt patterns from that one) |
| Pre-merge validation | `dhruv-module-audit` |
