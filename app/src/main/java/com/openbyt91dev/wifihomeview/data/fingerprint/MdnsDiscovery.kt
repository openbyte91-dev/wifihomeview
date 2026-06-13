package com.openbyt91dev.wifihomeview.data.fingerprint

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class MdnsDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    /**
     * Service types that reveal device identity.
     * Priority order: types that give the most human-readable device names first.
     */
    val serviceTypes = listOf(
        "_googlecast._tcp",     // Chromecast / Google TV / Nest speakers
        "_airplay._tcp",        // Apple AirPlay (Apple TV, HomePod, Mac)
        "_hap._tcp",            // Apple HomeKit accessories
        "_raop._tcp",           // Apple Remote Audio (AirPlay speakers)
        "_http._tcp",           // Generic HTTP services (printers, cameras, IoT)
        "_printer._tcp",        // IPP printers
        "_ipp._tcp",            // Internet Printing Protocol
        "_smb._tcp",            // Windows/Samba file sharing
        "_ssh._tcp",            // SSH services
        "_ftp._tcp",            // FTP services
        "_daap._tcp",           // iTunes music sharing
        "_spotify-connect._tcp",// Spotify Connect (smart speakers)
        "_sonos._tcp",          // Sonos speakers
        "_amzn-wplay._tcp",     // Amazon Alexa / Echo
        "_miio._udp",           // Xiaomi IoT devices
        "_elg._tcp",            // Elgato (Stream Deck, smart home)
        "_services._dns-sd._udp" // Fallback: list all available services
    )

    fun discoverServices(serviceType: String = "_services._dns-sd._udp"): Flow<NsdServiceInfo> = callbackFlow {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                try {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            trySend(resolvedService)
                        }
                    })
                } catch (_: Exception) { }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            nsdManager.stopServiceDiscovery(listener)
        }
    }
}