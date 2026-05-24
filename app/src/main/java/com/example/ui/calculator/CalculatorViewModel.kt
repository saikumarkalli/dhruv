package com.example.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import com.example.util.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class CalculatorViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result.asStateFlow()

    // Active calculations (not in recycle bin)
    val activeHistory: StateFlow<List<HistoryEntity>> = historyRepository.activeHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recycle bin calculations
    val recycleBinHistory: StateFlow<List<HistoryEntity>> = historyRepository.recycleBinHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isDegree: StateFlow<Boolean> = settingsRepository.isDegree
    val decimalPrecision: StateFlow<Int> = settingsRepository.decimalPrecision
    val isHistoryLocked: StateFlow<Boolean> = settingsRepository.isHistoryLocked
    val historyPinCode: StateFlow<String> = settingsRepository.historyPinCode

    private var isResultFresh = false

    init {
        // Auto remove items in recycle bin older than 30 days
        viewModelScope.launch {
            historyRepository.pruneOldRecycleBin()
        }
    }

    fun onKeyPress(key: String) {
        val currentInput = _input.value
        val currentResult = _result.value

        if (isResultFresh) {
            isResultFresh = false
            when (key) {
                "+", "-", "×", "÷", "%", "^" -> {
                    if (currentResult.isNotEmpty() && currentResult != "Error") {
                        _input.value = currentResult + key
                        _result.value = ""
                        return
                    }
                }
                "AC" -> {
                    _input.value = ""
                    _result.value = ""
                    return
                }
                "C" -> {
                    _input.value = ""
                    _result.value = ""
                    return
                }
                "⌫" -> {
                    _input.value = ""
                    _result.value = ""
                    return
                }
                "=" -> {
                    return
                }
                "±" -> {
                    if (currentResult.isNotEmpty() && currentResult != "Error") {
                        if (currentResult.startsWith("-")) {
                            _input.value = currentResult.removePrefix("-")
                        } else {
                            _input.value = "-$currentResult"
                        }
                        _result.value = ""
                        return
                    }
                }
                "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> {
                    if (currentResult.isNotEmpty() && currentResult != "Error") {
                        _input.value = "$key($currentResult"
                    } else {
                        _input.value = "$key("
                    }
                    _result.value = ""
                    return
                }
                else -> {
                    _input.value = key
                    _result.value = ""
                    return
                }
            }
        }

        when (key) {
            "AC" -> {
                _input.value = ""
                _result.value = ""
            }
            "C" -> {
                _input.value = ""
            }
            "⌫" -> {
                val cur = _input.value
                if (cur.isNotEmpty()) {
                    _input.value = cur.dropLast(1)
                }
            }
            "=" -> {
                evaluateExpression()
            }
            "±" -> {
                val cur = _input.value
                if (cur.isNotEmpty()) {
                    if (cur.startsWith("-")) {
                        _input.value = cur.removePrefix("-")
                    } else {
                        _input.value = "-$cur"
                    }
                }
            }
            "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> {
                _input.value += "$key("
            }
            else -> {
                _input.value += key
            }
        }
    }

    fun restoreEquation(expression: String, resultStr: String) {
        _input.value = expression
        _result.value = resultStr
        isResultFresh = true
    }

    // Toggle Favorite column
    fun toggleFavorite(historyItem: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.update(historyItem.copy(favorite = !historyItem.favorite))
        }
    }

    // Update Note column
    fun updateNote(historyItem: HistoryEntity, newNote: String) {
        viewModelScope.launch {
            historyRepository.update(historyItem.copy(note = newNote, edited = true))
        }
    }

    // Update Tags column
    fun updateTags(historyItem: HistoryEntity, newTags: String) {
        viewModelScope.launch {
            historyRepository.update(historyItem.copy(tags = newTags, edited = true))
        }
    }

    // Move single item to recycle bin
    fun moveToRecycleBin(id: Long) {
        viewModelScope.launch {
            historyRepository.moveToRecycleBin(id)
        }
    }

    // Move multiple items to recycle bin (Bulk Delete to Recycle Bin)
    fun moveMultipleToRecycleBin(ids: List<Long>) {
        viewModelScope.launch {
            historyRepository.moveMultipleToRecycleBin(ids)
        }
    }

    // Restore single item from recycle bin
    fun restoreFromRecycleBin(id: Long) {
        viewModelScope.launch {
            historyRepository.restoreFromRecycleBin(id)
        }
    }

    // Delete single item permanently
    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            historyRepository.delete(id)
        }
    }

    // Delete multiple items permanently
    fun deletePermanentlyMultiple(ids: List<Long>) {
        viewModelScope.launch {
            historyRepository.deleteMultiple(ids)
        }
    }

    // Empty recycle bin
    fun emptyRecycleBin() {
        viewModelScope.launch {
            historyRepository.emptyRecycleBin()
        }
    }

    // Clear active calculations (move all to recycle bin)
    fun clearActiveHistory() {
        viewModelScope.launch {
            val list = activeHistory.value
            if (list.isNotEmpty()) {
                val ids = list.map { it.id }
                historyRepository.moveMultipleToRecycleBin(ids)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clear()
        }
    }

    fun toggleAngleUnit() {
        settingsRepository.setDegree(!isDegree.value)
    }

    private fun evaluateExpression() {
        val currentExpression = _input.value
        if (currentExpression.isBlank()) return

        viewModelScope.launch {
            try {
                val rawResult = CalculatorEngine.evaluate(currentExpression, isDegree.value)
                if (rawResult.isNaN() || rawResult.isInfinite()) {
                    _result.value = "Error"
                    isResultFresh = true
                } else {
                    val formatted = formatResult(rawResult, decimalPrecision.value)
                    _result.value = formatted
                    isResultFresh = true
                    
                    val isSci = checkIsScientific(currentExpression)
                    historyRepository.insert(
                        HistoryEntity(
                            expression = currentExpression,
                            result = formatted,
                            isScientific = isSci,
                            calculationType = if (isSci) "scientific" else "standard",
                            deviceSource = "Android Device"
                        )
                    )
                }
            } catch (e: Exception) {
                _result.value = "Error"
                isResultFresh = true
            }
        }
    }

    private fun formatResult(value: Double, precision: Int): String {
        if (abs(value - round(value)) < 1e-11) {
            val longVal = value.toLong()
            if (longVal in -9223372036854775807..9223372036854775807) {
                return longVal.toString()
            }
        }
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = "#." + "#".repeat(precision)
        val df = DecimalFormat(pattern, symbols)
        return df.format(value)
    }

    private fun checkIsScientific(expr: String): Boolean {
        val scientificTokens = listOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "^", "!", "π", "e")
        return scientificTokens.any { expr.contains(it) }
    }

    class Factory(
        private val historyRepository: HistoryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CalculatorViewModel(historyRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
