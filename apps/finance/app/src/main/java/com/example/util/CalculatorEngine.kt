package com.example.util

import kotlin.math.*

object CalculatorEngine : ICalculatorEngine {
    override fun evaluate(expression: String, isDegree: Boolean): Double {
        if (expression.isBlank()) return 0.0
        
        // Core sanitization for calculator interface elements
        var sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")

        // Auto-close any unclosed parentheses
        var openCount = 0
        for (ch in sanitized) {
            if (ch == '(') openCount++
            else if (ch == ')') {
                if (openCount > 0) openCount--
            }
        }
        sanitized += ")".repeat(openCount)

        return Parser(sanitized, isDegree).parse()
    }

    override fun factorial(n: Double): Double {
        if (n < 0.0) return Double.NaN
        if (n != floor(n)) {
            return Double.NaN // Only support integer factorials for simplification
        }
        val num = n.toInt()
        if (num > 170) return Double.POSITIVE_INFINITY // Limit of double limits
        var result = 1.0
        for (i in 1..num) {
            result *= i
        }
        return result
    }

    private class Parser(val str: String, val isDegree: Boolean) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected character at position $pos: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm(null, false)
            while (true) {
                if (eat('+'.code)) {
                    x += parseTerm(contextValue = x, isAdder = true)
                } else if (eat('-'.code)) {
                    x -= parseTerm(contextValue = x, isAdder = true)
                } else {
                    return x
                }
            }
        }

        private fun isNextImplicitMultiplication(): Boolean {
            var tempPos = pos
            var tempCh = ch
            while (tempCh == ' '.code) {
                if (++tempPos < str.length) {
                    tempCh = str[tempPos].code
                } else {
                    tempCh = -1
                }
            }
            return (tempCh == '('.code ||
                    (tempCh >= '0'.code && tempCh <= '9'.code) ||
                    tempCh == '.'.code ||
                    tempCh == 'π'.code ||
                    tempCh == 'e'.code ||
                    (tempCh >= 'a'.code && tempCh <= 'z'.code))
        }

        fun parseTerm(contextValue: Double? = null, isAdder: Boolean = false): Double {
            var x = parseFactor(contextValue, isAdder)
            while (true) {
                if (eat('*'.code)) {
                    x *= parseFactor(null, false)
                } else if (eat('/'.code)) {
                    val divisor = parseFactor(null, false)
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                } else if (isNextImplicitMultiplication()) {
                    x *= parseFactor(null, false)
                } else {
                    return x
                }
            }
        }

        fun parseFactor(contextValue: Double? = null, isAdder: Boolean = false): Double {
            if (eat('+'.code)) return parseFactor(contextValue, isAdder) // unary plus
            if (eat('-'.code)) return -parseFactor(contextValue, isAdder) // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                // Check for empty parentheses
                var tempPos = pos
                var tempCh = ch
                while (tempCh == ' '.code) {
                    if (++tempPos < str.length) {
                        tempCh = str[tempPos].code
                    } else {
                        tempCh = -1
                    }
                }
                if (tempCh == ')'.code) {
                    throw RuntimeException("Empty parentheses")
                }

                x = parseExpression()
                if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
            } else if (eat('π'.code)) {
                x = Math.PI
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code || ch == 'e'.code || ch == 'E'.code) {
                    if (ch == 'e'.code || ch == 'E'.code) {
                        // Check if followed by digit, or +/- followed by digit
                        var isExponent = false
                        if (pos + 1 < str.length) {
                            val next = str[pos + 1]
                            if (next.isDigit()) {
                                isExponent = true
                            } else if ((next == '+' || next == '-') && pos + 2 < str.length) {
                                if (str[pos + 2].isDigit()) {
                                    isExponent = true
                                }
                            }
                        }
                        if (!isExponent) {
                            break
                        }
                    }
                    val prevCh = ch
                    nextChar()
                    if ((prevCh == 'e'.code || prevCh == 'E'.code) && (ch == '+'.code || ch == '-'.code)) {
                        nextChar()
                    }
                }
                val token = str.substring(startPos, this.pos)
                x = token.toDoubleOrNull() ?: throw RuntimeException("Invalid number format: $token")
            } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions and constant e
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                if (func == "e") {
                    x = Math.E
                } else {
                    x = parseFactor(null, false)
                    x = when (func) {
                        "sqrt" -> {
                            if (x < 0.0) throw ArithmeticException("Square root of negative number")
                            sqrt(x)
                        }
                        "sin" -> {
                            val angle = if (isDegree) Math.toRadians(x) else x
                            sin(angle)
                        }
                        "cos" -> {
                            val angle = if (isDegree) Math.toRadians(x) else x
                            cos(angle)
                        }
                        "tan" -> {
                            val angle = if (isDegree) Math.toRadians(x) else x
                            tan(angle)
                        }
                        "asin" -> {
                            if (x < -1.0 || x > 1.0) throw ArithmeticException("Arcsin domain violation")
                            val rad = asin(x)
                            if (isDegree) Math.toDegrees(rad) else rad
                        }
                        "acos" -> {
                            if (x < -1.0 || x > 1.0) throw ArithmeticException("Arccos domain violation")
                            val rad = acos(x)
                            if (isDegree) Math.toDegrees(rad) else rad
                        }
                        "atan" -> {
                            val rad = atan(x)
                            if (isDegree) Math.toDegrees(rad) else rad
                        }
                        "log" -> {
                            if (x <= 0.0) throw ArithmeticException("Log of non-positive number")
                            log10(x)
                        }
                        "ln" -> {
                            if (x <= 0.0) throw ArithmeticException("Log of non-positive number")
                            ln(x)
                        }
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                }
            } else {
                throw RuntimeException("Unexpected element: " + ch.toChar())
            }

            // Exponents
            if (eat('^'.code)) {
                x = x.pow(parseFactor(null, false))
            }
            
            // Factorials (trailing notation)
            while (eat('!'.code)) {
                x = CalculatorEngine.factorial(x)
            }

            // Percentage (trailing notation)
            while (eat('%'.code)) {
                x = if (isAdder && contextValue != null) {
                    contextValue * (x / 100.0)
                } else {
                    x / 100.0
                }
            }

            return x
        }
    }
}
