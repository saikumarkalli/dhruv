package com.dhruv.finance.data.tracker.net

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * ADR-0033: every `finance`-schema call must carry `Accept-Profile: finance`, and mutations must
 * additionally carry `Content-Profile: finance` — omitting either silently 404s against the
 * (empty) `public` schema instead of erroring loudly, which is exactly the failure class this
 * interceptor exists to make structural rather than per-endpoint discipline.
 */
class FinanceSchemaInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().addInterceptor(FinanceSchemaInterceptor()).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a GET request carries Accept-Profile finance but not Content-Profile`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val request = Request.Builder().url(server.url("/v_net_worth_by_sector")).get().build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals("finance", recorded.getHeader("Accept-Profile"))
        assertNull(recorded.getHeader("Content-Profile"))
    }

    @Test
    fun `a POST request carries both Accept-Profile and Content-Profile finance`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("\"holding-id\""))

        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(server.url("/rpc/create_holding_with_value")).post(body).build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals("finance", recorded.getHeader("Accept-Profile"))
        assertEquals("finance", recorded.getHeader("Content-Profile"))
    }
}
