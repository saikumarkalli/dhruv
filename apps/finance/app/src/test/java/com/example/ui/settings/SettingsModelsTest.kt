package com.example.ui.settings

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
        assertTrue(defaultState.isConverterEnabled)
        assertTrue(defaultState.isDateEnabled)
        assertTrue(defaultState.isFinanceEnabled)
        assertEquals("cyan", defaultState.calculatorColor)
        assertEquals("purple", defaultState.converterColor)
        assertEquals("coral", defaultState.dateColor)
        assertEquals("amber", defaultState.financeColor)
    }

    @Test
    fun testSettingsConstants() {
        // Assert that the constants lists have the expected number of tools
        assertTrue(SettingsConstants.CONVERTER_TOOLS.isNotEmpty())
        assertTrue(SettingsConstants.DATE_TOOLS.isNotEmpty())
        assertTrue(SettingsConstants.FINANCE_TOOLS.isNotEmpty())
        
        assertTrue(SettingsConstants.CONVERTER_TOOLS.contains("Currency"))
        assertTrue(SettingsConstants.DATE_TOOLS.contains("Date Difference"))
        assertTrue(SettingsConstants.FINANCE_TOOLS.contains("Loan EMI"))
    }
}
