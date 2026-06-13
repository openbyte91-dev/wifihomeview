package com.openbyt91dev.wifihomeview.data.fingerprint

import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.model.DeviceType
import com.openbyt91dev.wifihomeview.data.settings.ScanIntensity
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class Fingerprinter @Inject constructor() {

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 2000
        }
    }

    suspend fun enrichDevice(device: Device, scanIntensity: ScanIntensity = ScanIntensity.DEEP): Device {
        return withContext(Dispatchers.IO) {
            var updatedDevice = device

            // 1. Hostname (Reverse DNS)
            try {
                val addr = InetAddress.getByName(device.ipAddress)
                val hostname = addr.canonicalHostName
                if (hostname != device.ipAddress) {
                    updatedDevice = updatedDevice.copy(hostname = hostname)
                }
            } catch (_: Exception) { }

            // 2. Port scan (focused list for classification)
            val commonPorts = when (scanIntensity) {
                ScanIntensity.QUICK -> listOf(22, 80, 443, 445, 5353, 8008, 8080)
                ScanIntensity.DEEP -> listOf(
                    22, 53, 80, 443, 445, 515, 631, 1883, 3389,
                    5353, 5555, 8000, 8008, 8009, 8080, 8443, 8888, 9100, 62078
                )
            }
            val openPorts = commonPorts.map { port ->
                async { if (isPortOpen(device.ipAddress, port)) port else null }
            }.awaitAll().filterNotNull()

            if (openPorts.isNotEmpty()) {
                updatedDevice = updatedDevice.copy(openPorts = openPorts)

                // --- CATEGORIZATION ENGINE ---

                // 1. GATEWAY / ROUTER: DNS (53) + web admin (80/443) + common gateway IP
                val hasDns = openPorts.contains(53)
                val hasWebAdmin = openPorts.contains(80) || openPorts.contains(443)
                val isGatewayIp = device.ipAddress.endsWith(".1") || device.ipAddress.endsWith(".254")
                if (isGatewayIp || (hasDns && hasWebAdmin)) {
                    if (updatedDevice.type == DeviceType.UNKNOWN) {
                        updatedDevice = updatedDevice.copy(type = DeviceType.ROUTER, vendor = updatedDevice.vendor ?: "Gateway")
                    }
                }

                // 2. MEDIA / CAST: Chromecast, AirPlay, DLNA
                val isCast = openPorts.contains(8008) || openPorts.contains(8009)
                if (isCast) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.MEDIA, vendor = updatedDevice.vendor ?: "Google Cast")
                    // Probe Chromecast eureka_info for friendly name
                    val castName = getCastDeviceName(device.ipAddress)
                    if (castName != null && updatedDevice.customName == null) {
                        updatedDevice = updatedDevice.copy(customName = castName)
                    }
                }

                // 3. PRINTERS
                if (updatedDevice.type == DeviceType.UNKNOWN &&
                    (openPorts.contains(9100) || openPorts.contains(515) || openPorts.contains(631))) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.PRINTER)
                }

                // 4. PC / LAPTOP: Windows RPC/SMB/RDP or SSH
                if (updatedDevice.type == DeviceType.UNKNOWN &&
                    (openPorts.contains(445) || openPorts.contains(3389))) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.PC, vendor = updatedDevice.vendor ?: "Windows PC")
                }

                // 5. PHONES: ADB or Apple services
                if (updatedDevice.type == DeviceType.UNKNOWN && openPorts.contains(5555)) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.PHONE, vendor = "Android (ADB)")
                }
                if (updatedDevice.type == DeviceType.UNKNOWN && openPorts.contains(62078)) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.PHONE, vendor = "Apple iPhone")
                }

                // 6. IoT: MQTT, or SSH on non-standard devices
                if (updatedDevice.type == DeviceType.UNKNOWN && openPorts.contains(1883)) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.IOT, vendor = "MQTT Device")
                }
                if (updatedDevice.type == DeviceType.UNKNOWN && openPorts.contains(22)) {
                    updatedDevice = updatedDevice.copy(type = DeviceType.PC)
                }

                // 7. Web-based: try HTTP title for devices with port 80/8080/8000
                if (updatedDevice.type == DeviceType.UNKNOWN || updatedDevice.customName == null) {
                    val httpPort = if (openPorts.contains(80)) 80
                        else if (openPorts.contains(8080)) 8080
                        else if (openPorts.contains(8000)) 8000
                        else null
                    if (httpPort != null) {
                        val title = getHttpTitle(device.ipAddress, httpPort)
                        if (title != null && updatedDevice.customName == null) {
                            updatedDevice = updatedDevice.copy(customName = title)
                        }
                    }
                }
            }

            updatedDevice
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 350): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun getCastDeviceName(ip: String): String? {
        return try {
            val response = httpClient.get("http://$ip:8008/setup/eureka_info")
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                nameRegex.find(body)?.groupValues?.get(1)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getHttpTitle(ip: String, port: Int = 80): String? {
        return try {
            val response = httpClient.get("http://$ip:$port")
            if (response.status == HttpStatusCode.OK) {
                val html = response.bodyAsText()
                val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
                titleRegex.find(html)?.groupValues?.get(1)?.trim()
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
