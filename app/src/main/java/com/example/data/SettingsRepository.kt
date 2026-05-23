package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _isDegree = MutableStateFlow(prefs.getBoolean("is_degree", true))
    val isDegree: StateFlow<Boolean> = _isDegree

    private val _darkModePreference = MutableStateFlow(prefs.getString("dark_mode", "system") ?: "system")
    val darkModePreference: StateFlow<String> = _darkModePreference

    private val _decimalPrecision = MutableStateFlow(prefs.getInt("decimal_precision", 4))
    val decimalPrecision: StateFlow<Int> = _decimalPrecision

    private val _isHistoryLocked = MutableStateFlow(prefs.getBoolean("is_history_locked", false))
    val isHistoryLocked: StateFlow<Boolean> = _isHistoryLocked

    private val _historyPinCode = MutableStateFlow(prefs.getString("history_pin_code", "1234") ?: "1234")
    val historyPinCode: StateFlow<String> = _historyPinCode

    private val _calculatorColor = MutableStateFlow(prefs.getString("color_calculator", "cyan") ?: "cyan")
    val calculatorColor: StateFlow<String> = _calculatorColor

    private val _converterColor = MutableStateFlow(prefs.getString("color_converter", "purple") ?: "purple")
    val converterColor: StateFlow<String> = _converterColor

    private val _dateColor = MutableStateFlow(prefs.getString("color_date", "coral") ?: "coral")
    val dateColor: StateFlow<String> = _dateColor

    private val _financeColor = MutableStateFlow(prefs.getString("color_finance", "amber") ?: "amber")
    val financeColor: StateFlow<String> = _financeColor

    fun setDegree(degree: Boolean) {
        prefs.edit().putBoolean("is_degree", degree).apply()
        _isDegree.value = degree
    }

    fun setDarkModePreference(preference: String) {
        prefs.edit().putString("dark_mode", preference).apply()
        _darkModePreference.value = preference
    }

    fun setDecimalPrecision(precision: Int) {
        prefs.edit().putInt("decimal_precision", precision).apply()
        _decimalPrecision.value = precision
    }

    fun setHistoryLocked(locked: Boolean) {
        prefs.edit().putBoolean("is_history_locked", locked).apply()
        _isHistoryLocked.value = locked
    }

    fun setHistoryPinCode(pin: String) {
        prefs.edit().putString("history_pin_code", pin).apply()
        _historyPinCode.value = pin
    }

    fun setCalculatorColor(color: String) {
        prefs.edit().putString("color_calculator", color).apply()
        _calculatorColor.value = color
    }

    fun setConverterColor(color: String) {
        prefs.edit().putString("color_converter", color).apply()
        _converterColor.value = color
    }

    fun setDateColor(color: String) {
        prefs.edit().putString("color_date", color).apply()
        _dateColor.value = color
    }

    fun setFinanceColor(color: String) {
        prefs.edit().putString("color_finance", color).apply()
        _financeColor.value = color
    }
}
