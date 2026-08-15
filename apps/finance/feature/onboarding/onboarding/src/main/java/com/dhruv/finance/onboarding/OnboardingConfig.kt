package com.dhruv.finance.onboarding

/**
 * Every user-facing string for the onboarding flow (A2 sign-in, A3 DPDP consent, A4 empty start),
 * taken from functional spec §5 Group A as close to verbatim as the spec's prose allows — the
 * ViewModel and its screens (Task 3) must never hardcode copy (project no-hardcoding rule).
 * Strings the spec states as narrative requirements rather than exact on-screen copy (e.g. "Terms/
 * Privacy links", the retention/erasure block's body) are the minimal literal UI label implied by
 * that requirement, not embellished marketing copy.
 */
object OnboardingConfig {
    // --- A2 — Sign-in ---
    const val SIGN_IN_TAGLINE = "Every rupee you own, in one place."
    const val SIGN_IN_GOOGLE_CTA = "Continue with Google"
    const val SIGN_IN_OFFLINE_CTA = "Use offline — calculators only"
    const val SIGN_IN_TERMS_LABEL = "Terms of Service"
    const val SIGN_IN_PRIVACY_LABEL = "Privacy Policy"

    // A2 failure copy — each distinct root cause gets distinct wording rather than one generic
    // catch-all (found live: a device with broken DNS but a connected radio showed the same
    // "cancelled or unavailable" text as an actual user cancellation, which is not actionable for
    // the person reading it — "check your connection" tells them what to actually go do).
    const val SIGN_IN_ERROR_OFFLINE = "You're offline. Check your connection and try again."
    const val SIGN_IN_ERROR_CANCELLED = "Sign-in was cancelled or unavailable. Try again."
    const val SIGN_IN_ERROR_WRONG_ACCOUNT = "Couldn't sign in with that account. Try again."
    const val SIGN_IN_ERROR_BACKEND = "Couldn't finish signing in. Try again."
    const val SIGN_IN_ERROR_GENERIC = "Something went wrong. Try again."

    // --- A3 — DPDP consent ---
    const val CONSENT_HEADER = "Nothing syncs until you switch it on. Calculators always work offline."

    const val CONSENT_SYNC_LABEL = "Sync my financial records"
    const val CONSENT_SYNC_SCOPE = "Required for the net worth tracker to work."

    const val CONSENT_SMS_LABEL = "Read transaction SMS"
    const val CONSENT_SMS_SCOPE = "Parsed on device. You approve each one."

    const val CONSENT_ASK_DHRUV_LABEL = "Ask Dhruv about my money"
    const val CONSENT_ASK_DHRUV_SCOPE = "Anonymised summaries only — never your account numbers."

    const val CONSENT_RETENTION_TITLE = "Data retention & erasure"
    const val CONSENT_RETENTION_BODY =
        "Delete your data or your account any time from Settings › Privacy. Erasure is guaranteed within 7 days."

    const val CONSENT_CONTINUE_CTA = "Continue"

    /** [ConsentSwitch]-keyed label + scope-statement pairs, in the exact A3 display order. */
    val consentSwitchCopy: List<ConsentSwitchCopy> =
        listOf(
            ConsentSwitchCopy(ConsentSwitch.SYNC_FINANCIAL_RECORDS, CONSENT_SYNC_LABEL, CONSENT_SYNC_SCOPE),
            ConsentSwitchCopy(ConsentSwitch.READ_TRANSACTION_SMS, CONSENT_SMS_LABEL, CONSENT_SMS_SCOPE),
            ConsentSwitchCopy(ConsentSwitch.ASK_DHRUV_ABOUT_MONEY, CONSENT_ASK_DHRUV_LABEL, CONSENT_ASK_DHRUV_SCOPE),
        )

    // --- A4 — Empty start ---
    const val EMPTY_START_TASK_1_TITLE = "Add your first account"
    const val EMPTY_START_TASK_2_TITLE = "Record what you own"
    const val EMPTY_START_CSV_CTA = "Import a CSV"
    const val EMPTY_START_CSV_SUBTITLE = "Bank statement or old spreadsheet"

    // Fix 1 (final whole-branch review) — A4's exit affordance. Every signed-in user genuinely
    // sees A4 (Phase 2's accounts/holdings repository doesn't exist yet); this is what guarantees
    // an exit to the shell exists rather than skipping A4 itself.
    const val EMPTY_START_SKIP_CTA = "Skip for now"
}

/** One A3 switch's display copy — [OnboardingConfig.consentSwitchCopy]. */
data class ConsentSwitchCopy(
    val switch: ConsentSwitch,
    val label: String,
    val scopeStatement: String,
)
