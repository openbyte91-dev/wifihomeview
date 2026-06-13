package com.openbyt91dev.wifihomeview.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameCleanerTest {
    @Test
    fun `removes technical protocol fragments`() {
        assertEquals("Living Room", NameCleaner.clean("Living Room, Linux/5.4, UPnP/1.0"))
    }

    @Test
    fun `returns friendly overrides for known devices`() {
        assertEquals("Chromecast", NameCleaner.clean("Chromecast/1.56 Living Room"))
        assertEquals("Airtel Router", NameCleaner.clean("AOT-4221.airtel"))
    }

    @Test
    fun `filters blank and generic names`() {
        assertNull(NameCleaner.clean(""))
        assertNull(NameCleaner.clean("generic"))
    }
}
