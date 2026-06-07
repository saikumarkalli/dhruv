package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "app_settings_prefs"))
    }
)

class SettingsRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private object PreferencesKeys {
        val IS_DEGREE = booleanPreferencesKey("is_degree")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val IS_HISTORY_LOCKED = booleanPreferencesKey("is_history_locked")
        val HISTORY_PIN_CODE = stringPreferencesKey("history_pin_code")
        val COLOR_CALCULATOR = stringPreferencesKey("color_calculator")
        val COLOR_CONVERTER = stringPreferencesKey("color_converter")
        val COLOR_DATE = stringPreferencesKey("color_date")
        val COLOR_FINANCE = stringPreferencesKey("color_finance")
        val FORMAT_LOCALE = stringPreferencesKey("format_locale")
        val IS_CONVERTER_ENABLED = booleanPreferencesKey("is_converter_enabled")
        val IS_DATE_ENABLED = booleanPreferencesKey("is_date_enabled")
        val IS_FINANCE_ENABLED = booleanPreferencesKey("is_finance_enabled")
        val COLOR_TIME = stringPreferencesKey("color_time")
        val IS_TIME_ENABLED = booleanPreferencesKey("is_time_enabled")
    }

    private val initialPrefs = runBlocking {
        try {
            context.dataStore.data.first()
        } catch (e: Exception) {
            emptyPreferences()
        }
    }

    val isDegree: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_DEGREE] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_DEGREE] ?: true)

    val darkModePreference: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.DARK_MODE] ?: "system" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.DARK_MODE] ?: "system")

    val decimalPrecision: StateFlow<Int> = context.dataStore.data
        .map { it[PreferencesKeys.DECIMAL_PRECISION] ?: 4 }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.DECIMAL_PRECISION] ?: 4)

    val isHistoryLocked: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_HISTORY_LOCKED] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_HISTORY_LOCKED] ?: false)

    val historyPinCode: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.HISTORY_PIN_CODE] ?: "1234" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.HISTORY_PIN_CODE] ?: "1234")

    val calculatorColor: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLOR_CALCULATOR] ?: "cyan" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.COLOR_CALCULATOR] ?: "cyan")

    val converterColor: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLOR_CONVERTER] ?: "purple" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.COLOR_CONVERTER] ?: "purple")

    val dateColor: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLOR_DATE] ?: "coral" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.COLOR_DATE] ?: "coral")

    val financeColor: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLOR_FINANCE] ?: "amber" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.COLOR_FINANCE] ?: "amber")

    val formatLocale: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.FORMAT_LOCALE] ?: "international" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.FORMAT_LOCALE] ?: "international")

    val isConverterEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_CONVERTER_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_CONVERTER_ENABLED] ?: true)

    val isDateEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_DATE_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_DATE_ENABLED] ?: true)

    val isFinanceEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_FINANCE_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_FINANCE_ENABLED] ?: true)

    val timeColor: StateFlow<String> = context.dataStore.data
        .map { it[PreferencesKeys.COLOR_TIME] ?: "teal" }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.COLOR_TIME] ?: "teal")

    val isTimeEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.IS_TIME_ENABLED] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, initialPrefs[PreferencesKeys.IS_TIME_ENABLED] ?: true)

    fun setDegree(degree: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_DEGREE] = degree
            }
        }
    }

    fun setDarkModePreference(preference: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DARK_MODE] = preference
            }
        }
    }

    fun setDecimalPrecision(precision: Int) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DECIMAL_PRECISION] = precision
            }
        }
    }

    fun setHistoryLocked(locked: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_HISTORY_LOCKED] = locked
            }
        }
    }

    fun setHistoryPinCode(pin: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.HISTORY_PIN_CODE] = pin
            }
        }
    }

    fun setCalculatorColor(color: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_CALCULATOR] = color
            }
        }
    }

    fun setConverterColor(color: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_CONVERTER] = color
            }
        }
    }

    fun setDateColor(color: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_DATE] = color
            }
        }
    }

    fun setFinanceColor(color: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_FINANCE] = color
            }
        }
    }

    fun setFormatLocale(locale: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FORMAT_LOCALE] = locale
            }
        }
    }

    fun setConverterEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_CONVERTER_ENABLED] = enabled
            }
        }
    }

    fun setDateEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_DATE_ENABLED] = enabled
            }
        }
    }

    fun setFinanceEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_FINANCE_ENABLED] = enabled
            }
        }
    }

    fun setTimeColor(color: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_TIME] = color
            }
        }
    }

    fun setTimeEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_TIME_ENABLED] = enabled
            }
        }
    }

    fun isToolEnabled(key: String, defaultValue: Boolean = true): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey("tool_$key")] ?: defaultValue
        }
    }

    fun setToolEnabled(key: String, enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[booleanPreferencesKey("tool_$key")] = enabled
            }
        }
    }
}
