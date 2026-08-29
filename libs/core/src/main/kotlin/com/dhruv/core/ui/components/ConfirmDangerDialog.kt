package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A destructive-action confirmation — "Delete everything", "Delete my account", etc.
 *
 * [typeToConfirmText], when non-null, requires the user to type that exact string into a field
 * before the confirm button becomes enabled — for the highest-severity account-level deletions
 * (design system §5.1: "account-level deletions use type-to-confirm").
 */
@Composable
fun ConfirmDangerDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    typeToConfirmText: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    var typed by remember(typeToConfirmText) { mutableStateOf("") }
    val confirmEnabled = typeToConfirmText == null || typed == typeToConfirmText

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column {
                Text(body)
                if (typeToConfirmText != null) {
                    NxTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        placeholder = typeToConfirmText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(text = confirmLabel, color = if (confirmEnabled) colors.neg else colors.tx3)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}
