package com.example.core.monitor

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class CpuMonitor(private val context: Context) {
    data class CpuState(
        val totalUsagePercentage: Int = 15,
        val coreCount: Int = 8,
        val thermalStatusText: String = "Normal",
        val cpuTemperatureCelsius: Double = 32.5
    )

    private val _state = MutableStateFlow(CpuState())
    val state: StateFlow<CpuState> = _state.asStateFlow()

    init {
        update()
    }

    fun update() {
        try {
            val cores = Runtime.getRuntime().availableProcessors()
            val usage = getRealtimeCpuUsageEstimate()
            val thermalStatus = getThermalStatusString()
            val cpuTemp = getCpuTemperatureEstimate()

            _state.value = CpuState(
                totalUsagePercentage = usage,
                coreCount = cores,
                thermalStatusText = thermalStatus,
                cpuTemperatureCelsius = cpuTemp
            )
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun getThermalStatusString(): String {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                when (powerManager.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "Normal"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Overheated"
                    else -> "Normal"
                }
            } else {
                "Normal"
            }
        } catch (e: Exception) {
            "Normal"
        }
    }

    private fun getCpuTemperatureEstimate(): Double {
        return try {
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp"
            )
            for (path in thermalPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val tempStr = file.readText().trim()
                    val tempVal = tempStr.toDoubleOrNull() ?: 0.0
                    if (tempVal > 0) {
                        return if (tempVal > 1000) tempVal / 1000.0 else tempVal
                    }
                }
            }
            36.2 // Steady fallback temperature
        } catch (e: Exception) {
            36.2
        }
    }

    private fun getRealtimeCpuUsageEstimate(): Int {
        val procStatUsage = getCpuUsageFromProcStat()
        if (procStatUsage != null) {
            return procStatUsage
        }

        return try {
            val startCpu = android.os.Process.getElapsedCpuTime()
            val startTime = System.currentTimeMillis()
            Thread.sleep(10)
            val endCpu = android.os.Process.getElapsedCpuTime()
            val endTime = System.currentTimeMillis()

            val cpuTime = endCpu - startCpu
            val realTime = endTime - startTime
            val cores = Runtime.getRuntime().availableProcessors()

            if (realTime > 0) {
                val usage = (cpuTime * 100) / (realTime * cores)
                usage.toInt().coerceIn(8, 95)
            } else {
                25
            }
        } catch (e: Exception) {
            25
        }
    }

    private fun getCpuUsageFromProcStat(): Int? {
        return try {
            val file = File("/proc/stat")
            if (file.exists() && file.canRead()) {
                val line = file.useLines { it.firstOrNull() }
                if (line != null && line.startsWith("cpu")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 5) {
                        val user = parts[1].toLongOrNull() ?: 0L
                        val nice = parts[2].toLongOrNull() ?: 0L
                        val system = parts[3].toLongOrNull() ?: 0L
                        val idle = parts[4].toLongOrNull() ?: 0L
                        val total = user + nice + system + idle
                        val active = user + nice + system
                        if (total > 0) {
                            return ((active * 100) / total).toInt().coerceIn(1, 100)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
