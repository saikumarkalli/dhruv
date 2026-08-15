package com.dhruv.finance.data.tracker.auth

import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Erasure calls (ADR-0014 §7 / ADR-0029 decision 5), reachable from Settings › Privacy outside
 * onboarding: [deleteMyData] hard-deletes all tracker rows for the signed-in user via the
 * `delete_my_data()` RPC but leaves the account/session untouched — F-8's "delete my data" vs.
 * "delete my account" distinction, ONB-BR-008. [deleteMyAccount] calls `delete_my_account()`
 * (which itself wipes the same rows server-side, then deletes the `auth.users` row) and, only once
 * that RPC succeeds, forces sign-out via [SessionStore.clear] and resets
 * [ConsentRepository.setHasCompletedOnboarding] to `false` (ONB-BR-009) so the next app view routes
 * the user back through onboarding instead of leaving them signed-out inside a shell built for an
 * account that no longer exists.
 */
interface TrackerAccountRepository {
    suspend fun deleteMyData(): Result<Unit>

    suspend fun deleteMyAccount(): Result<Unit>
}

class TrackerAccountRepositoryImpl(
    private val trackerRpcApi: TrackerRpcApi,
    private val sessionStore: SessionStore,
    private val consentRepository: ConsentRepository,
) : TrackerAccountRepository {
    /** Builds [TrackerRpcApi] off [SupabaseClientFactory.erasureRetrofit] — mirrors
     * SessionStoreImpl/ConsentRepositoryImpl's `constructor(context: Context, ...)` convenience-
     * constructor pattern, and keeps `retrofit2.Retrofit` (an `implementation`-scoped dependency of
     * this module) from having to be re-exposed to callers like `:apps:finance:app`'s Koin module,
     * which only needs to hand this constructor a [SupabaseClientFactory].
     *
     * Deliberately [SupabaseClientFactory.erasureRetrofit], NOT [SupabaseClientFactory.dataRetrofit]
     * — erasure must succeed regardless of "Sync my financial records" consent (ONB-BR-008/009):
     * `dataRetrofit`'s `ConsentInterceptor` would otherwise permanently block both delete buttons
     * for exactly the two users most likely to press them — someone who never opted into sync, or
     * someone withdrawing consent right before deleting (found in Task 4 review; regression-locked
     * by `SupabaseClientFactoryTest`'s and this class's own consent-declined tests). */
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
        sessionStore: SessionStore,
        consentRepository: ConsentRepository,
    ) : this(
        supabaseClientFactory.erasureRetrofit.create(TrackerRpcApi::class.java),
        sessionStore,
        consentRepository,
    )

    // Deliberately broad, same shape as AuthRepositoryImpl: any network/HTTP failure surfaces as
    // Result.failure rather than crashing the caller's coroutine. CancellationException is excluded
    // and rethrown below so a caller navigating away mid-call still unwinds structured concurrency
    // correctly instead of being reported as a spurious feature error.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun deleteMyData(): Result<Unit> =
        try {
            trackerRpcApi.deleteMyData()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun deleteMyAccount(): Result<Unit> =
        try {
            trackerRpcApi.deleteMyAccount()
            // Only reached once the RPC has actually succeeded — a failed erasure must not sign the
            // user out of an account that still exists (ONB-BR-009). Once it HAS succeeded, the
            // account is already irreversibly gone server-side, so the two local writes below must
            // not be skipped by a caller-side cancellation (e.g. navigating away from Settings right
            // after tapping delete cancels the rememberCoroutineScope() this runs on) — NonCancellable
            // guarantees they finish, otherwise the device is left believing it's still signed in to
            // an account that no longer exists (security review finding, 2026-08-15).
            withContext(NonCancellable) {
                sessionStore.clear()
                consentRepository.setHasCompletedOnboarding(false)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
