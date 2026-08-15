package com.dhruv.core.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext's trash/undo pattern (`platform/DESIGN-SYSTEM.md` §5/§8): a [SnackbarHost] styled to the token
 * surface. Material3's [SnackbarDuration] is an enum (no raw millis) — [SnackbarDuration.Long]
 * (~10s) is the closest fit to the spec's "5s with Undo" and errs on the side of not missing it.
 */
@Composable
fun UndoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            containerColor = colors.tx,
            contentColor = colors.bg,
            action = {
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = { data.performAction() }) {
                        Text(text = label, color = colors.acc)
                    }
                }
            },
        ) {
            Text(data.visuals.message)
        }
    }
}

/** Shows a message with an "Undo" action; invokes [onUndo] if the user taps it. */
suspend fun SnackbarHostState.showUndoSnackbar(
    message: String,
    onUndo: () -> Unit,
) {
    val result = showSnackbar(message = message, actionLabel = "Undo", duration = SnackbarDuration.Long)
    if (result == SnackbarResult.ActionPerformed) onUndo()
}
