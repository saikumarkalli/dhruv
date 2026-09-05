package com.dhruv.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * FR-005/N4's "confirm before discarding unsaved changes" — Phase 0 explicitly descoped this
 * helper until a form actually needed it (contracts/routes.md); D3 (002-money-tab) is that form.
 * [isDirty] is read fresh each time [DiscardGuard.attemptDismiss] is called, so the caller's own
 * `isDirty` state (typically a ViewModel field, not a `remember`) drives the decision.
 */
class DiscardGuard internal constructor(
    private val isDirty: () -> Boolean,
    private val onDiscardConfirmed: () -> Unit,
    private val showConfirm: () -> Unit,
) {
    /** Call this instead of dismissing directly — dismisses immediately if nothing changed,
     * otherwise shows the confirm dialog via [showConfirm] and only calls [onDiscardConfirmed]
     * once the user actually confirms. */
    fun attemptDismiss() {
        if (isDirty()) showConfirm() else onDiscardConfirmed()
    }
}

/**
 * Wires a [DiscardGuard] plus the [ConfirmDangerDialog] it shows when [isDirty] is true at
 * dismiss time. Render the returned composable's dialog slot in the caller (it renders nothing
 * when not showing) and call [DiscardGuard.attemptDismiss] from the screen's close affordance.
 */
@Composable
fun rememberDiscardGuard(
    isDirty: () -> Boolean,
    onDiscardConfirmed: () -> Unit,
): Pair<DiscardGuard, @Composable () -> Unit> {
    var showing by remember { mutableStateOf(false) }
    val guard =
        remember(isDirty, onDiscardConfirmed) {
            DiscardGuard(isDirty, onDiscardConfirmed, showConfirm = { showing = true })
        }
    val dialog: @Composable () -> Unit = {
        if (showing) {
            ConfirmDangerDialog(
                title = "Discard changes?",
                body = "You have unsaved changes. Discarding will lose them.",
                confirmLabel = "Discard",
                onConfirm = {
                    showing = false
                    onDiscardConfirmed()
                },
                onDismiss = { showing = false },
            )
        }
    }
    return guard to dialog
}
