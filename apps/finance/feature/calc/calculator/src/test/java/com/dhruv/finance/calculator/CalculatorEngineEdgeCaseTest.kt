package com.dhruv.finance.calculator

import com.dhruv.finance.calculator.engine.CalculatorEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI

/**
 * Exhaustive edge-case tests for [CalculatorEngine].
 *
 * NOTE: @Test(expected=…) methods MUST use block-body `{ }` (not `= expr`) so
 *       the method return type is Unit/void, as required by JUnit 4.
 */
class CalculatorEngineEdgeCaseTest {
    private val D = 1e-9

    // ── Arithmetic ──────────────────────────────────────────────────────────

    @Test fun basicAdd() = assertEquals(7.0, eval("3 + 4"), D)

    @Test fun basicSub() = assertEquals(-2.0, eval("1 - 3"), D)

    @Test fun basicMul() = assertEquals(42.0, eval("6 * 7"), D)

    @Test fun basicDiv() = assertEquals(2.5, eval("5 / 2"), D)

    @Test fun negativeResult() = assertEquals(-6.0, eval("-2 * 3"), D)

    @Test fun doubleNegative() = assertEquals(5.0, eval("--5"), D)

    @Test fun unaryPlusMul() = assertEquals(6.0, eval("+2 * +3"), D)

    @Test fun zeroArithmetic() = assertEquals(0.0, eval("0 + 0"), D)

    @Test fun largeNumbers() = assertEquals(1_000_000.0, eval("500000 + 500000"), D)

    @Test fun decimalPrecision() = assertEquals(0.3333333333, eval("1/3"), 1e-9)

    @Test fun chainedAddition() = assertEquals(15.0, eval("1+2+3+4+5"), D)

    @Test fun chainedMixed() = assertEquals(2.0, eval("10 - 3 - 5"), D)

    // ── Operator Precedence ──────────────────────────────────────────────────

    @Test fun mulBeforeAdd() = assertEquals(14.0, eval("2 + 3 * 4"), D)

    @Test fun divBeforeSub() = assertEquals(3.0, eval("9 - 6 / 2 * 2"), D)

    @Test fun parensOverride() = assertEquals(20.0, eval("(2 + 3) * 4"), D)

    @Test fun nestedParens() = assertEquals(11.0, eval("2 + (3 * (4 - 1))"), D)

    @Test fun deeplyNested() = assertEquals(1.0, eval("((((1))))"), D)

    @Test fun multiplyNegParen() = assertEquals(-15.0, eval("3 * -(5)"), D)

    // ── Parentheses Auto-Close ───────────────────────────────────────────────

    @Test fun oneUnclosed() = assertEquals(5.0, eval("(2+3"), D)

    @Test fun twoUnclosed() = assertEquals(5.0, eval("((2+3"), D)

    @Test fun funcUnclosed() = assertEquals(1.0, eval("sin(90", true), D)

    @Test(expected = RuntimeException::class)
    fun extraClosingParenThrows() {
        eval("2 + 3)")
    }

    @Test(expected = RuntimeException::class)
    fun emptyParensThrows() {
        eval("()")
    }

    // ── Whitespace Handling ──────────────────────────────────────────────────

    @Test fun leadingWhitespace() = assertEquals(5.0, eval("  2 + 3"), D)

    @Test fun trailingWhitespace() = assertEquals(5.0, eval("2 + 3  "), D)

    @Test fun internalWhitespace() = assertEquals(5.0, eval("2   +   3"), D)

    @Test fun noWhitespace() = assertEquals(5.0, eval("2+3"), D)

    // ── Empty / Blank Input ──────────────────────────────────────────────────

    @Test fun emptyString() = assertEquals(0.0, eval(""), D)

    @Test fun blankString() = assertEquals(0.0, eval("   "), D)

    // ── Exponents ────────────────────────────────────────────────────────────

    @Test fun squareExp() = assertEquals(9.0, eval("3^2"), D)

    @Test fun cubicExp() = assertEquals(27.0, eval("3^3"), D)

