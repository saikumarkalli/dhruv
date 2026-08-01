package com.dhruv.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.security.EncryptedDataStoreFactory
import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ── Plaintext DataStore (legacy keys, SharedPreferences migration preserved) ──────────────────────
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "app_settings_prefs"))
    },
)

// Broad catches are intentional: encrypted-store reads/writes must always degrade to safe
// defaults (and report to Crashlytics) rather than crash settings, so each is funnelled here.

/**
 * Koin-injected implementation of [SettingsRepository].
 *
 * Storage strategy:
 * - All legacy operational keys live in the plaintext **"app_settings"** DataStore (same name as
 *   the old com.example.data.SettingsRepository), so no migration is needed and user data survives.
 * - [SettingsKeys.GEMINI_API_KEY] lives exclusively in a **separate encrypted** DataStore created
 *   by [EncryptedDataStoreFactory] ("secure_settings") — never stored in plaintext.
 */
@Suppress("TooGenericExceptionCaught")
class SettingsRepositoryImpl(
    private val context: Context,
    private val crashReporter: CrashReporter,
) : SettingsRepository {
    init {
        crashReporter.setModule("settings")
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /** Encrypted store for the Gemini API key only. */
    private val secureStore: DataStore<Preferences> by lazy {
        EncryptedDataStoreFactory.create(context, "secure_settings")
    }

    // ── Eager initial read so StateFlows have the right initial value synchronously ────────────
    private val initialPrefs: Preferences =
        runBlocking {
            try {
                context.appDataStore.data.first()
            } catch (e: Exception) {
                crashReporter.recordException(e)
                emptyPreferences()
            }
        }

    // ── Shared helpers for the legacy get/set pairs ────────────────────────────────────────────
    // Every legacy StateFlow property + setter below follows the exact same
    // "read with default, write plain" shape; these two helpers collapse that duplication.

    private fun <T> preferenceFlow(
        key: Preferences.Key<T>,
        default: T,
    ): StateFlow<T> =
        stateFlow(
            flow = context.appDataStore.data.catchAndLog().map { it[key] ?: default },
            initial = initialPrefs[key] ?: default,
        )

    private fun <T> setPreference(
        key: Preferences.Key<T>,
        value: T,
    ) = editPlain { it[key] = value }

    // ── Legacy StateFlow properties ────────────────────────────────────────────────────────────

    override val isDegree: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_DEGREE, true)

    override val darkModePreference: StateFlow<String> = preferenceFlow(SettingsKeys.DARK_MODE, "system")

    override val decimalPrecision: StateFlow<Int> =
        preferenceFlow(SettingsKeys.DECIMAL_PRECISION, DEFAULT_DECIMAL_PRECISION)

    override val isHistoryLocked: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_HISTORY_LOCKED, false)

    override val historyPinCode: StateFlow<String> = preferenceFlow(SettingsKeys.HISTORY_PIN_CODE, DEFAULT_PIN_CODE)

    override val calculatorColor: StateFlow<String> = preferenceFlow(SettingsKeys.COLOR_CALCULATOR, "cyan")

    override val converterColor: StateFlow<String> = preferenceFlow(SettingsKeys.COLOR_CONVERTER, "purple")

    override val dateColor: StateFlow<String> = preferenceFlow(SettingsKeys.COLOR_DATE, "coral")

    override val financeColor: StateFlow<String> = preferenceFlow(SettingsKeys.COLOR_FINANCE, "amber")

    override val formatLocale: StateFlow<String> = preferenceFlow(SettingsKeys.FORMAT_LOCALE, "international")

    override val isConverterEnabled: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_CONVERTER_ENABLED, true)

    override val isDateEnabled: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_DATE_ENABLED, true)

    override val isFinanceEnabled: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_FINANCE_ENABLED, true)

    override val timeColor: StateFlow<String> = preferenceFlow(SettingsKeys.COLOR_TIME, "teal")

    override val isTimeEnabled: StateFlow<Boolean> = preferenceFlow(SettingsKeys.IS_TIME_ENABLED, true)

    // ── New Phase-3 API ───────────────────────────────────────────────────────────────────────

    override fun observe(): Flow<AppSettings> {
        val plainFlow = context.appDataStore.data.catchAndLog()
        val secureFlow =
            secureStore.data.catch { e ->
                crashReporter.recordException(e)
                emit(emptyPreferences())
            }
        return combine(plainFlow, secureFlow) { plain, secure ->
            val darkMode = plain[SettingsKeys.DARK_MODE] ?: "system"
            val theme =
                when (darkMode) {
                    "always_dark" -> AppTheme.DARK
                    "always_light" -> AppTheme.LIGHT
                    else -> AppTheme.SYSTEM
                }
            val fontName = plain[SettingsKeys.FONT_FAMILY] ?: DhruvFont.DEFAULT.name
            val font = runCatching { DhruvFont.valueOf(fontName) }.getOrDefault(DhruvFont.DEFAULT)
            AppSettings(
                theme = theme,
                accentColorHex = plain[SettingsKeys.ACCENT_COLOR_HEX] ?: "#F05A28",
                fontFamily = font,
                biometricEnabled = plain[SettingsKeys.BIOMETRIC_ENABLED] ?: false,
                syncEnabled = plain[SettingsKeys.SYNC_ENABLED] ?: false,
                geminiApiKey = secure[SettingsKeys.GEMINI_API_KEY],
            )
        }
    }

    override suspend fun update(block: AppSettings.() -> AppSettings) {
        try {
            val current = observe().first()
            val updated = current.block()

            // Write plain preferences
            context.appDataStore.edit { prefs ->
                if (current.theme != updated.theme) {
                    prefs[SettingsKeys.DARK_MODE] =
                        when (updated.theme) {
                            AppTheme.DARK -> "always_dark"
                            AppTheme.LIGHT -> "always_light"
                            AppTheme.SYSTEM -> "system"
                        }
                }
                if (current.accentColorHex != updated.accentColorHex) {
                    prefs[SettingsKeys.ACCENT_COLOR_HEX] = updated.accentColorHex
                }
                if (current.fontFamily != updated.fontFamily) {
                    prefs[SettingsKeys.FONT_FAMILY] = updated.fontFamily.name
                }
                if (current.biometricEnabled != updated.biometricEnabled) {
                    prefs[SettingsKeys.BIOMETRIC_ENABLED] = updated.biometricEnabled
                }
                if (current.syncEnabled != updated.syncEnabled) {
                    prefs[SettingsKeys.SYNC_ENABLED] = updated.syncEnabled
                }
            }

            // Write encrypted preference (Gemini key)
            if (current.geminiApiKey != updated.geminiApiKey) {
                secureStore.edit { prefs ->
                    if (updated.geminiApiKey != null) {
                        prefs[SettingsKeys.GEMINI_API_KEY] = updated.geminiApiKey
                    } else {
                        prefs.remove(SettingsKeys.GEMINI_API_KEY)
                    }
                }
            }
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }
    }

    override suspend fun clearGeminiKey() {
        try {
            secureStore.edit { prefs -> prefs.remove(SettingsKeys.GEMINI_API_KEY) }
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }
    }

    // ── Legacy setters ────────────────────────────────────────────────────────────────────────

    override fun setDegree(degree: Boolean) = setPreference(SettingsKeys.IS_DEGREE, degree)

    override fun setDarkModePreference(preference: String) = setPreference(SettingsKeys.DARK_MODE, preference)

    override fun setDecimalPrecision(precision: Int) = setPreference(SettingsKeys.DECIMAL_PRECISION, precision)

    override fun setHistoryLocked(locked: Boolean) = setPreference(SettingsKeys.IS_HISTORY_LOCKED, locked)

    override fun setHistoryPinCode(pin: String) = setPreference(SettingsKeys.HISTORY_PIN_CODE, pin)

    override fun setCalculatorColor(color: String) = setPreference(SettingsKeys.COLOR_CALCULATOR, color)

    override fun setConverterColor(color: String) = setPreference(SettingsKeys.COLOR_CONVERTER, color)

    override fun setDateColor(color: String) = setPreference(SettingsKeys.COLOR_DATE, color)

    override fun setFinanceColor(color: String) = setPreference(SettingsKeys.COLOR_FINANCE, color)

    override fun setFormatLocale(locale: String) = setPreference(SettingsKeys.FORMAT_LOCALE, locale)

    override fun setConverterEnabled(enabled: Boolean) = setPreference(SettingsKeys.IS_CONVERTER_ENABLED, enabled)

    override fun setDateEnabled(enabled: Boolean) = setPreference(SettingsKeys.IS_DATE_ENABLED, enabled)

    override fun setFinanceEnabled(enabled: Boolean) = setPreference(SettingsKeys.IS_FINANCE_ENABLED, enabled)

    override fun setTimeColor(color: String) = setPreference(SettingsKeys.COLOR_TIME, color)

    override fun setTimeEnabled(enabled: Boolean) = setPreference(SettingsKeys.IS_TIME_ENABLED, enabled)

    override fun isToolEnabled(
        key: String,
        defaultValue: Boolean,
    ): Flow<Boolean> =
        context.appDataStore.data.catchAndLog().map { prefs ->
            prefs[booleanPreferencesKey("tool_$key")] ?: defaultValue
        }

    override fun setToolEnabled(
        key: String,
        enabled: Boolean,
    ) = editPlain { it[booleanPreferencesKey("tool_$key")] = enabled }

    // ── Private helpers ───────────────────────────────────────────────────────────────────────

    private fun editPlain(block: (MutablePreferences) -> Unit) {
        scope.launch {
            try {
                context.appDataStore.edit(block)
            } catch (e: Exception) {
                crashReporter.recordException(e)
            }
        }
    }

    private fun <T> Flow<T>.catchAndLog(): Flow<T> =
        catch { e ->
            crashReporter.recordException(e)
        }

    private fun <T> stateFlow(
        flow: Flow<T>,
        initial: T,
    ): StateFlow<T> {
        val sf = MutableStateFlow(initial)
        scope.launch {
            flow.collect { sf.value = it }
        }
        return sf
    }

    internal companion object Defaults {
        const val DEFAULT_PIN_CODE = "1234"
        const val DEFAULT_DECIMAL_PRECISION = 4
    }
}
