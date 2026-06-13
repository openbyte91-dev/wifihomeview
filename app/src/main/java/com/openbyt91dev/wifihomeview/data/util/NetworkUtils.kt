package com.openbyt91dev.wifihomeview.data.util

import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Gets the current device's IPv4 address and subnet prefix length.
     * Returns Pair<InetAddress, Short> (e.g., 192.168.1.5, 24)
     */
    fun getLocalIpAndPrefix(): Pair<InetAddress, Short>? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            
            // Prefer wlan0 or eth0 over other interfaces (like p2p or dummy)
            val preferredInterfaces = interfaces.filter { 
                it.name.contains("wlan") || it.name.contains("eth") 
            }.sortedBy { it.name }

            val allInterfaces = preferredInterfaces + interfaces.filter { 
                !it.name.contains("wlan") && !it.name.contains("eth") 
            }

            for (iface in allInterfaces) {
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.interfaceAddresses
                for (addr in addresses) {
                    val inetAddr = addr.address
                    if (inetAddr is java.net.Inet4Address) {
                        return Pair(inetAddr, addr.networkPrefixLength)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Generates a list of all scannable IPs in the subnet.
     * e.g., for 192.168.1.5/24, returns 192.168.1.1 ... 192.168.1.254
     */
    fun getSubnetIps(localIp: InetAddress, prefixLength: Short): List<String> {
        val ips = mutableListOf<String>()
        val addressBytes = localIp.address
        val addressInt =
            ((addressBytes[0].toInt() and 0xFF) shl 24) or
            ((addressBytes[1].toInt() and 0xFF) shl 16) or
            ((addressBytes[2].toInt() and 0xFF) shl 8) or
            (addressBytes[3].toInt() and 0xFF)

        val netmask = -1 shl (32 - prefixLength)
        val networkAddress = addressInt and netmask
        val broadcastAddress = networkAddress or (netmask.inv())

        // Iterate from Network + 1 to Broadcast - 1
        for (i in (networkAddress + 1) until broadcastAddress) {
            val ip = intToIp(i)
            // Skip our own IP? Optional. Usually good to include for completeness.
            ips.add(ip)
        }
        return ips
    }

    private fun intToIp(i: Int): String {
        return "${(i shr 24) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 8) and 0xFF}.${i and 0xFF}"
    }
}