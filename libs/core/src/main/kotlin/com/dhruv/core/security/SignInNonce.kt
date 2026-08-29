package com.dhruv.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The Google Sign-In nonce pair, shared by every surface that runs a Credential Manager sign-in —
 * today `com.dhruv.finance.onboarding.SignInScreen` (first-run) and
 * `com.dhruv.finance.app.ui.settings.AccountSettingsScreen` (Settings › Account, 0b.2).
 *
 * Lives in `:libs:core` rather than being copied per call site: these are pure functions with no
 * feature semantics, and both consumers already depend on this module. `SET-ARCH-003` forbids
 * Settings referencing a **feature-module type** — it does not license duplicating nonce
 * generation, and two independently-maintained copies of a security primitive on the auth path is
 * exactly the drift that rule exists to prevent.
 *
 * **Why two values.** `GetGoogleIdOption.setNonce(...)` wants the SHA-256 *hex digest*; Google
 * embeds whatever it is handed verbatim into the id_token's `nonce` claim and does not hash it
 * again. GoTrue is handed the **raw** value and hashes it itself to compare against that claim.
 * Sending the same form to both, in either direction, fails the exchange.
 */
data class SignInNonce(
    /** Handed to `AuthRepository.signInWithGoogleIdToken` — unhashed, as GoTrue expects. */
    val raw: String,
    /** Handed to `GetGoogleIdOption.setNonce` — the SHA-256 hex digest of [raw]. */
    val sha256Hex: String,
) {
    companion object {
        private const val NONCE_BYTES = 32

        /** Fresh per sign-in attempt, never reused. */
        fun generate(): SignInNonce {
            val bytes = ByteArray(NONCE_BYTES)
            SecureRandom().nextBytes(bytes)
            val raw = bytes.toHex()
            return SignInNonce(raw = raw, sha256Hex = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).toHex())
        }

        private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
    }
}
