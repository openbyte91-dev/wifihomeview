package com.openbyt91dev.wifihomeview.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.openbyt91dev.wifihomeview.R
import com.openbyt91dev.wifihomeview.data.local.dao.DeviceDao
import com.openbyt91dev.wifihomeview.data.settings.SettingsRepository
import com.openbyt91dev.wifihomeview.domain.repository.ScannerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ScannerRepository,
    private val deviceDao: DeviceDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!settingsRepository.settings.firstOrNull()?.backgroundAlertsEnabled.orDefaultTrue()) {
                return Result.success()
            }

            // Perform Scan (permission check moved to repository - ARP lookup is optional)
            repository.performScan()

            // Check for New (Unknown) Devices
            val newDevices = deviceDao.getNewDevices().firstOrNull() ?: emptyList()
            
            if (newDevices.isNotEmpty()) {
                val recentNewDevices = newDevices.filter { 
                    // Only notify for devices seen for the first time within the last 30 minutes
                    System.currentTimeMillis() - it.firstSeen < TimeUnit.MINUTES.toMillis(30)
                }

                if (recentNewDevices.isNotEmpty()) {
                    val count = recentNewDevices.size
                    val title = "New Device${if (count > 1) "s" else ""} Detected!"
                    val message = if (count == 1) {
                        val device = recentNewDevices.first()
                        "${device.customName ?: device.hostname ?: device.ipAddress} joined the network."
                    } else {
                        "$count new devices joined the network."
                    }
                    
                    showNotification(title, message)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "network_scan_alerts"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Device Alerts"
            val descriptionText = "Notifications when new devices join the network"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this resource exists or use default
            .setContentTitle(title as CharSequence)
            .setContentText(message as CharSequence)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
        }
    }

    private fun Boolean?.orDefaultTrue(): Boolean = this ?: true
}
