package com.openbyt91dev.wifihomeview.data.repository

import com.openbyt91dev.wifihomeview.data.fingerprint.MdnsDiscovery
import com.openbyt91dev.wifihomeview.data.local.entity.toEntity
import com.openbyt91dev.wifihomeview.data.local.entity.toDomain
import com.openbyt91dev.wifihomeview.data.settings.ScanIntensity
import com.openbyt91dev.wifihomeview.data.settings.SettingsRepository
import com.openbyt91dev.wifihomeview.data.util.NetworkUtils
import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.repository.ScannerRepository
import com.openbyt91dev.wifihomeview.data.util.ArpHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.openbyt91dev.wifihomeview.data.local.dao.DeviceDao
import com.openbyt91dev.wifihomeview.data.fingerprint.Fingerprinter
import com.openbyt91dev.wifihomeview.data.fingerprint.SsdpDiscovery

class ScannerRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val fingerprinter: Fingerprinter,
    private val ssdpDiscovery: SsdpDiscovery,
    private val mdnsDiscovery: MdnsDiscovery,
    private val networkMonitor: com.openbyt91dev.wifihomeview.data.connectivity.NetworkMonitor,
    private val ouiRepository: OuiRepository,
    private val settingsRepository: SettingsRepository
) : ScannerRepository {

    private val _scanResults = MutableStateFlow<List<Device>>(emptyList())
    
    private val _scanProgress = MutableStateFlow(-1f)
    override val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()
    @Volatile private var scanGeneration = 0
    
    private val deviceCache = ConcurrentHashMap<String, Device>()

    override fun startScan(): Flow<List<Device>> {
        _scanResults.value = emptyList()
        return _scanResults.asStateFlow()
    }

    private suspend fun saveOrUpdateDevice(device: Device, localIpOverride: String? = null) {
        val ip = device.ipAddress
        val currentSsid = networkMonitor.networkState.first().ssid
        val localIp = localIpOverride ?: NetworkUtils.getLocalIpAndPrefix()?.first?.hostAddress
        
        // 1. Check Cache first
        var cached = deviceCache[ip]
            ?: device.macAddress?.let { mac ->
                deviceCache.values.find { it.macAddress == mac }
            }
        
        // 2. If not in cache, check DB
        if (cached == null) {
            val dbEntity = device.macAddress?.let { deviceDao.getDeviceByMac(it) }
                ?: deviceDao.getDeviceByIp(ip)
            if (dbEntity != null) {
                cached = dbEntity.toDomain()
            }
        }
        
        val merged = DeviceMergePolicy.merge(
            scanned = device,
            cached = cached,
            currentSsid = currentSsid,
            localIp = localIp,
            localDeviceVendor = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
        )
        
        // 4. Update Cache
        deviceCache[ip] = merged
        
        // 5. Persist
        deviceDao.insertOrUpdate(merged.toEntity())
        
        _scanResults.emit(deviceCache.values.sortedBy { it.ipAddress })
    }

    override suspend fun performScan() = withContext(Dispatchers.IO) {
        val localInfo = NetworkUtils.getLocalIpAndPrefix() ?: return@withContext
        val (localIp, prefix) = localInfo
        val localIpStr = localIp.hostAddress ?: return@withContext
        val scanIntensity = settingsRepository.settings.first().scanIntensity

        val myGeneration = ++scanGeneration
        setProgress(0f, myGeneration)
        deviceCache.clear()

        // Add this device (phone) to the list BEFORE scanning
        saveOrUpdateDevice(
            Device(
                ipAddress = localIpStr,
                hostname = localIp.canonicalHostName,
                isOnline = true
            ),
            localIpStr
        )

        val mdnsJobs = if (scanIntensity == ScanIntensity.DEEP) {
            mdnsDiscovery.serviceTypes.map { serviceType ->
                launch {
                    try {
                        mdnsDiscovery.discoverServices(serviceType).collect { service ->
                            if (!isActive) return@collect
                            val ip = service.host?.hostAddress
                            if (ip != null) {
                                val device = Device(ipAddress = ip, mDnsName = service.serviceName, isOnline = true)
                                saveOrUpdateDevice(device, localIpStr)
                            }
                        }
                    } catch (_: kotlinx.coroutines.CancellationException) {
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            emptyList()
        }

        val ssdpJob = if (scanIntensity == ScanIntensity.DEEP) {
            launch {
                try {
                    val ssdpDevices = ssdpDiscovery.sendSsdpSearch()
                    ssdpDevices.forEach { device ->
                        if (!isActive) return@forEach
                        saveOrUpdateDevice(device, localIpStr)
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            null
        }

        // Standard IP Scan
        val targetIps = NetworkUtils.getSubnetIps(localIp, prefix)
        val totalIps = targetIps.size
        if (totalIps == 0) {
            ssdpJob?.join()
            mdnsJobs.forEach { it.join() }
            setProgress(1f, myGeneration)
            return@withContext
        }
        var scannedCount = 0

        if (scanIntensity == ScanIntensity.DEEP) {
            warmArpCache(targetIps)
            delay(300)
            setProgress(0.05f, myGeneration)
        }

        val chunkSize = if (scanIntensity == ScanIntensity.QUICK) 80 else 50
        targetIps.chunked(chunkSize).forEach { chunk ->
            if (!isActive) return@withContext

            val deferredResults = chunk.map { ip ->
                async {
                    val basicDevice = pingIp(ip, scanIntensity)
                    if (basicDevice != null) {
                        var enriched = fingerprinter.enrichDevice(basicDevice, scanIntensity)
                        val mac = try {
                            ArpHelper.getMacAddress(ip)
                        } catch (e: SecurityException) {
                            null
                        }
                        if (mac != null && enriched.macAddress == null) {
                            val vendor = ouiRepository.getVendor(mac)
                            enriched = enriched.copy(macAddress = mac, vendor = vendor ?: enriched.vendor)
                            enriched = applyOuiTypeHints(enriched, mac, vendor)
                        }
                        if (mac != null && isRandomizedMac(mac)) {
                            enriched = enriched.copy(vendor = enriched.vendor ?: "Randomized MAC (Privacy)")
                        }
                        enriched
                    } else {
                        // TCP failed but device might be alive (firewalled).
                        val mac = try {
                            ArpHelper.getMacAddress(ip)
                        } catch (e: SecurityException) {
                            null
                        }
                        if (mac != null) {
                            val vendor = ouiRepository.getVendor(mac)
                            var device = Device(ipAddress = ip, macAddress = mac, vendor = vendor, isOnline = true)
                            device = applyOuiTypeHints(device, mac, vendor)
                            if (isRandomizedMac(mac)) {
                                device = device.copy(vendor = vendor ?: "Randomized MAC (Privacy)")
                            }
                            device
                        } else {
                            null
                        }
                    }
                }
            }
            
            val results = deferredResults.awaitAll().filterNotNull()
            results.forEach { device ->
                saveOrUpdateDevice(device, localIpStr)
            }

            scannedCount += chunk.size
            val baseProgress = if (scanIntensity == ScanIntensity.DEEP) 0.05f else 0f
            val scanRange = 1f - baseProgress
            setProgress(baseProgress + (scanRange * scannedCount / totalIps.toFloat()), myGeneration)
        }
        
        ssdpJob?.join()
        mdnsJobs.forEach { it.join() }
        setProgress(1f, myGeneration)
    }

    override fun stopScan() {
        // Handled by cancellation
    }

    override suspend fun getLocalIpInfo(): Pair<String, String>? {
        val info = NetworkUtils.getLocalIpAndPrefix() ?: return null
        return Pair(info.first.hostAddress, info.second.toString())
    }

    override suspend fun saveCustomName(deviceMac: String, name: String) {
        deviceDao.updateCustomName(deviceMac, name)
        deviceDao.markAsKnown(deviceMac) // Implicitly mark as known
        
        // Update cache (mac or ip)
        deviceCache.values.find { it.macAddress == deviceMac || it.ipAddress == deviceMac }?.let {
            val updated = it.copy(customName = name, isKnown = true)
            deviceCache[it.ipAddress] = updated
            _scanResults.emit(deviceCache.values.sortedBy { it.ipAddress })
        }
    }

    override suspend fun markAsKnown(deviceMac: String) {
        deviceDao.markAsKnown(deviceMac)
        deviceDao.setSuspicious(deviceMac, false) // Mark as genuine
        
        deviceCache.values.find { it.macAddress == deviceMac || it.ipAddress == deviceMac }?.let {
            val updated = it.copy(isKnown = true, isSuspicious = false)
            deviceCache[it.ipAddress] = updated
            _scanResults.emit(deviceCache.values.sortedBy { it.ipAddress })
        }
    }

    override suspend fun markAsSuspicious(deviceMac: String, isSuspicious: Boolean) {
        deviceDao.setSuspicious(deviceMac, isSuspicious)
        if (isSuspicious) deviceDao.markAsKnown(deviceMac) // Acknowledge it even if suspicious
        
        deviceCache.values.find { it.macAddress == deviceMac || it.ipAddress == deviceMac }?.let {
            val updated = it.copy(
                isSuspicious = isSuspicious,
                isKnown = if (isSuspicious) true else it.isKnown
            )
            deviceCache[it.ipAddress] = updated
            _scanResults.emit(deviceCache.values.sortedBy { it.ipAddress })
        }
    }

    override suspend fun clearHistory() {
        deviceDao.deleteAll()
        deviceCache.clear()
        _scanResults.emit(emptyList())
    }

    override suspend fun pingDevice(ip: String): Long? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            if (isPortQuickCheck(ip, 80) || isPortQuickCheck(ip, 443) || isPortQuickCheck(ip, 22) || isPortQuickCheck(ip, 5353)) {
                return@withContext System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
            // ignore
        }
        null
    }

    override fun getDevice(id: String): Flow<Device?> {
        return kotlinx.coroutines.flow.flow {
            // First check cache
            val cached = deviceCache.values.find { it.macAddress == id || it.ipAddress == id }
            if (cached != null) {
                emit(cached)
            } else {
                // Check DB
                val entity = deviceDao.getDeviceByMac(id) ?: deviceDao.getDeviceByIp(id)
                emit(entity?.toDomain())
            }
        }
    }

    private suspend fun warmArpCache(targetIps: List<String>) {
        targetIps.chunked(100).forEach { chunk ->
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return
            kotlinx.coroutines.coroutineScope {
                chunk.map { ip ->
                    async {
                        try {
                            val socket = java.net.DatagramSocket()
                            val buffer = ByteArray(1)
                            val packet = java.net.DatagramPacket(buffer, buffer.size, InetAddress.getByName(ip), 137)
                            socket.send(packet)
                            socket.close()
                        } catch (_: Exception) {
                        }
                        try {
                            java.net.Socket().use { socket ->
                                socket.connect(java.net.InetSocketAddress(ip, 445), 50)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }.forEach { it.await() }
            }
        }
    }

    private fun pingIp(ip: String, scanIntensity: ScanIntensity): Device? {
        try {
            val address = InetAddress.getByName(ip)
            // TCP-based liveness check: ICMP is blocked on non-rooted Android
            // Probe common ports with reasonable Wi-Fi timeouts
            // Added 135, 137, 139 for Windows, 5353 for mDNS, 62078 for Apple
            val probePorts = when (scanIntensity) {
                ScanIntensity.QUICK -> listOf(80, 443, 445, 5353, 8008, 22, 8080)
                ScanIntensity.DEEP -> listOf(80, 443, 135, 137, 139, 445, 5353, 8008, 22, 8080, 62078)
            }
            for (port in probePorts) {
                if (isPortQuickCheck(ip, port, 300)) {
                    return Device(ipAddress = ip, hostname = address.canonicalHostName, isOnline = true)
                }
            }
            
            // Final attempt: isReachable (can work via TCP echo on port 7 if available)
            if (address.isReachable(500)) {
                return Device(ipAddress = ip, hostname = address.canonicalHostName, isOnline = true)
            }
        } catch (e: Exception) { }
        return null
    }

    private fun setProgress(value: Float, generation: Int) {
        if (generation == scanGeneration) {
            _scanProgress.value = value
        }
    }

    private fun isPortQuickCheck(ip: String, port: Int, timeoutMs: Int = 400): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isRandomizedMac(mac: String): Boolean {
        // Locally-administered MAC: second hex digit's bit 1 is set (2, 6, A, E)
        val firstByte = mac.substring(0, 2)
        val value = firstByte.toIntOrNull(16) ?: return false
        return (value and 0x02) != 0
    }

    private fun applyOuiTypeHints(device: com.openbyt91dev.wifihomeview.domain.model.Device, mac: String, vendor: String?): com.openbyt91dev.wifihomeview.domain.model.Device {
        val macPrefix = mac.take(8).uppercase()
        return when {
            macPrefix.startsWith("50:F2:65") || // Apple
            macPrefix.startsWith("F0:D1:A9") || // Apple
            macPrefix.startsWith("A4:B1:E9") || // Apple
            macPrefix.startsWith("A4:D1:D2") || // Apple
            macPrefix.startsWith("8C:85:90") || // Apple
            macPrefix.startsWith("AC:BC:32") ->  // Apple
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.PHONE, vendor = vendor ?: "Apple")
            macPrefix.startsWith("B8:27:EB") || // Raspberry Pi
            macPrefix.startsWith("DC:A6:32") || // Raspberry Pi
            macPrefix.startsWith("E4:5F:01") ->  // Raspberry Pi
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.IOT, vendor = vendor ?: "Raspberry Pi")
            macPrefix.startsWith("18:FE:34") || // Espressif
            macPrefix.startsWith("A0:20:A6") || // Espressif
            macPrefix.startsWith("24:0A:C4") || // Espressif
            macPrefix.startsWith("3C:71:BF") || // Espressif
            macPrefix.startsWith("FC:F5:C4") || // Espressif
            macPrefix.startsWith("EC:FA:BC") || // Espressif
            macPrefix.startsWith("C8:2B:96") || // Espressif
            macPrefix.startsWith("64:BB:1E") || // Earda (ODM for smart devices)
            macPrefix.startsWith("08:3A:F2") ->  // Wiz Connected / Philips
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.IOT, vendor = vendor ?: "Smart Home Device")
            macPrefix.startsWith("00:17:88") || // Philips Hue
            macPrefix.startsWith("00:17:F2") || // Apple (old)
            macPrefix.startsWith("00:1E:C2") || // Apple (old)
            macPrefix.startsWith("00:23:12") || // Apple (old)
            macPrefix.startsWith("00:25:00") || // Apple (old)
            macPrefix.startsWith("A4:D1:8C") || // Google
            macPrefix.startsWith("54:60:09") || // Google
            macPrefix.startsWith("38:8B:59") ->  // Google
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.MEDIA, vendor = vendor ?: "Google Cast")
            macPrefix.startsWith("B4:E6:2A") || // Amazon
            macPrefix.startsWith("F0:27:2D") || // Amazon
            macPrefix.startsWith("FC:A1:83") ->  // Amazon
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.IOT, vendor = vendor ?: "Amazon Alexa")
            macPrefix.startsWith("00:04:20") || // Netgear
            macPrefix.startsWith("78:BB:C1") || // SERVERCOM (Airtel)
            macPrefix.startsWith("C4:AD:34") || // TP-Link
            macPrefix.startsWith("14:CC:20") || // TP-Link
            macPrefix.startsWith("D8:47:32") || // D-Link
            macPrefix.startsWith("00:05:CD") ->  // TP-Link
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.ROUTER, vendor = vendor ?: "Router/Gateway")
            vendor?.contains("Samsung", ignoreCase = true) == true ->
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.PHONE, vendor = vendor)
            vendor?.contains("Xiaomi", ignoreCase = true) == true ||
            vendor?.contains("Huawei", ignoreCase = true) == true ||
            vendor?.contains("OnePlus", ignoreCase = true) == true ->
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.PHONE, vendor = vendor)
            vendor?.contains("Sony", ignoreCase = true) == true &&
            (vendor?.contains("PlayStation", ignoreCase = true) == true || vendor?.contains("PS", ignoreCase = true) == true) ->
                device.copy(type = com.openbyt91dev.wifihomeview.domain.model.DeviceType.GAME_CONSOLE, vendor = vendor)
            else -> device
        }
    }
}
