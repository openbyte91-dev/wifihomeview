package com.openbyt91dev.wifihomeview.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openbyt91dev.wifihomeview.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE macAddress = :mac")
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE ipAddress = :ip")
    suspend fun getDeviceByIp(ip: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE isKnown = 0")
    fun getNewDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: DeviceEntity)

    @Query("UPDATE devices SET customName = :name WHERE macAddress = :id OR ipAddress = :id")
    suspend fun updateCustomName(id: String, name: String)

    @Query("UPDATE devices SET isKnown = 1 WHERE macAddress = :id OR ipAddress = :id")
    suspend fun markAsKnown(id: String)

    @Query("UPDATE devices SET isSuspicious = :isSuspicious WHERE macAddress = :id OR ipAddress = :id")
    suspend fun setSuspicious(id: String, isSuspicious: Boolean)

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
