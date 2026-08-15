package com.dhruv.finance.date

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Calendar

/**
 * Regression tests for [DateViewModel]'s pure date math. Flag `date` ships OFF, but the code is live
 * and must stay regression-safe. All calculations are plain JVM (Calendar / java.time), exercised
 * with the no-op observability fakes. Fixed IANA zones (no DST at the tested instants) keep the
 * timezone assertions deterministic.
 */
class DateViewModelTest {
    private val vm = DateViewModel(NoOpCrashReporter, NoOpPerformanceTracer)

    private fun cal(
        year: Int,
        month: Int,
        day: Int,
    ): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }

    // --- Date difference ---

    @Test
    fun differenceAcrossALeapYearCountsAllDays() {
        val r = vm.calculateDifference(cal(2020, 1, 1), cal(2021, 1, 1)) // 2020 is a leap year
        assertEquals(366L, r.totalDays)
        assertEquals(1, r.years)
        assertEquals(0, r.months)
        assertEquals(52L, r.weeks)
        assertEquals(2L, r.remainingDays)
    }

    @Test
    fun differenceIsSymmetricRegardlessOfArgumentOrder() {
        val forward = vm.calculateDifference(cal(2021, 3, 1), cal(2021, 4, 1))
        val backward = vm.calculateDifference(cal(2021, 4, 1), cal(2021, 3, 1))
        assertEquals(forward.totalDays, backward.totalDays)
        assertEquals(31L, forward.totalDays)
        assertEquals(1, forward.months)
    }

    @Test
    fun sameDayDifferenceIsZero() {
        val r = vm.calculateDifference(cal(2024, 6, 15), cal(2024, 6, 15))
        assertEquals(0L, r.totalDays)
        assertEquals(0, r.years)
    }

    // --- Offset ---

    @Test
    fun offsetAddsAndSubtractsCalendarDays() {
        val added = Calendar.getInstance().apply { time = vm.offsetDate(cal(2024, 1, 10), 5, false) }
        assertEquals(15, added.get(Calendar.DAY_OF_MONTH))

        val subtracted = Calendar.getInstance().apply { time = vm.offsetDate(cal(2024, 1, 10), 5, true) }
        assertEquals(5, subtracted.get(Calendar.DAY_OF_MONTH))
    }

    // --- Age ---

    @Test
    fun ageOnExactBirthdayIsWholeYears() {
        val r = vm.calculateAge(birthDate = cal(2000, 1, 15), referenceDate = cal(2020, 1, 15))
        assertEquals(20, r.years)
        assertEquals(0, r.months)
        assertEquals(0, r.days)
        assertEquals(240, r.totalMonths)
    }

    @Test
    fun ageBeforeThisYearsBirthdayBorrowsMonths() {
        val r = vm.calculateAge(birthDate = cal(2000, 1, 20), referenceDate = cal(2020, 1, 15))
        assertEquals(19, r.years)
        assertEquals(11, r.months)
    }

    @Test
    fun ageForFutureBirthDateIsZeroed() {
        val r = vm.calculateAge(birthDate = cal(2030, 1, 1), referenceDate = cal(2020, 1, 1))
        assertEquals(0, r.years)
        assertEquals(0L, r.totalDays)
    }

    // --- Business days ---

    @Test
    fun businessDaysExcludeWeekends() {
        // 2024-01-01 is a Monday; through Sunday 2024-01-07 → 5 working days, 1 weekend pair.
        val r = vm.calculateBusinessDays(cal(2024, 1, 1), cal(2024, 1, 7))
        assertEquals(7, r.totalDays)
        assertEquals(2, r.weekends)
        assertEquals(5, r.workingDays)
    }

    // --- Time zone ---

    @Test
    fun timeZoneConversionShiftsByOffset() {
        // 10:00 UTC → 15:30 India (+5:30), no DST.
        val out = vm.convertTimeZone(10, 0, ZoneId.of("UTC"), ZoneId.of("Asia/Kolkata"))
        assertTrue("expected 03:30 PM… but was '$out'", out.startsWith("03:30 PM"))
    }

    // --- Unix epoch ---

    @Test
    fun unixEpochZeroRendersTheUtcEpoch() {
        assertTrue(vm.unixTimestampToDateString(0).contains("UTC: 1970-01-01 00:00:00 UTC"))
    }

    @Test
    fun dateComponentsToUnixRoundTripsUtc() {
        assertEquals(0L, vm.dateComponentsToUnixTimestamp(1970, 1, 1, 0, 0))
        assertEquals(946_684_800L, vm.dateComponentsToUnixTimestamp(2000, 1, 1, 0, 0))
    }
}
