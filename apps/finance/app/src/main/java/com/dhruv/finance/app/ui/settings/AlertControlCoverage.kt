package com.dhruv.finance.app.ui.settings

import com.dhruv.core.notification.NotificationChannelSpec
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsRow

/** The convention every alert `Toggle` follows (contract §3): its `key` is `alert_<channelId>`. */
private const val ALERT_KEY_PREFIX = "alert_"

/** Every alert `Toggle` row across [contributions], keyed by the channel id it controls. */
fun alertControlChannelIds(contributions: List<SettingsContribution>): Set<String> =
    contributions
        .flatMap { it.groups }
        .flatMap { it.rows }
        .filterIsInstance<SettingsRow.Toggle>()
        .mapNotNull { row -> row.key.takeIf { it.startsWith(ALERT_KEY_PREFIX) }?.removePrefix(ALERT_KEY_PREFIX) }
        .toSet()

/**
 * `SET-BR-006`: the notification channel registry and the contributed alert toggles must be equal
 * in count and map one-to-one — no channel without a control, no control without a channel
 * (contract §3 rule 11).
 */
fun alertChannelCoverageIsOneToOne(
    channels: List<NotificationChannelSpec>,
    contributions: List<SettingsContribution>,
): Boolean = channels.map { it.id }.toSet() == alertControlChannelIds(contributions)
