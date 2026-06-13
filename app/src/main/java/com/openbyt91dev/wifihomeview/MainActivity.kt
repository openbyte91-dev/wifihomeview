package com.openbyt91dev.wifihomeview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.openbyt91dev.wifihomeview.domain.model.Device
import com.openbyt91dev.wifihomeview.domain.model.DeviceType
import com.openbyt91dev.wifihomeview.ui.theme.WiFiHomeViewTheme
import com.openbyt91dev.wifihomeview.ui.viewmodel.ScannerViewModel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.navigation.navArgument
import androidx.compose.material3.Switch
import com.openbyt91dev.wifihomeview.data.settings.ScanIntensity
import com.openbyt91dev.wifihomeview.ui.viewmodel.SettingsViewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiFiHomeViewTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "scanner",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("scanner") {
                            ScannerScreen(
                                onDeviceClick = { device ->
                                    val id = device.macAddress ?: device.ipAddress
                                    navController.navigate("details/$id")
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "details/{deviceId}",
                            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                        ) {
                            DeviceDetailsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onDeviceClick: (Device) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val devices by viewModel.filteredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanCompleted by viewModel.scanCompleted.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyOnline by viewModel.showOnlyOnline.collectAsState()
    val showNewDeviceDialog by viewModel.showNewDeviceDialog.collectAsState()
    val newDevicesToVerify by viewModel.newDevicesToVerify.collectAsState()
    
    var editingDevice by remember { mutableStateOf<Device?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    var showRationale by remember { mutableStateOf(false) }

    // Export format dialog
    if (showExportMenu) {
        AlertDialog(
            onDismissRequest = { showExportMenu = false },
            title = { Text("Export Devices") },
            text = { Text("Choose export format:") },
            confirmButton = {
                TextButton(onClick = {
                    showExportMenu = false
                    viewModel.exportDevices()
                }) {
                    Text("JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportMenu = false
                    viewModel.exportDevicesAsCsv()
                }) {
                    Text("CSV")
                }
            }
        )
    }

    // New device verification dialog
    if (showNewDeviceDialog && newDevicesToVerify.isNotEmpty()) {
        NewDeviceVerificationDialog(
            devices = newDevicesToVerify,
            onVerify = { viewModel.verifyDevice(it) },
            onReject = { viewModel.rejectDevice(it) },
            onVerifyAll = { viewModel.markAllAsKnown() },
            onDismiss = { viewModel.dismissNewDeviceDialog() }
        )
    }

    // About dialog
    if (showAboutDialog) {
        val versionName = try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Text("About WiFiHomeView")
                }
            },
            text = {
                Column {
                    Text("Version: $versionName", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "WiFiHomeView is a local-only Wi-Fi scanner built to show you which devices are connected to your network. It does not contain ads, tracking, or cloud uploads.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Licensed under the MIT License.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Permission Required") },
            text = { Text("We need your location to identify the Wi-Fi network name. Without this, the scanner cannot function correctly.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            showRationale = true
        }
    }

    if (editingDevice != null) {
        EditDeviceDialog(
            device = editingDevice!!,
            onDismiss = { editingDevice = null },
            onSave = { name ->
                viewModel.saveDeviceName(editingDevice!!, name)
                editingDevice = null
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("WiFiHomeView", style = MaterialTheme.typography.titleMedium)
                    if (networkState.isWifiConnected) {
                        Text(
                            text = networkState.ssid ?: "Connected to Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "Not on Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                }
            },
            navigationIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                )
            },
            actions = {
                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "About App")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                }
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Red)
                }
                Button(
                    onClick = { viewModel.startScan() },
                    enabled = networkState.isWifiConnected && !isScanning
                ) {
                    Text(if (scanCompleted) "Scan Again" else "Scan")
                }
            }
        )

        // Progress bar during scan
        if (isScanning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (progress > 0f) progress else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (progress <= 0.05f) "Discovering services..."
                               else "Scanning IPs...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { viewModel.startScan() },
            modifier = Modifier.weight(1f)
        ) {
            Column {
                // Security Summary
                val knownCount = devices.count { it.isKnown && !it.isSuspicious }
                val unknownCount = devices.count { !it.isKnown }
                val suspiciousCount = devices.count { it.isSuspicious }

                if (devices.isNotEmpty()) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🛡️ $knownCount Genuine",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (unknownCount > 0) {
                                Text(
                                    text = "❓ $unknownCount Unknown",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (suspiciousCount > 0) {
                                Text(
                                    text = "⚠️ $suspiciousCount Suspicious",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Red
                                )
                            }
                        }
                    }
                }

                if (devices.isEmpty() && isScanning) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(3) {
                            ShimmerDeviceItem()
                        }
                    }
                } else if (devices.isEmpty() && !isScanning) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No devices found", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.startScan() }) {
                                Text("Scan Network")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(devices, key = { it.id }) { device ->
                            DeviceItem(
                                device = device,
                                onEdit = { editingDevice = device },
                                onMarkKnown = { viewModel.markDeviceAsKnown(device) },
                                onMarkSuspicious = { viewModel.markAsSuspicious(device, !device.isSuspicious) },
                                onClick = { onDeviceClick(device) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Background Intruder Alerts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scan periodically and notify when unknown devices appear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.backgroundAlertsEnabled,
                    onCheckedChange = viewModel::setBackgroundAlertsEnabled
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Scan Intensity", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.scanIntensity == ScanIntensity.QUICK,
                        onClick = { viewModel.setScanIntensity(ScanIntensity.QUICK) },
                        label = { Text("Quick") }
                    )
                    FilterChip(
                        selected = settings.scanIntensity == ScanIntensity.DEEP,
                        onClick = { viewModel.setScanIntensity(ScanIntensity.DEEP) },
                        label = { Text("Deep") }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: Device,
    onEdit: () -> Unit,
    onMarkKnown: () -> Unit,
    onMarkSuspicious: () -> Unit,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (device.isSuspicious) Color.Red.copy(alpha = 0.1f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isSuspicious) Color.Red.copy(alpha = 0.2f)
                        else if (device.isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Gray.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.isSuspicious) Icons.Default.Warning else getDeviceIcon(device.type),
                    contentDescription = null,
                    tint = if (device.isSuspicious) Color.Red else if (device.isOnline) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = device.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!device.isKnown) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Red, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text(
                    text = "${device.ipAddress} • ${device.vendor ?: "Unknown Vendor"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.isSuspicious) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMarkSuspicious,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Mark as Suspicious",
                        tint = if (device.isSuspicious) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!device.isKnown || device.isSuspicious) {
                    IconButton(
                        onClick = onMarkKnown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark as Known",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditDeviceDialog(
    device: Device,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(device.customName ?: device.getDisplayName()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Name") },
        text = {
            Column {
                Text("Assign a friendly name to this device:")
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewDeviceVerificationDialog(
    devices: List<Device>,
    onVerify: (Device) -> Unit,
    onReject: (Device) -> Unit,
    onVerifyAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (devices.size == 1) "New Device Detected"
                else "${devices.size} New Devices Detected"
            )
        },
        text = {
            Column {
                Text(
                    if (devices.size == 1)
                        "A new device has joined your network:"
                    else
                        "The following new devices joined your network:"
                )
                Spacer(modifier = Modifier.height(12.dp))
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "${device.ipAddress}  ${device.vendor?.take(20) ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        if (devices.size > 1) {
                            IconButton(onClick = { onVerify(device) }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Mark as known",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (devices.size == 1) {
                TextButton(onClick = { onVerify(devices.first()) }) {
                    Text("Yes, it's mine")
                }
            } else {
                TextButton(onClick = { onVerifyAll() }) {
                    Text("All Are Mine")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (devices.size == 1) "Not Sure" else "Dismiss")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    onBack: () -> Unit,
    viewModel: com.openbyt91dev.wifihomeview.ui.viewmodel.DeviceDetailsViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsState()
    val pingLatency by viewModel.pingLatency.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (device != null) {
            val d = device!!
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Header Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (d.isSuspicious) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (d.isSuspicious) Icons.Default.Warning else getDeviceIcon(d.type),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = if (d.isSuspicious) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = d.getDisplayName(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (d.isSuspicious) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = d.ipAddress,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (d.isSuspicious) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (d.isSuspicious) "⚠️ SUSPICIOUS / BLOCKED" 
                                   else if (d.isOnline) "🟢 ONLINE" else "🔴 OFFLINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (d.isSuspicious) Color.White else if (d.isOnline) Color(0xFF4CAF50) else Color.Red
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.ping() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (pingLatency != null) "Ping: ${pingLatency}ms" else "Ping Device")
                        }

                        if (d.openPorts.contains(80) || d.openPorts.contains(443)) {
                            val port = if (d.openPorts.contains(80)) 80 else 443
                            val protocol = if (port == 443) "https" else "http"
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$protocol://${d.ipAddress}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Browser")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(24.dp))

                    Text(
                        text = "Identity",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailItem("Friendly Name", d.getDisplayName())
                    DetailItem("Vendor", d.vendor ?: "Generic / Unknown")
                    DetailItem("Device Type", d.type.name)
                    DetailItem("Status", if (d.isSuspicious) "Suspicious" else if (d.isKnown) "Genuine / Trusted" else "Unknown")
                    
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailItem("First Discovered", formatDate(d.firstSeen))
                    DetailItem("Last Seen Online", formatDate(d.lastSeen))

                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Connectivity",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailItem("IP Address", d.ipAddress)
                    DetailItem("MAC Address", d.macAddress ?: "Unavailable (Android 10+ restriction)")
                    DetailItem("Hostname", d.hostname ?: "Not available")
                    DetailItem("Network (SSID)", d.ssid ?: "Unknown")
                    
                    if (d.mDnsName != null || d.upnpName != null) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Discovery Data",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        if (d.mDnsName != null) DetailItem("mDNS Name", d.mDnsName)
                        if (d.upnpName != null) DetailItem("UPnP Name", d.upnpName)
                    }

                    if (d.openPorts.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Open Ports",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = d.openPorts.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ShimmerDeviceItem() {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.3f),
        Color.Gray.copy(alpha = 0.1f),
        Color.Gray.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .background(brush)
                )
            }
        }
    }
}

fun getDeviceIcon(type: DeviceType): ImageVector {
    return when (type) {
        DeviceType.ROUTER -> Icons.Default.Router
        DeviceType.PHONE -> Icons.Default.PhoneAndroid
        DeviceType.PC -> Icons.Default.Computer
        DeviceType.MEDIA -> Icons.Default.Tv
        DeviceType.GAME_CONSOLE -> Icons.Default.VideogameAsset
        DeviceType.IOT -> Icons.Default.Router // Using router for IoT placeholder
        DeviceType.PRINTER -> Icons.Default.Computer
        else -> Icons.Default.Computer
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
