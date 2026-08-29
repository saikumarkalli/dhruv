package com.dhruv.finance.app.ui.settings

import com.dhruv.core.notification.NotificationChannelRegistry
import com.dhruv.core.notification.NotificationChannelSpec
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun toggle(key: String) = SettingsRow.Toggle(key = key, label = 1, description = 2, value = flowOf(false), onChange = {})

private fun contributionWithAlerts(
    moduleKey: String,
    alertKeys: List<String>,
) = SettingsContribution(
    moduleKey = moduleKey,
    title = 1,
    summary = 2,
    order = 0,
    groups = listOf(SettingsGroup(label = null, rows = alertKeys.map { toggle(it) })),
)

/**
 * `SET-BR-006` (contract §3 rule 11): every channel in [NotificationChannelRegistry] has exactly
 * one control, in the module that owns it — registry and contributed alert toggles stay 1:1.
 * `FR-031` (`SET-BR-` uncovered, analysis finding C4): an alert whose source feature hasn't
 * shipped is absent from its module's entry, not present-and-inert.
 */
class AlertControlCoverageTest {
    @Test
    fun `every registry channel has exactly one alert control, one to one`() {
        val channels = listOf(NotificationChannelSpec("daily_rates"), NotificationChannelSpec("app_updates"))
        val contributions =
            listOf(
                contributionWithAlerts("currency", listOf("alert_daily_rates")),
                contributionWithAlerts("app", listOf("alert_app_updates")),
            )
        assertTrue(alertChannelCoverageIsOneToOne(channels, contributions))
    }

    @Test
    fun `a channel with no control anywhere fails coverage`() {
        val channels = listOf(NotificationChannelSpec("daily_rates"), NotificationChannelSpec("app_updates"))
        val contributions = listOf(contributionWithAlerts("currency", listOf("alert_daily_rates")))
        assertFalse(alertChannelCoverageIsOneToOne(channels, contributions))
    }

    @Test
    fun `a control with no matching channel fails coverage`() {
        val channels = listOf(NotificationChannelSpec("daily_rates"))
        val contributions =
            listOf(contributionWithAlerts("currency", listOf("alert_daily_rates", "alert_metals_rates")))
        assertFalse(alertChannelCoverageIsOneToOne(channels, contributions))
    }

    @Test
    fun `the real registered channel registry and the real shipped contributions are one to one`() {
        // Built from the actual feature-module factories, not a stand-in: this is what makes the
        // test fail if currency ever drops its `alert_daily_rates` toggle, or if a channel is
        // added to the registry with no module owning it. Only `daily_rates` has a shipped owning
        // module today (currency, T090) — every other channel named in the surface-registry doc
        // belongs to an R4-R7 tracker feature that does not exist yet, so it is correctly absent
        // from both sides.
        assertTrue(alertChannelCoverageIsOneToOne(NotificationChannelRegistry.channels, realSettingsContributions()))
    }

    @Test
    fun `FR-031 - an alert whose source feature has not shipped is absent, not present-and-inert`() {
        // The currency module's metals-rates alert is designed but not built (see
        // apps/finance/docs/superpowers — currency-metals-notification plan). Asserted against the
        // real contributions, so shipping the row before the feature genuinely fails this.
        val alertKeys = alertControlChannelIds(realSettingsContributions())
        assertFalse("metals_rates" in alertKeys)
    }
}
