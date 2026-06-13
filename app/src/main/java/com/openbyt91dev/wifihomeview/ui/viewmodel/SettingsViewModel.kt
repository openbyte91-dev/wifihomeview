package com.openbyt91dev.wifihomeview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbyt91dev.wifihomeview.data.settings.AppSettings
import com.openbyt91dev.wifihomeview.data.settings.ScanIntensity
import com.openbyt91dev.wifihomeview.data.settings.SettingsRepository
import com.openbyt91dev.wifihomeview.worker.BackgroundScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backgroundScanScheduler: BackgroundScanScheduler
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setBackgroundAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBackgroundAlertsEnabled(enabled)
            backgroundScanScheduler.applyBackgroundAlerts(enabled)
        }
    }

    fun setScanIntensity(intensity: ScanIntensity) {
        viewModelScope.launch {
            settingsRepository.setScanIntensity(intensity)
        }
    }
}
