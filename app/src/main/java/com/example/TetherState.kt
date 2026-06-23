package com.example

data class WifiDetails(
    val isConnected: Boolean = false,
    val interfaceName: String? = null,
    val ssid: String = "Unknown SSID",
    val signalStrength: Int = 0, // 0-100%
    val linkSpeed: Int = 0, // Mbps
    val ipAddress: String = "Disconnected",
    val rxSpeedBytes: Long = 0, // Download speed
    val txSpeedBytes: Long = 0, // Upload speed
    val speedHistoryRx: List<Long> = emptyList(), // Last 15 readings
    val speedHistoryTx: List<Long> = emptyList()
)

data class UsbDetails(
    val isActive: Boolean = false,
    val interfaceName: String? = null,
    val ipAddress: String = "Disconnected",
    val rxSpeedBytes: Long = 0, // Received from laptop (Laptop Upload)
    val txSpeedBytes: Long = 0, // Transmitted to laptop (Laptop Download)
    val speedHistoryRx: List<Long> = emptyList(),
    val speedHistoryTx: List<Long> = emptyList()
)

data class DeviceDetails(
    val batteryPct: Int = 0,
    val batteryTemp: Float = 0.0f, // Celsius
    val isCharging: Boolean = false,
    val cpuUsagePct: Int = 0
)

data class DashboardState(
    val timeString: String = "00:00:00",
    val dateString: String = "",
    val wifi: WifiDetails = WifiDetails(),
    val usb: UsbDetails = UsbDetails(),
    val device: DeviceDetails = DeviceDetails(),
    val logs: List<String> = emptyList()
)
