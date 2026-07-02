package com.dhruv.finance.loans

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoansViewModelTest {
    private val vm = LoansViewModel(NoOpCrashReporter, NoOpPerformanceTracer)
    private val delta = 0.5

    @Test
    fun emiZeroInterestSplitsPrincipalEvenly() {
        val result = vm.calculateEmi(principal = 120_000.0, annualRate = 0.0, tenureYears = 1.0)
        assertEquals(10_000.0, result.emi.toDouble(), 0.001)
        assertEquals(0.0, result.totalInterest.toDouble(), 0.001)
        assertEquals(120_000.0, result.totalPayment.toDouble(), 0.001)
    }

    @Test
    fun emiStandardLoanMatchesKnownValue() {
        // P=100000, 12% p.a., 1 year -> EMI ~= 8884.88 by the standard amortization formula.
        val result = vm.calculateEmi(principal = 100_000.0, annualRate = 12.0, tenureYears = 1.0)
        assertEquals(8884.88, result.emi.toDouble(), delta)
        assertEquals(result.emi.toDouble() * 12.0, result.totalPayment.toDouble(), delta)
        assertEquals(
            result.totalPayment.toDouble() - 100_000.0,
            result.totalInterest.toDouble(),
            delta,
        )
    }

    @Test
    fun emiInvalidInputsReturnZero() {
        val result = vm.calculateEmi(principal = 0.0, annualRate = 10.0, tenureYears = 5.0)
        assertEquals(0.0, result.emi.toDouble(), 0.0)
        assertEquals(0.0, result.totalPayment.toDouble(), 0.0)
    }

    @Test
    fun emiInterestIsNeverNegative() {
        val result = vm.calculateEmi(principal = 50_000.0, annualRate = 8.5, tenureYears = 3.0)
        assertTrue(result.totalInterest.toDouble() >= 0.0)
    }
}
