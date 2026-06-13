package com.openbyt91dev.wifihomeview.data.fingerprint

import com.openbyt91dev.wifihomeview.domain.model.Device
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

class SsdpDiscovery @Inject constructor() {

    private val SSDP_IP = "239.255.255.250"
    private val SSDP_PORT = 1900
    private val DISCOVER_MESSAGE = "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: $SSDP_IP:$SSDP_PORT\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 1\r\n" +
            "ST: ssdp:all\r\n" +
            "\r\n"

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 2000
        }
    }

    suspend fun sendSsdpSearch(): List<Device> = withContext(Dispatchers.IO) {
        val discoveredDevices = mutableListOf<Device>()
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()
            socket.soTimeout = 2000

            val address = InetAddress.getByName(SSDP_IP)
            val packet = DatagramPacket(DISCOVER_MESSAGE.toByteArray(), DISCOVER_MESSAGE.length, address, SSDP_PORT)
            
            socket.send(packet)

            val buffer = ByteArray(2048)
            val receivePacket = DatagramPacket(buffer, buffer.size)

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 3000) {
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val senderIp = receivePacket.address.hostAddress

                    val headers = parseHeaders(response)
                    val location = headers["LOCATION"]
                    
                    var upnpName = headers["SERVER"] ?: headers["USN"]
                    var vendor = "Generic UPnP"
                    var model: String? = null

                    // If LOCATION is available, try to fetch deeper info
                    if (location != null) {
                        val details = fetchSsdpDetails(location)
                        if (details != null) {
                            upnpName = details.friendlyName ?: upnpName
                            vendor = details.manufacturer ?: vendor
                            model = details.modelName
                        }
                    }

                    discoveredDevices.add(
                        Device(
                            ipAddress = senderIp,
                            upnpName = upnpName,
                            vendor = vendor,
                            hostname = model, // Using model as a hostname hint if empty
                            isOnline = true
                        )
                    )
                } catch (e: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }

        discoveredDevices
    }

    private suspend fun fetchSsdpDetails(url: String): SsdpDetails? {
        return try {
            val response = httpClient.get(url)
            val xml = response.bodyAsText()
            
            val friendlyName = extractTag(xml, "friendlyName")
            val manufacturer = extractTag(xml, "manufacturer")
            val modelName = extractTag(xml, "modelName")
            
            if (friendlyName != null || manufacturer != null || modelName != null) {
                SsdpDetails(friendlyName, manufacturer, modelName)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTag(xml: String, tag: String): String? {
        val regex = "<$tag>(.*?)</$tag>".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun parseHeaders(response: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val lines = response.split("\r\n")
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                headers[parts[0].trim().uppercase()] = parts[1].trim()
            }
        }
        return headers
    }

    private data class SsdpDetails(
        val friendlyName: String?,
        val manufacturer: String?,
        val modelName: String?
    )
}
