package com.dhruv.finance.data.tracker.auth

import com.dhruv.finance.data.tracker.dto.GoogleIdTokenRequest
import kotlinx.coroutines.CancellationException

/**
 * Thin composition of [GoTrueApi] + [SessionStore] so feature ViewModels (Task 2's
 * `OnboardingViewModel`) depend on one call instead of wiring the network + persistence steps
 * themselves. Sign-in is pre-consent (ONB-BR-001) — this repository makes exactly one network
 * call and one local write, nothing else.
 */
interface AuthRepository {
    /**
     * [rawNonce] is the same unhashed nonce the caller passed (as its SHA-256 hash) to Credential
     * Manager's `GetGoogleIdOption.setNonce(...)` — GoTrue hashes this value itself and compares it
     * against the `nonce` claim baked into [idToken]. Required because this project's Supabase
     * instance does not set `external_google_skip_nonce_check` (found live, see
     * [com.dhruv.finance.data.tracker.dto.GoogleIdTokenRequest]'s doc comment).
     */
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String,
    ): Result<Unit>
}

class AuthRepositoryImpl(
    private val goTrueApi: GoTrueApi,
    private val sessionStore: SessionStore,
) : AuthRepository {
    // Deliberately broad: any failure from the network call or the local session write
    // (IOException, HttpException, a Moshi parse failure, ...) should surface to the caller as
    // Result.failure, not crash the sign-in coroutine. CancellationException is excluded and
    // rethrown below, so this only ever catches genuine failures. Same accepted pattern as
    // ConsentRepositoryImpl/SessionStoreImpl's equivalent catch blocks.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String,
    ): Result<Unit> =
        try {
            val session =
                goTrueApi.signInWithIdToken(body = GoogleIdTokenRequest(idToken = idToken, nonce = rawNonce))
            sessionStore.save(session)
            Result.success(Unit)
        } catch (e: CancellationException) {
            // Must propagate, not be reported as a feature error — swallowing it here (as a bare
            // `runCatching` previously did) breaks structured concurrency: the coroutine would
            // never actually finish cancelling (ONB-BR-001 review finding).
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
