package com.openbyt91dev.wifihomeview.data.settings

data class AppSettings(
    val backgroundAlertsEnabled: Boolean = true,
    val scanIntensity: ScanIntensity = ScanIntensity.DEEP
)

enum class ScanIntensity {
    QUICK,
    DEEP
}
