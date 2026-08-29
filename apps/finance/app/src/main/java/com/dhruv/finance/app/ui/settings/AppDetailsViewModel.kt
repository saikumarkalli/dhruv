package com.dhruv.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The three honest outcomes FR-040 requires — never silently "current" on a failure. */
sealed interface UpdateCheckResult {
    data object Current : UpdateCheckResult

    data class Available(
        val version: String,
    ) : UpdateCheckResult

    data class Failed(
        val reason: String,
    ) : UpdateCheckResult
}

/**
 * The update source. No implementation is wired in production yet (T102 — no update channel
 * exists: distribution is a signed APK via GitHub Releases, ADR-0008, with no in-app check against
 * it today) — `AppDetailsViewModel(updateChecker = null)` is what App details actually runs with,
 * which is what makes the row absent rather than inert (FR-043).
 */
fun interface UpdateChecker {
    suspend fun check(): UpdateCheckResult
}

/**
 * App details (0b.4, T099-T102). Takes the installed version as plain values, not a `Context` —
 * matches this codebase's pattern of keeping ViewModels Android-Context-free and testable
 * (`AppSettingsViewModel`'s `hasEnrolledCredential` lambda is the same shape). The real
 * `versionName`/`versionCode` are read from `PackageInfo` in `appModule`'s Koin definition.
 *
 * **Resolve this via `koinViewModel()`, never `remember { AppDetailsViewModel(...) }`.** It was
 * originally constructed with `remember` in `AppSettingsScreen`, which never puts it in a
 * `ViewModelStore` — so `onCleared()` never ran and [viewModelScope] was never cancelled. That was
 * dormant only because the shipped `updateChecker` is `null` and [checkForUpdate] returns before
 * launching anything; wiring a real checker would have made it a live coroutine leak.
 */
class AppDetailsViewModel(
    val versionName: String,
    val versionCode: Long,
    private val updateChecker: UpdateChecker?,
) : ViewModel() {
    val updateCheckAvailable: Boolean = updateChecker != null

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    fun checkForUpdate() {
        val checker = updateChecker ?: return
        viewModelScope.launch {
            _updateCheckResult.value =
                runCatching { checker.check() }
                    .getOrElse { e -> UpdateCheckResult.Failed(e.localizedMessage ?: "Update check failed") }
        }
    }
}