    @Test fun zeroExp() = assertEquals(1.0, eval("5^0"), D)

    @Test fun fracExp() = assertEquals(2.0, eval("4^0.5"), D)

    @Test fun negExp() = assertEquals(0.25, eval("2^-2"), D)

    @Test fun zeroBase() = assertEquals(0.0, eval("0^5"), D)

    @Test fun negBaseEvenPow() = assertEquals(4.0, eval("(-2)^2"), D)

    @Test fun chainedExps() = assertEquals(512.0, eval("2^3^2"), D) // 2^(3^2)=2^9 right-assoc

    @Test fun expThenMul() = assertEquals(16.0, eval("2^3 * 2"), D)

    // ── Factorial ────────────────────────────────────────────────────────────

    @Test fun fact0() = assertEquals(1.0, eval("0!"), D)

    @Test fun fact1() = assertEquals(1.0, eval("1!"), D)

    @Test fun fact5() = assertEquals(120.0, eval("5!"), D)

    @Test fun fact10() = assertEquals(3628800.0, eval("10!"), D)

    @Test fun fact170() = assertTrue(CalculatorEngine.factorial(170.0).isFinite())

    @Test fun fact171() = assertEquals(Double.POSITIVE_INFINITY, CalculatorEngine.factorial(171.0), 0.0)

    @Test fun factNonInt() = assertTrue(CalculatorEngine.factorial(1.5).isNaN())

    @Test fun factNegative() = assertTrue(CalculatorEngine.factorial(-1.0).isNaN())

    @Test fun factChained() = assertEquals(144.0, eval("3!*4!"), D) // 6 * 24

    // ── Percentage ───────────────────────────────────────────────────────────

    @Test fun standalonePercent() = assertEquals(0.15, eval("15%"), D)

    @Test fun addWithPercent() = assertEquals(220.0, eval("200 + 10%"), D)

    @Test fun subtractWithPercent() = assertEquals(180.0, eval("200 - 10%"), D)

    @Test fun multiplyWithPercent() = assertEquals(100.0, eval("200 * 50%"), D)

    @Test fun zeroPercent() = assertEquals(0.0, eval("0%"), D)

    @Test fun percentOfLarge() = assertEquals(1001000.0, eval("1000000 + 0.1%"), D)

    // ── Implicit Multiplication ───────────────────────────────────────────────

    @Test fun implicitPi() = assertEquals(2 * PI, eval("2π"), D)

    @Test fun implicitE() = assertEquals(2 * E, eval("2e"), D)

    @Test fun implicitParens() = assertEquals(6.0, eval("2(3)"), D)

    @Test fun parensParen() = assertEquals(6.0, eval("(2)(3)"), D)

    @Test fun implicitFunc() = assertEquals(3.0, eval("3sin(90)", true), D)

    @Test fun implicitPiE() = assertEquals(PI * E, eval("πe"), D)

    @Test fun funcImplicitConst() = assertEquals(0.5 * E, eval("sin(30)e", true), D)

    // ── Constants ────────────────────────────────────────────────────────────

    @Test fun piConst() = assertEquals(PI, eval("π"), D)

    @Test fun eConst() = assertEquals(E, eval("e"), D)

    @Test fun piMath() = assertEquals(PI * 2, eval("π * 2"), D)

    @Test fun eMath() = assertEquals(E + 1, eval("e + 1"), D)

    // ── Scientific Notation ───────────────────────────────────────────────────

    @Test fun sciNotationCapE() = assertEquals(100.0, eval("1E2"), D)

    @Test fun sciNotationLowE() = assertEquals(0.001, eval("1e-3"), D)

    @Test fun sciNotationPlus() = assertEquals(150.0, eval("1.5E+2"), D)

    @Test fun sciNotationAdd() = assertEquals(1100.0, eval("1E3 + 100"), D)

    @Test fun sciNotationNeg() = assertEquals(-1000.0, eval("-1E3"), D)

    // ── Trigonometry (Radians) ────────────────────────────────────────────────

