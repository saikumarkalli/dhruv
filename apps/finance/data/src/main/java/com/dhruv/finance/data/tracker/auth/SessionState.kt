package com.dhruv.finance.data.tracker.auth

/**
 * Observable auth state for the tracker domain (ADR-0029). Derived from the encrypted session
 * store's persisted `expires_at` — never a live network check.
 */
sealed interface SessionState {
    /** No session (never signed in, or explicitly signed out / forced out by [AuthInterceptor]). */
    data object SignedOut : SessionState

    /**
     * A valid, unexpired session. [displayName]/[avatarUrl] come from Google's profile claims
     * (`GoTrueUserMetadataDto`, captured at sign-in) — both nullable because a non-Google future
     * provider, or a Google account with no public name/photo, may not supply them.
     */
    data class Active(
        val userId: String,
        val email: String?,
        val displayName: String? = null,
        val avatarUrl: String? = null,
    ) : SessionState

    /** A session exists but its `expires_at` has passed and refresh has not (yet) run. */
    data object Expired : SessionState
}
