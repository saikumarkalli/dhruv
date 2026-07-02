package com.dhruv.finance.everyday

import androidx.lifecycle.ViewModel
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

class EverydayViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    init {
        crashReporter.setModule("everyday")
    }

    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    @Suppress("unused")
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            crashReporter.recordException(throwable)
            _featureError.value = throwable
        }

    // --- Data structures for results ---

    data class SimpleCompoundResult(
        val simpleInterest: BigDecimal,
        val simpleTotal: BigDecimal,
        val compoundInterest: BigDecimal,
        val compoundTotal: BigDecimal,
    )

    data class DiscountMarkupResult(
        val offset: BigDecimal,
        val finalVal: BigDecimal,
    )

    data class TipSplitResult(
        val totalTip: BigDecimal,
        val overallTotal: BigDecimal,
        val splitTip: BigDecimal,
        val splitBill: BigDecimal,
    )

    data class InflationResult(
        val futurePurchasePower: BigDecimal,
        val amountNeeded: BigDecimal,
    )

    // --- Calculations ---

    // Simple & Compound Interest
    fun calculateSimpleCompound(
        principal: Double,
        rate: Double,
        years: Double,
        compoundingPeriods: Int,
    ): SimpleCompoundResult {
        return performanceTracer.trace("everyday_calc") {
            if (principal <= 0.0 || rate < 0.0 || years <= 0.0) {
                return@trace SimpleCompoundResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
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

            SimpleCompoundResult(simpleInterest, simpleTotal, compoundInterest, compoundTotal)
        }
    }

    // Discount & Markup
    fun calculateDiscountMarkup(
        basePrice: Double,
        percent: Double,
        isDiscount: Boolean,
    ): DiscountMarkupResult {
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

    // Tip & Bill Split
    fun calculateTipSplit(
        bill: Double,
        tipPercent: Double,
        people: Int,
    ): TipSplitResult {
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

    // Inflation Adjusted Value
    fun calculateInflation(
        amount: Double,
        rate: Double,
        years: Double,
    ): InflationResult {
        if (amount <= 0.0 || rate < 0.0 || years <= 0.0) {
            return InflationResult(BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val futureMultiplier = (1.0 + rate / 100.0).pow(years)
        val futurePurchasePower = BigDecimal(amount / futureMultiplier)
        val amountNeeded = BigDecimal(amount * futureMultiplier)
        return InflationResult(futurePurchasePower, amountNeeded)
    }
}
