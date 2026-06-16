package com.dhruv.finance.calculator

import com.dhruv.finance.calculator.engine.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deep edge-case suite for [CalculatorEngine], complementing CalculatorEngineCoreTest /
 * CalculatorEngineEdgeCaseTest. Includes **characterization** tests (prefixed `quirk_`) that pin
 * down the engine's known precedence limitations so a future change to them is caught.
 */
class CalculatorEngineDeepEdgeTest {

    private val D = 1e-9
    private fun ev(expr: String, deg: Boolean = false) = CalculatorEngine.evaluate(expr, deg)

    // ── Unary operator chains ────────────────────────────────────────────────

    @Test fun tripleNegative()     = assertEquals(-5.0, ev("---5"), D)
    @Test fun mixedUnary()         = assertEquals(5.0, ev("-+-5"), D)
    @Test fun unaryBeforeParen()   = assertEquals(-7.0, ev("-(3+4)"), D)
    @Test fun unaryTimesUnary()    = assertEquals(6.0, ev("-2*-3"), D)
    @Test fun unaryMinusPower()    = assertEquals(-4.0, ev("-2^2"), D) // -(2^2), unary binds looser
    @Test fun negParenPower()      = assertEquals(4.0, ev("(-2)^2"), D)

    // ── Whitespace & sanitisation ────────────────────────────────────────────

    @Test fun manyInnerSpaces()    = assertEquals(6.0, ev("2   ×   3"), D)
    @Test fun spacesAroundPow()    = assertEquals(8.0, ev(" 2 ^ 3 "), D)
    @Test fun spaceSeparatedImpl() = assertEquals(6.0, ev("2 3"), D) // implicit multiplication

    // ── Implicit multiplication combinations ─────────────────────────────────

    @Test fun numberThenParenChain() = assertEquals(24.0, ev("2(3)(4)"), D)
    @Test fun parenThenNumber()      = assertEquals(6.0, ev("(2)3"), D)
    @Test fun numberSqrt()           = assertEquals(6.0, ev("3sqrt(4)"), D)
    @Test fun factorialThenNumber()  = assertEquals(12.0, ev("3!2"), D) // (3!)*2 = 6*2
    @Test fun piTimesParen()         = assertEquals(PI * 5, ev("π(5)"), D)
    @Test fun divThenImplicitPi()    = assertEquals(PI / 2, ev("1/2π"), D) // (1/2)π, left-to-right

    // ── Percentage semantics ─────────────────────────────────────────────────

    @Test fun percentStandalone()      = assertEquals(0.15, ev("15%"), D)
    @Test fun percentAddChain()        = assertEquals(231.0, ev("200+10%+5%"), D) // 220 then +5% of 220
    @Test fun percentSubtract()        = assertEquals(180.0, ev("200-10%"), D)
    @Test fun percentLeadingOperand()  = assertEquals(200.1, ev("10%+200"), D)
    @Test fun percentInsideParen()     = assertEquals(220.0, ev("200+(10)%"), D)
    @Test fun percentMultiply()        = assertEquals(30.0, ev("200*15%"), D)
    @Test fun percentOfTiny()          = assertEquals(1000.1, ev("1000+0.01%"), D)

    // ── Exponent associativity & forms ───────────────────────────────────────

    @Test fun rightAssocPower()  = assertEquals(512.0, ev("2^3^2"), D)
    @Test fun fractionalPower()  = assertEquals(3.0, ev("9^0.5"), D)
    @Test fun powerOfZero()      = assertEquals(1.0, ev("0^0"), D) // Math.pow(0,0) == 1.0
    @Test fun powerThenFact()    = assertEquals(64.0, ev("2^3!"), D) // 2^(3!) = 2^6

    // ── Factorial ─────────────────────────────────────────────────────────────

    @Test fun factOfParen()       = assertEquals(6.0, ev("(2+1)!"), D)
    @Test fun chainedFactMul()    = assertEquals(144.0, ev("3!*4!"), D)
    // (5!)! = 120!, which is ≈6.69e198 — finite (170! is the overflow threshold), not Infinity.
    @Test fun doubleBang()        = assertEquals(CalculatorEngine.factorial(120.0), ev("5!!"), 0.0)
    @Test fun doubleBangFinite()  = assertTrue(ev("5!!").isFinite())

    // ── Scientific notation round-trips ───────────────────────────────────────

    @Test fun sciInt()        = assertEquals(1000.0, ev("1E3"), D)
    @Test fun sciNeg()        = assertEquals(-1000.0, ev("-1E3"), D)
    @Test fun sciFracMinus()  = assertEquals(0.0015, ev("1.5e-3"), D)
    @Test fun sciSum()        = assertEquals(1100.0, ev("1E3+1E2"), D)
    @Test fun sciLowerCase()  = assertEquals(120.0, ev("1.2e2"), D)

