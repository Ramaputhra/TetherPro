# ⚡ Tether Dashboard

> **Transform any spare Android phone into a dedicated high-speed 5GHz Wi-Fi bridge for your PC or laptop — with real-time throughput telemetry, hardware thermals, and an immersive mirrored HUD.**

---

## 💡 The Problem & The Smart Solution

### Why Buy an Expensive 5GHz Wi-Fi Dongle?
Many older laptops, desktop motherboards, and budget workstations come equipped with legacy single-band **2.4 GHz Wi-Fi** cards or faulty built-in antennas. This causes:
- ❌ Congested channels and frequent packet drops.
- ❌ Capped download/upload speeds (rarely exceeding 30–50 Mbps in real-world conditions).
- ❌ High latency and jitter during gaming, streaming, or video calls.
- ❌ Needing to spend $20–$50 on a USB Wi-Fi dongle that often suffers from thermal throttling, poor reception, or driver issues.

### The Zero-Cost Upgrade: Spare Mobile as a 5GHz Receiver
Modern Android smartphones have **high-grade MIMO Wi-Fi antennas** capable of connecting to **5 GHz (802.11ac / Wi-Fi 6)** with link speeds exceeding **433 Mbps to 866+ Mbps**.

By pairing your phone with **Tether Dashboard**:
1. **Your Phone connects to 5 GHz Wi-Fi** with strong reception and low latency.
2. **USB Tethering relays the full-speed internet** directly to your PC/Laptop over a standard USB cable (using plug-and-play RNDIS/CDC-Ethernet).
3. **Your PC gets instant 5 GHz gigabit-class speeds** without installing third-party drivers or buying extra hardware!
4. **Tether Dashboard turns your phone screen into a live desktop telemetry console**, showing transfer speeds, link quality, CPU load, and battery thermals.

```
┌─────────────────┐       5 GHz Wi-Fi       ┌──────────────────────┐       USB Cable       ┌─────────────────┐
│ 5GHz Wi-Fi      │ ~~~~~~~~~~~~~~~~~~~~~>  │ Spare Android Phone  │ ====================> │ PC / Laptop     │
│ Router / AP     │   (Up to 866+ Mbps)     │ (Runs Tether Dash)   │   (Direct RNDIS bus)  │ (High-Speed Net)│
└─────────────────┘                         └──────────────────────┘                       └─────────────────┘
                                                        │
                                            ┌───────────┴───────────┐
                                            │  Live Telemetry & HUD │
                                            │  • Bandwidth Graphs   │
                                            │  • Battery Thermals   │
                                            │  • Mirrored HUD Mode  │
                                            └───────────────────────┘
```

---

## ✨ Features at a Glance

### 🚀 High-Speed Wi-Fi to USB Bridge Telemetry
- **Dual-Link Real-Time Monitoring**: Independently tracks both the incoming Wi-Fi stream (`wlan0`) and the outgoing USB tethering pipe (`rndis0`/`usb0`).
- **Precision Speed Calculations**: Live download (RX) and upload (TX) rates formatted dynamically in **Mbps** and **KB/s / MB/s**.
- **Smooth Spline Waveform Charts**: Dynamic, self-scaling canvas graphs visualising download and upload throughput history with neon gradients.
- **Link & Signal Analytics**: Displays active SSID, link speed negotiation (e.g. 433 / 866 Mbps), RSSI signal strength in dBm and percentage, and assigned local IP addresses.

### 🖥️ Immersive Fullscreen HUD & Maximize Mode
- **Tap-to-Maximize Output Stream**: When USB tethering is active, tap anywhere on the **OUTPUT STREAM** card or the maximize icon to enter an edge-to-edge, zero-distraction fullscreen monitoring cockpit.
- **Bi-Directional Orientation**: Automatically adapts layout for both **Landscape** (wide dashboard mode) and **Portrait** (docked phone stand mode).
- **Primary Metric Toggle**: Switch between highlighting Download or Upload as the prominent primary gauge with a single tap.

### 🪞 Heads-Up Display (HUD) Mirror Mode
- **One-Tap Horizontal Flip**: Easily toggle horizontal mirroring (`scaleX = -1`) directly from the Output Stream card or inside the Fullscreen HUD.
- **Reflection / Windshield Ready**: Place your phone under an angled glass shelf, teleprompter mirror, or vehicle windshield for a crisp, heads-up projected speed readout.

