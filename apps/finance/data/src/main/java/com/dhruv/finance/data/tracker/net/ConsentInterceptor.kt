package com.dhruv.finance.data.tracker.net

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Thrown by [ConsentInterceptor] when sync consent is off. A distinct subtype (not a bare
 * [IOException]) so [ErrorMapper] can map it to [TrackerError.ConsentRequired] precisely. */
class ConsentRequiredException(
    message: String = "Tracker sync consent not granted; request blocked before dispatch (DAT-BR-001)",
) : IOException(message)

/**
 * Gates every PostgREST call behind the "Sync my financial records" consent switch
 * (DAT-BR-001, ONB-BR-003, ADR-0029). Attached only to `SupabaseClientFactory.dataClient` —
 * `authClient` never carries this interceptor, because sign-in itself is pre-consent
 * (ONB-BR-001).
 */
class ConsentInterceptor(
    private val hasSyncConsent: () -> Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!hasSyncConsent()) {
            throw ConsentRequiredException()
        }
        return chain.proceed(chain.request())
    }
}