    // ── Nested functions ──────────────────────────────────────────────────────

    @Test fun nestedSqrt()    = assertEquals(2.0, ev("sqrt(sqrt(16))"), D)
    @Test fun logOfPow()      = assertEquals(2.0, ev("log(10^2)"), D)
    @Test fun lnOfPow()       = assertEquals(3.0, ev("ln(e^3)"), D)
    @Test fun sqrtOfConst()   = assertEquals(sqrt(E), ev("sqrt(e)"), D)

    // ── Auto-close of parentheses ──────────────────────────────────────────────

    @Test fun autoCloseOne()    = assertEquals(5.0, ev("(2+3"), D)
    @Test fun autoCloseTwo()    = assertEquals(5.0, ev("((2+3"), D)
    @Test fun autoCloseFunc()   = assertEquals(1.0, ev("sin(90", true), D)
    @Test fun autoCloseMixed()  = assertEquals(14.0, ev("2*(3+4"), D)

    // ── Domain / parse errors ──────────────────────────────────────────────────

    @Test(expected = ArithmeticException::class) fun divZero()       { ev("1/0") }
    @Test(expected = ArithmeticException::class) fun divZeroParen()  { ev("5/(2-2)") }
    @Test(expected = ArithmeticException::class) fun divNegZero()    { ev("1/(0*-1)") }
    @Test(expected = ArithmeticException::class) fun sqrtNeg()       { ev("sqrt(-4)") }
    @Test(expected = ArithmeticException::class) fun log0()          { ev("log(0)") }
    @Test(expected = ArithmeticException::class) fun lnNeg()         { ev("ln(-1)") }
    @Test(expected = ArithmeticException::class) fun asinOutOfRange(){ ev("asin(2)") }
    @Test(expected = ArithmeticException::class) fun acosOutOfRange(){ ev("acos(-2)") }
    @Test(expected = RuntimeException::class)    fun unknownFunc()   { ev("foo(3)") }
    @Test(expected = RuntimeException::class)    fun badChar()       { ev("2 @ 3") }
    @Test(expected = RuntimeException::class)    fun multiDot()      { ev("1.2.3") }
    @Test(expected = RuntimeException::class)    fun emptyParens()   { ev("()") }
    @Test(expected = RuntimeException::class)    fun extraClose()    { ev("(2+3))") }
    @Test(expected = RuntimeException::class)    fun danglingOp()    { ev("2+") }

    // ── NaN / Infinity propagation (engine returns, does not throw) ─────────────

    @Test fun overflowPowIsInfinite() = assertTrue(ev("10^400").isInfinite())
    @Test fun negFracPowIsNaN()       = assertTrue(ev("(-2)^0.5").isNaN())

    // ── Domain-specific exception messages (drive the ViewModel error mapping) ──

    @Test fun divZeroMessage() {
        val m = runCatching { ev("1/0") }.exceptionOrNull()?.message.orEmpty()
        assertTrue(m.contains("Division by zero", ignoreCase = true))
    }

    @Test fun sqrtNegMessageIsNotDivZero() {
        val m = runCatching { ev("sqrt(-1)") }.exceptionOrNull()?.message.orEmpty()
        assertTrue(m.contains("Square root", ignoreCase = true))
    }

    // ── Characterization of the documented precedence quirks ────────────────────

    /** `sin(x)^2` is parsed as `sin(x^2)` because the function greedily consumes a trailing `^`. */
    @Test fun quirk_funcThenPowerBindsToArgument() =
        assertEquals(sin(9.0), ev("sin(3)^2"), D)

    /** `sin(3)!` is parsed as `sin(3!)` for the same reason. */
    @Test fun quirk_funcThenFactorialBindsToArgument() =
        assertEquals(sin(Math.toRadians(6.0)), ev("sin(3)!", true), D)

    /** `%` is percentage, not modulo: `10 % 3` → (10/100) implicitly times 3. */
    @Test fun quirk_percentIsNotModulo() =
        assertEquals(0.3, ev("10%3"), D)

    /** Factorial-then-power is unsupported: `^` is only consumed before `!`, so `3!^2` won't parse. */
    @Test(expected = RuntimeException::class)
    fun quirk_factorialThenPowerThrows() { ev("3!^2") }

    // ── Constants arithmetic ────────────────────────────────────────────────────

    @Test fun piArithmetic() = assertEquals(PI * 2 + 1, ev("π*2+1"), D)
    @Test fun eArithmetic()  = assertEquals(ln(E) + E, ev("ln(e)+e"), D)
}
