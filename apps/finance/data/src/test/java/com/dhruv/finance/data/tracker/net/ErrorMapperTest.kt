package com.dhruv.finance.data.tracker.net

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** DAT-BR-001 — ConsentInterceptor's rejection maps to a distinct, identifiable TrackerError so
 * callers can show a "turn consent on" prompt rather than a generic network-error message. */
class ErrorMapperTest {
    // DAT-BR-001
    @Test
    fun `ConsentRequiredException maps to ConsentRequired`() {
        val result = ErrorMapper.map(ConsentRequiredException())

        assertEquals(TrackerError.ConsentRequired, result)
    }

    // Internal error-mapping coverage supporting DAT-BR-003 (401 handling) — no dedicated catalog
    // row for this branch; AuthInterceptor itself (not ErrorMapper) is what DAT-BR-003 tests.
    @Test
    fun `a 401 HttpException maps to NotAuthenticated`() {
        val response = Response.error<Any>(401, "".toResponseBody("application/json".toMediaType()))

        val result = ErrorMapper.map(HttpException(response))

        assertEquals(TrackerError.NotAuthenticated, result)
    }

    // Internal error-mapping coverage supporting DAT-BR-002/003 (every tracker call is built and
    // handled consistently) — no dedicated catalog row; this phase has no real 5xx-returning
    // endpoint to catalog a scenario against yet (Phase 2 adds holdings/valuations calls).
    @Test
    fun `a 500 HttpException maps to ServerError with the status code`() {
        val response = Response.error<Any>(500, "".toResponseBody("application/json".toMediaType()))

        val result = ErrorMapper.map(HttpException(response))

        assertEquals(TrackerError.ServerError(500), result)
    }

    // Internal error-mapping coverage supporting DAT-BR-002/003 (tracker network calls) — no
    // dedicated catalog row for the generic-IOException branch.
    @Test
    fun `a plain IOException maps to NetworkUnavailable`() {
        val result = ErrorMapper.map(IOException("connect timed out"))

        assertEquals(TrackerError.NetworkUnavailable, result)
    }

    // Internal error-mapping coverage (fallback branch) — no dedicated catalog row; asserts the
    // mapper degrades to Unknown rather than throwing for an uncategorized Throwable.
    @Test
    fun `anything else maps to Unknown carrying the original cause`() {
        val cause = IllegalStateException("boom")

        val result = ErrorMapper.map(cause)

        assertTrue(result is TrackerError.Unknown)
        assertEquals(cause, (result as TrackerError.Unknown).cause)
    }
}
