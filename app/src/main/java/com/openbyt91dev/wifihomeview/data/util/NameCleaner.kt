package com.openbyt91dev.wifihomeview.data.util

object NameCleaner {
    
    private val technicalPatterns = listOf(
        "Linux/\\d+(\\.\\d+)*",
        "UPnP/\\d+(\\.\\d+)*",
        "Android/\\d+(\\.\\d+)*",
        "Chromecast/\\d+(\\.\\d+)*",
        "Portable SDK for UPnP devices/.*",
        "Microsoft-HTTPAPI/\\d+(\\.\\d+)*",
        "tplink-smartbulb",
        "tplink-smartplug"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    fun clean(name: String?): String? {
        if (name.isNullOrBlank()) return null
        
        // Check model/vendor hints before removing protocol fragments like Chromecast/1.56.
        if (name.contains("Chromecast", ignoreCase = true)) return "Chromecast"
        if (name.contains("Apple TV", ignoreCase = true)) return "Apple TV"
        if (name.contains("iPhone", ignoreCase = true)) return "iPhone"
        if (name.contains("iPad", ignoreCase = true)) return "iPad"
        if (name.contains("Philips Hue", ignoreCase = true)) return "Philips Hue Bridge"
        if (name.contains("XStream", ignoreCase = true)) return "Airtel XStream Box"
        if (name.contains("AOT-", ignoreCase = true)) return "Airtel Router"
        if (name.contains(".airtel", ignoreCase = true)) return "Airtel Device"
        
        var cleaned = name
        
        // Remove technical version strings
        technicalPatterns.forEach { regex ->
            cleaned = cleaned?.replace(regex, "")?.trim()
        }

        // Clean up remaining commas and whitespace
        cleaned = cleaned?.replace(", ,", ",")
            ?.trim(',', ' ', '\n', '\r')

        if (cleaned.isNullOrBlank()) return null

        // Filter out generic names
        if (cleaned.matches(Regex("(?i)^(android|linux|ubuntu|windows|generic|unknown|device)$"))) {
            return null
        }

        return cleaned
    }
}
