package com.dhruv.finance.loans

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

class LoansViewModel(
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "loans") {
    // --- Data structures for results ---

    data class EmiResult(
        val emi: BigDecimal,
        val totalInterest: BigDecimal,
        val totalPayment: BigDecimal,
    )

    // --- Calculations ---

    // Loan EMI
    fun calculateEmi(
        principal: Double,
        annualRate: Double,
        tenureYears: Double,
    ): EmiResult {
        return performanceTracer.trace("loans_calc") {
            if (principal <= 0.0 || annualRate < 0.0 || tenureYears <= 0.0) {
                return@trace EmiResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            }
            val p = BigDecimal(principal.toString())
            val n = tenureYears * 12.0

            if (annualRate == 0.0) {
                val emi = p.divide(BigDecimal(n.toString()), 10, RoundingMode.HALF_UP)
                return@trace EmiResult(emi, BigDecimal.ZERO, p)
            }

            val r = (annualRate / 12.0) / 100.0
            val compound = (1.0 + r).pow(n)
            val emiDouble = (principal * r * compound) / (compound - 1.0)

            val emi = BigDecimal(emiDouble.toString())
            val totalPayment = emi.multiply(BigDecimal(n.toString()))
            val totalInterest = totalPayment.subtract(p).coerceAtLeast(BigDecimal.ZERO)

            EmiResult(emi, totalInterest, totalPayment)
        }
    }
}
