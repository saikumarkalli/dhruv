package com.example

import com.example.util.CalculatorEngine
import com.example.util.ICalculatorEngine
import com.example.util.IUnitConverter
import com.example.util.UnitConverter
import com.example.util.LengthUnit
import com.example.util.MassUnit
import org.junit.Assert.*
import org.junit.Test

class SddSpecFirstTests {

    private val calculator: ICalculatorEngine = CalculatorEngine
    private val converter: IUnitConverter = UnitConverter
    private val precisionDelta = 1e-9

    // --- SPEC-001: Calculator Parser Engine Tests ---

    @Test
    fun evaluate_SanitizeCharacters_CorrectValue() {
        // Test replaces: × -> *, ÷ -> /, π -> Math.PI, e -> Math.E
        val result = calculator.evaluate("2 × 3 ÷ 2", false)
        assertEquals(3.0, result, precisionDelta)
        
        val resultPi = calculator.evaluate("π", false)
        assertEquals(Math.PI, resultPi, precisionDelta)

        val resultE = calculator.evaluate("e", false)
        assertEquals(Math.E, resultE, precisionDelta)
    }

    @Test
    fun evaluate_RecursiveTopDownParsing_CorrectValue() {
        // parseExpression -> parseTerm -> parseFactor
        val result = calculator.evaluate("2 + 3 * 4", false)
        assertEquals(14.0, result, precisionDelta)
    }

    @Test
    fun evaluate_OperatorPrecedence_CorrectValue() {
        // Multiplication before addition, parentheses override
        val result1 = calculator.evaluate("5 - 2 * 2", false) // 5 - 4 = 1
        assertEquals(1.0, result1, precisionDelta)

        val result2 = calculator.evaluate("(5 - 2) * 3", false) // 3 * 3 = 9
        assertEquals(9.0, result2, precisionDelta)
    }

    @Test
    fun evaluate_ParsesComplexFormula_CorrectValue() {
        // Parses complex scientific tokens
        val result = calculator.evaluate("sqrt(16) + 2^3 - 0!", false) // 4 + 8 - 1 = 11
        assertEquals(11.0, result, precisionDelta)
    }

    @Test
    fun evaluate_ReturnsDoublePrecision_CorrectValue() {
        // Computed Double precision matches expectations
        val result = calculator.evaluate("1 / 3.0", false)
        assertEquals(0.3333333333333333, result, precisionDelta)
    }

    @Test
    fun evaluate_UnaryOperators_CorrectValue() {
        // Unary plus and minus
        val resultMinus = calculator.evaluate("-5", false)
        assertEquals(-5.0, resultMinus, precisionDelta)

        val resultPlus = calculator.evaluate("+8", false)
        assertEquals(8.0, resultPlus, precisionDelta)
    }

    @Test
    fun evaluate_EmptyExpression_ReturnsZero() {
        // Empty or blank string returns 0.0
        val resultEmpty = calculator.evaluate("", false)
        assertEquals(0.0, resultEmpty, precisionDelta)

        val resultBlank = calculator.evaluate("   ", false)
        assertEquals(0.0, resultBlank, precisionDelta)
    }

    @Test
    fun factorial_NonInteger_ReturnsNaN() {
        // Factorial of non-integers returns NaN
        val result = calculator.factorial(2.5)
        assertTrue(result.isNaN())
    }

    @Test
    fun factorial_NegativeNumber_ReturnsNaN() {
        // Factorial of negative numbers returns NaN
        val result = calculator.factorial(-1.0)
        assertTrue(result.isNaN())
    }

    @Test
    fun factorial_Over170_ReturnsInfinity() {
        // Factorial of numbers > 170 returns Double.POSITIVE_INFINITY
        val result = calculator.factorial(175.0)
        assertEquals(Double.POSITIVE_INFINITY, result, 0.0)
    }

    @Test
    fun evaluate_DegreeTrig_ScalesToRadians() {
        // sin(90) in degrees is 1.0
        val result = calculator.evaluate("sin(90)", true)
        assertEquals(1.0, result, precisionDelta)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_DivisionByZero_ThrowsArithmeticException() {
        // Division by zero throws ArithmeticException
        calculator.evaluate("10 / 0", false)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_SqrtOfNegative_ThrowsArithmeticException() {
        // Square root of negative number throws ArithmeticException
        calculator.evaluate("sqrt(-4)", false)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_ArcsinDomainViolation_ThrowsArithmeticException() {
        // Arcsin input outside [-1.0, 1.0] throws ArithmeticException
        calculator.evaluate("asin(1.5)", false)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_ArccosDomainViolation_ThrowsArithmeticException() {
        // Arccos input outside [-1.0, 1.0] throws ArithmeticException
        calculator.evaluate("acos(-1.2)", false)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_LogOfZero_ThrowsArithmeticException() {
        // Log of non-positive number throws ArithmeticException
        calculator.evaluate("log(0)", false)
    }

    @Test(expected = ArithmeticException::class)
    fun evaluate_LnOfNegative_ThrowsArithmeticException() {
        // Ln of non-positive number throws ArithmeticException
        calculator.evaluate("ln(-5)", false)
    }

    @Test
    fun evaluate_UnmatchedParenthesis_AutoCloses() {
        // Unmatched parenthesis is auto-closed
        val result = calculator.evaluate("(2 + 3", false)
        assertEquals(5.0, result, precisionDelta)
    }

    @Test(expected = RuntimeException::class)
    fun evaluate_UnrecognizedText_ThrowsRuntimeException() {
        // Unrecognized character throws RuntimeException
        calculator.evaluate("2 x_opt 3", false)
    }

    // --- SPEC-003: Physical Unit Converter Tests ---

    @Test
    fun convertLength_ScaleToBase_CorrectValue() {
        // Km to Meters base conversion check: 2 km = 2000 m
        val baseVal = converter.convertLength(2.0, LengthUnit.KILOMETERS, LengthUnit.METERS)
        assertEquals(2000.0, baseVal, precisionDelta)
    }

    @Test
    fun convertLength_ScaleToTarget_CorrectValue() {
        // Meters to Inches: 1 meter = 39.37007874 inches
        val targetVal = converter.convertLength(1.0, LengthUnit.METERS, LengthUnit.INCHES)
        assertEquals(1.0 / 0.0254, targetVal, precisionDelta)
    }

    @Test
    fun convertMass_MilliGramsToPounds_CorrectValue() {
        // Milligrams to Pounds conversion check
        val result = converter.convertMass(1000000.0, MassUnit.MILLIGRAMS, MassUnit.POUNDS)
        // 1,000,000 mg = 1 kg. 1 kg / 0.45359237 lb = 2.2046226218 lb
        assertEquals(1.0 / 0.45359237, result, precisionDelta)
    }
}
