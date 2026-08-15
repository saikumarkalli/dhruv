package com.dhruv.finance.tax

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import org.junit.Assert.assertEquals
import org.junit.Test

class TaxViewModelTest {
    private val vm = TaxViewModel(NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun gstAddedOnTopOfBase() {
        val r = vm.calculateGst(amount = 1000.0, gstPercent = 18.0, isAddGst = true)
        assertEquals(1000.0, r.preTaxBase.toDouble(), 0.001)
        assertEquals(180.0, r.taxAmount.toDouble(), 0.001)
        assertEquals(1180.0, r.totalAmount.toDouble(), 0.001)
    }

    @Test
    fun gstExtractedFromGrossAmount() {
        val r = vm.calculateGst(amount = 1180.0, gstPercent = 18.0, isAddGst = false)
        assertEquals(1000.0, r.preTaxBase.toDouble(), 0.001)
        assertEquals(180.0, r.taxAmount.toDouble(), 0.001)
        assertEquals(1180.0, r.totalAmount.toDouble(), 0.001)
    }

    @Test
    fun salaryBreakupHighEarnerCapsPfAndTaxes() {
        val r = vm.calculateSalaryBreakup(ctc = 1_200_000.0)
        assertEquals(100_000.0, r.grossMonthly.toDouble(), 0.001)
        assertEquals(12_000.0, r.pfContribution.toDouble(), 0.001)
        assertEquals(15_000.0, r.estimatedTax.toDouble(), 0.001)
        assertEquals(73_000.0, r.takeHome.toDouble(), 0.001)
    }

    @Test
    fun salaryBreakupLowEarnerHasNoTax() {
        val r = vm.calculateSalaryBreakup(ctc = 300_000.0)
        assertEquals(25_000.0, r.grossMonthly.toDouble(), 0.001)
        assertEquals(0.0, r.estimatedTax.toDouble(), 0.001)
        assertEquals(22_000.0, r.takeHome.toDouble(), 0.001)
    }

    @Test
    fun invalidGstReturnsZero() {
        val r = vm.calculateGst(amount = 0.0, gstPercent = 18.0, isAddGst = true)
        assertEquals(0.0, r.totalAmount.toDouble(), 0.0)
    }
}
