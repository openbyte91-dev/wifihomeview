package com.openbyt91dev.wifihomeview.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.model.DeviceType

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String, // Use MAC if available, or IP as fallback (with caution)
    val ipAddress: String,
    val hostname: String?,
    val vendor: String?,
    val mDnsName: String?,
    val upnpName: String?,
    val netBiosName: String?,
    val customName: String?,
    val type: DeviceType,
    val firstSeen: Long,
    val lastSeen: Long,
    val isKnown: Boolean = false, // User has acknowledged/trusted this device
    val isSuspicious: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val ssid: String? = null
)

fun DeviceEntity.toDomain(): Device {
    return Device(
        ipAddress = ipAddress,
        macAddress = if (macAddress == ipAddress) null else macAddress,
        hostname = hostname,
        vendor = vendor,
        mDnsName = mDnsName,
        upnpName = upnpName,
        netBiosName = netBiosName,
        customName = customName,
        type = type,
        isOnline = false,
        isKnown = isKnown,
        isSuspicious = isSuspicious,
        openPorts = openPorts,
        ssid = ssid,
        firstSeen = firstSeen,
        lastSeen = lastSeen
    )
}

fun Device.toEntity(): DeviceEntity {
    return DeviceEntity(
        macAddress = macAddress ?: ipAddress, // Fallback to IP if MAC is missing (common on Android 10+)
        ipAddress = ipAddress,
        hostname = hostname,
        vendor = vendor,
        mDnsName = mDnsName,
        upnpName = upnpName,
        netBiosName = netBiosName,
        customName = customName,
        type = type,
        firstSeen = firstSeen,
        lastSeen = lastSeen,
        isKnown = isKnown,
        isSuspicious = isSuspicious,
        openPorts = openPorts,
        ssid = ssid
    )
}
