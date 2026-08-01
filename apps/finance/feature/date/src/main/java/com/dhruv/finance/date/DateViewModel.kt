package com.dhruv.finance.date

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DateViewModel(
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "date") {

    private val _activeSubCalculator = MutableStateFlow<Int?>(null)
    val activeSubCalculator: StateFlow<Int?> = _activeSubCalculator.asStateFlow()

    fun setActiveSubCalculator(index: Int?) {
        _activeSubCalculator.value = index
    }

    // --- 1. Date Difference Calculations ---
    data class DateDifferenceResult(
        val totalDays: Long,
        val years: Int,
        val months: Int,
        val days: Int,
        val weeks: Long,
        val remainingDays: Long,
    )

    fun calculateDifference(
        date1: Calendar,
        date2: Calendar,
    ): DateDifferenceResult {
        val d1 = date1.timeInMillis
        val d2 = date2.timeInMillis
        val diffMs = Math.abs(d2 - d1)
        val totalDays = TimeUnit.MILLISECONDS.toDays(diffMs)

        val calStart = if (date1.before(date2)) date1.clone() as Calendar else date2.clone() as Calendar
        val calEnd = if (date1.before(date2)) date2.clone() as Calendar else date1.clone() as Calendar

        var yrs = calEnd.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)
        var mths = calEnd.get(Calendar.MONTH) - calStart.get(Calendar.MONTH)
        var dys = calEnd.get(Calendar.DAY_OF_MONTH) - calStart.get(Calendar.DAY_OF_MONTH)

        if (dys < 0) {
            mths -= 1
            val tempCal = calStart.clone() as Calendar
            tempCal.add(Calendar.MONTH, yrs * 12 + mths)
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dys += daysInMonth
        }
        if (mths < 0) {
            yrs -= 1
            mths += 12
        }

        return DateDifferenceResult(
            totalDays = totalDays,
            years = yrs,
            months = mths,
            days = dys,
            weeks = totalDays / 7,
            remainingDays = totalDays % 7,
        )
    }

    // --- 2. Add / Subtract Days ---
    fun offsetDate(
        baseDate: Calendar,
        offsetDays: Int,
        isSubtract: Boolean,
    ): Date {
        val clone = baseDate.clone() as Calendar
        if (isSubtract) {
            clone.add(Calendar.DAY_OF_YEAR, -offsetDays)
        } else {
            clone.add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        return clone.time
    }

    // --- 3. Age Calculator ---
    data class AgeResult(
        val years: Int,
        val months: Int,
        val days: Int,
        val nextMonths: Int,
        val nextDays: Int,
        val dayOfWeekOfNextBirthday: String,
        val totalDays: Long,
        val totalMonths: Int,
        val totalWeeks: Long,
        val totalHours: Long,
        val totalMinutes: Long,
    )

    fun calculateAge(
        birthDate: Calendar,
        referenceDate: Calendar,
    ): AgeResult = performanceTracer.trace("date_age_calc") {
        if (birthDate.after(referenceDate)) {
            return@trace AgeResult(0, 0, 0, 0, 0, "Monday", 0L, 0, 0L, 0L, 0L)
        }

        var yrs = referenceDate.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        var mths = referenceDate.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH)
        var dys = referenceDate.get(Calendar.DAY_OF_MONTH) - birthDate.get(Calendar.DAY_OF_MONTH)

        if (dys < 0) {
            mths -= 1
            val tempCal = birthDate.clone() as Calendar
            tempCal.add(Calendar.MONTH, yrs * 12 + mths)
            val daysInBirthMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dys += daysInBirthMonth
        }
        if (mths < 0) {
            yrs -= 1
            mths += 12
        }

        val nextBd = birthDate.clone() as Calendar
        nextBd.set(Calendar.YEAR, referenceDate.get(Calendar.YEAR))
        if (nextBd.before(referenceDate) || nextBd.equals(referenceDate)) {
            nextBd.add(Calendar.YEAR, 1)
        }

        val dayOfWeekOfNextBirthday = SimpleDateFormat("EEEE", Locale.US).format(nextBd.time)

        var nMonths = nextBd.get(Calendar.MONTH) - referenceDate.get(Calendar.MONTH)
        var nDays = nextBd.get(Calendar.DAY_OF_MONTH) - referenceDate.get(Calendar.DAY_OF_MONTH)

        if (nDays < 0) {
            nMonths -= 1
            val tempCal = referenceDate.clone() as Calendar
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            nDays += daysInMonth
        }
        if (nMonths < 0) {
            nMonths += 12
        }

        val totalMs = referenceDate.timeInMillis - birthDate.timeInMillis
        val tDays = if (totalMs >= 0) TimeUnit.MILLISECONDS.toDays(totalMs) else 0L

        AgeResult(
            years = yrs,
            months = mths,
            days = dys,
            nextMonths = nMonths,
            nextDays = nDays,
            dayOfWeekOfNextBirthday = dayOfWeekOfNextBirthday,
            totalDays = tDays,
            totalMonths = yrs * 12 + mths,
            totalWeeks = tDays / 7,
            totalHours = tDays * 24,
            totalMinutes = tDays * 24 * 60,
        )
    }

    // --- 4. Business Working Days ---
    data class BusinessDaysResult(
        val workingDays: Int,
        val weekends: Int,
        val totalDays: Int,
    )

    fun calculateBusinessDays(
        date1: Calendar,
        date2: Calendar,
    ): BusinessDaysResult {
        val y1 = date1.get(Calendar.YEAR)
        val m1 = date1.get(Calendar.MONTH) + 1
        val d1 = date1.get(Calendar.DAY_OF_MONTH)

        val y2 = date2.get(Calendar.YEAR)
        val m2 = date2.get(Calendar.MONTH) + 1
        val d2 = date2.get(Calendar.DAY_OF_MONTH)

        return try {
            val localStart = LocalDate.of(y1, m1, d1)
            val localEnd = LocalDate.of(y2, m2, d2)

            val dStart = if (localStart.isBefore(localEnd)) localStart else localEnd
            val dEnd = if (localStart.isBefore(localEnd)) localEnd else localStart

            val totalDays = ChronoUnit.DAYS.between(dStart, dEnd) + 1
            val weeks = totalDays / 7
            var weekends = weeks * 2
            val remainingDays = totalDays % 7

            if (remainingDays > 0) {
                for (i in 0 until remainingDays) {
                    val checkDay = dStart.plusDays(weeks * 7 + i)
                    val dayOfWeek = checkDay.dayOfWeek
                    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                        weekends++
                    }
                }
            }

            BusinessDaysResult(
                workingDays = (totalDays - weekends).toInt(),
                weekends = weekends.toInt(),
                totalDays = totalDays.toInt(),
            )
        } catch (e: Exception) {
            crashReporter.recordException(e)
            BusinessDaysResult(0, 0, 0)
        }
    }

    // --- 5. Time Zone Conversion ---
    fun convertTimeZone(
        sourceHour: Int,
        sourceMinute: Int,
        sourceZone: ZoneId,
        targetZone: ZoneId,
    ): String =
        try {
            val now = Calendar.getInstance()
            val zonedDateTime =
                ZonedDateTime.of(
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    sourceHour.coerceIn(0, 23),
                    sourceMinute.coerceIn(0, 59),
                    0,
                    0,
                    sourceZone,
                )
            val convertedTime = zonedDateTime.withZoneSameInstant(targetZone)
            val formatter = DateTimeFormatter.ofPattern("hh:mm a (EEEE)", Locale.US)
            convertedTime.format(formatter)
        } catch (e: Exception) {
            crashReporter.recordException(e)
            "---"
        }

    // --- 6. Unix Epoch Conversion ---
    fun unixTimestampToDateString(unixSeconds: Long): String =
        try {
            val date = Date(unixSeconds * 1000)
            val sdfLocal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.US)
            // 'UTC' must be quoted — unquoted U/T/C are reserved SimpleDateFormat pattern letters and
            // throw IllegalArgumentException, which previously made this whole method return the
            // "Invalid Timestamp Number" fallback for every input.
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            "Local: ${sdfLocal.format(date)}\nUTC: ${sdfUtc.format(date)}"
        } catch (e: Exception) {
            crashReporter.recordException(e)
            "Invalid Timestamp Number"
        }

    fun dateComponentsToUnixTimestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        try {
            val cal =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            cal.timeInMillis / 1000
        } catch (e: Exception) {
            crashReporter.recordException(e)
            0L
        }
}
