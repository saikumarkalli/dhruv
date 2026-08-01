package com.dhruv.finance.investments

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import java.math.BigDecimal
import kotlin.math.pow

class InvestmentsViewModel(
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "investments") {

    // --- Data structures for results ---

    data class SipResult(
        val totalInvested: BigDecimal,
        val estimatedReturns: BigDecimal,
        val futureValue: BigDecimal,
    )

    data class RoiCagrResult(
        val absoluteReturn: Double,
        val cagr: Double,
    )

    data class FdRdResult(
        val principalInvested: BigDecimal,
        val interestGains: BigDecimal,
        val maturityValue: BigDecimal,
    )

    // --- Calculations ---

    // SIP Growth
    fun calculateSip(
        amount: Double,
        rate: Double,
        years: Double,
    ): SipResult {
        return performanceTracer.trace("investments_calc") {
            if (amount <= 0.0 || rate < 0.0 || years <= 0.0) {
                return@trace SipResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            }
            val n = years * 12.0
            val p = BigDecimal(amount.toString())
            val totalInvested = p.multiply(BigDecimal(n.toString()))

            if (rate == 0.0) {
                return@trace SipResult(totalInvested, BigDecimal.ZERO, totalInvested)
            }

            val i = (rate / 100.0) / 12.0
            val futureValueDouble = amount * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
            val futureValue = BigDecimal(futureValueDouble.toString())
            val estimatedReturns = futureValue.subtract(totalInvested).coerceAtLeast(BigDecimal.ZERO)

            SipResult(totalInvested, estimatedReturns, futureValue)
        }
    }

    // ROI / CAGR
    fun calculateRoiCagr(
        initial: Double,
        finalVal: Double,
        years: Double,
    ): RoiCagrResult {
        if (initial <= 0.0 || finalVal < 0.0 || years <= 0.0) {
            return RoiCagrResult(0.0, 0.0)
        }
        val absoluteReturn = ((finalVal - initial) / initial) * 100.0
        val cagr = ((finalVal / initial).pow(1.0 / years) - 1.0) * 100.0
        return RoiCagrResult(absoluteReturn, cagr)
    }

    // FD / RD Maturity
    fun calculateFdRd(
        amount: Double,
        rate: Double,
        years: Double,
        isFixedDeposit: Boolean,
    ): FdRdResult {
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
