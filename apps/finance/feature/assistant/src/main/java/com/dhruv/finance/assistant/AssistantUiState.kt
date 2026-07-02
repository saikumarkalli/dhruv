package com.dhruv.finance.assistant

/**
 * UI state for the standalone AI assistant screen.
 *
 * [ConsentNeeded] is the initial state: a DPDP consent gate must be shown before any
 * network call is made to Gemini. Only after [AssistantViewModel.grantConsent] is called
 * does the state transition to [Idle] and further interactions become available.
 */
sealed interface AssistantUiState {
    /** User has not yet granted consent for online Gemini calls (DPDP gate). */
    data object ConsentNeeded : AssistantUiState

    /** Consent granted; waiting for the user to type a prompt. */
    data object Idle : AssistantUiState

    /** A Gemini request is in-flight. */
    data object Loading : AssistantUiState

    /** Gemini returned a non-blank response. */
    data class Success(
        val response: String,
    ) : AssistantUiState

    /** The call failed or the API key is unconfigured. */
    data class Error(
        val message: String,
    ) : AssistantUiState
}
