---
name: dhruv-feature-scaffold
description: Scaffold a new Dhruv feature module with all required files. Use whenever the user says "create feature", "add feature", "scaffold", "new module", "add a X feature to tools/finance/vault", or any request to add a new capability to a Dhruv app. Also triggers on "stub out", "wire up", "add route for". This skill creates the entire module — build.gradle, Screen, ViewModel, Navigation, Hilt module, flag entry, Crashlytics tag, Performance trace, test stubs, and ArchUnit compliance — in one pass. Always use this instead of creating files manually.
---

# Dhruv Feature Scaffold

Creates a complete, architecture-compliant feature module inside the Dhruv monorepo.

## Before you start

1. Read `platform/AGENTS.md` — confirm you know the hard rules.
2. Identify: which app (`finance`, `tools`, `vault`) and what feature name.
3. If vault: remember vault features have NO network/ai/analytics deps.

## What gets created

For a feature called `{name}` in app `{app}`:

```
apps/{app}/feature/{name}/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── java/com/dhruv/{app}/feature/{name}/
    │       ├── {Name}Screen.kt          # Compose UI
    │       ├── {Name}ViewModel.kt       # StateFlow-based VM
    │       ├── {Name}Navigation.kt      # NavGraphBuilder extension
    │       ├── {Name}UiState.kt         # Sealed interface for UI states
    │       └── di/
    │           └── {Name}Module.kt      # Hilt module (if data deps exist)
    └── test/
        └── java/com/dhruv/{app}/feature/{name}/
            └── {Name}ViewModelTest.kt   # Unit test stub
```

Plus updates to:
- `settings.gradle.kts` — add module include
- `apps/{app}/app/build.gradle.kts` — add dependency
- `apps/{app}/app/.../NavHost` — add FeatureHost-wrapped route
- `platform/feature-flags/dhruv-{app}.json` — add flag entry

## Templates

### build.gradle.kts
```kotlin
plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
    id("dhruv.hilt")
}

android {
    namespace = "com.dhruv.{app}.feature.{name}"
}

dependencies {
    implementation(project(":libs:core"))
    // Add feature-specific deps here
}
```

### {Name}Screen.kt
```kotlin
package com.dhruv.{app}.feature.{name}

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun {Name}Screen(
    viewModel: {Name}ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is {Name}UiState.Loading -> {
            // Loading indicator from :libs:core
        }
        is {Name}UiState.Success -> {
            {Name}Content(state = state)
        }
        is {Name}UiState.Error -> {
            // Error display from :libs:core
        }
    }
}

@Composable
private fun {Name}Content(state: {Name}UiState.Success) {
    // TODO: Implement UI with glassmorphism tokens from :libs:core
}
```

### {Name}ViewModel.kt
```kotlin
package com.dhruv.{app}.feature.{name}

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.crash.CrashReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class {Name}ViewModel @Inject constructor(
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow<{Name}UiState>({Name}UiState.Loading)
    val uiState: StateFlow<{Name}UiState> = _uiState.asStateFlow()

    init {
        crashReporter.setModule("{name}")
        // TODO: Initialize feature
    }
}
```

### {Name}UiState.kt
```kotlin
package com.dhruv.{app}.feature.{name}

sealed interface {Name}UiState {
    data object Loading : {Name}UiState
    data class Success(/* TODO: add fields */) : {Name}UiState
    data class Error(val message: String) : {Name}UiState
}
```

### {Name}Navigation.kt
```kotlin
package com.dhruv.{app}.feature.{name}

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val {NAME}_ROUTE = "{name}"

fun NavGraphBuilder.{name}Screen() {
    composable(route = {NAME}_ROUTE) {
        {Name}Screen()
    }
}
```

### NavHost wiring (add to existing NavHost in apps/{app}/app)
```kotlin
// Inside NavHost { ... }
FeatureHost(featureKey = "{name}") {
    {name}Screen()
}
```

### Feature flag entry (add to platform/feature-flags/dhruv-{app}.json)
```json
"{name}": { "enabled": true, "minVersion": "CURRENT_VERSION" }
```
If the feature requires consent (sends data off-device), add `"requiresConsent": true`.

### {Name}ViewModelTest.kt
```kotlin
package com.dhruv.{app}.feature.{name}

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class {Name}ViewModelTest {

    @Test
    fun `initial state is Loading`() = runTest {
        // TODO: Create VM with fake dependencies
        // assertEquals({Name}UiState.Loading, viewModel.uiState.value)
    }
}
```

## Checklist (verify before done)

- [ ] Module compiles: `./gradlew :apps:{app}:feature:{name}:assembleDebug`
- [ ] App compiles with new module: `./gradlew :apps:{app}:app:assembleDebug`
- [ ] settings.gradle.kts includes the module
- [ ] App build.gradle.kts depends on the module
- [ ] NavHost has FeatureHost-wrapped route
- [ ] Feature flag entry exists in dhruv-{app}.json
- [ ] CrashReporter.setModule("{name}") called in ViewModel init
- [ ] No feature→feature dependencies (ArchUnit will catch this)
- [ ] If vault feature: no network/ai/analytics imports
- [ ] Test stub exists and passes

## Common variations

**Feature with Room data**: also create `{Name}Entity.kt` (implementing DhruvEntity),
`{Name}Dao.kt`, `{Name}Repository.kt` interface, and `{Name}RepositoryImpl.kt`. Use the
`dhruv-room-entity` skill for the data layer.

**Feature with AI**: depend on `AiProviderResolver` from `:libs:core`. Add
`requiresConsent: true` to the flag. Wire `ConsentManager` check before any AI call.

**Vault feature**: FORBIDDEN deps on network, ai, analytics. Entity does NOT implement
DhruvEntity. FLAG_SECURE on all screens.
