package com.openbyt91dev.wifihomeview.data.repository

import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.model.DeviceType

internal object DeviceMergePolicy {
    fun merge(
        scanned: Device,
        cached: Device?,
        currentSsid: String?,
        localIp: String?,
        localDeviceVendor: String
    ): Device {
        val merged = if (cached != null) {
            scanned.copy(
                customName = cached.customName ?: scanned.customName,
                isKnown = cached.isKnown,
                isSuspicious = cached.isSuspicious,
                firstSeen = cached.firstSeen,
                ssid = currentSsid ?: cached.ssid,
                hostname = if (scanned.hostname != scanned.ipAddress) scanned.hostname else cached.hostname,
                vendor = scanned.vendor ?: cached.vendor,
                mDnsName = scanned.mDnsName ?: cached.mDnsName,
                upnpName = scanned.upnpName ?: cached.upnpName,
                type = if (scanned.type != DeviceType.UNKNOWN) scanned.type else cached.type,
                macAddress = scanned.macAddress ?: cached.macAddress,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            scanned.copy(ssid = currentSsid)
        }

        return if (scanned.ipAddress == localIp) {
            merged.copy(
                customName = "This Device (Your Phone)",
                vendor = localDeviceVendor,
                type = DeviceType.PHONE,
                isKnown = true
            )
        } else {
            merged
        }
    }
}
