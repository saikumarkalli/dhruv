package com.dhruv.finance.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the calculator's pure input/error rules
 * ([arithmeticErrorMessage], [appendOperator]) — the two bug fixes:
 *
 *  - Bug 1: math-domain errors were all shown as "Error: ÷0".
 *  - Bug 2: pressing an operator after a percentage operand silently dropped the `%`.
 */
class CalculatorInputRulesTest {
    // ── Bug 1: domain errors must not all read "÷0" ──────────────────────────

    @Test fun divisionByZeroKeepsDivZeroMessage() =
        assertEquals(
            "Error: ÷0",
            arithmeticErrorMessage(ArithmeticException("Division by zero")),
        )

    @Test fun sqrtNegativeIsNotDivZero() {
        val msg = arithmeticErrorMessage(ArithmeticException("Square root of negative number"))
        assertEquals("Error: √ of negative", msg)
    }

    @Test fun logNonPositiveIsNotDivZero() =
        assertEquals("Error: log domain", arithmeticErrorMessage(ArithmeticException("Log of non-positive number")))

    @Test fun arcsinDomainIsNotDivZero() =
        assertEquals(
            "Error: domain",
            arithmeticErrorMessage(ArithmeticException("Arcsin domain violation")),
        )

    @Test fun arccosDomainIsNotDivZero() =
        assertEquals(
            "Error: domain",
            arithmeticErrorMessage(ArithmeticException("Arccos domain violation")),
        )

    @Test fun unknownArithmeticErrorFallsBackToGenericError() = assertEquals("Error", arithmeticErrorMessage(ArithmeticException(null)))

    // ── Bug 2: a trailing `%` must survive a following operator ───────────────

    @Test fun operatorAfterPercentKeepsPercent() = assertEquals("50%+", appendOperator("50%", "+"))

    @Test fun minusAfterPercentKeepsPercent() = assertEquals("50%-", appendOperator("50%", "-"))

    @Test fun multiplyAfterPercentKeepsPercent() = assertEquals("200%×", appendOperator("200%", "×"))

    // ── Regression: existing operator behaviour is unchanged ──────────────────

    @Test fun operatorAfterNumber() = assertEquals("200+", appendOperator("200", "+"))

    @Test fun emptyPlusBecomesZeroPlus() = assertEquals("0+", appendOperator("", "+"))

    @Test fun emptyTimesBecomesZeroTimes() = assertEquals("0×", appendOperator("", "×"))

    @Test fun emptyMinusBecomesMinus() = assertEquals("-", appendOperator("", "-"))

    @Test fun trailingPlusReplacedByOp() = assertEquals("5×", appendOperator("5+", "×"))

    @Test fun trailingPlusToMinus() = assertEquals("200-", appendOperator("200+", "-"))

    @Test fun minusAfterOpenParen() = assertEquals("(-", appendOperator("(", "-"))

    @Test fun plusAfterOpenParenNoOp() = assertEquals("(", appendOperator("(", "+"))

    @Test fun doubleMinusKeepsOne() = assertEquals("5-", appendOperator("5-", "-"))

    @Test fun timesThenMinusAllowed() = assertEquals("5×-", appendOperator("5×", "-"))

    @Test fun opMinusThenOpCollapses() = assertEquals("5+", appendOperator("5×-", "+"))

    @Test fun caretAfterPercentKept() = assertEquals("50%^", appendOperator("50%", "^"))

    @Test fun divideAfterNumber() = assertEquals("9÷", appendOperator("9", "÷"))

    // ── arithmeticErrorMessage: blank / non-domain messages ───────────────────

    @Test fun blankMessageFallsBackToError() = assertEquals("Error", arithmeticErrorMessage(ArithmeticException("")))

    @Test fun unrelatedMessageFallsBackToError() = assertEquals("Error", arithmeticErrorMessage(ArithmeticException("overflow")))
}
