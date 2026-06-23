package com.example

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TetherViewModel = viewModel(),
    onRequestPermissions: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var isSimModeEnabled by remember { mutableStateOf(false) }

    // Synchronize simulation mode with viewModel
    LaunchedEffect(isSimModeEnabled) {
        viewModel.toggleSimulation(isSimModeEnabled)
    }

    // Gradient background following the "Immersive UI" color scheme
    val mainBgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF05080F), // Deep Slate/Black
            Color(0xFF020408)  // Solid Midnight
        )
    )

    // Pulse animation for the glowing header LED indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBgGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Column (Compact header, Telemetry, Log panel, Buttons)
            Column(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactHeaderSection(
                    timeString = state.timeString,
                    activeIp = when {
                        state.wifi.isConnected -> state.wifi.ipAddress
                        state.usb.isActive -> state.usb.ipAddress
                        else -> "192.168.43.1"
                    },
                    isWifiConnected = state.wifi.isConnected,
                    isUsbActive = state.usb.isActive,
                    pulseAlpha = pulseAlpha
                )

                if (isSimModeEnabled) {
                    CompactSimulationAlertBanner()
                }

                TelemetryPanel(device = state.device)

                TerminalLogPanel(
                    logs = state.logs,
                    modifier = Modifier.weight(1f)
                )

                CompactFooterControls(
                    isSimModeEnabled = isSimModeEnabled,
                    onToggleSim = { isSimModeEnabled = !isSimModeEnabled },
                    onSettingsClick = onRequestPermissions
                )
            }

            // Right Column (LazyColumn for Wi-Fi and USB Tether Details)
            LazyColumn(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    SourceConnectionCard(
                        wifi = state.wifi,
                        onRequestPermissions = onRequestPermissions
                    )
                }

                item {
                    CompactRelayFlowIndicator(
                        isFlowing = state.wifi.isConnected && state.usb.isActive,
                        pulseAlpha = pulseAlpha
                    )
                }

                item {
                    OutputStreamCard(
                        usb = state.usb
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(mainBgGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- IMMERSIVE APP HEADER ---
            HeaderSection(
                timeString = state.timeString,
                dateString = state.dateString,
                activeIp = when {
                    state.wifi.isConnected -> state.wifi.ipAddress
                    state.usb.isActive -> state.usb.ipAddress
                    else -> "192.168.43.1"
                },
                isWifiConnected = state.wifi.isConnected,
                isUsbActive = state.usb.isActive,
                pulseAlpha = pulseAlpha
            )

            // --- DASHBOARD CONTAINER LIST ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                // Warning Banner for Demo Simulation
                if (isSimModeEnabled) {
                    item {
                        SimulationAlertBanner()
                    }
                }

                // --- SOURCE CONNECTION CARD (WI-FI) ---
                item {
                    SourceConnectionCard(
                        wifi = state.wifi,
                        onRequestPermissions = onRequestPermissions
                    )
                }

                // --- MIDDLE CONNECTOR / RELAY FLOW ---
                item {
                    RelayFlowIndicator(
                        isFlowing = state.wifi.isConnected && state.usb.isActive,
                        pulseAlpha = pulseAlpha
                    )
                }

                // --- OUTPUT STREAM CARD (USB TETHER) ---
                item {
                    OutputStreamCard(
                        usb = state.usb
                    )
                }

                // --- HARDWARE DIAGNOSTICS & HEAT PANEL ---
                item {
                    TelemetryPanel(
                        device = state.device
                    )
                }

                // --- TERMINAL LOGS PANEL ---
                item {
                    TerminalLogPanel(
                        logs = state.logs
                    )
                }
            }

            // --- FIXED FOOTER ACTION BUTTONS ---
            FooterControls(
                isSimModeEnabled = isSimModeEnabled,
                onToggleSim = { isSimModeEnabled = !isSimModeEnabled },
                onSettingsClick = onRequestPermissions
            )
        }
    }
}

