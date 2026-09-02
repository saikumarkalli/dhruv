package com.dhruv.finance.networth

import com.dhruv.finance.data.tracker.model.LiabilityMeta
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.round

private const val BASIS_POINTS_PER_UNIT = 10_000.0
private const val MONTHS_PER_YEAR = 12.0

/** A loan-type liability's prepay-savings projection (spec.md Story 4 Scenario 3, NW-UI-004,
 * FR-009) — a **derived, estimated** figure (`platform/DESIGN-SYSTEM.md` §10: "derived/AI output is
 * labelled as derived"), assuming the rate and EMI stay fixed for the rest of the term. */
data class PrepayProjection(
    val currentPayoffMonths: Int,
    val newPayoffMonths: Int,
    val monthsSaved: Int,
    val interestSavedPaise: Long,
)

/** Remaining whole months to amortise [outstandingPaise] at [meta]'s rate/EMI, ceiling-rounded.
 * Null when the liability lacks EMI terms, or its EMI is at or below the interest-only payment (the
 * balance would never shrink). Double/`ln` math is fine in this file: it lives in the feature
 * module, not `:apps:finance:data`'s tracker path that `checkTrackerMoneyPrecision` scans
 * (Article VII / DAT-BR-008 only restricts that path). */
// Deliberately multiple early returns: EMI presence, EMI positivity, outstanding positivity, and
// the not-computable case are four genuinely distinct exits — same accepted pattern as
// AuthInterceptor.intercept() and ValuationRepositoryImpl.recordValue().
@Suppress("ReturnCount")
fun projectedPayoffMonths(
    meta: LiabilityMeta,
    outstandingPaise: Long,
): Int? {
    val emi = meta.emiPaise ?: return null
    if (emi <= 0L) return null
    if (outstandingPaise <= 0L) return null
    val monthlyRate = meta.rateBps / BASIS_POINTS_PER_UNIT / MONTHS_PER_YEAR
    val months = remainingMonths(outstandingPaise.toDouble(), emi.toDouble(), monthlyRate) ?: return null
    return ceil(months).toInt()
}

/** [extraPaymentPaise] is a one-time additional payment applied to the current outstanding balance
 * today (FR-009's "hypothetical extra payment"). Null when the projection isn't computable — no EMI
 * terms, the extra payment isn't smaller than what's owed, or the EMI can never amortise the
 * balance either before or after the extra payment. */
// Deliberately multiple early returns, same accepted pattern as [projectedPayoffMonths] above —
// each guard is its own genuinely distinct exit, kept as single-operator conditions rather than one
// large boolean expression.
@Suppress("ReturnCount")
fun computePrepayProjection(
    meta: LiabilityMeta,
    outstandingPaise: Long,
    extraPaymentPaise: Long,
): PrepayProjection? {
    val emi = meta.emiPaise ?: return null
    if (emi <= 0L) return null
    if (outstandingPaise <= 0L) return null
    if (extraPaymentPaise <= 0L) return null
    if (extraPaymentPaise >= outstandingPaise) return null
    val monthlyRate = meta.rateBps / BASIS_POINTS_PER_UNIT / MONTHS_PER_YEAR
    val currentMonths = remainingMonths(outstandingPaise.toDouble(), emi.toDouble(), monthlyRate) ?: return null
    val newPrincipal = outstandingPaise - extraPaymentPaise
    val newMonths = remainingMonths(newPrincipal.toDouble(), emi.toDouble(), monthlyRate) ?: return null
    val interestSavedPaise =
        (emi * currentMonths - emi * newMonths - extraPaymentPaise)
            .let { round(it).toLong() }
            .coerceAtLeast(0L)
    return PrepayProjection(
        currentPayoffMonths = ceil(currentMonths).toInt(),
        newPayoffMonths = ceil(newMonths).toInt(),
        monthsSaved = (ceil(currentMonths) - ceil(newMonths)).toInt().coerceAtLeast(0),
        interestSavedPaise = interestSavedPaise,
    )
}

/** Standard amortisation formula, `n = ln(EMI / (EMI - r*P)) / ln(1 + r)` for `r > 0`, or a plain
 * `P / EMI` when the liability is interest-free. Null when [emi] can never amortise [principal] —
 * the interest-only payment (`r*P`) is at or above [emi]. */
@Suppress("ReturnCount")
private fun remainingMonths(
    principal: Double,
    emi: Double,
    monthlyRate: Double,
): Double? {
    if (monthlyRate <= 0.0) return principal / emi
    val interestOnlyPayment = monthlyRate * principal
    if (emi <= interestOnlyPayment) return null
    return ln(emi / (emi - interestOnlyPayment)) / ln(1 + monthlyRate)
}
