package com.example

import com.example.util.CalculatorEngine
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import kotlin.math.abs

/**
 * Robust unit tests verifying all mathematical operations, parsing priority scenarios,
 * limits, domains, and edge cases inside CalculatorEngine.
 */
class ExampleUnitTest {

  private val precisionDelta = 1e-9

  @Test
  fun testAdditionAndSubtraction() {
    assertEquals(5.0, CalculatorEngine.evaluate("2 + 3", false), precisionDelta)
    assertEquals(-1.0, CalculatorEngine.evaluate("2 - 3", false), precisionDelta)
    assertEquals(10.5, CalculatorEngine.evaluate("5.5 + 5", false), precisionDelta)
  }

  @Test
  fun testMultiplicationAndDivision() {
    assertEquals(6.0, CalculatorEngine.evaluate("2 * 3", false), precisionDelta)
    assertEquals(2.5, CalculatorEngine.evaluate("5 / 2", false), precisionDelta)
    assertEquals(1.5, CalculatorEngine.evaluate("3 * 0.5", false), precisionDelta)
  }

  @Test
  fun testOperatorPrecedenceAndParentheses() {
    // 2 + 3 * 4 should be 14, not 20
    assertEquals(14.0, CalculatorEngine.evaluate("2 + 3 * 4", false), precisionDelta)
    // (2 + 3) * 4 should be 20
    assertEquals(20.0, CalculatorEngine.evaluate("(2 + 3) * 4", false), precisionDelta)
    // Nested parentheses
    assertEquals(11.0, CalculatorEngine.evaluate("2 + (3 * (4 - 1))", false), precisionDelta)
  }

  @Test(expected = ArithmeticException::class)
  fun testDivisionByZeroThrowsException() {
    CalculatorEngine.evaluate("10 / 0", false)
  }

  @Test
  fun testPercentages() {
    assertEquals(0.85, CalculatorEngine.evaluate("85%", false), precisionDelta)
    assertEquals(550.0, CalculatorEngine.evaluate("500 + 10%", false), precisionDelta)
    assertEquals(30.0, CalculatorEngine.evaluate("200 * 15%", false), precisionDelta)
  }

