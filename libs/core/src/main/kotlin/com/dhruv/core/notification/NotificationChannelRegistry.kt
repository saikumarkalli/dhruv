package com.dhruv.core.notification

/**
 * One entry per Android notification channel this app has actually shipped code for — not the
 * full aspirational channel list in `apps/finance/docs/superpowers/specs/2026-08-09-finance-
 * surface-registries.md` §2, most of which name a channel whose owning module (R4–R7 tracker
 * features) does not exist yet (FR-031: an alert whose source feature hasn't shipped must be
 * absent, not present-and-inert — that applies to this registry too, not only to a module's rows).
 */
data class NotificationChannelSpec(
    val id: String,
)

/**
 * `SET-BR-006`'s other half: every entry here MUST have exactly one contributed alert `Toggle`
 * (contract §3 rule 11), in the module that owns it. Append-only as real channels ship.
 */
object NotificationChannelRegistry {
    val channels: List<NotificationChannelSpec> = listOf(NotificationChannelSpec(id = "daily_rates"))
}
