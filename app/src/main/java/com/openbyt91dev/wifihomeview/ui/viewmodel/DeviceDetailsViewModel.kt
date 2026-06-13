package com.openbyt91dev.wifihomeview.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.repository.ScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScannerRepository
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])

    val device: StateFlow<Device?> = repository.getDevice(deviceId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _pingLatency = MutableStateFlow<Long?>(null)
    val pingLatency = _pingLatency.asStateFlow()

    fun ping() {
        val ip = device.value?.ipAddress ?: return
        viewModelScope.launch {
            _pingLatency.value = repository.pingDevice(ip)
        }
    }
}
