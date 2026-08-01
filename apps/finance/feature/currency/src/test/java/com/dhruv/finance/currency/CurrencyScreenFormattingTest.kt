package com.dhruv.finance.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the pure formatting helpers backing DhruvNext §6.6's currency-selector subtitle,
 * rate/staleness caption row, and "in your currencies" per-row rate subline (CurrencyScreen.kt).
 * These are plain top-level functions (no ViewModel/Compose runtime involved), so they're testable
 * without Robolectric.
 */
class CurrencyScreenFormattingTest {
    private val rates = mapOf("USD" to 1.0, "EUR" to 0.5, "INR" to 80.0)

    @Test
    fun currencyDisplayName_knownCode_returnsMappedName() {
        assertEquals("Rupee", currencyDisplayName("INR"))
        assertEquals("Dollar", currencyDisplayName("USD"))
        assertEquals("Euro", currencyDisplayName("EUR"))
    }

    @Test
    fun currencyDisplayName_unknownCode_fallsBackToCodeItself() {
        assertEquals("XYZ", currencyDisplayName("XYZ"))
    }

    @Test
    fun buildRateText_validRates_formatsHeadlineRate() {
        // 1 USD -> INR: toRate/fromRate = 80.0/1.0 = 80
        assertEquals("1 USD = 80 INR", buildRateText("USD", "INR", rates))
    }

    @Test
    fun buildRateText_nonUsdBasePair_convertsViaUsdCorrectly() {
        // 1 EUR -> INR: toRate/fromRate = 80.0/0.5 = 160
        assertEquals("1 EUR = 160 INR", buildRateText("EUR", "INR", rates))
    }

    @Test
    fun buildRateText_missingRate_returnsNull() {
        assertNull(buildRateText("USD", "GBP", rates))
    }

    @Test
    fun buildRateText_zeroFromRate_returnsNull() {
        assertNull(buildRateText("USD", "INR", mapOf("USD" to 0.0, "INR" to 80.0)))
    }

    @Test
    fun buildFreshnessText_neverSynced_whenTimeIsNull() {
        assertEquals(
            "Updated never synced",
            buildFreshnessText(null, CurrencyViewModel.CurrencyStatus.Success(isOffline = false)),
        )
    }

    @Test
    fun buildFreshnessText_recentOnlineSync_hasNoCachedSuffix() {
        val justNow = System.currentTimeMillis()
        assertEquals(
            "Updated just now",
            buildFreshnessText(justNow, CurrencyViewModel.CurrencyStatus.Success(isOffline = false)),
        )
    }

    @Test
    fun buildFreshnessText_offlineCachedSync_appendsCachedSuffix() {
        val justNow = System.currentTimeMillis()
        assertEquals(
            "Updated just now · cached",
            buildFreshnessText(justNow, CurrencyViewModel.CurrencyStatus.Success(isOffline = true)),
        )
    }

    @Test
    fun buildUnitRateCaption_validRates_formatsPerRowSubline() {
        // 1 EUR expressed in USD: (1 / eurRate) * usdRate = (1/0.5)*1.0 = 2
        assertEquals("1 EUR = 2 USD", buildUnitRateCaption("EUR", "USD", rates))
    }

    @Test
    fun buildUnitRateCaption_missingRate_returnsNull() {
        assertNull(buildUnitRateCaption("GBP", "USD", rates))
    }
}
