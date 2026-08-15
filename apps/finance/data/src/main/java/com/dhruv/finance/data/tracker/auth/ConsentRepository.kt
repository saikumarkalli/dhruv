package com.dhruv.finance.data.tracker.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.dhruv.core.observability.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The three independently-revocable A3 consent switches (ONB-BR-004/005), plus
 * [hasCompletedOnboarding] — a one-way flag `MainActivity` reads on cold launch to decide whether
 * to show the onboarding flow at all (Task 3 decision 2). It is deliberately grouped with the
 * consent switches rather than a separate repository: same DataStore, same persistence shape, and
 * it is set at the same two call sites ([OnboardingViewModel]'s exit-to-shell transitions) that
 * already touch this repository. The retention/erasure block is deliberately not represented here
 * — it is UI-only, not a flag gate (functional spec §5.5 table).
 */
data class ConsentState(
    val syncFinancialRecords: Boolean = false,
    val readTransactionSms: Boolean = false,
    val askDhruvAboutMoney: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
)

interface ConsentRepository {
    val state: StateFlow<ConsentState>

    suspend fun setSyncFinancialRecords(enabled: Boolean)

    suspend fun setReadTransactionSms(enabled: Boolean)

    suspend fun setAskDhruvAboutMoney(enabled: Boolean)

    suspend fun setHasCompletedOnboarding(completed: Boolean)
}

/** Plain (unencrypted) DataStore, same `preferencesDataStore` delegate pattern as
 * SettingsRepositoryImpl's `appDataStore` — booleans aren't secrets. */
private val Context.trackerConsentDataStore: DataStore<Preferences> by preferencesDataStore(name = "tracker_consent")

private object ConsentKeys {
    val SYNC_FINANCIAL_RECORDS = booleanPreferencesKey("sync_financial_records")
    val READ_TRANSACTION_SMS = booleanPreferencesKey("read_transaction_sms")
    val ASK_DHRUV_ABOUT_MONEY = booleanPreferencesKey("ask_dhruv_about_money")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
}

class ConsentRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val crashReporter: CrashReporter,
) : ConsentRepository {
    constructor(context: Context, crashReporter: CrashReporter) : this(context.trackerConsentDataStore, crashReporter)

    // Deliberately broad: a corrupt DataStore file can fail in several exception shapes
    // (IOException, CorruptionException, ...) and every one of them must degrade to defaults
    // rather than crash construction. Same accepted pattern as AuthRepositoryImpl's equivalent
    // catch block (final whole-branch review, Fix 3 — mirrors SessionStoreImpl's identical case).
    @Suppress("TooGenericExceptionCaught")
    private val initialPrefs: Preferences =
        runBlocking {
            try {
                dataStore.data.first()
            } catch (e: Exception) {
                // Corrupt store must degrade to defaults (all consent off), not crash — but
                // silently is a diagnosability gap, so it's still reported (mirrors
                // SettingsRepositoryImpl's equivalent catch block).
                crashReporter.recordException(e)
                emptyPreferences()
            }
        }

    private val _state = MutableStateFlow(deriveState(initialPrefs))
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    override suspend fun setSyncFinancialRecords(enabled: Boolean) = setFlag(ConsentKeys.SYNC_FINANCIAL_RECORDS, enabled)

    override suspend fun setReadTransactionSms(enabled: Boolean) = setFlag(ConsentKeys.READ_TRANSACTION_SMS, enabled)

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) = setFlag(ConsentKeys.ASK_DHRUV_ABOUT_MONEY, enabled)

    override suspend fun setHasCompletedOnboarding(completed: Boolean) =
        setFlag(ConsentKeys.HAS_COMPLETED_ONBOARDING, completed)

    private suspend fun setFlag(
        key: Preferences.Key<Boolean>,
        enabled: Boolean,
    ) {
        val updated = dataStore.edit { prefs -> prefs[key] = enabled }
        _state.value = deriveState(updated)
    }

    private fun deriveState(prefs: Preferences) =
        ConsentState(
            syncFinancialRecords = prefs[ConsentKeys.SYNC_FINANCIAL_RECORDS] ?: false,
            readTransactionSms = prefs[ConsentKeys.READ_TRANSACTION_SMS] ?: false,
            askDhruvAboutMoney = prefs[ConsentKeys.ASK_DHRUV_ABOUT_MONEY] ?: false,
            hasCompletedOnboarding = prefs[ConsentKeys.HAS_COMPLETED_ONBOARDING] ?: false,
        )
}
