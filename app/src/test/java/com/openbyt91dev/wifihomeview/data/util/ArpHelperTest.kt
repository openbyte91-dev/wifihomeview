package com.openbyt91dev.wifihomeview.data.util

import org.junit.Test
import org.junit.Assert.*

class ArpHelperTest {
    @Test
    fun `test ArpHelper returns null for non existent ip`() {
        val mac = ArpHelper.getMacAddress("255.255.255.255")
        assertNull(mac)
    }
}
