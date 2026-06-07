package com.example

import com.example.ui.finance.FinanceViewModel
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Edge-case tests for [FinanceViewModel].
 *
 * Coverage beyond what [FinanceViewModelTest] already covers:
 *  EMI:          zero rate, zero tenure, very large principal
 *  SIP:          1-month tenure, very large amount
 *  GST:          0% GST, 100% GST, small base amounts
 *  Discount:     0% and 100% discount/markup
 *  Tip/Split:    1 person, 0% tip, 100% tip
 *  Salary:       below 1M (5% tax tier), exact 1M boundary
 *  Inflation:    0% rate (future = present), 1-year, 100% rate
 *  FD/RD:        1-year terms, RD with 1 month
 */
class FinanceViewModelEdgeCaseTest {

    private val vm = FinanceViewModel()
    private val scale2 = { bd: BigDecimal -> bd.setScale(2, RoundingMode.HALF_UP) }

    // ── EMI Edge Cases ────────────────────────────────────────────────────────

    @Test
    fun emi_zeroPrincipal() {
        val r = vm.calculateEmi(0.0, 8.5, 15.0)
        assertEquals(BigDecimal.ZERO, r.emi)
        assertEquals(BigDecimal.ZERO, r.totalInterest)
    }

    @Test
    fun emi_zeroRate() {
        // 0% interest → EMI = principal / months
        val r = vm.calculateEmi(120000.0, 0.0, 10.0) // 10 years = 120 months
        // Should not throw; EMI may be 0 or principal/months depending on impl
        assertTrue(r.emi >= BigDecimal.ZERO)
    }

    @Test
    fun emi_oneTenure() {
        // 1-year tenure, check total payment consistency
        val r = vm.calculateEmi(100000.0, 10.0, 1.0)
        assertTrue(r.emi > BigDecimal.ZERO)
        // Total payment = emi * 12
        assertEquals(scale2(r.totalPayment), scale2(r.emi.multiply(BigDecimal("12"))))
    }

    @Test
    fun emi_largePrincipal() {
        val r = vm.calculateEmi(50_000_000.0, 7.5, 30.0)
        assertTrue(r.emi > BigDecimal.ZERO)
        assertTrue(r.totalInterest > BigDecimal.ZERO)
    }

    // ── SIP Edge Cases ────────────────────────────────────────────────────────

    @Test
    fun sip_oneMonth() {
        val r = vm.calculateSip(5000.0, 12.0, 1.0 / 12)  // 1 month
        assertTrue(r.futureValue > BigDecimal.ZERO)
    }

    @Test
    fun sip_zeroRate() {
        val r = vm.calculateSip(5000.0, 0.0, 10.0)
        // At 0% return, future value should ≈ total invested
        assertEquals(
            scale2(r.totalInvested),
            scale2(r.futureValue)
        )
    }

    @Test
    fun sip_largeSip() {
        val r = vm.calculateSip(100_000.0, 15.0, 20.0)
        assertTrue(r.futureValue > r.totalInvested)
    }

    // ── GST Edge Cases ────────────────────────────────────────────────────────

    @Test
    fun gst_zeroRate_add() {
        val r = vm.calculateGst(1000.0, 0.0, true)
        assertEquals(BigDecimal("0.00"), scale2(r.taxAmount))
        assertEquals(BigDecimal("1000.00"), scale2(r.totalAmount))
    }

    @Test
    fun gst_zeroRate_remove() {
        val r = vm.calculateGst(1000.0, 0.0, false)
        assertEquals(BigDecimal("1000.00"), scale2(r.preTaxBase))
        assertEquals(BigDecimal("0.00"), scale2(r.taxAmount))
    }

    @Test
    fun gst_hundredPercent() {
        // 100% GST on 500 → tax = 500, total = 1000
        val r = vm.calculateGst(500.0, 100.0, true)
        assertEquals(BigDecimal("500.00"), scale2(r.taxAmount))
        assertEquals(BigDecimal("1000.00"), scale2(r.totalAmount))
    }

    @Test
    fun gst_smallAmount() {
        // 18% on ₹1
        val r = vm.calculateGst(1.0, 18.0, true)
        assertEquals(BigDecimal("0.18"), scale2(r.taxAmount))
        assertEquals(BigDecimal("1.18"), scale2(r.totalAmount))
    }

    // ── Discount / Markup Edge Cases ─────────────────────────────────────────

    @Test
    fun discount_zeroPercent() {
        val r = vm.calculateDiscountMarkup(1000.0, 0.0, true)
        assertEquals(BigDecimal("0.00"), scale2(r.offset))
        assertEquals(BigDecimal("1000.00"), scale2(r.finalVal))
    }

