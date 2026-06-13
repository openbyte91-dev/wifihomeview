package com.openbyt91dev.wifihomeview.data.repository

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class OuiRepository private constructor(
    private val openDatabase: () -> InputStream
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({
        context.assets.open("oui.csv")
    })

    internal constructor(csvContent: String) : this({
        csvContent.byteInputStream()
    })

    private val vendorMap = mutableMapOf<String, String>()
    private var isLoaded = false

    @Synchronized
    fun loadDatabase() {
        if (isLoaded) return
        try {
            BufferedReader(InputStreamReader(openDatabase())).use { reader ->
                reader.readLine()
                var line: String? = reader.readLine()
                while (line != null) {
                    parseCsvLine(line)?.let { (assignment, vendor) ->
                        vendorMap[assignment] = vendor
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Exception) {
            vendorMap.clear()
        } finally {
            isLoaded = true
        }
    }

    fun getVendor(macAddress: String): String? {
        if (!isLoaded) loadDatabase()
        
        val cleanedMac = macAddress.replace(":", "").replace("-", "").uppercase()
        if (cleanedMac.length >= 6) {
            val prefix = cleanedMac.substring(0, 6)
            return vendorMap[prefix]
        }
        return null
    }

    private fun parseCsvLine(line: String): Pair<String, String>? {
        val commaIndex = line.indexOf(',')
        if (commaIndex <= 0) return null

        val assignment = line.substring(0, commaIndex)
            .trim()
            .uppercase()
            .filter { it.isLetterOrDigit() }

        if (assignment.length != 6) return null

        val vendor = line.substring(commaIndex + 1)
            .trim()
            .removeSurrounding("\"")
            .replace("\"\"", "\"")
            .trim()

        if (vendor.isBlank()) return null
        return assignment to vendor
    }
}
