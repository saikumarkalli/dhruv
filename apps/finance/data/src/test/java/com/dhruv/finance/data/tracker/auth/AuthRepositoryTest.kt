package com.dhruv.finance.data.tracker.auth

import com.dhruv.finance.data.FakeSessionStore
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.GoTrueUserDto
import com.dhruv.finance.data.tracker.dto.GoogleIdTokenRequest
import com.dhruv.finance.data.tracker.dto.RefreshTokenRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * ONB-BR-001 (Android/Task-2's OnboardingViewModel depends on this): Google sign-in composes
 * [GoTrueApi.signInWithIdToken] with [SessionStore.save] behind one call, so the ViewModel layer
 * never touches GoTrueApi or SessionStore directly. This test proves the composition, not the
 * network/persistence internals — those are covered by AuthInterceptorTest/SessionStoreTest.
 */
class AuthRepositoryTest {
    private class FakeGoTrueApi(
        private val signInResult: (GoogleIdTokenRequest) -> GoTrueSessionDto,
    ) : GoTrueApi {
        var signInCallCount = 0
            private set

        override suspend fun signInWithIdToken(
            grantType: String,
            body: GoogleIdTokenRequest,
        ): GoTrueSessionDto {
            signInCallCount++
            return signInResult(body)
        }

        override suspend fun refresh(
            grantType: String,
            body: RefreshTokenRequest,
        ): GoTrueSessionDto = throw UnsupportedOperationException("not exercised by AuthRepositoryTest")
    }

    private fun sessionOf(idToken: String) =
        GoTrueSessionDto(
            accessToken = "access-$idToken",
            tokenType = "bearer",
            expiresIn = 3600,
            expiresAt = 9_999_999_999,
            refreshToken = "refresh-$idToken",
            user = GoTrueUserDto(id = "user-1", email = "user@example.com"),
        )

    @Test
    fun `sign-in success saves the returned session and reports Result success`() =
        runTest {
            val api = FakeGoTrueApi { request -> sessionOf(request.idToken) }
            val sessionStore = FakeSessionStore()
            val repo: AuthRepository = AuthRepositoryImpl(api, sessionStore)

            val result = repo.signInWithGoogleIdToken("raw-id-token", "raw-nonce")

            assertTrue(result.isSuccess)
            assertEquals(1, api.signInCallCount)
            assertEquals(SessionState.Active("user-1", "user@example.com"), sessionStore.state.value)
        }

    @Test
    fun `sign-in passes the raw id token straight through as the request body`() =
        runTest {
            var receivedToken: String? = null
            val api =
                FakeGoTrueApi { request ->
                    receivedToken = request.idToken
                    sessionOf(request.idToken)
                }
            val repo: AuthRepository = AuthRepositoryImpl(api, FakeSessionStore())

            repo.signInWithGoogleIdToken("the-exact-token", "raw-nonce")

            assertEquals("the-exact-token", receivedToken)
        }

    // Nonce mismatch was the actual live cause of a "couldn't finish signing in" failure — GoTrue
    // hashes whatever it's given here and compares it to the id_token's nonce claim, so the exact
    // raw value reaching GoogleIdTokenRequest.nonce matters as much as the id token itself.
    @Test
    fun `sign-in passes the raw nonce straight through to the request body, unhashed`() =
        runTest {
            var receivedNonce: String? = null
            val api =
                FakeGoTrueApi { request ->
                    receivedNonce = request.nonce
                    sessionOf(request.idToken)
                }
            val repo: AuthRepository = AuthRepositoryImpl(api, FakeSessionStore())

            repo.signInWithGoogleIdToken("raw-id-token", "the-exact-raw-nonce")

            assertEquals("the-exact-raw-nonce", receivedNonce)
        }

    @Test
    fun `sign-in failure returns Result failure and never touches the session store`() =
        runTest {
            val api = FakeGoTrueApi { throw IllegalStateException("network down") }
            val sessionStore = FakeSessionStore()
            val repo: AuthRepository = AuthRepositoryImpl(api, sessionStore)

            val result = repo.signInWithGoogleIdToken("raw-id-token", "raw-nonce")

            assertTrue(result.isFailure)
            assertEquals(SessionState.SignedOut, sessionStore.state.value)
            assertNull(sessionStore.currentTokens())
        }

    // ONB-BR-001 (review fix): CancellationException must propagate out of signInWithGoogleIdToken
    // rather than being wrapped in Result.failure — e.g. the user navigates away from A2 mid-sign-in
    // and the enclosing coroutine is cancelled. A bare `runCatching` would swallow it and report a
    // spurious feature error instead of letting the coroutine unwind normally.
    @Test
    fun `sign-in propagates CancellationException instead of wrapping it in Result failure`() =
        runTest {
            val api = FakeGoTrueApi { throw CancellationException("navigated away from A2") }
            val sessionStore = FakeSessionStore()
            val repo: AuthRepository = AuthRepositoryImpl(api, sessionStore)

            try {
                repo.signInWithGoogleIdToken("raw-id-token", "raw-nonce")
                fail("expected CancellationException to propagate, but signInWithGoogleIdToken returned normally")
            } catch (expected: CancellationException) {
                // propagated correctly — not wrapped in Result.failure
            }

            assertEquals(SessionState.SignedOut, sessionStore.state.value)
        }
}
