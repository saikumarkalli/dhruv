package com.dhruv.finance.data.tracker.net

import retrofit2.HttpException
import java.io.IOException

/** Coarse tracker-domain error taxonomy (ADR-0029). Kept deliberately small — Phase 1 only has
 * the auth surface to exercise this against; real PostgREST error-JSON-body parsing is a Phase 2
 * concern once holdings/valuations calls exist. */
sealed interface TrackerError {
    data object NotAuthenticated : TrackerError

    data object ConsentRequired : TrackerError

    data object NetworkUnavailable : TrackerError

    data class ServerError(
        val code: Int,
    ) : TrackerError

    data class Unknown(
        val cause: Throwable?,
    ) : TrackerError
}

object ErrorMapper {
    fun map(throwable: Throwable): TrackerError =
        when (throwable) {
            is ConsentRequiredException -> TrackerError.ConsentRequired
            is HttpException ->
                if (throwable.code() == UNAUTHORIZED) {
                    TrackerError.NotAuthenticated
                } else {
                    TrackerError.ServerError(throwable.code())
                }
            is IOException -> TrackerError.NetworkUnavailable
            else -> TrackerError.Unknown(throwable)
        }

    private const val UNAUTHORIZED = 401
}