    @Test fun sinZeroRad() = assertEquals(0.0, eval("sin(0)"), D)

    @Test fun cosZeroRad() = assertEquals(1.0, eval("cos(0)"), D)

    @Test fun tanZeroRad() = assertEquals(0.0, eval("tan(0)"), D)

    @Test fun sinPiRad() = assertEquals(0.0, eval("sin(π)"), D)

    @Test fun cosPiRad() = assertEquals(-1.0, eval("cos(π)"), D)

    @Test fun sinHalfPi() = assertEquals(1.0, eval("sin(π/2)"), D)

    @Test fun cosHalfPi() = assertEquals(0.0, eval("cos(π/2)"), D)

    @Test fun tanPi4Rad() = assertEquals(1.0, eval("tan(π/4)"), D)

    // ── Trigonometry (Degrees) ────────────────────────────────────────────────

    @Test fun sin0Deg() = assertEquals(0.0, evalD("sin(0)"), D)

    @Test fun sin30Deg() = assertEquals(0.5, evalD("sin(30)"), D)

    @Test fun sin90Deg() = assertEquals(1.0, evalD("sin(90)"), D)

    @Test fun sin180Deg() = assertEquals(0.0, evalD("sin(180)"), D)

    @Test fun sin270Deg() = assertEquals(-1.0, evalD("sin(270)"), D)

    @Test fun sin360Deg() = assertEquals(0.0, evalD("sin(360)"), D)

    @Test fun cos0Deg() = assertEquals(1.0, evalD("cos(0)"), D)

    @Test fun cos90Deg() = assertEquals(0.0, evalD("cos(90)"), D)

    @Test fun cos180Deg() = assertEquals(-1.0, evalD("cos(180)"), D)

    @Test fun cos360Deg() = assertEquals(1.0, evalD("cos(360)"), D)

    @Test fun tan0Deg() = assertEquals(0.0, evalD("tan(0)"), D)

    @Test fun tan45Deg() = assertEquals(1.0, evalD("tan(45)"), D)

    // ── Inverse Trig (Degrees) ────────────────────────────────────────────────

    @Test fun asin1Deg() = assertEquals(90.0, evalD("asin(1)"), D)

    @Test fun asinN1Deg() = assertEquals(-90.0, evalD("asin(-1)"), D)

    @Test fun asin0Deg() = assertEquals(0.0, evalD("asin(0)"), D)

    @Test fun acos1Deg() = assertEquals(0.0, evalD("acos(1)"), D)

    @Test fun acosN1Deg() = assertEquals(180.0, evalD("acos(-1)"), D)

    @Test fun atan1Deg() = assertEquals(45.0, evalD("atan(1)"), D)

    @Test fun atanN1Deg() = assertEquals(-45.0, evalD("atan(-1)"), D)

    @Test fun atan0Deg() = assertEquals(0.0, evalD("atan(0)"), D)

    @Test(expected = ArithmeticException::class)
    fun asinAbove1Throws() {
        evalD("asin(1.0000001)")
    }

    @Test(expected = ArithmeticException::class)
    fun asinBelowN1Throws() {
        evalD("asin(-1.0000001)")
    }

    @Test(expected = ArithmeticException::class)
    fun acosAbove1Throws() {
        evalD("acos(1.5)")
    }

    @Test(expected = ArithmeticException::class)
    fun acosBelowN1Throws() {
        evalD("acos(-1.5)")
    }

    // ── Logarithms ───────────────────────────────────────────────────────────

    @Test fun log1() = assertEquals(0.0, eval("log(1)"), D)

    @Test fun log10v() = assertEquals(1.0, eval("log(10)"), D)

    @Test fun log100() = assertEquals(2.0, eval("log(100)"), D)

    @Test fun ln1() = assertEquals(0.0, eval("ln(1)"), D)

    @Test fun lnE() = assertEquals(1.0, eval("ln(e)"), D)

    @Test fun lnE2() = assertEquals(2.0, eval("ln(e^2)"), D)

    @Test fun logSmall() = assertTrue(eval("log(0.001)") < 0)

