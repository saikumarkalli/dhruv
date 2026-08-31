package com.dhruv.settings.contribution

import androidx.annotation.StringRes

/**
 * A group of rows inside one [SettingsContribution]. `label = null` means ungrouped rows shown at
 * the top of the entry; a non-null label names a submodule (contract §1 rule 4 — submodules are
 * groups inside a contribution, never sibling entries).
 */
data class SettingsGroup(
    @StringRes val label: Int? = null,
    val rows: List<SettingsRow>,
)
