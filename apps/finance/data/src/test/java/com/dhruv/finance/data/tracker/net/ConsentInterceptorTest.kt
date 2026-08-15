package com.dhruv.finance.data.tracker.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * DAT-BR-001 / ONB-BR-003: a consent flag is false -> any tracker repository method touching that
 * scope is called -> ConsentInterceptor short-circuits before an HTTP request is dispatched.
 * Verified against a real MockWebServer so "zero bytes hit the wire" is provable, not assumed.
 */
class ConsentInterceptorTest {
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

    private fun clientWith(hasSyncConsent: () -> Boolean) =
        OkHttpClient
            .Builder()
            .addInterceptor(ConsentInterceptor(hasSyncConsent))
            .build()

    // DAT-BR-001
    @Test
    fun `consent false short-circuits before dispatch, zero bytes hit the wire`() {
        val client = clientWith { false }
        val request = Request.Builder().url(server.url("/rest/v1/holdings")).build()

        assertThrows(IOException::class.java) {
            client.newCall(request).execute()
        }

        assertEquals(0, server.requestCount)
    }

    // DAT-BR-001
    @Test
    fun `consent false throws the specific ConsentRequiredException subtype`() {
        val client = clientWith { false }
        val request = Request.Builder().url(server.url("/rest/v1/holdings")).build()

        assertThrows(ConsentRequiredException::class.java) {
            client.newCall(request).execute()
        }
    }

    // DAT-BR-001 — the converse: consent true dispatches normally
    @Test
    fun `consent true dispatches the request normally`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWith { true }
        val request = Request.Builder().url(server.url("/rest/v1/holdings")).build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
    }
}
