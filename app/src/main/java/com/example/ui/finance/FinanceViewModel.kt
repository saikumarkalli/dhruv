package com.example.ui.finance

import androidx.lifecycle.ViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FinanceViewModel : ViewModel() {

    private val _activeFinanceCalc = MutableStateFlow<Int?>(null)
    val activeFinanceCalc: StateFlow<Int?> = _activeFinanceCalc.asStateFlow()

    fun setActiveFinanceCalc(index: Int?) {
        _activeFinanceCalc.value = index
    }

    // --- Data structures for results ---
    data class EmiResult(
        val emi: BigDecimal,
        val totalInterest: BigDecimal,
        val totalPayment: BigDecimal
    )

    data class SimpleCompoundResult(
        val simpleInterest: BigDecimal,
        val simpleTotal: BigDecimal,
        val compoundInterest: BigDecimal,
        val compoundTotal: BigDecimal
    )

    data class SipResult(
        val totalInvested: BigDecimal,
        val estimatedReturns: BigDecimal,
        val futureValue: BigDecimal
    )

    data class RoiCagrResult(
        val absoluteReturn: Double,
        val cagr: Double
    )

    data class GstResult(
        val preTaxBase: BigDecimal,
        val taxAmount: BigDecimal,
        val totalAmount: BigDecimal
    )

    data class DiscountMarkupResult(
        val offset: BigDecimal,
        val finalVal: BigDecimal
    )

    data class TipSplitResult(
        val totalTip: BigDecimal,
        val overallTotal: BigDecimal,
        val splitTip: BigDecimal,
        val splitBill: BigDecimal
    )

    data class SalaryBreakupResult(
        val grossMonthly: BigDecimal,
        val pfContribution: BigDecimal,
        val estimatedTax: BigDecimal,
        val takeHome: BigDecimal
    )

    data class InflationResult(
        val futurePurchasePower: BigDecimal,
        val amountNeeded: BigDecimal
    )

    data class FdRdResult(
        val principalInvested: BigDecimal,
        val interestGains: BigDecimal,
        val maturityValue: BigDecimal
    )

    // --- Calculations ---

    // 0. Loan EMI
    fun calculateEmi(principal: Double, annualRate: Double, tenureYears: Double): EmiResult {
        if (principal <= 0.0 || annualRate < 0.0 || tenureYears <= 0.0) {
            return EmiResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val p = BigDecimal(principal.toString())
        val n = tenureYears * 12.0
        
        if (annualRate == 0.0) {
            val emi = p.divide(BigDecimal(n.toString()), 10, RoundingMode.HALF_UP)
            return EmiResult(emi, BigDecimal.ZERO, p)
        }

        val r = (annualRate / 12.0) / 100.0
        val compound = (1.0 + r).pow(n)
        val emiDouble = (principal * r * compound) / (compound - 1.0)
        
        val emi = BigDecimal(emiDouble.toString())
        val totalPayment = emi.multiply(BigDecimal(n.toString()))
        val totalInterest = totalPayment.subtract(p).coerceAtLeast(BigDecimal.ZERO)
        
        return EmiResult(emi, totalInterest, totalPayment)
    }

    // 1. Simple & Compound Interest
    fun calculateSimpleCompound(principal: Double, rate: Double, years: Double, compoundingPeriods: Int): SimpleCompoundResult {
        if (principal <= 0.0 || rate < 0.0 || years <= 0.0) {
            return SimpleCompoundResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val p = BigDecimal(principal.toString())
        
        // Simple Interest
        val rSimple = rate / 100.0
        val simpleInterest = p.multiply(BigDecimal(rSimple.toString())).multiply(BigDecimal(years.toString()))
        val simpleTotal = p.add(simpleInterest)

        // Compound Interest
        val periods = compoundingPeriods.coerceAtLeast(1)
        val rCompound = rate / 100.0 / periods
        val nt = periods * years
        val compoundTotalDouble = principal * (1.0 + rCompound).pow(nt)
        val compoundTotal = BigDecimal(compoundTotalDouble.toString())
        val compoundInterest = compoundTotal.subtract(p).coerceAtLeast(BigDecimal.ZERO)

        return SimpleCompoundResult(simpleInterest, simpleTotal, compoundInterest, compoundTotal)
    }

    // 2. SIP Growth
    fun calculateSip(amount: Double, rate: Double, years: Double): SipResult {
        if (amount <= 0.0 || rate < 0.0 || years <= 0.0) {
            return SipResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val n = years * 12.0
        val p = BigDecimal(amount.toString())
        val totalInvested = p.multiply(BigDecimal(n.toString()))

        if (rate == 0.0) {
            return SipResult(totalInvested, BigDecimal.ZERO, totalInvested)
        }

        val i = (rate / 100.0) / 12.0
        val futureValueDouble = amount * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
        val futureValue = BigDecimal(futureValueDouble.toString())
        val estimatedReturns = futureValue.subtract(totalInvested).coerceAtLeast(BigDecimal.ZERO)

        return SipResult(totalInvested, estimatedReturns, futureValue)
    }

    // 3. ROI / CAGR
    fun calculateRoiCagr(initial: Double, finalVal: Double, years: Double): RoiCagrResult {
        if (initial <= 0.0 || finalVal < 0.0 || years <= 0.0) {
            return RoiCagrResult(0.0, 0.0)
        }
        val absoluteReturn = ((finalVal - initial) / initial) * 100.0
        val cagr = ((finalVal / initial).pow(1.0 / years) - 1.0) * 100.0
        return RoiCagrResult(absoluteReturn, cagr)
    }

    // 4. GST / Tax
    fun calculateGst(amount: Double, gstPercent: Double, isAddGst: Boolean): GstResult {
        if (amount <= 0.0 || gstPercent < 0.0) {
            return GstResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val amt = BigDecimal(amount.toString())
        val gst = BigDecimal(gstPercent.toString())
        val hundred = BigDecimal("100")

        return if (isAddGst) {
            val taxAmount = amt.multiply(gst).divide(hundred, 10, RoundingMode.HALF_UP)
            val totalAmount = amt.add(taxAmount)
            GstResult(amt, taxAmount, totalAmount)
        } else {
            val factor = BigDecimal.ONE.add(gst.divide(hundred, 10, RoundingMode.HALF_UP))
            val originalBase = amt.divide(factor, 10, RoundingMode.HALF_UP)
            val taxAmount = amt.subtract(originalBase)
            GstResult(originalBase, taxAmount, amt)
        }
    }

    // 5. Discount & Markup
    fun calculateDiscountMarkup(basePrice: Double, percent: Double, isDiscount: Boolean): DiscountMarkupResult {
        if (basePrice <= 0.0 || percent < 0.0) {
            return DiscountMarkupResult(BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val base = BigDecimal(basePrice.toString())
        val pct = BigDecimal(percent.toString())
        val hundred = BigDecimal("100")
        val offset = base.multiply(pct).divide(hundred, 10, RoundingMode.HALF_UP)
        val finalVal = if (isDiscount) base.subtract(offset) else base.add(offset)
        return DiscountMarkupResult(offset, finalVal.coerceAtLeast(BigDecimal.ZERO))
    }

    // 6. Tip & Bill Split
    fun calculateTipSplit(bill: Double, tipPercent: Double, people: Int): TipSplitResult {
        if (bill <= 0.0 || tipPercent < 0.0 || people <= 0) {
            return TipSplitResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val b = BigDecimal(bill.toString())
        val tPct = BigDecimal(tipPercent.toString())
        val pCount = BigDecimal(people.toString())
        val hundred = BigDecimal("100")

        val totalTip = b.multiply(tPct).divide(hundred, 10, RoundingMode.HALF_UP)
        val overallTotal = b.add(totalTip)
        val splitTip = totalTip.divide(pCount, 10, RoundingMode.HALF_UP)
        val splitBill = overallTotal.divide(pCount, 10, RoundingMode.HALF_UP)

        return TipSplitResult(totalTip, overallTotal, splitTip, splitBill)
    }

    // 7. Salary CTC Breakup
    fun calculateSalaryBreakup(ctc: Double): SalaryBreakupResult {
        if (ctc <= 0.0) {
            return SalaryBreakupResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val ctcBD = BigDecimal(ctc.toString())
        val twelve = BigDecimal("12")
        val grossMonthly = ctcBD.divide(twelve, 10, RoundingMode.HALF_UP)

        val pf = grossMonthly.multiply(BigDecimal("0.12"))
        val capPF = if (pf > BigDecimal("15000")) BigDecimal("15000") else pf

        val estimatedTax = when {
            ctc > 1000000.0 -> grossMonthly.multiply(BigDecimal("0.15"))
            ctc > 500000.0 -> grossMonthly.multiply(BigDecimal("0.05"))
            else -> BigDecimal.ZERO
        }

        val takeHome = grossMonthly.subtract(capPF).subtract(estimatedTax).coerceAtLeast(BigDecimal.ZERO)
        return SalaryBreakupResult(grossMonthly, capPF, estimatedTax, takeHome)
    }

    // 8. Inflation Adjusted Value
    fun calculateInflation(amount: Double, rate: Double, years: Double): InflationResult {
        if (amount <= 0.0 || rate < 0.0 || years <= 0.0) {
            return InflationResult(BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val futureMultiplier = (1.0 + rate / 100.0).pow(years)
        val futurePurchasePower = BigDecimal(amount / futureMultiplier)
        val amountNeeded = BigDecimal(amount * futureMultiplier)
        return InflationResult(futurePurchasePower, amountNeeded)
    }

    // 9. FD / RD Maturity
    fun calculateFdRd(amount: Double, rate: Double, years: Double, isFixedDeposit: Boolean): FdRdResult {
        if (amount <= 0.0 || rate < 0.0 || years <= 0.0) {
            return FdRdResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val pInvested: BigDecimal
        val maturity: BigDecimal

        if (isFixedDeposit) {
            pInvested = BigDecimal(amount.toString())
            val r = rate / 100.0 / 4.0
            val nt = 4.0 * years
            val maturityDouble = amount * (1.0 + r).pow(nt)
            maturity = BigDecimal(maturityDouble.toString())
        } else {
            val n = (years * 12.0).toInt()
            pInvested = BigDecimal(amount.toString()).multiply(BigDecimal(n.toString()))
            val tempR = (rate / 100.0) / 12.0
            if (tempR > 0.0) {
                var total = 0.0
                for (month in 1..n) {
                    total = (total + amount) * (1.0 + tempR)
                }
                maturity = BigDecimal(total.toString())
            } else {
                maturity = pInvested
            }
        }

        val interest = maturity.subtract(pInvested).coerceAtLeast(BigDecimal.ZERO)
        return FdRdResult(pInvested, interest, maturity)
    }
}
