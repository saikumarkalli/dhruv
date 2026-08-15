package com.dhruv.finance.data.tracker.auth

import com.dhruv.finance.data.FakeConsentRepository
import com.dhruv.finance.data.FakeSessionStore
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.GoTrueUserDto
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Shared by both test classes below — a plain [GoTrueSessionDto] fixture, same shape as
 * `AuthRepositoryTest`'s equivalent. */
private fun trackerAccountTestSession() =
    GoTrueSessionDto(
        accessToken = "access-1",
        tokenType = "bearer",
        expiresIn = 3600,
        expiresAt = 9_999_999_999,
        refreshToken = "refresh-1",
        user = GoTrueUserDto(id = "user-1", email = "user@example.com"),
    )

/**
 * ONB-BR-008 (`deleteMyData` wipes tracker rows via the RPC but leaves the session and the
 * `hasCompletedOnboarding` flag untouched — the account stays signed in) and ONB-BR-009
 * (`deleteMyAccount` wipes rows, forces [SessionState.SignedOut], and resets
 * `hasCompletedOnboarding` to `false` so the next app view routes the user back through
 * onboarding). `DAT-FLOW-001` (every table + `auth.users` actually gone) is
 * `Automatable: N (against dev project)` — this test only proves the Kotlin-side composition calls
 * the RPC and reacts correctly, not that the SQL functions themselves work.
 */
class TrackerAccountRepositoryTest {
    private class FakeTrackerRpcApi(
        private val deleteMyDataResult: () -> Unit = {},
        private val deleteMyAccountResult: () -> Unit = {},
    ) : TrackerRpcApi {
        var deleteMyDataCallCount = 0
            private set
        var deleteMyAccountCallCount = 0
            private set

        override suspend fun deleteMyData() {
            deleteMyDataCallCount++
            deleteMyDataResult()
        }

        override suspend fun deleteMyAccount() {
            deleteMyAccountCallCount++
            deleteMyAccountResult()
        }
    }

    private fun activeSession() =
        GoTrueSessionDto(
            accessToken = "access-1",
            tokenType = "bearer",
            expiresIn = 3600,
            expiresAt = 9_999_999_999,
            refreshToken = "refresh-1",
            user = GoTrueUserDto(id = "user-1", email = "user@example.com"),
        )

    // ONB-BR-008: the account/session must not be touched by deleteMyData — only deleteMyAccount
    // signs the user out.
    @Test
    fun `deleteMyData success calls the RPC once and leaves session and onboarding flag untouched`() =
        runTest {
            val api = FakeTrackerRpcApi()
            val sessionStore = FakeSessionStore()
            sessionStore.save(activeSession())
            val consentRepository = FakeConsentRepository()
            consentRepository.setHasCompletedOnboarding(true)
            val repo: TrackerAccountRepository = TrackerAccountRepositoryImpl(api, sessionStore, consentRepository)

            val result = repo.deleteMyData()

            assertTrue(result.isSuccess)
            assertEquals(1, api.deleteMyDataCallCount)
            assertEquals(0, api.deleteMyAccountCallCount)
            assertEquals(SessionState.Active("user-1", "user@example.com"), sessionStore.state.value)
            assertTrue(consentRepository.state.value.hasCompletedOnboarding)
        }

    @Test
    fun `deleteMyData failure returns Result failure and leaves session and onboarding flag untouched`() =
        runTest {
            val api = FakeTrackerRpcApi(deleteMyDataResult = { throw IllegalStateException("network down") })
            val sessionStore = FakeSessionStore()
            sessionStore.save(activeSession())
            val consentRepository = FakeConsentRepository()
            consentRepository.setHasCompletedOnboarding(true)
            val repo: TrackerAccountRepository = TrackerAccountRepositoryImpl(api, sessionStore, consentRepository)

            val result = repo.deleteMyData()

            assertTrue(result.isFailure)
            assertEquals(SessionState.Active("user-1", "user@example.com"), sessionStore.state.value)
            assertTrue(consentRepository.state.value.hasCompletedOnboarding)
        }