@Composable
fun HeaderSection(
    timeString: String,
    dateString: String,
    activeIp: String,
    isWifiConnected: Boolean,
    isUsbActive: Boolean,
    pulseAlpha: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tether",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Pro",
                    color = Color(0xFF22D3EE),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Time & Date Right aligned
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeString.substringBeforeLast(":"), // HH:mm
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 26.sp
                )
                Text(
                    text = dateString.uppercase(Locale.getDefault()),
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // IP Address Badge
        val isLinkActive = isWifiConnected || isUsbActive
        val statusColor = if (isLinkActive) Color(0xFF22D3EE) else Color(0xFFF43F5E)
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0x1A0F172A), RoundedCornerShape(50))
                .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "IP: $activeIp",
                color = if (isLinkActive) Color(0xFF22D3EE).copy(alpha = 0.9f) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun SimulationAlertBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sim_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0x1AF43F5E)),
        border = BorderStroke(1.dp, Color(0x4DF43F5E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Demo Sandbox Warning",
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SANDBOX DEMO FLOW",
                    color = Color(0xFFF43F5E),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Displaying mock bandwidth data. Switch to Live mode for true device monitoring.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun SourceConnectionCard(
    wifi: WifiDetails,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x660F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "SOURCE CONNECTION",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (wifi.isConnected) wifi.ssid else "Wi-Fi Interface Inactive",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Styled icon container
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "WiFi connection",
                        tint = if (wifi.isConnected) Color(0xFF22D3EE) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Speeds row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOWNLOAD",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatBytes(wifi.rxSpeedBytes),
                        color = Color(0xFF22D3EE),
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Upload
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPLOAD",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatBytes(wifi.txSpeedBytes),
                        color = Color(0xFF34D399), // Emerald
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (wifi.isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "INTERFACE",
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = wifi.interfaceName ?: "Unknown",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LINK SPEED / SIGNAL",
                            color = Color(0xFF64748B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${wifi.linkSpeed}Mbps / ${wifi.signalStrength}%",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time Wave Monitor
                SpeedWaveChart(
                    historyRx = wifi.speedHistoryRx,
                    historyTx = wifi.speedHistoryTx,
                    lineColorRx = Color(0xFF22D3EE),
                    lineColorTx = Color(0xFF34D399),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            } else if (wifi.ssid == "Unknown SSID") {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Permission needed",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT WI-FI DETAIL ACCESS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RelayFlowIndicator(
    isFlowing: Boolean,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vertical background link lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midX = size.width / 2
            drawLine(
                color = Color(0xFF22D3EE).copy(alpha = 0.2f),
                start = Offset(midX, 0f),
                end = Offset(midX, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Active Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF22D3EE).copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF22D3EE).copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF22D3EE).copy(alpha = pulseAlpha * 0.6f))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF22D3EE).copy(alpha = pulseAlpha * 0.3f))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "RELAYING PACKETS",
                color = Color(0xFF22D3EE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun OutputStreamCard(
    usb: UsbDetails
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x660F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "OUTPUT STREAM",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (usb.isActive) "USB Tethering Active" else "USB Cable Standby",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Styled icon container
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "USB Tether",
                        tint = if (usb.isActive) Color(0xFFFB923C) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (usb.isActive) {
                // Speeds row (Laptop download & Laptop upload perspective)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // To Laptop (Transmitted from phone)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TO LAPTOP (DOWN)",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatBytes(usb.txSpeedBytes),
                            color = Color(0xFFFB923C), // Orange-400
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // From Laptop (Received by phone)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FROM LAPTOP (UP)",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatBytes(usb.rxSpeedBytes),
                            color = Color(0xFFFFD8A8), // Light Orange/Peach
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Wave Chart
                SpeedWaveChart(
                    historyRx = usb.speedHistoryRx,
                    historyTx = usb.speedHistoryTx,
                    lineColorRx = Color(0xFFFFD8A8),
                    lineColorTx = Color(0xFFFB923C),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTED DEVICE",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = usb.interfaceName ?: "Ethernet Interface",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PLUG IN USB CABLE & ENABLE TETHERING IN PHONE SYSTEM SETTINGS",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryPanel(
    device: DeviceDetails
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x660F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Diagnostics",
                    tint = Color(0xFFE2E8F0),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEVICE TELEMETRY",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Battery
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(text = "BATTERY CHARGE", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (device.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = "Battery charge status",
                            tint = if (device.batteryPct > 20) Color(0xFF34D399) else Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${device.batteryPct}%" + if (device.isCharging) " [AC]" else "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Temp
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "TEMPERATURE", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val tempColor = when {
                        device.batteryTemp >= 40.0f -> Color(0xFFF43F5E)
                        device.batteryTemp >= 35.0f -> Color(0xFFFB923C)
                        else -> Color(0xFF22D3EE)
                    }
                    Text(
                        text = String.format(Locale.US, "%.1f °C", device.batteryTemp),
                        color = tempColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // CPU
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(text = "CPU LOAD", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${device.cpuUsagePct}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalLogPanel(
    logs: List<String>,
    modifier: Modifier = Modifier.height(130.dp)
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020408)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF22D3EE))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONSOLE STDOUT / TTY0",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "DISPATCHER",
                    color = Color(0xFF1E293B),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = Color(0xFF22D3EE).copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FooterControls(
    isSimModeEnabled: Boolean,
    onToggleSim: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings Button / Grant WiFi permission trigger
        Button(
            onClick = onSettingsClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WI-FI ACCESS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Sim toggle button matching the Stop Link button from Design HTML with its beautiful red hue
        Button(
            onClick = onToggleSim,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("simulation_toggle"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSimModeEnabled) Color(0x33F43F5E) else Color(0x1AF43F5E)
            ),
            border = BorderStroke(
                1.dp,
                if (isSimModeEnabled) Color(0xFFF43F5E).copy(alpha = 0.8f) else Color(0xFFF43F5E).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF43F5E))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSimModeEnabled) "STOP DEMO" else "SIMULATE",
                color = Color(0xFFF43F5E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SpeedWaveChart(
    historyRx: List<Long>,
    historyTx: List<Long>,
    lineColorRx: Color,
    lineColorTx: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (historyRx.isEmpty() && historyTx.isEmpty()) {
            return@Canvas
        }

        // Draw grid background lines
        val gridCount = 3
        val gridSpacingY = height / gridCount
        for (i in 1 until gridCount) {
            val y = i * gridSpacingY
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Render waves
        drawSpline(historyRx, lineColorRx, width, height)
        drawSpline(historyTx, lineColorTx, width, height)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpline(
    history: List<Long>,
    color: Color,
    width: Float,
    height: Float
) {
    if (history.size < 2) return
    val maxVal = (history.maxOrNull() ?: 1L).coerceAtLeast(1024L).toFloat()
    val stepX = width / (history.size - 1)

    val path = Path()
    val fillPath = Path()

    history.forEachIndexed { index, value ->
        val x = index * stepX
        val y = height - ((value.toFloat() / maxVal) * (height - 8.dp.toPx())).coerceAtMost(height - 4.dp.toPx())

        if (index == 0) {
            path.moveTo(x, y)
            fillPath.moveTo(x, height)
            fillPath.lineTo(x, y)
        } else {
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        if (index == history.size - 1) {
            fillPath.lineTo(x, height)
            fillPath.close()
        }
    }

    // Gradient fill under the wave
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.12f), Color.Transparent),
            startY = 0f,
            endY = height
        )
    )

    // Glowing border line
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 1.5.dp.toPx(),
            join = StrokeJoin.Round,
            cap = StrokeCap.Round
        )
    )
}

fun formatBytes(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0.0 B/s"
    if (bytesPerSec < 1024) return "$bytesPerSec B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB/s", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB/s", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.1f GB/s", gb)
}

@Composable
fun CompactHeaderSection(
    timeString: String,
    activeIp: String,
    isWifiConnected: Boolean,
    isUsbActive: Boolean,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tether",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Pro",
                color = Color(0xFF22D3EE),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        // IP Address Badge & Clock together
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isLinkActive = isWifiConnected || isUsbActive
            val statusColor = if (isLinkActive) Color(0xFF22D3EE) else Color(0xFFF43F5E)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0x1A0F172A), RoundedCornerShape(50))
                    .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "IP: $activeIp",
                    color = if (isLinkActive) Color(0xFF22D3EE).copy(alpha = 0.9f) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = timeString.substringBeforeLast(":"), // HH:mm
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun CompactSimulationAlertBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sim_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0x1AF43F5E)),
        border = BorderStroke(1.dp, Color(0x4DF43F5E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Demo Sandbox Warning",
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SANDBOX DEMO: DISPLAYING SIMULATED METRICS",
                color = Color(0xFFCBD5E1),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CompactRelayFlowIndicator(
    isFlowing: Boolean,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vertical background link lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midX = size.width / 2
            drawLine(
                color = Color(0xFF22D3EE).copy(alpha = 0.2f),
                start = Offset(midX, 0f),
                end = Offset(midX, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Active Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF22D3EE).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF22D3EE).copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF22D3EE).copy(alpha = pulseAlpha * 0.6f))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RELAYING PACKETS",
                color = Color(0xFF22D3EE),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CompactFooterControls(
    isSimModeEnabled: Boolean,
    onToggleSim: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Settings Button / Grant WiFi permission trigger
        Button(
            onClick = onSettingsClick,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "WI-FI PERM",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Sim toggle button
        Button(
            onClick = onToggleSim,
            modifier = Modifier
                .weight(1.3f)
                .height(40.dp)
                .testTag("simulation_toggle"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSimModeEnabled) Color(0x33F43F5E) else Color(0x1AF43F5E)
            ),
            border = BorderStroke(
                1.dp,
                if (isSimModeEnabled) Color(0xFFF43F5E).copy(alpha = 0.8f) else Color(0xFFF43F5E).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF43F5E))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSimModeEnabled) "STOP DEMO" else "SIMULATE",
                color = Color(0xFFF43F5E),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
