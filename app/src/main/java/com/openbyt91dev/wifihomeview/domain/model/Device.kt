package com.openbyt91dev.wifihomeview.domain.model

import com.openbyt91dev.wifihomeview.data.util.NameCleaner

enum class DeviceType {
    UNKNOWN,
    ROUTER,
    PHONE,
    PC,
    IOT, // Smart Plugs, Bulbs
    MEDIA, // TV, Speaker, Chromecast
    PRINTER,
    GAME_CONSOLE,
    CAMERA
}

/**
 * Represents a device discovered on the network.
 *
 * @property ipAddress The current IPv4 address (e.g., "192.168.1.5").
 * @property macAddress The hardware address if available (e.g., "00:11:22:33:44:55").
 *                      Note: Often unavailable on Android 10+ without root or specific APIs.
 * @property hostname The DNS hostname (e.g., "android-123456789.local").
 * @property vendor The manufacturer name derived from MAC OUI or mDNS records (e.g., "Apple", "Espressif").
 * @property mDnsName The friendly name advertised via mDNS/Bonjour (e.g., "Living Room TV").
 * @property upnpName The friendly name advertised via SSDP/UPnP (e.g., "Samsung Smart TV").
 * @property netBiosName The NetBIOS name for Windows/Samba devices.
 * @property customName A user-defined name for this device.
 * @property type The inferred device category.
 * @property isOnline True if the device is currently responding.
 * @property lastSeen Timestamp of last successful ping/discovery.
 */
data class Device(
    val ipAddress: String,
    val macAddress: String? = null,
    val hostname: String? = null,
    val vendor: String? = null,
    val mDnsName: String? = null,
    val upnpName: String? = null,
    val netBiosName: String? = null,
    val customName: String? = null,
    val type: DeviceType = DeviceType.UNKNOWN,
    val isOnline: Boolean = true,
    val isKnown: Boolean = false,
    val isSuspicious: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val ssid: String? = null,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
) {
    /**
     * returns the best available name for display.
     * Priority: Custom > cleaned mDNS > cleaned UPnP > NetBIOS > Hostname > Vendor > IP
     */
    fun getDisplayName(): String {
        val cleanedMDns = NameCleaner.clean(mDnsName)
        val cleanedUpnp = NameCleaner.clean(upnpName)
        
        val name = customName
            ?: cleanedMDns
            ?: cleanedUpnp
            ?: if (hostname != null && !hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+".toRegex()) && !hostname.contains(".airtel")) {
                hostname
            } else {
                null
            }
            ?: vendor?.let { 
                if (it.contains("Unknown", ignoreCase = true) || it.contains("Generic", ignoreCase = true)) null 
                else it 
            }
            ?: "Unknown Device"
            
        // Final polish for known technical models
        return when {
            name.contains("AOT-", true) -> "Airtel Router ($name)"
            name.contains("XStream", true) -> "Airtel XStream Box"
            else -> name
        }
    }

    /**
     * unique identifier for DiffUtil / Lists.
     * Prefers MAC, falls back to IP (though IP can change).
     */
    val id: String
        get() = macAddress ?: ipAddress
}