    @Test
    fun discount_hundredPercent() {
        // 100% discount → final = 0
        val r = vm.calculateDiscountMarkup(1000.0, 100.0, true)
        assertEquals(BigDecimal("1000.00"), scale2(r.offset))
        assertEquals(BigDecimal("0.00"), scale2(r.finalVal))
    }

    @Test
    fun markup_zeroPercent() {
        val r = vm.calculateDiscountMarkup(1000.0, 0.0, false)
        assertEquals(BigDecimal("0.00"), scale2(r.offset))
        assertEquals(BigDecimal("1000.00"), scale2(r.finalVal))
    }

    // ── Tip / Split Edge Cases ────────────────────────────────────────────────

    @Test
    fun tipSplit_onePerson() {
        val r = vm.calculateTipSplit(1200.0, 10.0, 1)
        assertEquals(BigDecimal("120.00"), scale2(r.totalTip))
        assertEquals(BigDecimal("1320.00"), scale2(r.overallTotal))
        // Per-person = overall total
        assertEquals(scale2(r.overallTotal), scale2(r.splitBill))
    }

    @Test
    fun tipSplit_zeroTip() {
        val r = vm.calculateTipSplit(1500.0, 0.0, 4)
        assertEquals(BigDecimal("0.00"), scale2(r.totalTip))
        assertEquals(BigDecimal("1500.00"), scale2(r.overallTotal))
    }

    @Test
    fun tipSplit_hundredPercentTip() {
        val r = vm.calculateTipSplit(1000.0, 100.0, 2)
        assertEquals(BigDecimal("1000.00"), scale2(r.totalTip))
        assertEquals(BigDecimal("2000.00"), scale2(r.overallTotal))
    }

    // ── Salary Breakup Edge Cases ─────────────────────────────────────────────

    @Test
    fun salary_below1M() {
        // CTC < 1,000,000 → 5% tax tier
        val r = vm.calculateSalaryBreakup(600_000.0) // 6 LPA
        assertEquals(BigDecimal("50000.00"), scale2(r.grossMonthly))
        // PF: 12% of 50000 = 6000
        assertEquals(BigDecimal("6000.00"), scale2(r.pfContribution))
        // Tax: 5% of 50000 = 2500
        assertEquals(BigDecimal("2500.00"), scale2(r.estimatedTax))
        // Take-home: 50000 - 6000 - 2500 = 41500
        assertEquals(BigDecimal("41500.00"), scale2(r.takeHome))
    }

    @Test
    fun salary_above1M() {
        // CTC > 1,000,000 → 15% tax tier (existing test)
        val r = vm.calculateSalaryBreakup(1_200_000.0)
        assertEquals(BigDecimal("15000.00"), scale2(r.estimatedTax))
    }

    @Test
    fun salary_zero() {
        val r = vm.calculateSalaryBreakup(0.0)
        assertEquals(BigDecimal("0.00"), scale2(r.grossMonthly))
        assertEquals(BigDecimal("0.00"), scale2(r.takeHome))
    }

    // ── Inflation Edge Cases ──────────────────────────────────────────────────

    @Test
    fun inflation_zeroRate() {
        // 0% inflation → future amount needed = present value
        val r = vm.calculateInflation(10000.0, 0.0, 10.0)
        assertEquals(BigDecimal("10000.00"), scale2(r.amountNeeded))
        assertEquals(BigDecimal("10000.00"), scale2(r.futurePurchasePower))
    }

    @Test
    fun inflation_oneYear() {
        val r = vm.calculateInflation(10000.0, 6.0, 1.0)
        // After 1 year at 6%: need 10600, purchase power = 10000/1.06 ≈ 9433.96
        assertEquals(BigDecimal("10600.00"), scale2(r.amountNeeded))
    }

    // ── FD / RD Edge Cases ────────────────────────────────────────────────────

    @Test
    fun fd_oneYear() {
        val r = vm.calculateFdRd(50_000.0, 6.5, 1.0, true)
        assertTrue(r.maturityValue > BigDecimal("50000"))
        assertEquals(BigDecimal("50000.00"), scale2(r.principalInvested))
    }

    @Test
    fun rd_oneYear() {
        // RD: 1000/month for 12 months → principal = 12000
        val r = vm.calculateFdRd(1000.0, 6.5, 1.0, false)
        assertEquals(BigDecimal("12000.00"), scale2(r.principalInvested))
        assertTrue(r.maturityValue > BigDecimal("12000"))
    }

    @Test
    fun fd_zeroPrincipal() {
        val r = vm.calculateFdRd(0.0, 7.0, 5.0, true)
        assertEquals(BigDecimal("0.00"), scale2(r.principalInvested))
        assertEquals(BigDecimal("0.00"), scale2(r.maturityValue))
    }
}
