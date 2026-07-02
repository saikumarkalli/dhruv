package com.dhruv.core.observability

import com.google.firebase.perf.FirebasePerformance

/**
 * Thin abstraction over Firebase Performance tracing.
 * Feature ViewModels call [trace] around their primary operations to satisfy the
 * "at least one Performance trace per feature" platform rule.
 */
interface PerformanceTracer {
    /** Run [block], bracketing it with a named Firebase Performance trace. */
    fun <T> trace(
        name: String,
        block: () -> T,
    ): T
}

/**
 * Production implementation backed by Firebase Performance.
 * Wire via Koin in the app module:
 *   single<PerformanceTracer> { FirebasePerformanceTracer() }
 */
class FirebasePerformanceTracer : PerformanceTracer {
    override fun <T> trace(
        name: String,
        block: () -> T,
    ): T {
        // Firebase may be absent (FirebaseApp not initialized); tracing must never crash the app.
        val trace = runCatching { FirebasePerformance.getInstance().newTrace(name) }.getOrNull()
        trace?.start()
        return try {
            block()
        } finally {
            trace?.stop()
        }
    }
}

/** No-op implementation for unit tests and modules that do not need real tracing. */
object NoOpPerformanceTracer : PerformanceTracer {
    override fun <T> trace(
        name: String,
        block: () -> T,
    ): T = block()
}
