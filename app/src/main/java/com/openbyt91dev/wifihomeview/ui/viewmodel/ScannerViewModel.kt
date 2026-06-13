package com.openbyt91dev.wifihomeview.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openbyt91dev.wifihomeview.domain.repository.ScannerRepository
import com.openbyt91dev.wifihomeview.domain.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.openbyt91dev.wifihomeview.data.util.ExportUtils
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.openbyt91dev.wifihomeview.data.connectivity.NetworkMonitor
import com.openbyt91dev.wifihomeview.data.connectivity.NetworkState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ScannerViewModel @Inject constructor(
    application: Application,
    private val repository: ScannerRepository,
    private val networkMonitor: NetworkMonitor
) : AndroidViewModel(application) {

    val networkState: StateFlow<NetworkState> = networkMonitor.networkState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkState()
        )

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showOnlyOnline = MutableStateFlow(false)
    val showOnlyOnline = _showOnlyOnline.asStateFlow()

    val filteredDevices = combine(
        _devices, _searchQuery, _showOnlyOnline, networkMonitor.networkState
    ) { devices, query, onlyOnline, network ->
        devices.filter { device ->
            val matchesNetwork = network.ssid == null || device.ssid == null || device.ssid == network.ssid
            val matchesQuery = query.isEmpty() ||
                    device.ipAddress.contains(query, ignoreCase = true) ||
                    (device.customName?.contains(query, ignoreCase = true) ?: false) ||
                    (device.hostname?.contains(query, ignoreCase = true) ?: false) ||
                    (device.vendor?.contains(query, ignoreCase = true) ?: false)
            val matchesOnline = !onlyOnline || device.isOnline
            matchesNetwork && matchesQuery && matchesOnline
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleOnlineFilter() {
        _showOnlyOnline.value = !_showOnlyOnline.value
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val scanProgress: StateFlow<Float> = repository.scanProgress

    private val _scanCompleted = MutableStateFlow(false)
    val scanCompleted: StateFlow<Boolean> = _scanCompleted.asStateFlow()

    private val _showNewDeviceDialog = MutableStateFlow(false)
    val showNewDeviceDialog: StateFlow<Boolean> = _showNewDeviceDialog.asStateFlow()

    private val _newDevicesToVerify = MutableStateFlow<List<Device>>(emptyList())
    val newDevicesToVerify: StateFlow<List<Device>> = _newDevicesToVerify.asStateFlow()

    private var scanJob: Job? = null
    private var collectJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return

        _isScanning.value = true
        _scanCompleted.value = false
        
        // Cancel previous jobs completely
        scanJob?.cancel()
        collectJob?.cancel()
        
        // Collect live device updates on main scope
        collectJob = viewModelScope.launch {
            try {
                repository.startScan().collect { scannedDevices ->
                    _devices.value = scannedDevices
                }
            } catch (_: kotlinx.coroutines.CancellationException) { }
        }
        
        // Run the scan on IO, wait for completion
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.performScan()
            } catch (_: kotlinx.coroutines.CancellationException) {
                return@launch
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Scan actually completed
            _isScanning.value = false
            _scanCompleted.value = true

            // Check for new devices
            val currentDevices = _devices.value
            val trulyNewDevices = currentDevices.filter { device ->
                !device.isKnown &&
                device.customName == null &&
                (device.mDnsName == null || device.mDnsName == device.ipAddress)
            }

            if (trulyNewDevices.isNotEmpty()) {
                _newDevicesToVerify.value = trulyNewDevices
                _showNewDeviceDialog.value = true
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        collectJob?.cancel()
        repository.stopScan()
        _isScanning.value = false
    }

    fun dismissNewDeviceDialog() {
        _showNewDeviceDialog.value = false
        _newDevicesToVerify.value = emptyList()
    }

    fun verifyDevice(device: Device) {
        viewModelScope.launch {
            val id = device.macAddress ?: device.ipAddress
            repository.markAsKnown(id)
            _newDevicesToVerify.value = _newDevicesToVerify.value.filter { it.id != device.id }
            if (_newDevicesToVerify.value.isEmpty()) {
                _showNewDeviceDialog.value = false
            }
        }
    }

    fun rejectDevice(device: Device) {
        _newDevicesToVerify.value = _newDevicesToVerify.value.filter { it.id != device.id }
        if (_newDevicesToVerify.value.isEmpty()) {
            _showNewDeviceDialog.value = false
        }
    }

    fun markAllAsKnown() {
        viewModelScope.launch {
            _newDevicesToVerify.value.forEach { device ->
                val id = device.macAddress ?: device.ipAddress
                repository.markAsKnown(id)
            }
            _newDevicesToVerify.value = emptyList()
            _showNewDeviceDialog.value = false
        }
    }

    fun saveDeviceName(device: Device, name: String) {
        viewModelScope.launch {
            val id = device.macAddress ?: device.ipAddress
            repository.saveCustomName(id, name)
        }
    }

    fun markDeviceAsKnown(device: Device) {
        viewModelScope.launch {
            val id = device.macAddress ?: device.ipAddress
            repository.markAsKnown(id)
        }
    }

    fun markAsSuspicious(device: Device, isSuspicious: Boolean) {
        viewModelScope.launch {
            val id = device.macAddress ?: device.ipAddress
            repository.markAsSuspicious(id, isSuspicious)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun exportDevices() {
        viewModelScope.launch {
            val uri = ExportUtils.exportDevicesToJson(getApplication(), _devices.value)
            shareFile(uri, "application/json")
        }
    }

    fun exportDevicesAsCsv() {
        viewModelScope.launch {
            val uri = ExportUtils.exportDevicesToCsv(getApplication(), _devices.value)
            shareFile(uri, "text/csv")
        }
    }

    private fun shareFile(uri: Uri?, mimeType: String) {
        if (uri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ContextCompat.startActivity(
                getApplication(),
                Intent.createChooser(intent, "Share Network Devices")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
