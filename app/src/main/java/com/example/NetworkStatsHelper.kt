package com.example

import android.net.TrafficStats
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class InterfaceStats(val rxBytes: Long, val txBytes: Long)

object NetworkStatsHelper {
    
    fun getInterfaceStats(iface: String): InterfaceStats {
        // 1. Try TrafficStats on API 28+ (Pie+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val rx = TrafficStats.getRxBytes(iface)
                val tx = TrafficStats.getTxBytes(iface)
                if (rx != TrafficStats.UNSUPPORTED.toLong() && tx != TrafficStats.UNSUPPORTED.toLong() && rx > 0) {
                    return InterfaceStats(rx, tx)
                }
            } catch (e: Exception) {
                // Fallback to other methods
            }
        }
        
        // 2. Try sysfs statistics direct files (very fast fallback)
        try {
            val rxFile = File("/sys/class/net/$iface/statistics/rx_bytes")
            val txFile = File("/sys/class/net/$iface/statistics/tx_bytes")
            if (rxFile.exists() && txFile.exists()) {
                val rx = rxFile.readText().trim().toLongOrNull() ?: 0L
                val tx = txFile.readText().trim().toLongOrNull() ?: 0L
                if (rx > 0 || tx > 0) {
                    return InterfaceStats(rx, tx)
                }
            }
        } catch (e: Exception) {
            // Ignore and try proc/net/dev
        }
        
        // 3. Parse /proc/net/dev as final fallback (guaranteed on all Linux/Android platforms)
        try {
            val file = File("/proc/net/dev")
            if (file.exists()) {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        if (currentLine.contains(":")) {
                            val parts = currentLine.split(":")
                            val name = parts[0].trim()
                            if (name == iface) {
                                val statsStr = parts[1].trim()
                                val tokens = statsStr.split("\\s+".toRegex())
                                if (tokens.size >= 9) {
                                    val rx = tokens[0].toLongOrNull() ?: 0L
                                    val tx = tokens[8].toLongOrNull() ?: 0L
                                    return InterfaceStats(rx, tx)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return InterfaceStats(0L, 0L)
    }
}
