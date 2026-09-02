package com.dhruv.finance.networth

import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun loan(
    rateBps: Int,
    emiPaise: Long?,
) = LiabilityMeta(
    holdingId = "h1",
    liabilityType = LiabilityType.HOME_LOAN,
    rateBps = rateBps,
    emiPaise = emiPaise,
    debitDay = 5,
    tenureMonths = 240,
    paidMonths = 24,
    originalPrincipalPaise = 50_00_000_00L,
    collateral = null,
    linkedAccountId = null,
)

class AmortisationMathTest {
    // 0% interest is the easiest case to hand-verify: months = principal / EMI exactly, and an
    // extra payment saves zero interest (there is none to save).
    @Test
    fun `zero-rate payoff is exactly principal over EMI, and prepay saves no interest`() {
        val meta = loan(rateBps = 0, emiPaise = 10_000_00L)

        assertEquals(12, projectedPayoffMonths(meta, outstandingPaise = 1_20_000_00L))

        val projection = computePrepayProjection(meta, outstandingPaise = 1_20_000_00L, extraPaymentPaise = 20_000_00L)!!
        assertEquals(12, projection.currentPayoffMonths)
        assertEquals(10, projection.newPayoffMonths)
        assertEquals(2, projection.monthsSaved)
        assertEquals(0L, projection.interestSavedPaise)
    }

    @Test
    fun `a positive rate produces a payoff that is longer than the zero-rate case`() {
        val zeroRate = loan(rateBps = 0, emiPaise = 45_000_00L)
        val withRate = loan(rateBps = 850, emiPaise = 45_000_00L)
        val outstanding = 47_50_000_00L

        val zeroRateMonths = projectedPayoffMonths(zeroRate, outstanding)!!
        val withRateMonths = projectedPayoffMonths(withRate, outstanding)!!

        assertTrue(withRateMonths > zeroRateMonths)
    }

    // spec.md Story 4 Scenario 3: an extra payment always shortens the payoff and saves interest.
    @Test
    fun `an extra payment shortens payoff and saves a positive amount of interest at a positive rate`() {
        val meta = loan(rateBps = 850, emiPaise = 45_000_00L)
        val outstanding = 47_50_000_00L

        val projection = computePrepayProjection(meta, outstandingPaise = outstanding, extraPaymentPaise = 5_00_000_00L)!!

        assertTrue(projection.newPayoffMonths < projection.currentPayoffMonths)
        assertTrue(projection.interestSavedPaise > 0L)
    }

    @Test
    fun `projectedPayoffMonths is null without an EMI`() {
        val meta = loan(rateBps = 3600, emiPaise = null)

        assertNull(projectedPayoffMonths(meta, outstandingPaise = 25_000_00L))
    }

    @Test
    fun `projectedPayoffMonths is null when the EMI can never amortise the balance`() {
        // Interest-only payment at 36% APR on 1,00,000 paise is 3,000 paise/month — an EMI at or
        // below that never reduces the principal.
        val meta = loan(rateBps = 3600, emiPaise = 3_000L)

        assertNull(projectedPayoffMonths(meta, outstandingPaise = 1_00_000L))
    }

    @Test
    fun `computePrepayProjection is null when the extra payment is not smaller than the balance`() {
        val meta = loan(rateBps = 850, emiPaise = 45_000_00L)

        assertNull(computePrepayProjection(meta, outstandingPaise = 10_000_00L, extraPaymentPaise = 10_000_00L))
        assertNull(computePrepayProjection(meta, outstandingPaise = 10_000_00L, extraPaymentPaise = 20_000_00L))
    }
}
