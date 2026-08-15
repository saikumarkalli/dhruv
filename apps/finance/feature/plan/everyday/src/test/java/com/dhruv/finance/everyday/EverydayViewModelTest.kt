package com.dhruv.finance.everyday

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import org.junit.Assert.assertEquals
import org.junit.Test

class EverydayViewModelTest {
    private val vm = EverydayViewModel(NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun simpleAndCompoundInterest() {
        val r =
            vm.calculateSimpleCompound(
                principal = 1000.0,
                rate = 10.0,
                years = 2.0,
                compoundingPeriods = 1,
            )
        assertEquals(200.0, r.simpleInterest.toDouble(), 0.001)
        assertEquals(1200.0, r.simpleTotal.toDouble(), 0.001)
        assertEquals(1210.0, r.compoundTotal.toDouble(), 0.001)
        assertEquals(210.0, r.compoundInterest.toDouble(), 0.001)
    }

    @Test
    fun discountReducesPrice() {
        val r = vm.calculateDiscountMarkup(basePrice = 1000.0, percent = 20.0, isDiscount = true)
        assertEquals(200.0, r.offset.toDouble(), 0.001)
        assertEquals(800.0, r.finalVal.toDouble(), 0.001)
    }

    @Test
    fun markupIncreasesPrice() {
        val r = vm.calculateDiscountMarkup(basePrice = 1000.0, percent = 20.0, isDiscount = false)
        assertEquals(1200.0, r.finalVal.toDouble(), 0.001)
    }

    @Test
    fun tipSplitAcrossPeople() {
        val r = vm.calculateTipSplit(bill = 1000.0, tipPercent = 10.0, people = 2)
        assertEquals(100.0, r.totalTip.toDouble(), 0.001)
        assertEquals(1100.0, r.overallTotal.toDouble(), 0.001)
        assertEquals(50.0, r.splitTip.toDouble(), 0.001)
        assertEquals(550.0, r.splitBill.toDouble(), 0.001)
    }

    @Test
    fun inflationErodesAndInflatesValue() {
        // 10% over 2 years -> multiplier 1.21.
        val r = vm.calculateInflation(amount = 1000.0, rate = 10.0, years = 2.0)
        assertEquals(826.45, r.futurePurchasePower.toDouble(), 0.1)
        assertEquals(1210.0, r.amountNeeded.toDouble(), 0.1)
    }

    @Test
    fun invalidTipReturnsZero() {
        val r = vm.calculateTipSplit(bill = 0.0, tipPercent = 10.0, people = 2)
        assertEquals(0.0, r.overallTotal.toDouble(), 0.0)
    }
}