    @Test
    fun `deleteMyData propagates CancellationException instead of wrapping it in Result failure`() =
        runTest {
            val api = FakeTrackerRpcApi(deleteMyDataResult = { throw CancellationException("navigated away") })
            val repo: TrackerAccountRepository =
                TrackerAccountRepositoryImpl(api, FakeSessionStore(), FakeConsentRepository())

            try {
                repo.deleteMyData()
                fail("expected CancellationException to propagate, but deleteMyData returned normally")
            } catch (expected: CancellationException) {
                // propagated correctly — not wrapped in Result.failure
            }
        }

    // ONB-BR-009: deleteMyAccount wipes rows via the RPC, then forces SignedOut and resets
    // hasCompletedOnboarding so MainActivity's cold-launch gate (Task 3 decision 2) routes the user
    // back through onboarding rather than leaving them in a shell with no account.
    @Test
    fun `deleteMyAccount success calls the RPC once, clears the session, and resets the onboarding flag`() =
        runTest {
            val api = FakeTrackerRpcApi()
            val sessionStore = FakeSessionStore()
            sessionStore.save(activeSession())
            val consentRepository = FakeConsentRepository()
            consentRepository.setHasCompletedOnboarding(true)
            val repo: TrackerAccountRepository = TrackerAccountRepositoryImpl(api, sessionStore, consentRepository)

            val result = repo.deleteMyAccount()

            assertTrue(result.isSuccess)
            assertEquals(1, api.deleteMyAccountCallCount)
            assertEquals(0, api.deleteMyDataCallCount)
            assertEquals(SessionState.SignedOut, sessionStore.state.value)
            assertFalse(consentRepository.state.value.hasCompletedOnboarding)
        }

    // A failed RPC must not sign the user out of an account that still exists.
    @Test
    fun `deleteMyAccount failure returns Result failure and never touches session or onboarding flag`() =
        runTest {
            val api = FakeTrackerRpcApi(deleteMyAccountResult = { throw IllegalStateException("network down") })
            val sessionStore = FakeSessionStore()
            sessionStore.save(activeSession())
            val consentRepository = FakeConsentRepository()
            consentRepository.setHasCompletedOnboarding(true)
            val repo: TrackerAccountRepository = TrackerAccountRepositoryImpl(api, sessionStore, consentRepository)

            val result = repo.deleteMyAccount()

            assertTrue(result.isFailure)
            assertEquals(SessionState.Active("user-1", "user@example.com"), sessionStore.state.value)
            assertTrue(consentRepository.state.value.hasCompletedOnboarding)
        }

    // ONB-BR-009 (mirrors ONB-BR-001's review fix on AuthRepositoryImpl): CancellationException
    // must propagate, not be wrapped in Result.failure.
    @Test
    fun `deleteMyAccount propagates CancellationException instead of wrapping it in Result failure`() =
        runTest {
            val api = FakeTrackerRpcApi(deleteMyAccountResult = { throw CancellationException("navigated away") })
            val sessionStore = FakeSessionStore()
            sessionStore.save(activeSession())
            val repo: TrackerAccountRepository =
                TrackerAccountRepositoryImpl(api, sessionStore, FakeConsentRepository())

            try {
                repo.deleteMyAccount()
                fail("expected CancellationException to propagate, but deleteMyAccount returned normally")
            } catch (expected: CancellationException) {
                // propagated correctly — not wrapped in Result.failure
            }

            assertEquals(SessionState.Active("user-1", "user@example.com"), sessionStore.state.value)
        }
}

