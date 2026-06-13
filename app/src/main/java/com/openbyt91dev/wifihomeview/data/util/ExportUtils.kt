package com.openbyt91dev.wifihomeview.data.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.openbyt91dev.wifihomeview.domain.model.Device
import java.io.File
import java.io.FileWriter

object ExportUtils {
    fun exportDevicesToCsv(context: Context, devices: List<Device>): Uri? {
        return try {
            val fileName = "network_devices_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("IP Address,MAC Address,Hostname,Vendor,Device Name,Device Type,Is Known\n")

            for (device in devices) {
                val ip = device.ipAddress
                val mac = device.macAddress ?: "Unknown"
                val hostname = device.hostname ?: "Unknown"
                val vendor = device.vendor?.replace(",", " ") ?: "Unknown"
                val name = device.getDisplayName().replace(",", " ")
                val type = device.type.name
                val isKnown = if (device.isKnown) "Yes" else "No"
                
                writer.append("$ip,$mac,$hostname,$vendor,$name,$type,$isKnown\n")
            }

            writer.flush()
            writer.close()

            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportDevicesToJson(context: Context, devices: List<Device>): Uri? {
        return try {
            val fileName = "network_devices_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            val sb = StringBuilder()
            sb.appendLine("[")
            devices.forEachIndexed { index, device ->
                sb.appendLine("  {")
                sb.appendLine("    \"ipAddress\": \"${device.ipAddress}\",")
                sb.appendLine("    \"macAddress\": \"${device.macAddress ?: "null"}\",")
                sb.appendLine("    \"displayName\": \"${device.getDisplayName().replace("\"", "\\\"")}\",")
                sb.appendLine("    \"hostname\": \"${device.hostname ?: "null"}\",")
                sb.appendLine("    \"vendor\": \"${device.vendor ?: "null"}\",")
                sb.appendLine("    \"type\": \"${device.type.name}\",")
                sb.appendLine("    \"isKnown\": ${device.isKnown},")
                sb.appendLine("    \"isOnline\": ${device.isOnline},")
                sb.appendLine("    \"openPorts\": [${device.openPorts.joinToString(", ")}],")
                sb.appendLine("    \"mDnsName\": \"${device.mDnsName ?: "null"}\",")
                sb.appendLine("    \"upnpName\": \"${device.upnpName ?: "null"}\",")
                sb.appendLine("    \"ssid\": \"${device.ssid ?: "null"}\",")
                sb.appendLine("    \"firstSeen\": ${device.firstSeen},")
                sb.appendLine("    \"lastSeen\": ${device.lastSeen}")
                sb.append("  }")
                if (index < devices.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("]")

            writer.write(sb.toString())
            writer.flush()
            writer.close()

            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