  @Test
  fun testExponents() {
    assertEquals(8.0, CalculatorEngine.evaluate("2^3", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("5^0", false), precisionDelta)
    assertEquals(0.25, CalculatorEngine.evaluate("2^-2", false), precisionDelta)
  }

  @Test
  fun testFactorials() {
    assertEquals(120.0, CalculatorEngine.evaluate("5!", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("0!", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("1!", false), precisionDelta)
    
    // Test helper directly
    assertTrue(CalculatorEngine.factorial(2.5).isNaN())
    assertTrue(CalculatorEngine.factorial(-5.0).isNaN())
    assertEquals(Double.POSITIVE_INFINITY, CalculatorEngine.factorial(175.0), 0.0)
  }

  @Test
  fun testTrigonometricFunctionsInRadians() {
    assertEquals(0.0, CalculatorEngine.evaluate("sin(0)", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("cos(0)", false), precisionDelta)
    assertEquals(0.0, CalculatorEngine.evaluate("tan(0)", false), precisionDelta)
  }

  @Test
  fun testTrigonometricFunctionsInDegrees() {
    assertEquals(1.0, CalculatorEngine.evaluate("sin(90)", true), precisionDelta)
    assertEquals(0.5, CalculatorEngine.evaluate("sin(30)", true), precisionDelta)
    assertEquals(0.0, CalculatorEngine.evaluate("cos(90)", true), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("tan(45)", true), precisionDelta)
  }

  @Test
  fun testInverseTrigonometricFunctions() {
    assertEquals(90.0, CalculatorEngine.evaluate("asin(1)", true), precisionDelta)
    assertEquals(0.0, CalculatorEngine.evaluate("acos(1)", true), precisionDelta)
    assertEquals(45.0, CalculatorEngine.evaluate("atan(1)", true), precisionDelta)
  }

  @Test(expected = ArithmeticException::class)
  fun testArcsinDomainViolation() {
    CalculatorEngine.evaluate("asin(2.5)", false)
  }

  @Test(expected = ArithmeticException::class)
  fun testArccosDomainViolation() {
    CalculatorEngine.evaluate("acos(-1.1)", false)
  }

  @Test
  fun testLogarithms() {
    assertEquals(2.0, CalculatorEngine.evaluate("log(100)", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("ln(e)", false), precisionDelta)
  }

  @Test(expected = ArithmeticException::class)
  fun testLogOfZeroThrowsException() {
    CalculatorEngine.evaluate("log(0)", false)
  }

  @Test(expected = ArithmeticException::class)
  fun testLogOfNegativeThrowsException() {
    CalculatorEngine.evaluate("ln(-5)", false)
  }

  @Test
  fun testConstants() {
    assertEquals(Math.PI, CalculatorEngine.evaluate("π", false), precisionDelta)
    assertEquals(Math.E, CalculatorEngine.evaluate("e", false), precisionDelta)
  }

  @Test
  fun testSquareRoot() {
    assertEquals(4.0, CalculatorEngine.evaluate("sqrt(16)", false), precisionDelta)
    assertEquals(0.0, CalculatorEngine.evaluate("sqrt(0)", false), precisionDelta)
  }

  @Test(expected = ArithmeticException::class)
  fun testSquareRootOfNegativeThrows() {
    CalculatorEngine.evaluate("sqrt(-1)", false)
  }

  @Test
  fun testParenthesesAutoClosing() {
    assertEquals(5.0, CalculatorEngine.evaluate("(2 + 3", false), precisionDelta)
    assertEquals(1.0, CalculatorEngine.evaluate("sin(90", true), precisionDelta)
  }

  @Test(expected = RuntimeException::class)
  fun testUnmatchedClosingParenthesesThrows() {
    CalculatorEngine.evaluate("2 + 3)", false)
  }

  @Test
  fun testImplicitMultiplication() {
    assertEquals(2 * Math.PI, CalculatorEngine.evaluate("2π", false), precisionDelta)
    assertEquals(2 * Math.E, CalculatorEngine.evaluate("2e", false), precisionDelta)
    assertEquals(Math.PI * Math.E, CalculatorEngine.evaluate("πe", false), precisionDelta)
    assertEquals(6.0, CalculatorEngine.evaluate("(2)(3)", false), precisionDelta)
    assertEquals(6.0, CalculatorEngine.evaluate("2(3)", false), precisionDelta)
    assertEquals(3.0, CalculatorEngine.evaluate("3sin(90)", true), precisionDelta)
    assertEquals(0.5 * Math.E, CalculatorEngine.evaluate("sin(30)e", true), precisionDelta)
  }

  @Test
  fun testScientificNotation() {
    assertEquals(123.0, CalculatorEngine.evaluate("1.23E2", false), precisionDelta)
    assertEquals(0.001, CalculatorEngine.evaluate("1e-3", false), precisionDelta)
    assertEquals(150.0, CalculatorEngine.evaluate("1.5E+2", false), precisionDelta)
  }

  @Test(expected = RuntimeException::class)
  fun testMultipleDecimalsThrows() {
    CalculatorEngine.evaluate("2.3.4", false)
  }

  @Test(expected = RuntimeException::class)
  fun testEmptyParenthesesThrows() {
    CalculatorEngine.evaluate("()", false)
  }

  @Ignore("% is a percentage operator, not modulo. `10 % 0` evaluates to 0.0, not an exception.")
  @Test(expected = ArithmeticException::class)
  fun testModuloByZeroThrows() {
    CalculatorEngine.evaluate("10 % 0", false)
  }

  @Test
  fun testPercentageOperator() {
    // Standalone: 15% = 0.15
    assertEquals(0.15, CalculatorEngine.evaluate("15%", false), precisionDelta)
    // Contextual add: 200 + 10% = 200 + 20 = 220
    assertEquals(220.0, CalculatorEngine.evaluate("200 + 10%", false), precisionDelta)
    // Multiplication: 200 * 50% = 200 * 0.5 = 100
    assertEquals(100.0, CalculatorEngine.evaluate("200 * 50%", false), precisionDelta)
  }
}

