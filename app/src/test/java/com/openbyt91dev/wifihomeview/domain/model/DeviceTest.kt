package com.openbyt91dev.wifihomeview.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTest {

    @Test
    fun `getDisplayName returns Custom Name when present`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            hostname = "android-123",
            mDnsName = "TV",
            customName = "My TV"
        )
        assertEquals("My TV", device.getDisplayName())
    }

    @Test
    fun `getDisplayName returns mDNS Name when Custom Name is missing`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            hostname = "android-123",
            mDnsName = "Bedroom TV"
        )
        assertEquals("Bedroom TV", device.getDisplayName())
    }

    @Test
    fun `getDisplayName returns UPnP Name when mDNS is missing`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            hostname = "android-123",
            upnpName = "Samsung 6 Series"
        )
        assertEquals("Samsung 6 Series", device.getDisplayName())
    }

    @Test
    fun `getDisplayName returns Vendor + Device when no friendly names`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            vendor = "Espressif"
        )
        assertEquals("Espressif", device.getDisplayName())
    }

    @Test
    fun `getDisplayName falls back to Unknown Device`() {
        val device = Device(
            ipAddress = "192.168.1.5"
        )
        assertEquals("Unknown Device", device.getDisplayName())
    }

    @Test
    fun `getDisplayName returns hostname when not IP-like`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            hostname = "my-laptop",
            vendor = "Espressif"
        )
        // Non-IP hostname wins over vendor
        assertEquals("my-laptop", device.getDisplayName())
    }

    @Test
    fun `getDisplayName filters IP-like hostname`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            hostname = "192.168.1.5",
            vendor = "Espressif"
        )
        // IP-like hostname is filtered, vendor wins
        assertEquals("Espressif", device.getDisplayName())
    }

    @Test
    fun `getDisplayName filters Unknown vendor`() {
        val device = Device(
            ipAddress = "192.168.1.5",
            vendor = "Unknown Vendor"
        )
        assertEquals("Unknown Device", device.getDisplayName())
    }
}