### 🔋 Battery Safety & Thermal Guard
Running continuous high-speed downloads while tethering and charging can heat up mobile devices. Tether Dashboard keeps you informed:
- **Battery Percentage & State**: Real-time state of charge with visual power indicators.
- **Battery Temperature Guard**: Live temperature readouts with color-coded safety warnings (Cool / Warm / Critical).
- **CPU Utilization**: Live processor load monitoring to ensure your phone operates efficiently without background lag.

### 🛠️ Diagnostics & Usability
- **Direct System Settings Shortcut**: Launch directly into Android's native Tethering & Portable Hotspot settings with one click.
- **Real-Time Terminal Log**: Console panel displaying network interface changes, IP assignments, and connection states.
- **Safety Simulation Mode**: An integrated demo simulator allowing testing and demonstrations without requiring physical USB cables.

---

## 📋 Quick Setup Guide

Turn your spare Android phone into a high-speed Wi-Fi adapter in 3 easy steps:

| Step | Action | Description |
| :---: | :--- | :--- |
| **1** | **Connect to 5 GHz** | On your phone, connect to your router's **5 GHz Wi-Fi** SSID (look for 5G / 5GHz in your Wi-Fi settings). |
| **2** | **Connect USB Cable** | Plug a quality USB data cable from your phone into a **USB 3.0 / USB-C port** on your PC or laptop. |
| **3** | **Enable USB Tethering** | Open **Tether Dashboard** and tap **Configure Tethering** (or enable *USB Tethering* in Android Settings). |

> [!TIP]
> **Pro Tip for Maximum Speed**: Use a USB 3.0 or USB-C port (usually colored blue or labeled with `SS` for SuperSpeed) on your PC. This ensures the USB tethering link negotiation doesn't bottleneck high-speed 5GHz Wi-Fi connections.

---

## 🎛️ Navigation & UI Guide

### 1. Main Dashboard
- **Top Bar**: Displays system clock, date, and status indicators.
- **Source Stream (Wi-Fi)**: Shows current 5GHz connection, IP address, negotiated link speed, signal strength, and live download/upload graphs.
- **Relay Flow Indicator**: Visual pulse animation confirming active data bridging between Wi-Fi and USB.
- **Output Stream (USB)**: Displays tethering interface state, IP, packet throughput, mirror toggle, and tap-to-maximize prompt.
- **Device Telemetry Panel**: Battery level, charging status, temperature gauge, and CPU load.
- **System Event Log**: Timestamped network lifecycle events.

### 2. Fullscreen HUD Mode
- Tap the **OUTPUT STREAM** card (or tap the Maximize icon) when USB tethering is active.
- Use the **MIRROR** button to flip the display horizontally for HUD glass reflection.
- Use the **SWAP** button to switch the primary metric between Download (RX) and Upload (TX).
- Tap the **Close / Minimize** icon (or press the Android Back button) to return to the main dashboard.

---

## 🏗️ Technical Architecture

- **UI Framework**: Modern Jetpack Compose with Material 3 theming and edge-to-edge support.
- **Network Telemetry**: Low-overhead Linux interface tracking (`/proc/net/dev`) combined with Android `TrafficStats` and `ConnectivityManager`.
- **State Architecture**: MVVM pattern powered by Kotlin Coroutines, `StateFlow`, and lifecycle-aware collectors.
- **Hardware Telemetry**: Android `BatteryManager` broadcast receiver and system thermal sensors.
- **Canvas Visuals**: Hardware-accelerated Compose `Canvas` drawing cubic Bezier splines with anti-aliasing and zero memory churn.

---

## 🔒 Permissions & Privacy

- `ACCESS_NETWORK_STATE` & `ACCESS_WIFI_STATE`: Required to monitor Wi-Fi signal level, SSID, and link speed.
- `CHANGE_NETWORK_STATE`: Used to detect interface changes smoothly.
- `INTERNET`: Standard network state querying.
- **Zero Cloud Tracking**: All telemetry is processed **100% locally** on the device. No data, traffic, or analytics are ever uploaded or transmitted externally.

---

## 📄 License
This project is open-source and built for performance enthusiasts looking to recycle existing hardware for superior desktop networking.
