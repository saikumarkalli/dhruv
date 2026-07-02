package com.dhruv.finance.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AssistantContent(
        state = uiState,
        onGrantConsent = viewModel::grantConsent,
        onAsk = viewModel::ask,
        modifier = modifier,
    )
}

@Composable
private fun AssistantContent(
    state: AssistantUiState,
    onGrantConsent: () -> Unit,
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        when (state) {
            is AssistantUiState.ConsentNeeded -> ConsentGate(onGrantConsent = onGrantConsent)

            is AssistantUiState.Idle -> PromptInput(onAsk = onAsk, previousResponse = null)

            is AssistantUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Thinking…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            is AssistantUiState.Success ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                    ) {
                        Text(
                            text = state.response,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    PromptInput(onAsk = onAsk, previousResponse = state.response)
                }

            is AssistantUiState.Error ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    PromptInput(onAsk = onAsk, previousResponse = null)
                }
        }
    }
}

/** DPDP consent gate — shown before any data is sent to Google Gemini. */
@Composable
private fun ConsentGate(
    onGrantConsent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        "Your prompt will be sent to Google Gemini (an online service). " +
                            "This means your text will leave this device and be processed by Google's servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "By continuing you consent to this data transfer as required under DPDP Rules 2025.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onGrantConsent,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("I understand — Continue")
                }
            }
        }
    }
}

/** Text input + send button shown once consent is granted. */
@Composable
private fun PromptInput(
    onAsk: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") previousResponse: String?,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Ask Gemini…") },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(
                onClick = {
                    onAsk(text)
                    text = ""
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint =
                        if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            }
        },
        maxLines = 4,
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Consent gate — light", showBackground = true)
@Composable
private fun PreviewConsentLight() {
    MaterialTheme {
        AssistantContent(
            state = AssistantUiState.ConsentNeeded,
            onGrantConsent = {},
            onAsk = {},
        )
    }
}

@Preview(name = "Consent gate — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConsentDark() {
    MaterialTheme {
        AssistantContent(
            state = AssistantUiState.ConsentNeeded,
            onGrantConsent = {},
            onAsk = {},
        )
    }
}

@Preview(name = "Success — light", showBackground = true)
@Composable
private fun PreviewSuccessLight() {
    MaterialTheme {
        AssistantContent(
            state =
                AssistantUiState.Success(
                    "The expression 2 + 2 equals 4. This is simple addition — combining two quantities of two gives a total of four.",
                ),
            onGrantConsent = {},
            onAsk = {},
        )
    }
}

@Preview(name = "Error — light", showBackground = true)
@Composable
private fun PreviewErrorLight() {
    MaterialTheme {
        AssistantContent(
            state = AssistantUiState.Error("Network error. Please check your internet connection and try again."),
            onGrantConsent = {},
            onAsk = {},
        )
    }
}
