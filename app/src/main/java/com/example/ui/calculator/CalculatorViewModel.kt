package com.example.ui.calculator

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import com.example.util.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

import com.example.data.GeminiRepository

sealed interface AiExplanationState {
    object Idle : AiExplanationState
    object Loading : AiExplanationState
    data class Success(val explanation: String) : AiExplanationState
    data class Error(val message: String) : AiExplanationState
}

class CalculatorViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _inputState = MutableStateFlow(TextFieldValue(""))
    val inputState: StateFlow<TextFieldValue> = _inputState.asStateFlow()

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result.asStateFlow()

    private val _isResultFinalised = MutableStateFlow(false)
    val isResultFinalised: StateFlow<Boolean> = _isResultFinalised.asStateFlow()

    private val _lastExpression = MutableStateFlow("")
    val lastExpression: StateFlow<String> = _lastExpression.asStateFlow()

    private val _shakeEvent = MutableSharedFlow<Unit>()
    val shakeEvent: SharedFlow<Unit> = _shakeEvent.asSharedFlow()

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
    val formatLocale: StateFlow<String> = settingsRepository.formatLocale

    private val _aiExplanation = MutableStateFlow<AiExplanationState>(AiExplanationState.Idle)
    val aiExplanation: StateFlow<AiExplanationState> = _aiExplanation.asStateFlow()

    fun explainCurrentResult() {
        val expr = _input.value
        val res = _result.value
        if (expr.isBlank() || res.isBlank() || res.startsWith("Error")) return

        viewModelScope.launch {
            _aiExplanation.value = AiExplanationState.Loading
            geminiRepository.explainCalculation(expr, res)
                .onSuccess { explanation ->
                    _aiExplanation.value = AiExplanationState.Success(explanation)
                }
                .onFailure { exception ->
                    _aiExplanation.value = AiExplanationState.Error(exception.localizedMessage ?: "Failed to get explanation")
                }
        }
    }

    fun clearAiExplanation() {
        _aiExplanation.value = AiExplanationState.Idle
    }

    private var isResultFresh = false

    init {
        // Auto remove items in recycle bin older than 30 days
        viewModelScope.launch {
            historyRepository.pruneOldRecycleBin()
        }
    }

    fun updateInputState(newValue: TextFieldValue) {
        if (newValue.text.length > 50) {
            triggerShake()
            return
        }
        _inputState.value = newValue
        _input.value = newValue.text
        _isResultFinalised.value = false
        updateLivePreview()
    }

    private fun triggerShake() {
        viewModelScope.launch {
            _shakeEvent.emit(Unit)
        }
    }

    fun onKeyPress(key: String) {
        if (key != "=") {
            _isResultFinalised.value = false
        }
        val currentTextFieldValue = _inputState.value
        val currentInput = currentTextFieldValue.text
        val selection = currentTextFieldValue.selection
        val currentResult = _result.value

        val hasError = currentResult.startsWith("Error")
        if (hasError && (key.firstOrNull()?.isDigit() == true || key == "." || key == "sin" || key == "cos" || key == "tan" || key == "asin" || key == "acos" || key == "atan" || key == "log" || key == "ln" || key == "sqrt" || key == "(")) {
            _inputState.value = TextFieldValue("")
            _input.value = ""
            _result.value = ""
            isResultFresh = false
        }

        if (isResultFresh) {
            isResultFresh = false
            _isResultFinalised.value = false
            val hasErrorBefore = currentResult.startsWith("Error")
            when (key) {
                "+", "-", "×", "÷", "%", "^" -> {
                    if (!hasErrorBefore && currentInput.isNotEmpty()) {
                        val newText = currentInput + key
                        val newVal = TextFieldValue(newText, TextRange(newText.length))
                        _inputState.value = newVal
                        _input.value = newText
                        _result.value = ""
                        updateLivePreview()
                        return
                    }
                }
                "AC", "C", "⌫" -> {
                    _inputState.value = TextFieldValue("")
                    _input.value = ""
                    _result.value = ""
                    return
                }
                "=" -> {
                    return
                }
                "±" -> {
                    if (!hasErrorBefore && currentInput.isNotEmpty()) {
                        val newText = if (currentInput.startsWith("-")) {
                            currentInput.removePrefix("-")
                        } else {
                            "-$currentInput"
                        }
                        val newVal = TextFieldValue(newText, TextRange(newText.length))
                        _inputState.value = newVal
                        _input.value = newText
                        _result.value = ""
                        updateLivePreview()
                        return
                    }
                }
                "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> {
                    val newText = if (!hasErrorBefore && currentInput.isNotEmpty()) {
                        "$key($currentInput)"
                    } else {
                        "$key("
                    }
                    val newVal = TextFieldValue(newText, TextRange(newText.length))
                    _inputState.value = newVal
                    _input.value = newText
                    _result.value = ""
                    updateLivePreview()
                    return
                }
                "." -> {
                    val newText = "0."
                    val newVal = TextFieldValue(newText, TextRange(newText.length))
                    _inputState.value = newVal
                    _input.value = newText
                    _result.value = ""
                    updateLivePreview()
                    return
                }
                else -> {
                    val newText = key
                    val newVal = TextFieldValue(newText, TextRange(newText.length))
                    _inputState.value = newVal
                    _input.value = newText
                    _result.value = ""
                    updateLivePreview()
                    return
                }
            }
        }

        val start = selection.min
        val end = selection.max

        when (key) {
            "AC", "C" -> {
                _inputState.value = TextFieldValue("")
                _input.value = ""
                _result.value = ""
            }
            "⌫" -> {
                if (start > 0 || start != end) {
                    val newText: String
                    val newCursorPos: Int
                    if (start != end) {
                        newText = currentInput.substring(0, start) + currentInput.substring(end)
                        newCursorPos = start
                    } else {
                        val beforeCursor = currentInput.substring(0, start)
                        val functions = listOf("asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(")
                        var deleteLength = 1
                        for (func in functions) {
                            if (beforeCursor.endsWith(func)) {
                                deleteLength = func.length
                                break
                            }
                        }
                        newText = currentInput.substring(0, start - deleteLength) + currentInput.substring(start)
                        newCursorPos = start - deleteLength
                    }
                    val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                    _inputState.value = newVal
                    _input.value = newText
                    updateLivePreview()
                }
            }
            "=" -> {
                evaluateExpression()
            }
            "±" -> {
                val beforeCursor = currentInput.substring(0, start)
                val toggledBefore = toggleLastOperand(beforeCursor)
                val newText = toggledBefore + currentInput.substring(end)
                val newCursorPos = toggledBefore.length
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
            "+", "-", "×", "÷", "%", "^" -> {
                val beforeCursor = currentInput.substring(0, start)
                val appendedBefore = appendOperator(beforeCursor, key)
                val newText = appendedBefore + currentInput.substring(end)
                val newCursorPos = appendedBefore.length
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
            "." -> {
                val beforeCursor = currentInput.substring(0, start)
                val appendedBefore = appendDecimal(beforeCursor)
                val newText = appendedBefore + currentInput.substring(end)
                val newCursorPos = appendedBefore.length
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
            "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt" -> {
                val insertStr = "$key("
                if (currentInput.length + insertStr.length > 50) {
                    triggerShake()
                    return
                }
                val newText = currentInput.substring(0, start) + insertStr + currentInput.substring(end)
                val newCursorPos = start + insertStr.length
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
            "(" -> {
                val insertStr = "()"
                if (currentInput.length + insertStr.length > 50) {
                    triggerShake()
                    return
                }
                val newText = currentInput.substring(0, start) + insertStr + currentInput.substring(end)
                val newCursorPos = start + 1
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
            else -> {
                val insertLen = key.length
                if (currentInput.length + insertLen > 50) {
                    triggerShake()
                    return
                }
                val newText = currentInput.substring(0, start) + key + currentInput.substring(end)
                val newCursorPos = start + insertLen
                val newVal = TextFieldValue(newText, TextRange(newCursorPos))
                _inputState.value = newVal
                _input.value = newText
                updateLivePreview()
            }
        }
    }

    private fun toggleLastOperand(expr: String): String {
        if (expr.isEmpty()) return "-"
        
        val trimmed = expr.trimEnd()
        if (trimmed.isEmpty()) return expr
        
        var end = trimmed.length - 1
        var hasFactorial = false
        if (trimmed[end] == '!') {
            hasFactorial = true
            end--
        }
        
        var start = -1
        if (end >= 0 && trimmed[end] == ')') {
            var depth = 0
            var i = end
            while (i >= 0) {
                if (trimmed[i] == ')') depth++
                else if (trimmed[i] == '(') {
                    depth--
                    if (depth == 0) {
                        start = i
                        break
                    }
                }
                i--
            }
            if (start != -1) {
                var j = start - 1
                while (j >= 0 && trimmed[j] in 'a'..'z') {
                    j--
                }
                start = j + 1
            }
        } else if (end >= 0 && (trimmed[end] == 'π' || trimmed[end] == 'e')) {
            start = end
        } else if (end >= 0 && (trimmed[end].isDigit() || trimmed[end] == '.')) {
            var i = end
            while (i >= 0 && (trimmed[i].isDigit() || trimmed[i] == '.')) {
                i--
            }
            start = i + 1
        }
        
        val operators = setOf("+", "-", "×", "÷", "%", "^")
        if (start == -1 || start > end) {
            if (trimmed.endsWith("-")) {
                val beforeMinus = trimmed.dropLast(1).trimEnd()
                val isUnary = beforeMinus.isEmpty() || beforeMinus.last().toString() in operators || beforeMinus.last() == '('
                if (isUnary) {
                    return beforeMinus
                }
            }
            return expr + "-"
        }
        
        val operand = trimmed.substring(start)
        val prefix = trimmed.substring(0, start)
        
        val trimmedPrefix = prefix.trimEnd()
        if (trimmedPrefix.endsWith("-")) {
            val beforeMinus = trimmedPrefix.dropLast(1).trimEnd()
            val isUnary = beforeMinus.isEmpty() || beforeMinus.last().toString() in operators || beforeMinus.last() == '('
            if (isUnary) {
                return beforeMinus + operand
            }
        }
        
        return prefix + "-" + operand
    }

    private fun appendOperator(expr: String, op: String): String {
        if (expr.isEmpty()) {
            return if (op == "-") "-" else "0$op"
        }
        
        val operators = setOf("+", "-", "×", "÷", "%", "^")
        val trimmed = expr.trimEnd()
        if (trimmed.isEmpty()) return expr
        
        if (trimmed.endsWith("(")) {
            return if (op == "-") trimmed + "-" else expr
        }
        
        val lastChar = trimmed.last().toString()
        if (lastChar in operators) {
            if (op == "-") {
                if (lastChar == "-") {
                    return expr
                }
                if (lastChar == "+") {
                    return trimmed.dropLast(1) + "-"
                }
                return trimmed + "-"
            } else {
                if (trimmed.length >= 2) {
                    val secondLastChar = trimmed[trimmed.length - 2].toString()
                    if (secondLastChar in operators && lastChar == "-") {
                        return trimmed.dropLast(2) + op
                    }
                }
                return trimmed.dropLast(1) + op
            }
        }
        
        return expr + op
    }

    private fun appendDecimal(expr: String): String {
        if (expr.isEmpty()) return "0."
        
        val trimmed = expr.trimEnd()
        if (trimmed.isEmpty()) return "0."
        
        var i = trimmed.length - 1
        while (i >= 0 && (trimmed[i].isDigit() || trimmed[i] == '.')) {
            if (trimmed[i] == '.') {
                return expr
            }
            i--
        }
        
        val lastChar = trimmed.last()
        return if (lastChar.isDigit()) {
            expr + "."
        } else {
            expr + "0."
        }
    }

    fun restoreEquation(expression: String, resultStr: String) {
        val rawRes = resultStr.replace(",", "")
        _inputState.value = TextFieldValue(expression, TextRange(expression.length))
        _input.value = expression
        _result.value = rawRes
        isResultFresh = true
        _isResultFinalised.value = false
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

    private fun updateLivePreview() {
        val currentExpression = _inputState.value.text
        if (currentExpression.isBlank()) {
            _result.value = ""
            return
        }

        try {
            val rawResult = CalculatorEngine.evaluate(currentExpression, isDegree.value)
            if (rawResult.isNaN() || rawResult.isInfinite()) {
                _result.value = ""
            } else {
                _result.value = formatResult(rawResult, decimalPrecision.value)
            }
        } catch (e: ArithmeticException) {
            _result.value = "Error: ÷0"
        } catch (e: Exception) {
            _result.value = ""
        }
    }

    private fun evaluateExpression() {
        val currentExpression = _inputState.value.text
        if (currentExpression.isBlank()) return

        viewModelScope.launch {
            try {
                val rawResult = CalculatorEngine.evaluate(currentExpression, isDegree.value)
                if (rawResult.isNaN() || rawResult.isInfinite()) {
                    _result.value = "Error"
                    _lastExpression.value = currentExpression
                    _isResultFinalised.value = true
                    isResultFresh = true
                } else {
                    val formatted = formatResult(rawResult, decimalPrecision.value)

                    // Auto-close open parentheses in history expression
                    var autoClosedExpression = currentExpression
                    var openCount = 0
                    for (ch in autoClosedExpression) {
                        if (ch == '(') openCount++
                        else if (ch == ')') {
                            if (openCount > 0) openCount--
                        }
                    }
                    if (openCount > 0) {
                        autoClosedExpression += ")".repeat(openCount)
                    }

                    val isSci = checkIsScientific(autoClosedExpression)
                    historyRepository.insert(
                        HistoryEntity(
                            expression = autoClosedExpression,
                            result = formatLocaleSeparator(formatted, formatLocale.value),
                            isScientific = isSci,
                            calculationType = if (isSci) "scientific" else "standard",
                            deviceSource = "Android Device"
                        )
                    )

                    _lastExpression.value = autoClosedExpression
                    _isResultFinalised.value = true
                    _inputState.value = TextFieldValue(formatted, TextRange(formatted.length))
                    _input.value = formatted
                    _result.value = ""
                    isResultFresh = true
                }
            } catch (e: ArithmeticException) {
                _result.value = "Error: ÷0"
                _lastExpression.value = currentExpression
                _isResultFinalised.value = true
                isResultFresh = true
            } catch (e: Exception) {
                _result.value = "Error"
                _lastExpression.value = currentExpression
                _isResultFinalised.value = true
                isResultFresh = true
            }
        }
    }

    fun formatLocaleSeparator(numberStr: String, format: String): String {
        if (numberStr.isEmpty() || numberStr == "Error" || numberStr.startsWith("Error")) return numberStr
        if (numberStr.contains("E") || numberStr.contains("e") || numberStr.contains("Infinity") || numberStr.contains("NaN")) return numberStr
        
        val parts = numberStr.split(".")
        val intPart = parts[0]
        val isNegative = intPart.startsWith("-")
        val cleanInt = if (isNegative) intPart.substring(1) else intPart
        
        val formattedInt = if (format == "indian") {
            if (cleanInt.length <= 3) {
                cleanInt
            } else {
                val last3 = cleanInt.substring(cleanInt.length - 3)
                val remaining = cleanInt.substring(0, cleanInt.length - 3)
                val sb = StringBuilder()
                var count = 0
                for (i in remaining.length - 1 downTo 0) {
                    sb.insert(0, remaining[i])
                    count++
                    if (count == 2 && i > 0) {
                        sb.insert(0, ',')
                        count = 0
                    }
                }
                sb.append(',').append(last3).toString()
            }
        } else {
            val sb = StringBuilder()
            var count = 0
            for (i in cleanInt.length - 1 downTo 0) {
                sb.insert(0, cleanInt[i])
                count++
                if (count == 3 && i > 0) {
                    sb.insert(0, ',')
                    count = 0
                }
            }
            sb.toString()
        }
        
        val finalInt = if (isNegative) "-$formattedInt" else formattedInt
        return if (parts.size > 1) {
            "$finalInt.${parts[1]}"
        } else {
            finalInt
        }
    }

    private fun formatResult(value: Double, precision: Int): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        
        val roundedValue = try {
            val bd = java.math.BigDecimal(value)
            bd.setScale(12, java.math.RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            value
        }

        val absVal = abs(roundedValue)
        
        if (absVal >= 1e15 || (absVal > 0.0 && absVal < 1e-4)) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("0.######E0", symbols)
            return df.format(roundedValue)
        }
        
        if (abs(roundedValue - round(roundedValue)) < 1e-11) {
            val longVal = roundedValue.toLong()
            if (longVal in -9223372036854775807..9223372036854775807) {
                return longVal.toString()
            }
        }
        
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = "#." + "#".repeat(precision)
        val df = DecimalFormat(pattern, symbols)
        return df.format(roundedValue)
    }

    private fun checkIsScientific(expr: String): Boolean {
        val scientificTokens = listOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "^", "!", "π", "e")
        return scientificTokens.any { expr.contains(it) }
    }

    fun deleteLastToken() {
        val current = _inputState.value.text
        if (current.isEmpty()) return
        val operators = setOf('+', '-', '×', '÷', '%', '^')
        var i = current.length - 1
        while (i >= 0 && current[i] == ' ') i--
        if (i < 0) {
            updateInputState(TextFieldValue(""))
            return
        }
        if (current[i] in operators) {
            val newText = current.substring(0, i)
            updateInputState(TextFieldValue(newText, TextRange(newText.length)))
        } else {
            while (i >= 0 && current[i] != ' ' && current[i] !in operators) {
                i--
            }
            val newText = current.substring(0, i + 1).trimEnd()
            updateInputState(TextFieldValue(newText, TextRange(newText.length)))
        }
    }

    class Factory(
        private val historyRepository: HistoryRepository,
        private val settingsRepository: SettingsRepository,
        private val geminiRepository: GeminiRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CalculatorViewModel(historyRepository, settingsRepository, geminiRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
