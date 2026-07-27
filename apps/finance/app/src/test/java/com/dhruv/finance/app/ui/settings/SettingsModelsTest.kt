package com.dhruv.finance.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelsTest {
    @Test
    fun testSettingsUiStateDefaults() {
        val defaultState = SettingsUiState()

        // Assert default values match expectations
        assertTrue(defaultState.isDegree)
        assertEquals("system", defaultState.darkModePreference)
        assertEquals(4, defaultState.decimalPrecision)
        assertEquals("international", defaultState.formatLocale)
        assertEquals(false, defaultState.isHistoryLocked)
        assertEquals("", defaultState.historyPinCode)
        assertEquals("#F05A28", defaultState.accentColorHex)
        assertEquals(false, defaultState.biometricEnabled)
    }
}
