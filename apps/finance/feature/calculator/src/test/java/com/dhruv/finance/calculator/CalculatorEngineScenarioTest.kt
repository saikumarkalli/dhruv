package com.dhruv.finance.calculator

import com.dhruv.finance.calculator.engine.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineScenarioTest {
    private val delta = 1e-9

    private fun eval(expr: String, isDegree: Boolean = true) = CalculatorEngine.evaluate(expr, isDegree)

    // ── Percentage in additive context ──
    @Test fun percentAdditive100Plus10Pct() = assertEquals(110.0, eval("100+10%"), delta)
    @Test fun percentAdditive200Minus25Pct() = assertEquals(150.0, eval("200-25%"), delta)
    @Test fun percentStandalone() = assertEquals(0.5, eval("50%"), delta)
    @Test fun percentChained() = assertEquals(115.5, eval("100+10%+5%"), delta)

    // ── Factorial boundaries ──
    @Test fun factorial0() = assertEquals(1.0, eval("0!"), delta)
    @Test fun factorial1() = assertEquals(1.0, eval("1!"), delta)
    @Test fun factorial5() = assertEquals(120.0, eval("5!"), delta)
    @Test fun factorial170IsFinite() = assertTrue(eval("170!").isFinite())
    @Test fun factorial171IsInfinity() = assertEquals(Double.POSITIVE_INFINITY, eval("171!"), delta)
    @Test fun factorialNegativeIsNaN() = assertTrue(eval("(-1)!").isNaN())

    // ── Implicit multiplication ──
    @Test fun implicitMul2Pi() = assertEquals(2 * Math.PI, eval("2π"), delta)
    @Test fun implicitMul3Parens() = assertEquals(15.0, eval("3(4+1)"), delta)
    @Test fun implicitMulParensParens() = assertEquals(6.0, eval("(2)(3)"), delta)
    @Test fun implicitMul2e() = assertEquals(2 * Math.E, eval("2e"), delta)

    // ── Trig degree mode ──
    @Test fun sinDeg90() = assertEquals(1.0, eval("sin(90)", isDegree = true), delta)
    @Test fun cosDeg0() = assertEquals(1.0, eval("cos(0)", isDegree = true), delta)
    @Test fun tanDeg45() = assertEquals(1.0, eval("tan(45)", isDegree = true), delta)
    @Test fun sinDeg0() = assertEquals(0.0, eval("sin(0)", isDegree = true), delta)
    @Test fun cosDeg90() = assertEquals(0.0, eval("cos(90)", isDegree = true), 1e-9)

    // ── Trig radian mode ──
    @Test fun sinRadPiOver2() = assertEquals(1.0, eval("sin(π/2)", isDegree = false), delta)
    @Test fun cosRad0() = assertEquals(1.0, eval("cos(0)", isDegree = false), delta)
    @Test fun sinRadPi() = assertEquals(0.0, eval("sin(π)", isDegree = false), 1e-9)

    // ── Nested functions ──
    @Test fun sqrtSinDeg90() = assertEquals(1.0, eval("sqrt(sin(90))", isDegree = true), delta)
    @Test fun log100() = assertEquals(2.0, eval("log(100)"), delta)
    @Test fun lnE() = assertEquals(1.0, eval("ln(e)"), delta)
    @Test fun sqrtOfExpr() = assertEquals(5.0, eval("sqrt(25)"), delta)
    @Test fun sqrtOf4Times9() = assertEquals(6.0, eval("sqrt(4)*sqrt(9)"), delta)

    // ── Scientific notation ──
    @Test fun sciNotation1500() = assertEquals(1500.0, eval("1.5e3"), delta)
    @Test fun sciNotationPlus500() = assertEquals(2000.0, eval("1.5e3+500"), delta)
    @Test fun sciNotationSmall() = assertEquals(0.005, eval("5e-3"), delta)

    // ── Division by zero ──
    @Test(expected = ArithmeticException::class)
    fun divisionByZero() { eval("1/0") }

    @Test(expected = ArithmeticException::class)
    fun divisionByZeroNested() { eval("10/(5-5)") }

    // ── Negative square root ──
    @Test(expected = ArithmeticException::class)
    fun sqrtNegative() { eval("sqrt(-4)") }

    // ── Auto-close parentheses ──
    @Test fun autoCloseOneParen() = assertEquals(5.0, eval("(2+3"), delta)
    @Test fun autoCloseTwoParens() = assertEquals(10.0, eval("((2+3)*2"), delta)

    // ── Nested parentheses ──
    @Test fun nestedParens() = assertEquals(25.0, eval("((2+3)*(4+1))"), delta)
    @Test fun deepNesting() = assertEquals(3.0, eval("(((1+2)))"), delta)

    // ── Exponents ──
    @Test fun exponent2Pow3() = assertEquals(8.0, eval("2^3"), delta)
    @Test fun exponent2Pow10() = assertEquals(1024.0, eval("2^10"), delta)
    @Test fun exponentFractional() = assertEquals(2.0, eval("4^0.5"), delta)
    @Test fun exponentZero() = assertEquals(1.0, eval("5^0"), delta)

    // ── Empty/whitespace expressions ──
    @Test fun emptyExpr() = assertEquals(0.0, eval(""), delta)
    @Test fun blankExpr() = assertEquals(0.0, eval("   "), delta)

    // ── Inverse trig ──
    @Test fun asinDeg() = assertEquals(90.0, eval("asin(1)", isDegree = true), delta)
    @Test fun acosDeg() = assertEquals(0.0, eval("acos(1)", isDegree = true), delta)
    @Test fun atanDeg() = assertEquals(45.0, eval("atan(1)", isDegree = true), delta)

    @Test(expected = ArithmeticException::class)
    fun asinOutOfRange() { eval("asin(2)") }

    @Test(expected = ArithmeticException::class)
    fun acosOutOfRange() { eval("acos(2)") }

    // ── Log domain ──
    @Test(expected = ArithmeticException::class)
    fun logZero() { eval("log(0)") }

    @Test(expected = ArithmeticException::class)
    fun logNegative() { eval("log(-1)") }

    @Test(expected = ArithmeticException::class)
    fun lnZero() { eval("ln(0)") }

    // ── Order of operations ──
    @Test fun orderOfOps() = assertEquals(14.0, eval("2+3*4"), delta)
    @Test fun orderOfOpsWithParens() = assertEquals(20.0, eval("(2+3)*4"), delta)
    @Test fun unaryInExpr() = assertEquals(-1.0, eval("2+-3"), delta)

    // ── Constants ──
    @Test fun piValue() = assertEquals(Math.PI, eval("π"), delta)
    @Test fun eValue() = assertEquals(Math.E, eval("e"), delta)
    @Test fun piTimesE() = assertEquals(Math.PI * Math.E, eval("πe"), delta)
}
