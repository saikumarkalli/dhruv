package com.dhruv.core.integrity

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.tasks.await

sealed class IntegrityResult {
    object Pass : IntegrityResult()

    data class Fail(
        val reason: String,
        val fatal: Boolean = false,
    ) : IntegrityResult()
}

/**
 * Wrapper around the Play Integrity API.
 *
 * Result is always non-fatal/warn-only per ADR-0006. The app continues normally on Fail;
 * the result is logged via CrashReporter for server-side analysis.
 *
 * Usage:
 *   val wrapper = PlayIntegrityWrapper(context, cloudProjectNumber = 123456789L)
 *   wrapper.prepare()
 *   val result = wrapper.check("action_name")
 */
class PlayIntegrityWrapper(
    context: Context,
    private val cloudProjectNumber: Long,
) {
    private val manager =
        IntegrityManagerFactory
            .createStandard(context.applicationContext)

    private var tokenProvider: com.google.android.play.core.integrity.StandardIntegrityManager
        .StandardIntegrityTokenProvider? = null

    suspend fun prepare() {
        runCatching {
            tokenProvider =
                manager
                    .prepareIntegrityToken(
                        PrepareIntegrityTokenRequest
                            .builder()
                            .setCloudProjectNumber(cloudProjectNumber)
                            .build(),
                    ).await()
        }
    }

    suspend fun check(requestHash: String): IntegrityResult {
        val provider =
            tokenProvider
                ?: return IntegrityResult.Fail("Integrity not prepared", fatal = false)

        return runCatching {
            provider
                .request(
                    StandardIntegrityTokenRequest
                        .builder()
                        .setRequestHash(requestHash)
                        .build(),
                ).await()
            IntegrityResult.Pass
        }.getOrElse { e ->
            IntegrityResult.Fail(reason = e.message ?: "Unknown integrity error", fatal = false)
        }
    }
}
