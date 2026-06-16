package com.dhruv.finance.tax

import androidx.lifecycle.ViewModel
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode

class TaxViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer
) : ViewModel() {

    init {
        crashReporter.setModule("tax")
    }

    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    @Suppress("unused")
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        crashReporter.recordException(throwable)
        _featureError.value = throwable
    }

    // --- Data structures for results ---

    data class GstResult(
        val preTaxBase: BigDecimal,
        val taxAmount: BigDecimal,
        val totalAmount: BigDecimal
    )

    data class SalaryBreakupResult(
        val grossMonthly: BigDecimal,
        val pfContribution: BigDecimal,
        val estimatedTax: BigDecimal,
        val takeHome: BigDecimal
    )

    // --- Calculations ---

    // GST / Tax
    fun calculateGst(amount: Double, gstPercent: Double, isAddGst: Boolean): GstResult {
        return performanceTracer.trace("tax_calc") {
            if (amount <= 0.0 || gstPercent < 0.0) {
                return@trace GstResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            }
            val amt = BigDecimal(amount.toString())
            val gst = BigDecimal(gstPercent.toString())
            val hundred = BigDecimal("100")

            if (isAddGst) {
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
    }

    // Salary CTC Breakup
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
}