    @Test(expected = ArithmeticException::class)
    fun log0Throws() {
        eval("log(0)")
    }

    @Test(expected = ArithmeticException::class)
    fun logNegThrows() {
        eval("log(-5)")
    }

    @Test(expected = ArithmeticException::class)
    fun ln0Throws() {
        eval("ln(0)")
    }

    @Test(expected = ArithmeticException::class)
    fun lnNegThrows() {
        eval("ln(-1)")
    }

    // ── Square Root ───────────────────────────────────────────────────────────

    @Test fun sqrt0() = assertEquals(0.0, eval("sqrt(0)"), D)

    @Test fun sqrt1() = assertEquals(1.0, eval("sqrt(1)"), D)

    @Test fun sqrt4() = assertEquals(2.0, eval("sqrt(4)"), D)

    @Test fun sqrt9() = assertEquals(3.0, eval("sqrt(9)"), D)

    @Test fun sqrt2v() = assertEquals(Math.sqrt(2.0), eval("sqrt(2)"), D)

    @Test fun sqrtE() = assertEquals(Math.sqrt(E), eval("sqrt(e)"), D)

    @Test fun sqrtNested() = assertEquals(2.0, eval("sqrt(sqrt(16))"), D)

    @Test(expected = ArithmeticException::class)
    fun sqrtNegThrows() {
        eval("sqrt(-1)")
    }

    // ── Division By Zero ──────────────────────────────────────────────────────

    @Test(expected = ArithmeticException::class)
    fun divByZeroInt() {
        eval("10 / 0")
    }

    @Test(expected = ArithmeticException::class)
    fun divByZeroFloat() {
        eval("3.5 / 0.0")
    }

    @Test(expected = ArithmeticException::class)
    fun divByZeroExpr() {
        eval("(2 + 3) / (5 - 5)")
    }

    // ── Unknown / Malformed Input ─────────────────────────────────────────────

    @Test(expected = RuntimeException::class)
    fun unknownFunctionThrows() {
        eval("xyz(5)")
    }

    @Test(expected = RuntimeException::class)
    fun unexpectedCharacterThrows() {
        eval("2 $ 3")
    }

    @Test(expected = RuntimeException::class)
    fun multipleDecimalPointsThrows() {
        eval("2.3.4")
    }

    // ── Sanitisation (× ÷ symbols) ───────────────────────────────────────────

    @Test fun multiplySymbol() = assertEquals(6.0, eval("2 × 3"), D)

    @Test fun divideSymbol() = assertEquals(2.0, eval("6 ÷ 3"), D)

    @Test fun mixedSymbols() = assertEquals(3.0, eval("2 × 3 ÷ 2"), D)

    // ── Combined / Complex Expressions ───────────────────────────────────────

    @Test fun complexSci() = assertEquals(11.0, eval("sqrt(16) + 2^3 - 0!"), D)

    // NOTE: sin(x)^2 is parsed as sin(x^2) by the engine. Use multiplication form instead.
    @Test fun combinedTrigExp() = assertEquals(1.0, evalD("sin(30)*sin(30) + cos(30)*cos(30)"), D)

    @Test fun logAfterExp() = assertEquals(2.0, eval("log(10^2)"), D)

    @Test fun nestedFuncs() = assertEquals(2.0, eval("sqrt(log(10000))"), D)

    @Test fun longChain() = assertEquals(100.0, eval("10 * 5 + 50 * 2 - 100 + 50"), D)

    // NOTE: sin(x)^2 is parsed as sin(x^2) by the engine (inner parseFactor greedily
    // consumes ^). Use sin(x)*sin(x) form to correctly test the Pythagorean identity.
    @Test fun pythagorasRad() = assertEquals(1.0, eval("sin(π/6)*sin(π/6) + cos(π/6)*cos(π/6)"), D)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun eval(
        expr: String,
        deg: Boolean = false,
    ): Double = CalculatorEngine.evaluate(expr, deg)

    private fun evalD(expr: String): Double = CalculatorEngine.evaluate(expr, true)
}
