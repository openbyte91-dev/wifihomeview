package com.openbyt91dev.wifihomeview.data.repository

import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.model.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMergePolicyTest {
    @Test
    fun `merge preserves persistent fields for mac backed device`() {
        val cached = Device(
            ipAddress = "192.168.1.10",
            macAddress = "AA:BB:CC:DD:EE:FF",
            customName = "Office Laptop",
            isKnown = true,
            isSuspicious = true,
            firstSeen = 100L,
            vendor = "Old Vendor",
            type = DeviceType.PC,
            ssid = "Home"
        )
        val scanned = Device(
            ipAddress = "192.168.1.22",
            macAddress = "AA:BB:CC:DD:EE:FF",
            hostname = "laptop.local",
            vendor = "New Vendor",
            type = DeviceType.UNKNOWN
        )

        val merged = DeviceMergePolicy.merge(
            scanned = scanned,
            cached = cached,
            currentSsid = "Home",
            localIp = "192.168.1.2",
            localDeviceVendor = "Phone"
        )

        assertEquals("Office Laptop", merged.customName)
        assertTrue(merged.isKnown)
        assertTrue(merged.isSuspicious)
        assertEquals(100L, merged.firstSeen)
        assertEquals("New Vendor", merged.vendor)
        assertEquals(DeviceType.PC, merged.type)
        assertEquals("AA:BB:CC:DD:EE:FF", merged.macAddress)
    }

    @Test
    fun `merge preserves custom name and known state for ip only device`() {
        val cached = Device(
            ipAddress = "192.168.1.50",
            customName = "Guest Tablet",
            isKnown = true,
            firstSeen = 200L
        )
        val scanned = Device(ipAddress = "192.168.1.50", vendor = "Samsung")

        val merged = DeviceMergePolicy.merge(
            scanned = scanned,
            cached = cached,
            currentSsid = "Home",
            localIp = "192.168.1.2",
            localDeviceVendor = "Phone"
        )

        assertEquals("Guest Tablet", merged.customName)
        assertTrue(merged.isKnown)
        assertEquals(200L, merged.firstSeen)
        assertEquals("Samsung", merged.vendor)
    }

    @Test
    fun `merge marks current phone as known`() {
        val merged = DeviceMergePolicy.merge(
            scanned = Device(ipAddress = "192.168.1.2"),
            cached = null,
            currentSsid = "Home",
            localIp = "192.168.1.2",
            localDeviceVendor = "Google Pixel"
        )

        assertEquals("This Device (Your Phone)", merged.customName)
        assertEquals("Google Pixel", merged.vendor)
        assertEquals(DeviceType.PHONE, merged.type)
        assertTrue(merged.isKnown)
    }
}
