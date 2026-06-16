package com.dhruv.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable AI assistant settings section — Gemini API key management.
 *
 * No finance dependencies. Callback-driven.
 *
 * @param geminiApiKey Current stored key, or null if not set.
 * @param onKeyChanged Called when the user finishes entering a new key (non-blank).
 * @param onClearKey Called when the user taps "Clear key".
 */
@Composable
fun AiAssistantSection(
    geminiApiKey: String?,
    onKeyChanged: (String) -> Unit,
    onClearKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    var keyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey ?: "") }
    var keyVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Gemini API Key",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "Optional. Your key bypasses the shared usage limit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API key") },
            placeholder = { Text("Paste your Gemini key here") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (keyVisible) "Hide key" else "Show key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (geminiApiKey != null) {
                TextButton(onClick = {
                    keyInput = ""
                    onClearKey()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Text("Clear key")
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            if (keyInput.isNotBlank() && keyInput != geminiApiKey) {
                TextButton(onClick = { onKeyChanged(keyInput.trim()) }) {
                    Text("Save key")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
