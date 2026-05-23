package com.example.util

import kotlin.math.*

class CalculatorEngine {
    companion object {
        fun evaluate(expression: String, isDegree: Boolean): Double {
            if (expression.isBlank()) return 0.0
            
            // Core sanitization for calculator interface elements
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", Math.PI.toString())
                .replace("e", Math.E.toString())

            return Parser(sanitized, isDegree).parse()
        }

        fun factorial(n: Double): Double {
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
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm() // addition
                else if (eat('-'.code)) x -= parseTerm() // subtraction
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor() // multiplication
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor // division
                }
                else if (eat('%'.code)) {
                    val divisor = parseFactor()
                    x %= divisor // modulus
                }
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                val token = str.substring(startPos, this.pos)
                x = token.toDoubleOrNull() ?: 0.0
            } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, this.pos)
                x = parseFactor()
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
            } else {
                throw RuntimeException("Unexpected element: " + ch.toChar())
            }

            // Exponents
            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }
            
            // Factorials (trailing notation)
            while (eat('!'.code)) {
                x = CalculatorEngine.factorial(x)
            }

            return x
        }
    }
}
