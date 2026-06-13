package com.openbyt91dev.wifihomeview.domain.repository

import com.openbyt91dev.wifihomeview.domain.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for the network scanning logic.
 *
 * This abstraction allows us to:
 * 1. Mock the repository in UI tests (Compose Previews).
 * 2. Swap out the real implementation (e.g., Use a fake scanner in CI).
 * 3. Use Dependency Injection (Hilt/Koin) effectively.
 *
 * @see com.openbyt91dev.wifihomeview.data.repository.ScannerRepositoryImpl
 */
interface ScannerRepository {
    
    /**
     * Starts a full network scan.
     * 
     * @return A Flow emitting the list of currently discovered devices.
     *         The Flow updates as new devices are found or data is refined.
     */
    fun startScan(): Flow<List<Device>>
    suspend fun performScan()

    /**
     * Progress of the current scan (0f to 1f), or -1f if not scanning.
     */
    val scanProgress: StateFlow<Float>
    
    /**
     * Stops the current scan immediately.
     */
    fun stopScan()
    
    /**
     * Gets the device's own IP address and subnet info.
     *
     * @return Pair<IP, SubnetMask> (e.g., "192.168.1.5", "255.255.255.0")
     */
    suspend fun getLocalIpInfo(): Pair<String, String>?
    
    /**
     * Saves a user-defined name for a device.
     */
    suspend fun saveCustomName(deviceMac: String, name: String)

    /**
     * Marks a device as "Known" / trusted.
     */
    suspend fun markAsKnown(deviceMac: String)

    suspend fun markAsSuspicious(deviceMac: String, isSuspicious: Boolean)

    fun getDevice(id: String): Flow<Device?>

    suspend fun clearHistory()

    suspend fun pingDevice(ip: String): Long? // Returns latency in ms or null
}
