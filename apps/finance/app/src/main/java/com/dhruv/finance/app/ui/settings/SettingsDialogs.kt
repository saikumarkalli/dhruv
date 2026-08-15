package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

@Composable
fun LocaleFormatDialog(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Separators Format", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val locales =
                    listOf(
                        "international" to "International (1,000,000)",
                        "indian" to "Indian (10,00,000)",
                    )
                locales.forEach { (key, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLocaleSelected(key) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                    ) {
                        RadioButton(
                            selected = currentLocale == key,
                            onClick = { onLocaleSelected(key) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(DhruvNextRadii.card),
    )
}

@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear history?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This permanently deletes saved calculation logs on this device.",
                fontSize = DhruvNextType.cardTitle,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.neg),
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(DhruvNextRadii.card),
    )
}

/** Confirmation for "Delete my data" (ONB-BR-008) — same shape as [ClearHistoryDialog], scoped to
 * the tracker's server-side rows rather than on-device calculator history. The account/session
 * stays signed in; only [DeleteMyAccountDialog] forces sign-out. */
@Composable
fun DeleteMyDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete my data?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This permanently erases your net worth holdings and valuations from the server. " +
                    "Your account stays signed in and calculators keep working offline.",
                fontSize = DhruvNextType.cardTitle,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.neg),
            ) {
                Text("Delete data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(DhruvNextRadii.card),
    )
}

/** Confirmation for "Delete my account" (ONB-BR-009) — unlike [DeleteMyDataDialog], this also
 * forces sign-out and sends the user back through onboarding on next launch, so the copy says so
 * explicitly rather than only describing the data that's erased. */
@Composable
fun DeleteMyAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete my account?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "This permanently erases all your tracker data and your account, then signs you out. " +
                    "You'll go through setup again the next time you open the app.",
                fontSize = DhruvNextType.cardTitle,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.neg),
            ) {
                Text("Delete account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(DhruvNextRadii.card),
    )
}

@Composable
fun PinEntryDialog(
    onPinSaved: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var tempPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val colors = LocalDhruvNextColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Log Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a 4-digit numeric code to unlock your private log records:", fontSize = DhruvNextType.body)
                OutlinedTextField(
                    value = tempPin,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                            tempPin = newVal
                            pinError = null
                        }
                    },
                    label = { Text("Enter 4-digit PIN") },
                    isError = pinError != null,
                    supportingText = {
                        if (pinError != null) {
                            Text(pinError!!, color = colors.neg)
                        } else {
                            Text("Only numeric digits are permitted")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("e.g., 1234") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_pin_textfield"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tempPin.length == 4) {
                        onPinSaved(tempPin)
                    } else {
                        pinError = "PIN must be exactly 4 digits long."
                    }
                },
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        shape = RoundedCornerShape(DhruvNextRadii.card),
    )
}
