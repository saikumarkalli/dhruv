package com.dhruv.finance.money

import com.dhruv.finance.data.tracker.model.Account
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * FR-020's staleness rule, shared by D6 ([AccountsScreen]) and D7 ([AccountDetailScreen]) so the
 * two screens never drift on the definition (`MoneyConfig.STALENESS_THRESHOLD_DAYS`). An account
 * that has never been reconciled is treated as stale too — it has never had its balance confirmed
 * within any window, not just the current one.
 */
fun Account.isStale(now: Instant = Instant.now()): Boolean {
    val last = reconciledAt ?: return true
    return Duration.between(last, now).toDays() > MoneyConfig.STALENESS_THRESHOLD_DAYS
}

/** "Reconciled 28 Jul · needs check" (design wording) — or the never-reconciled equivalent. */
fun Account.staleMessage(now: Instant = Instant.now()): String {
    val last = reconciledAt ?: return "Never reconciled · needs check"
    val label = last.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM"))
    return "Reconciled $label · needs check"
}
