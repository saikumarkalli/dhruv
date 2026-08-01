package com.dhruv.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {

    @Test
    fun `default pin code is 1234`() {
        assertEquals("1234", SettingsRepositoryImpl.DEFAULT_PIN_CODE)
    }

    @Test
    fun `default decimal precision is 4`() {
        assertEquals(4, SettingsRepositoryImpl.DEFAULT_DECIMAL_PRECISION)
    }
}
