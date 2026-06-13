package com.openbyt91dev.wifihomeview.data.util

import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader

object ArpHelper {
    fun getMacAddress(ip: String): String? {
        val mappedFromArp = getMacAddressFromArp(ip)
        if (mappedFromArp != null) {
            return mappedFromArp
        }
        return getMacAddressFromIpNeigh(ip)
    }

    private fun getMacAddressFromArp(ip: String): String? {
        val arpFile = java.io.File("/proc/net/arp")
        if (!arpFile.exists() || !arpFile.canRead()) {
            return null
        }
        try {
            val br = BufferedReader(FileReader(arpFile))
            var line: String? = br.readLine() // skip header
            while (line != null) {
                line = br.readLine()
                if (line == null) break
                val splitted = line.split(" +".toRegex())
                if (splitted.size >= 4 && ip == splitted[0]) {
                    val mac = splitted[3]
                    if (mac.matches("..:..:..:..:..:..".toRegex()) && mac != "00:00:00:00:00:00") {
                        br.close()
                        return mac.uppercase()
                    }
                }
            }
            br.close()
        } catch (_: Exception) {
            // Silently fail as this is restricted on modern Android
        }
        return null
    }

    private fun getMacAddressFromIpNeigh(ip: String): String? {
        try {
            val process = Runtime.getRuntime().exec("ip neigh show $ip")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.waitFor()

            if (line != null) {
                // Example output: 192.168.1.10 dev wlan0 lladdr 00:11:22:33:44:55 STALE
                // Or: 192.168.1.10 dev wlan0  FAILED
                val parts = line.split("\\s+".toRegex())
                val lladdrIndex = parts.indexOf("lladdr")
                if (lladdrIndex != -1 && lladdrIndex + 1 < parts.size) {
                    val mac = parts[lladdrIndex + 1]
                    if (mac.matches("..:..:..:..:..:..".toRegex()) && mac != "00:00:00:00:00:00") {
                        return mac.uppercase()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
