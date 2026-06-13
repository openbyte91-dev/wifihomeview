package com.openbyt91dev.wifihomeview.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OuiRepositoryTest {
    @Test
    fun `looks up vendor from colon or dash formatted mac`() {
        val repository = OuiRepository(
            """
            Assignment,Organization Name
            AABBCC,"Acme Devices, Inc."
            DDEEFF,Router Corp
            malformed
            """.trimIndent()
        )

        assertEquals("Acme Devices, Inc.", repository.getVendor("AA:BB:CC:11:22:33"))
        assertEquals("Router Corp", repository.getVendor("DD-EE-FF-00-11-22"))
    }

    @Test
    fun `returns null for unknown or invalid mac`() {
        val repository = OuiRepository(
            """
            Assignment,Organization Name
            AABBCC,Acme Devices
            """.trimIndent()
        )

        assertNull(repository.getVendor("11:22:33:44:55:66"))
        assertNull(repository.getVendor("bad"))
    }
}
