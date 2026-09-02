package com.dhruv.finance.data.tracker.model

/** The four frozen liability types (BR-C3) — mirrors `finance.liabilities_meta`'s CHECK
 * constraint. Only HOME_LOAN/CAR_LOAN carry a full amortisation schedule in practice (rate + EMI +
 * tenure + original principal all present); CREDIT_CARD/BNPL commonly leave tenure/principal null
 * (`liabilities_meta.sql`'s own comment). */
enum class LiabilityType {
    HOME_LOAN,
    CAR_LOAN,
    CREDIT_CARD,
    BNPL,
    ;

    companion object {
        /** Validates a raw liability-type code, same fixed-set discipline as [Sector.fromCode]. */
        fun fromCode(code: String): LiabilityType? = entries.find { it.name == code }
    }
}

/** A liability holding's loan/card terms (`finance.liabilities_meta`, ADR-0033) — mutable, unlike
 * [Valuation] (BR-C1's append-only rule does not apply here; loan terms genuinely change). The
 * *outstanding balance* is never a field here — it is the liability holding's latest valuation
 * (C6's "outstanding, not original" rule, FR-008). */
data class LiabilityMeta(
    val holdingId: String,
    val liabilityType: LiabilityType,
    val rateBps: Int,
    val emiPaise: Long?,
    val debitDay: Int?,
    val tenureMonths: Int?,
    val paidMonths: Int,
    val originalPrincipalPaise: Long?,
    val collateral: String?,
    val linkedAccountId: String?,
)

/** Request to [com.dhruv.finance.data.tracker.repo.LiabilityRepository.createMeta] — bundled into
 * one type rather than a long parameter list (same shape as [CreateHoldingRequest]).
 * [liabilityTypeCode] is raw/unvalidated input; the repository validates it against
 * [LiabilityType]'s fixed set (NW-BR-004-style). */
data class CreateLiabilityMetaRequest(
    val holdingId: String,
    val liabilityTypeCode: String,
    val rateBps: Int,
    val emiPaise: Long? = null,
    val debitDay: Int? = null,
    val tenureMonths: Int? = null,
    val originalPrincipalPaise: Long? = null,
    val collateral: String? = null,
    val requestId: String? = null,
)

/** Request to [com.dhruv.finance.data.tracker.repo.LiabilityRepository.updateMeta] — every field is
 * the new full value (a plain PATCH), never a partial merge; `liabilities_meta_update_own` is the
 * only RLS path that ever changes these columns client-side. */
data class UpdateLiabilityMetaRequest(
    val rateBps: Int,
    val emiPaise: Long?,
    val debitDay: Int?,
    val tenureMonths: Int?,
    val paidMonths: Int,
    val originalPrincipalPaise: Long?,
    val collateral: String?,
)

/** C7's amortisation split (spec.md Story 4 Scenario 2) — sums to the "total obligation" figure
 * (money paid so far + money still owed), never to the original principal alone. Paise-only
 * arithmetic (Article VII / DAT-BR-008 — this module's tracker data path forbids floating-point
 * numeric types). */
data class AmortisationSplit(
    val principalPaidPaise: Long,
    val interestPaidPaise: Long,
    val remainingPaise: Long,
)

/**
 * [remainingPaise] is the liability's current outstanding balance (its latest valuation, FR-008 —
 * never [LiabilityMeta.originalPrincipalPaise]). Null when either
 * [LiabilityMeta.originalPrincipalPaise] or [LiabilityMeta.emiPaise] is absent (a credit card or
 * BNPL line with no sanctioned principal) — there is no defined amortisation split without both.
 *
 * `principalPaid = originalPrincipal - remaining`; `totalPaidSoFar = emi * paidMonths`;
 * `interestPaid = totalPaidSoFar - principalPaid`. Algebraically
 * `principalPaid + interestPaid + remaining == totalPaidSoFar + remaining`, the total-obligation
 * figure spec.md Story 4 Scenario 2 requires the three parts to sum to.
 */
fun LiabilityMeta.amortisationSplit(remainingPaise: Long): AmortisationSplit? =
    originalPrincipalPaise?.let { originalPrincipal ->
        emiPaise?.let { emi ->
            val principalPaid = originalPrincipal - remainingPaise
            val totalPaidSoFar = emi * paidMonths
            val interestPaid = totalPaidSoFar - principalPaid
            AmortisationSplit(
                principalPaidPaise = principalPaid,
                interestPaidPaise = interestPaid,
                remainingPaise = remainingPaise,
            )
        }
    }
