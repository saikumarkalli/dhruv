package com.example

import com.example.ui.finance.FinanceViewModel
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FinanceViewModelTest {

    private val viewModel = FinanceViewModel()

    @Test
    fun testCalculateEmi() {
        // Principal: 1,000,000, Interest Rate: 8.5% p.a., Tenure: 15 years
        val result = viewModel.calculateEmi(1000000.0, 8.5, 15.0)
        // Check EMI value
        assertTrue(result.emi > BigDecimal.ZERO)
        assertTrue(result.totalInterest > BigDecimal.ZERO)
        assertEquals(result.totalPayment, result.emi.multiply(BigDecimal("180.0")))
        
        // Zero or invalid cases
        val zeroResult = viewModel.calculateEmi(0.0, 8.5, 15.0)
        assertEquals(BigDecimal.ZERO, zeroResult.emi)
    }

    @Test
    fun testCalculateSimpleCompound() {
        // Principal: 100,000, Rate: 7.5%, Tenure: 5 years, Compounding: 1 (annual)
        val result = viewModel.calculateSimpleCompound(100000.0, 7.5, 5.0, 1)
        // Simple Interest: 100000 * 0.075 * 5 = 37500
        assertEquals(BigDecimal("37500.00"), result.simpleInterest.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("137500.00"), result.simpleTotal.setScale(2, java.math.RoundingMode.HALF_UP))
        
        // Compound Interest
        assertTrue(result.compoundInterest > result.simpleInterest)
    }

    @Test
    fun testCalculateSip() {
        val result = viewModel.calculateSip(5000.0, 12.0, 10.0)
        assertTrue(result.futureValue > BigDecimal.ZERO)
        assertEquals(BigDecimal("600000.00"), result.totalInvested.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testCalculateRoiCagr() {
        val result = viewModel.calculateRoiCagr(50000.0, 95000.0, 4.0)
        assertEquals(90.0, result.absoluteReturn, 1e-2)
        assertEquals(17.4, result.cagr, 1e-1)
    }

    @Test
    fun testCalculateGst() {
        // Add 18% GST to 1500
        val addResult = viewModel.calculateGst(1500.0, 18.0, true)
        assertEquals(BigDecimal("270.00"), addResult.taxAmount.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("1770.00"), addResult.totalAmount.setScale(2, java.math.RoundingMode.HALF_UP))

        // Remove 18% GST from 1770
        val removeResult = viewModel.calculateGst(1770.0, 18.0, false)
        assertEquals(BigDecimal("1500.00"), removeResult.preTaxBase.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("270.00"), removeResult.taxAmount.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testCalculateDiscountMarkup() {
        // 20% discount on 1000
        val discount = viewModel.calculateDiscountMarkup(1000.0, 20.0, true)
        assertEquals(BigDecimal("200.00"), discount.offset.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("800.00"), discount.finalVal.setScale(2, java.math.RoundingMode.HALF_UP))

        // 20% markup on 1000
        val markup = viewModel.calculateDiscountMarkup(1000.0, 20.0, false)
        assertEquals(BigDecimal("200.00"), markup.offset.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("1200.00"), markup.finalVal.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testCalculateTipSplit() {
        // Bill: 1500, Tip: 12%, People: 4
        val result = viewModel.calculateTipSplit(1500.0, 12.0, 4)
        assertEquals(BigDecimal("180.00"), result.totalTip.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("1680.00"), result.overallTotal.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("45.00"), result.splitTip.setScale(2, java.math.RoundingMode.HALF_UP))
        assertEquals(BigDecimal("420.00"), result.splitBill.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testCalculateSalaryBreakup() {
        // Gross CTC: 12 LPA (1,200,000)
        val result = viewModel.calculateSalaryBreakup(1200000.0)
        assertEquals(BigDecimal("100000.00"), result.grossMonthly.setScale(2, java.math.RoundingMode.HALF_UP))
        // PF contribution: 12% of gross monthly = 12000
        assertEquals(BigDecimal("12000.00"), result.pfContribution.setScale(2, java.math.RoundingMode.HALF_UP))
        // Tax: 15% of gross monthly (CTC > 1,000,000) = 15000
        assertEquals(BigDecimal("15000.00"), result.estimatedTax.setScale(2, java.math.RoundingMode.HALF_UP))
        // Take home: 100000 - 12000 - 15000 = 73000
        assertEquals(BigDecimal("73000.00"), result.takeHome.setScale(2, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testCalculateInflation() {
        val result = viewModel.calculateInflation(10000.0, 6.0, 15.0)
        assertTrue(result.amountNeeded > BigDecimal("10000"))
        assertTrue(result.futurePurchasePower < BigDecimal("10000"))
    }

    @Test
    fun testCalculateFdRd() {
        // FD: 100,000 at 7.1% for 5 years
        val fd = viewModel.calculateFdRd(100000.0, 7.1, 5.0, true)
        assertEquals(BigDecimal("100000.00"), fd.principalInvested.setScale(2, java.math.RoundingMode.HALF_UP))
        assertTrue(fd.maturityValue > BigDecimal("100000"))
        assertTrue(fd.interestGains > BigDecimal.ZERO)

        // RD: 5,000 monthly at 7.1% for 5 years (60 months)
        val rd = viewModel.calculateFdRd(5000.0, 7.1, 5.0, false)
        assertEquals(BigDecimal("300000.00"), rd.principalInvested.setScale(2, java.math.RoundingMode.HALF_UP))
        assertTrue(rd.maturityValue > BigDecimal("300000"))
    }
}
