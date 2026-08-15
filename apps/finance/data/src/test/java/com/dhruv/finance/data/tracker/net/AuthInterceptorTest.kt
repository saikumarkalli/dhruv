package com.dhruv.finance.data.tracker.net

import com.dhruv.finance.data.FakeSessionStore
import com.dhruv.finance.data.tracker.auth.GoTrueApi
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.GoTrueUserDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * DAT-BR-002: an active session -> any tracker API request is built -> apikey and
 * Authorization: Bearer headers are present.
 * DAT-BR-003: a request returns 401 -> exactly one refresh-token attempt occurs; a second
 * consecutive 401 forces SignedOut, no retry loop.
 *
 * Uses a real MockWebServer (no live network) as both the target API and the refresh endpoint,
 * mirroring SupabaseClientFactory's real wiring: the refresh call is made through a GoTrueApi
 * built from the *same* interceptor-wrapped client (guarded against recursion by AuthInterceptor
 * skipping its own retry logic for the `/token` endpoint itself).
 */
class AuthInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var sessionStore: FakeSessionStore
    private val moshi = Moshi.Builder().build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessionStore = FakeSessionStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun newInterceptor(anonKey: String = "anon-key"): AuthInterceptor {
        lateinit var api: GoTrueApi
        val interceptor = AuthInterceptor(sessionStore, anonKey) { api }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/auth/v1/"))
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        api = retrofit.create(GoTrueApi::class.java)
        return interceptor
    }

    private fun rawGet(
        path: String,
        interceptor: AuthInterceptor,
    ): Response {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder().url(server.url(path)).build()
        return client.newCall(request).execute()
    }

    private fun fakeSession(
        accessToken: String,
        refreshToken: String = "refresh-1",
    ) = GoTrueSessionDto(
        accessToken = accessToken,
        tokenType = "bearer",
        expiresIn = 3600,
        expiresAt = (System.currentTimeMillis() / 1000) + 3600,
        refreshToken = refreshToken,
        user = GoTrueUserDto(id = "user-1", email = "user@example.com"),
    )

    private fun sessionJsonBody(accessToken: String) =
        """{"access_token":"$accessToken","token_type":"bearer","expires_in":3600,""" +
            """"expires_at":${(System.currentTimeMillis() / 1000) + 3600},"refresh_token":"refresh-2",""" +
            """"user":{"id":"user-1","email":"user@example.com"}}"""

    // DAT-BR-002
    @Test
    fun `apikey header always present, Authorization absent when signed out`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200))
            val interceptor = newInterceptor(anonKey = "my-anon-key")

            rawGet("/rest/v1/holdings", interceptor)

            val recorded = server.takeRequest()
            assertEquals("my-anon-key", recorded.getHeader("apikey"))
            assertNull(recorded.getHeader("Authorization"))
        }

    // DAT-BR-002
    @Test
    fun `Authorization Bearer present when a session is active`() =
        runTest {
            sessionStore.save(fakeSession("access-xyz"))
            server.enqueue(MockResponse().setResponseCode(200))
            val interceptor = newInterceptor(anonKey = "my-anon-key")

            rawGet("/rest/v1/holdings", interceptor)

            val recorded = server.takeRequest()
            assertEquals("my-anon-key", recorded.getHeader("apikey"))
            assertEquals("Bearer access-xyz", recorded.getHeader("Authorization"))
        }

    // DAT-BR-003 — refresh succeeds, retry succeeds
    @Test
    fun `single 401 triggers exactly one refresh then a successful retry`() =
        runTest {
            sessionStore.save(fakeSession("stale-token", refreshToken = "refresh-1"))
            server.enqueue(MockResponse().setResponseCode(401)) // original request
            server.enqueue(MockResponse().setResponseCode(200).setBody(sessionJsonBody("fresh-token"))) // refresh
            server.enqueue(MockResponse().setResponseCode(200)) // retried original
            val interceptor = newInterceptor()

            val response = rawGet("/rest/v1/holdings", interceptor)

            assertEquals(200, response.code)
            assertEquals(3, server.requestCount)
            assertEquals("fresh-token", sessionStore.currentTokens()?.accessToken)
        }

    // DAT-BR-003 — second consecutive 401 forces SignedOut, no further retry
    @Test
    fun `a second consecutive 401 after refresh forces SignedOut with no further retry`() =
        runTest {
            sessionStore.save(fakeSession("stale-token", refreshToken = "refresh-1"))
            server.enqueue(MockResponse().setResponseCode(401)) // original request
            server.enqueue(MockResponse().setResponseCode(200).setBody(sessionJsonBody("fresh-token"))) // refresh
            server.enqueue(MockResponse().setResponseCode(401)) // retried original — still 401
            val interceptor = newInterceptor()

            val response = rawGet("/rest/v1/holdings", interceptor)

            assertEquals(401, response.code)
            assertEquals(3, server.requestCount) // no 4th request
            assertEquals(SessionState.SignedOut, sessionStore.state.value)
        }

    // DAT-BR-003 — refresh call itself fails -> forced SignedOut, original 401 returned, no retry
    @Test
    fun `refresh failure forces SignedOut and returns the original 401`() =
        runTest {
            sessionStore.save(fakeSession("stale-token", refreshToken = "bad-refresh"))
            server.enqueue(MockResponse().setResponseCode(401)) // original request
            server.enqueue(MockResponse().setResponseCode(401)) // refresh itself rejected
            val interceptor = newInterceptor()

            val response = rawGet("/rest/v1/holdings", interceptor)

            assertEquals(401, response.code)
            assertEquals(2, server.requestCount)
            assertEquals(SessionState.SignedOut, sessionStore.state.value)
        }

    // DAT-BR-003 — no session/no refresh token at all: 401 just passes through, no refresh attempt
    @Test
    fun `401 with no session present passes through untouched`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(401))
            val interceptor = newInterceptor()

            val response = rawGet("/rest/v1/holdings", interceptor)

            assertEquals(401, response.code)
            assertEquals(1, server.requestCount)
        }
}
