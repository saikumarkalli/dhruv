package com.dhruv.finance.onboarding

import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState

/**
 * One case per onboarding screen (functional spec §5 Group A: A2 sign-in, A3 consent, A4 empty
 * start). [OnboardingViewModel.exitToShell] is the separate signal for leaving this flow entirely
 * once it's done — it is orthogonal to which of these three screens is currently shown.
 */
sealed interface OnboardingUiState {
    data object SignIn : OnboardingUiState

    data class Consent(
        val switches: ConsentState,
        val isSubmitting: Boolean,
    ) : OnboardingUiState

    data class EmptyStart(
        val hasAccountOrHolding: Boolean,
    ) : OnboardingUiState
}

/**
 * The three independently-persisted, revocable A3 switches (functional spec §5 Group A). The
 * fourth item in that row — the data-retention/erasure block — is informational-only copy, not a
 * toggle: see [ConsentRepository]'s own doc comment, which deliberately does not represent it as
 * a flag.
 */
enum class ConsentSwitch {
    SYNC_FINANCIAL_RECORDS,
    READ_TRANSACTION_SMS,
    ASK_DHRUV_ABOUT_MONEY,
}
