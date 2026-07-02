package com.dhruv.finance.investments

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentsViewModelTest {
    private val vm = InvestmentsViewModel(NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun sipTotalsAndGrowth() {
        // 1000/month, 12% p.a., 1 year.
        val r = vm.calculateSip(amount = 1000.0, rate = 12.0, years = 1.0)
        assertEquals(12_000.0, r.totalInvested.toDouble(), 0.001)
        assertEquals(12_809.3, r.futureValue.toDouble(), 1.0)
        assertEquals(
            r.futureValue.toDouble() - r.totalInvested.toDouble(),
            r.estimatedReturns.toDouble(),
            0.001,
        )
    }

    @Test
    fun sipZeroRateReturnsPrincipalOnly() {
        val r = vm.calculateSip(amount = 1000.0, rate = 0.0, years = 2.0)
        assertEquals(24_000.0, r.totalInvested.toDouble(), 0.001)
        assertEquals(24_000.0, r.futureValue.toDouble(), 0.001)
        assertEquals(0.0, r.estimatedReturns.toDouble(), 0.001)
    }

    @Test
    fun roiCagrForDoublingOverTwoYears() {
        val r = vm.calculateRoiCagr(initial = 1000.0, finalVal = 2000.0, years = 2.0)
        assertEquals(100.0, r.absoluteReturn, 0.001)
        assertEquals(41.42, r.cagr, 0.01)
    }

    @Test
    fun fdMaturityCompoundsQuarterly() {
        // 10000 @ 10% for 1 year, compounded quarterly -> 10000 * 1.025^4 ~= 11038.13
        val r = vm.calculateFdRd(amount = 10_000.0, rate = 10.0, years = 1.0, isFixedDeposit = true)
        assertEquals(10_000.0, r.principalInvested.toDouble(), 0.001)
        assertEquals(11_038.13, r.maturityValue.toDouble(), 0.5)
        assertTrue(r.interestGains.toDouble() > 0.0)
    }

    @Test
    fun invalidInputsReturnZero() {
        val r = vm.calculateSip(amount = -5.0, rate = 12.0, years = 1.0)
        assertEquals(0.0, r.futureValue.toDouble(), 0.0)
    }
}
