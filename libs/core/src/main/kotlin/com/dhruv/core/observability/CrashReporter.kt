package com.dhruv.core.observability

import com.google.firebase.crashlytics.FirebaseCrashlytics

interface CrashReporter {
    /** Tag all subsequent reports with the feature module name. */
    fun setModule(name: String)

    fun recordException(t: Throwable)

    fun log(message: String)
}

/** Use in unit tests and before Crashlytics is wired. */
object NoOpCrashReporter : CrashReporter {
    override fun setModule(name: String) = Unit

    override fun recordException(t: Throwable) = Unit

    override fun log(message: String) = Unit
}

/**
 * Production implementation backed by Firebase Crashlytics.
 * Wire via Koin or Hilt in the app module:
 *   single<CrashReporter> { CrashlyticsReporter() }
 */
class CrashlyticsReporter : CrashReporter {
    // Firebase may be absent in this build (no google-services.json / FirebaseApp not initialized).
    // Observability must NEVER crash the app (PLATFORM.md §4), so resolve defensively once and
    // degrade to no-op if unavailable.
    private val crashlytics: FirebaseCrashlytics? by lazy {
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    override fun setModule(name: String) {
        crashlytics?.setCustomKey("module", name)
    }

    override fun recordException(t: Throwable) {
        crashlytics?.recordException(t)
    }

    override fun log(message: String) {
        crashlytics?.log(message)
    }
}