/**
 * ONB-FLOW-005 (QA catalog: `Auto: Y`, `Owner: Backend`) — toggle "Sync my financial records" off,
 * then call `deleteMyData()`/`deleteMyAccount()` in the same session: both must still succeed. This
 * is the test that would have caught the Critical bug found in Task 4 review: every test in
 * [TrackerAccountRepositoryTest] above fakes out [TrackerRpcApi] directly, so none of them ever
 * exercise the real [com.dhruv.finance.data.tracker.net.ConsentInterceptor]/
 * [com.dhruv.finance.data.tracker.net.AuthInterceptor] chain — a fake-based test alone could not
 * have caught (and would not catch a regression of) a bug
 * that lived specifically in *which* [SupabaseClientFactory] Retrofit instance
 * `TrackerAccountRepositoryImpl`'s production constructor was built from. This class instead
 * constructs a real [SupabaseClientFactory] against a [MockWebServer] (mirrors
 * `com.dhruv.finance.data.tracker.net.SupabaseClientFactoryErasureBypassesConsentTest`, which
 * proves the same fact one layer down) and drives it through the actual
 * `TrackerAccountRepositoryImpl(SupabaseClientFactory, ...)` constructor — the same one
 * `PlatformModule.kt` uses in production.
 */
class TrackerAccountRepositoryConsentDeclinedTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun repositoryAgainstMockServer(
        consentRepository: ConsentRepository,
        sessionStore: SessionStore,
    ): TrackerAccountRepository {
        val supabaseClientFactory =
            SupabaseClientFactory(
                supabaseUrl = server.url("/").toString(),
                anonKey = "anon-key",
                sessionStore = sessionStore,
                hasSyncConsent = { consentRepository.state.value.syncFinancialRecords },
            )
        return TrackerAccountRepositoryImpl(supabaseClientFactory, sessionStore, consentRepository)
    }

    // ONB-FLOW-005 / ONB-BR-008 — declined sync consent must not block deleteMyData.
    @Test
    fun `deleteMyData succeeds through the real chain after sync consent is declined`() =
        runTest {
            val consentRepository = FakeConsentRepository()
            consentRepository.setSyncFinancialRecords(false) // explicit — also the default
            val sessionStore = FakeSessionStore()
            sessionStore.save(trackerAccountTestSession())
            val repo = repositoryAgainstMockServer(consentRepository, sessionStore)
            server.enqueue(MockResponse().setResponseCode(200).setBody("null"))

            val result = repo.deleteMyData()

            assertTrue(result.isSuccess)
            assertEquals(1, server.requestCount)
        }

    // ONB-FLOW-005 / ONB-BR-009 — declined sync consent must not block deleteMyAccount, and the
    // forced sign-out + onboarding-flag reset must still happen on success.
    @Test
    fun `deleteMyAccount succeeds through the real chain after sync consent is declined, still forces sign-out`() =
        runTest {
            val consentRepository = FakeConsentRepository()
            consentRepository.setSyncFinancialRecords(false)
            consentRepository.setHasCompletedOnboarding(true)
            val sessionStore = FakeSessionStore()
            sessionStore.save(trackerAccountTestSession())
            val repo = repositoryAgainstMockServer(consentRepository, sessionStore)
            server.enqueue(MockResponse().setResponseCode(200).setBody("null"))

            val result = repo.deleteMyAccount()

            assertTrue(result.isSuccess)
            assertEquals(1, server.requestCount)
            assertEquals(SessionState.SignedOut, sessionStore.state.value)
            assertFalse(consentRepository.state.value.hasCompletedOnboarding)
        }

    // Toggle-then-erasure in the exact ONB-FLOW-005 sequence: consent starts granted, gets
    // withdrawn mid-session, and erasure still succeeds right after — the scenario the bug report
    // called out explicitly (someone revoking consent right before deleting).
    @Test
    fun `withdrawing sync consent then immediately deleting my data still succeeds (ONB-FLOW-005 sequence)`() =
        runTest {
            val consentRepository = FakeConsentRepository()
            consentRepository.setSyncFinancialRecords(true) // starts granted
            val sessionStore = FakeSessionStore()
            sessionStore.save(trackerAccountTestSession())
            val repo = repositoryAgainstMockServer(consentRepository, sessionStore)

            consentRepository.setSyncFinancialRecords(false) // withdrawn, same session
            server.enqueue(MockResponse().setResponseCode(200).setBody("null"))
            val result = repo.deleteMyData()

            assertTrue(result.isSuccess)
            assertEquals(1, server.requestCount)
        }
}
