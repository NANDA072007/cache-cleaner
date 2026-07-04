package com.example.core.scanner

import com.example.core.model.OptimizationLog
import com.example.core.model.DeviceScoreLog
import com.example.core.monitor.StorageMonitor
import com.example.core.monitor.MemoryMonitor
import com.example.core.monitor.BatteryMonitor
import com.example.core.monitor.CpuMonitor

class ReportGenerator {

    data class DeviceHealthReport(
        val overallHealthIndex: Int,
        val storageTrendText: String,
        val ramTrendText: String,
        val batteryHealthStatus: String,
        val cpuLoadAverage: Int,
        val totalBytesRecovered: Long,
        val optimizationRunsCount: Int,
        val diagnosticRecommendationsList: List<String>
    )

    fun generateReport(
        storage: StorageMonitor.StorageState,
        memory: MemoryMonitor.MemoryState,
        battery: BatteryMonitor.BatteryState,
        cpu: CpuMonitor.CpuState,
        optimizationLogs: List<OptimizationLog>,
        scoreLogs: List<DeviceScoreLog>
    ): DeviceHealthReport {
        // Compute overall score average or default to current
        val averageScore = if (scoreLogs.isNotEmpty()) {
            scoreLogs.map { it.overallScore }.average().toInt()
        } else {
            85
        }

        // Evaluate storage trend statement
        val storageUsedPct = storage.usedPercentage
        val storageTrend = when {
            storageUsedPct > 85 -> "Critical Storage Contraction: Less than 15% remaining capacity."
            storageUsedPct > 70 -> "High Storage Saturation: Clear cache or duplicate media files."
            else -> "Optimal Storage Footprint: Healthy sectors allocation."
        }

        // Evaluate RAM trend statement
        val ramUsedPct = memory.usedPercentage
        val ramTrend = when {
            ramUsedPct > 80 -> "Elevated Memory Pressure: Recommend system garbage collection sweep."
            ramUsedPct > 60 -> "Moderate RAM consumption. Background cache is active."
            else -> "Efficient Memory Usage: Plentiful free allocations."
        }

        // Sum up total bytes freed locally in the history
        val totalBytesFreed = optimizationLogs.filter { it.type == "JUNK_CLEAN" }.sumOf { it.bytesFreed }
        val optimizationRuns = optimizationLogs.size

        // Assemble actionable items list
        val recommendations = mutableListOf<String>()
        if (storageUsedPct > 75) recommendations.add("Initiate Deep Clean sequence immediately to claim system sectors.")
        if (memory.isSystemLowMemory || ramUsedPct > 80) recommendations.add("Invoke Memory Boost to release cached heap allocations.")
        if (battery.temperatureCelsius > 40.0f) recommendations.add("Thermal throttling active. Unplug charger and restrict heavy activity.")
        if (recommendations.isEmpty()) {
            recommendations.add("All system telemetry components align within standard operating thresholds.")
        }

        return DeviceHealthReport(
            overallHealthIndex = averageScore,
            storageTrendText = storageTrend,
            ramTrendText = ramTrend,
            batteryHealthStatus = "Status: ${battery.statusText} | Health: ${battery.healthText} | Temp: ${battery.temperatureCelsius}°C",
            cpuLoadAverage = cpu.totalUsagePercentage,
            totalBytesRecovered = totalBytesFreed,
            optimizationRunsCount = optimizationRuns,
            diagnosticRecommendationsList = recommendations
        )
    }
}
