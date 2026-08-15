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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors
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
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DhruvNextSpacing.screenGutter),
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
                        fontSize = DhruvNextType.body,
                        color = colors.tx2,
                    )
                }

            is AssistantUiState.Success ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
                ) {
                    NxCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.response,
                            fontSize = DhruvNextType.body,
                            color = colors.acc,
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
                    verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
                ) {
                    NxCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.message,
                            fontSize = DhruvNextType.body,
                            color = colors.neg,
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
    val colors = LocalDhruvNextColors.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
            ) {
                Text(
                    text = "AI Assistant",
                    fontSize = DhruvNextType.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.tx,
                )
                Text(
                    text =
                        "Your prompt will be sent to Google Gemini (an online service). " +
                            "This means your text will leave this device and be processed by Google's servers.",
                    fontSize = DhruvNextType.body,
                    color = colors.tx2,
                )
                Text(
                    text = "By continuing you consent to this data transfer as required under DPDP Rules 2025.",
                    fontSize = DhruvNextType.meta,
                    color = colors.tx2,
                )
                NxButton(
                    text = "I understand — Continue",
                    onClick = onGrantConsent,
                    modifier = Modifier.fillMaxWidth(),
                )
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
    ) {
        NxTextField(
            value = text,
            onValueChange = { text = it },
            label = "Ask Gemini…",
            placeholder = "Type your question",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        NxButton(
            text = "Send",
            onClick = {
                onAsk(text)
                text = ""
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Consent gate — light", showBackground = true)
@Composable
private fun PreviewConsentLight() {
    DhruvTheme {
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
    DhruvTheme {
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
    DhruvTheme {
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
    DhruvTheme {
        AssistantContent(
            state = AssistantUiState.Error("Network error. Please check your internet connection and try again."),
            onGrantConsent = {},
            onAsk = {},
        )
    }
}
