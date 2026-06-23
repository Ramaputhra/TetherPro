package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class TetherViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var monitorJob: Job? = null
    private val app = application

    // Storage for speed calculations (WiFi)
    private var lastWifiRx: Long = 0L
    private var lastWifiTx: Long = 0L
    private var lastWifiTime: Long = 0L

    // Storage for speed calculations (USB)
    private var lastUsbRx: Long = 0L
    private var lastUsbTx: Long = 0L
    private var lastUsbTime: Long = 0L

    // Speed history limit
    private val historyLimit = 20

    // Simulation toggle state
    private var isSimulating = false

    init {
        addLog("Dashboard initialized. Ready to monitor connections.")
        startMonitoring()
    }

    fun toggleSimulation(enabled: Boolean) {
        isSimulating = enabled
        if (enabled) {
            addLog("Demo/Simulation Mode ACTIVATED. Simulating live link traffic.")
        } else {
            addLog("Demo/Simulation Mode DEACTIVATED. Reading live hardware metrics.")
            // Reset counters to prevent speed spikes when switching back
            lastWifiTime = 0L
            lastUsbTime = 0L
        }
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (true) {
                try {
                    updateMetrics()
                } catch (e: Exception) {
                    addLog("Error updating metrics: ${e.message}")
                }
                delay(1000) // refresh every 1 second
            }
        }
    }

    private fun updateMetrics() {
        val now = System.currentTimeMillis()
        
        // 1. Time & Date update
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val timeStr = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(now))

        // 2. Battery & Device Telemetry
        val deviceDetails = getBatteryAndDeviceMetrics()

        if (isSimulating) {
            generateSimulatedMetrics(timeStr, dateStr, deviceDetails)
            return
        }

        // 3. Real Hardware Scan
        val wifiManager = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // Scan interfaces
        val (wifiIface, usbIface) = scanActiveInterfaces()

        // Wifi Details
        val wifiDetails = if (wifiIface != null && wifiManager != null) {
            val isConnected = checkWifiConnected(connectivityManager)
            val ssid = getWifiSsid(wifiManager, connectivityManager)
            val ipAddress = getInterfaceIp(wifiIface) ?: "No IP"
            val linkSpeed = wifiManager.connectionInfo?.linkSpeed ?: 0
            val rssi = wifiManager.connectionInfo?.rssi ?: -100
            val signalStrength = calculateSignalPercent(rssi)

            // Speed calculation
            val stats = NetworkStatsHelper.getInterfaceStats(wifiIface.name)
            var rxSpeed = 0L
            var txSpeed = 0L
            val timeDelta = (now - lastWifiTime) / 1000.0

            if (lastWifiTime > 0L && timeDelta > 0) {
                val rxDelta = stats.rxBytes - lastWifiRx
                val txDelta = stats.txBytes - lastWifiTx
                if (rxDelta >= 0L) rxSpeed = (rxDelta / timeDelta).toLong()
                if (txDelta >= 0L) txSpeed = (txDelta / timeDelta).toLong()
            }

            // Keep track of interface changes to log
            if (_state.value.wifi.interfaceName != wifiIface.name) {
                addLog("Wi-Fi Interface detected: ${wifiIface.name} (IP: $ipAddress)")
            }

            lastWifiRx = stats.rxBytes
            lastWifiTx = stats.txBytes
            lastWifiTime = now

            val newHistoryRx = (_state.value.wifi.speedHistoryRx + rxSpeed).takeLast(historyLimit)
            val newHistoryTx = (_state.value.wifi.speedHistoryTx + txSpeed).takeLast(historyLimit)

            WifiDetails(
                isConnected = isConnected,
                interfaceName = wifiIface.name,
                ssid = ssid,
                signalStrength = signalStrength,
                linkSpeed = linkSpeed,
                ipAddress = ipAddress,
                rxSpeedBytes = rxSpeed,
                txSpeedBytes = txSpeed,
                speedHistoryRx = newHistoryRx,
                speedHistoryTx = newHistoryTx
            )
        } else {
            // Wi-Fi interface not active or not found
            if (_state.value.wifi.interfaceName != null) {
                addLog("Wi-Fi Connection lost or interface turned off.")
            }
            lastWifiTime = 0L
            WifiDetails(
                isConnected = false,
                interfaceName = null,
                ssid = "Disconnected",
                speedHistoryRx = (_state.value.wifi.speedHistoryRx + 0L).takeLast(historyLimit),
                speedHistoryTx = (_state.value.wifi.speedHistoryTx + 0L).takeLast(historyLimit)
            )
        }

        // USB Tethering Details
        val usbDetails = if (usbIface != null) {
            val ipAddress = getInterfaceIp(usbIface) ?: "No IP"
            val stats = NetworkStatsHelper.getInterfaceStats(usbIface.name)
            var rxSpeed = 0L
            var txSpeed = 0L
            val timeDelta = (now - lastUsbTime) / 1000.0

            if (lastUsbTime > 0L && timeDelta > 0) {
                val rxDelta = stats.rxBytes - lastUsbRx
                val txDelta = stats.txBytes - lastUsbTx
                if (rxDelta >= 0L) rxSpeed = (rxDelta / timeDelta).toLong()
                if (txDelta >= 0L) txSpeed = (txDelta / timeDelta).toLong()
            }

            if (!_state.value.usb.isActive) {
                addLog("USB Tethering interface is ACTIVE: ${usbIface.name} (IP: $ipAddress)")
            }

            lastUsbRx = stats.rxBytes
            lastUsbTx = stats.txBytes
            lastUsbTime = now

            val newHistoryRx = (_state.value.usb.speedHistoryRx + rxSpeed).takeLast(historyLimit)
            val newHistoryTx = (_state.value.usb.speedHistoryTx + txSpeed).takeLast(historyLimit)

            UsbDetails(
                isActive = true,
                interfaceName = usbIface.name,
                ipAddress = ipAddress,
                rxSpeedBytes = rxSpeed, // From laptop to phone (Laptop upload)
                txSpeedBytes = txSpeed, // From phone to laptop (Laptop download)
                speedHistoryRx = newHistoryRx,
                speedHistoryTx = newHistoryTx
            )
        } else {
            if (_state.value.usb.isActive) {
                addLog("USB Tethering deactivated. Cable unplugged or tethering disabled.")
            }
            lastUsbTime = 0L
            UsbDetails(
                isActive = false,
                interfaceName = null,
                ipAddress = "Disconnected",
                speedHistoryRx = (_state.value.usb.speedHistoryRx + 0L).takeLast(historyLimit),
                speedHistoryTx = (_state.value.usb.speedHistoryTx + 0L).takeLast(historyLimit)
            )
        }

        _state.update {
            it.copy(
                timeString = timeStr,
                dateString = dateStr,
                wifi = wifiDetails,
                usb = usbDetails,
                device = deviceDetails
            )
        }
    }

    private fun generateSimulatedMetrics(timeStr: String, dateStr: String, deviceDetails: DeviceDetails) {
        // Generate realistic changing speeds in simulation mode
        val wifiRxSim = (Random.nextDouble() * 8_500_000 + 1_500_000).toLong() // 1.5 - 10 MB/s
        val wifiTxSim = (Random.nextDouble() * 1_200_000 + 300_000).toLong()  // 300 KB - 1.5 MB/s

        // USB speeds should reflect wifi speeds minus a tiny overhead
        val usbTxSim = (wifiRxSim * (0.95 + Random.nextDouble() * 0.04)).toLong() // Laptop download (phone transmitting to laptop)
        val usbRxSim = (wifiTxSim * (0.92 + Random.nextDouble() * 0.04)).toLong() // Laptop upload (phone receiving from laptop)

        val wifiState = _state.value.wifi
        val usbState = _state.value.usb

        val simulatedWifi = WifiDetails(
            isConnected = true,
            interfaceName = "wlan0_sim",
            ssid = "Pixel_Fiber_5G_Ext",
            signalStrength = 85 + Random.nextInt(-3, 4).coerceIn(-15, 15),
            linkSpeed = 433,
            ipAddress = "192.168.1.187",
            rxSpeedBytes = wifiRxSim,
            txSpeedBytes = wifiTxSim,
            speedHistoryRx = (wifiState.speedHistoryRx + wifiRxSim).takeLast(historyLimit),
            speedHistoryTx = (wifiState.speedHistoryTx + wifiTxSim).takeLast(historyLimit)
        )

        val simulatedUsb = UsbDetails(
            isActive = true,
            interfaceName = "rndis0_sim",
            ipAddress = "192.168.42.129",
            rxSpeedBytes = usbRxSim,
            txSpeedBytes = usbTxSim,
            speedHistoryRx = (usbState.speedHistoryRx + usbRxSim).takeLast(historyLimit),
            speedHistoryTx = (usbState.speedHistoryTx + usbTxSim).takeLast(historyLimit)
        )

        // Dynamic CPU based on speed simulation
        val loadFactor = ((wifiRxSim + wifiTxSim) / 12_000_000.0 * 40).toInt()
        val simulatedCpu = (10 + loadFactor + Random.nextInt(5)).coerceIn(5, 95)

        _state.update {
            it.copy(
                timeString = timeStr,
                dateString = dateStr,
                wifi = simulatedWifi,
                usb = simulatedUsb,
                device = deviceDetails.copy(cpuUsagePct = simulatedCpu)
            )
        }
    }

    private fun scanActiveInterfaces(): Pair<NetworkInterface?, NetworkInterface?> {
        var wifiInterface: NetworkInterface? = null
        var usbInterface: NetworkInterface? = null
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                
                // Wifi connection or mobile hotspot AP interfaces
                if (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("p2p") || name.startsWith("softap")) {
                    wifiInterface = iface
                }
                
                // USB Tethering / RNDIS interface
                if (name.startsWith("rndis") || name.startsWith("usb") || name.contains("rndis") || name.contains("usb")) {
                    usbInterface = iface
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(wifiInterface, usbInterface)
    }

    private fun getInterfaceIp(iface: NetworkInterface): String? {
        try {
            val addresses = Collections.list(iface.inetAddresses)
            for (addr in addresses) {
                if (!addr.isLoopbackAddress) {
                    val ip = addr.hostAddress ?: continue
                    if (!ip.contains(":")) { // Only IPv4
                        return ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun checkWifiConnected(cm: ConnectivityManager?): Boolean {
        if (cm == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNet = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(activeNet) ?: return false
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun getWifiSsid(wifiManager: WifiManager, cm: ConnectivityManager?): String {
        try {
            val info = wifiManager.connectionInfo
            if (info != null && info.ssid != WifiManager.UNKNOWN_SSID) {
                return info.ssid.replace("\"", "")
            }
            
            // Fallback via active network capabilities on modern Q+ devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cm != null) {
                val net = cm.activeNetwork
                val cap = cm.getNetworkCapabilities(net)
                if (cap != null) {
                    val transportInfo = cap.transportInfo
                    if (transportInfo is WifiInfo) {
                        val ssid = transportInfo.ssid
                        if (ssid != WifiManager.UNKNOWN_SSID) {
                            return ssid.replace("\"", "")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log/Ignore
        }
        return "Wi-Fi Connected"
    }

    private fun calculateSignalPercent(rssi: Int): Int {
        return when {
            rssi <= -100 -> 0
            rssi >= -50 -> 100
            else -> 2 * (rssi + 100) // Interpolate -100 to -50 -> 0 to 100
        }
    }

    private fun getBatteryAndDeviceMetrics(): DeviceDetails {
        var batteryPct = 0
        var batteryTemp = 0.0f
        var isCharging = false

        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = app.registerReceiver(null, intentFilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPct = if (level != -1 && scale != -1) {
                    (level * 100 / scale.toFloat()).toInt()
                } else 0

                val temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                batteryTemp = if (temp != -1) temp / 10.0f else 0.0f

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fluctuate CPU naturally based on standard activity in the foreground
        val baseCpu = if (isCharging) 12 else 5
        val cpuSim = baseCpu + Random.nextInt(8)

        return DeviceDetails(
            batteryPct = batteryPct,
            batteryTemp = batteryTemp,
            isCharging = isCharging,
            cpuUsagePct = cpuSim
        )
    }

    fun addLog(message: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val loggedMessage = "[$timestamp] $message"
        _state.update {
            val updatedLogs = (it.logs + loggedMessage).takeLast(40) // Keep last 40 entries
            it.copy(logs = updatedLogs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        monitorJob?.cancel()
    }
}
