package com.example.data.repository

import com.example.data.model.RecommendationItem
import com.example.data.model.StorageItem
import com.example.data.model.AppInfoItem

class RecommendationEngine {

    fun generateRecommendations(
        storageUsage: DeviceScanner.StorageUsage,
        memoryUsage: DeviceScanner.MemoryUsage,
        batteryStatus: DeviceScanner.BatteryStatus,
        cpuStatus: DeviceScanner.CpuStatus,
        storageItems: List<StorageItem>,
        installedApps: List<AppInfoItem>
    ): List<RecommendationItem> {
        val recommendations = mutableListOf<RecommendationItem>()

        // 1. Storage checks
        val storageFreePct = (storageUsage.freeBytes.toFloat() / storageUsage.totalBytes.toFloat()) * 100f
        if (storageFreePct < 15f) {
            recommendations.add(
                RecommendationItem(
                    id = "storage_critical",
                    title = "Storage is running critically low!",
                    description = "Less than 15% of your total device storage is available. Clean large junk files or uninstall unused apps immediately.",
                    category = "STORAGE",
                    severity = "CRITICAL",
                    actionText = "Open Storage Analyzer",
                    actionRoute = "analyzer"
                )
            )
        } else if (storageFreePct < 30f) {
            recommendations.add(
                RecommendationItem(
                    id = "storage_warning",
                    title = "Optimize storage usage",
                    description = "Storage is over 70% full. Perform a junk scan to recover space occupied by duplicate media and temporary files.",
                    category = "STORAGE",
                    severity = "WARNING",
                    actionText = "Run Junk Cleaner",
                    actionRoute = "cleaner"
                )
            )
        }

        // Check for specific file warnings
        val largeFiles = storageItems.filter { it.category == "LARGE_FILES" }
        if (largeFiles.isNotEmpty()) {
            val totalSize = largeFiles.sumOf { it.size }
            val formattedSize = formatSize(totalSize)
            recommendations.add(
                RecommendationItem(
                    id = "large_files_info",
                    title = "Found large files consuming space",
                    description = "There are ${largeFiles.size} large files (over 10MB) taking up $formattedSize in total. Review and delete unnecessary ones.",
                    category = "STORAGE",
                    severity = "INFO",
                    actionText = "Analyze Large Files",
                    actionRoute = "analyzer"
                )
            )
        }

        val duplicateFiles = storageItems.filter { it.category == "DUPLICATES" }
        if (duplicateFiles.isNotEmpty()) {
            val totalSize = duplicateFiles.sumOf { it.size }
            recommendations.add(
                RecommendationItem(
                    id = "duplicate_files_warning",
                    title = "Duplicate media files detected",
                    description = "${duplicateFiles.size} exact-duplicate files found. Deleting duplicate copies will instantly free up ${formatSize(totalSize)} of space.",
                    category = "STORAGE",
                    severity = "WARNING",
                    actionText = "Clean Duplicates",
                    actionRoute = "cleaner"
                )
            )
        }

        // 2. Memory checks
        if (memoryUsage.isSystemLowMemory) {
            recommendations.add(
                RecommendationItem(
                    id = "memory_critical",
                    title = "System is in low memory state!",
                    description = "Android OS is reporting critical RAM pressure. Close non-essential background processes or restrict high-drain apps.",
                    category = "MEMORY",
                    severity = "CRITICAL",
                    actionText = "Boost Memory",
                    actionRoute = "dashboard"
                )
            )
        } else if (memoryUsage.usedPercentage > 80) {
            recommendations.add(
                RecommendationItem(
                    id = "memory_warning",
                    title = "High RAM usage detected",
                    description = "RAM usage is at ${memoryUsage.usedPercentage}%. Background activities are causing memory pressure, which might slow down your foreground apps.",
                    category = "MEMORY",
                    severity = "WARNING",
                    actionText = "Close Heavy Apps",
                    actionRoute = "apps"
                )
            )
        }

        // 3. Battery checks
        if (batteryStatus.temperatureCelsius > 42.0f) {
            recommendations.add(
                RecommendationItem(
                    id = "battery_overheat",
                    title = "Battery is overheating!",
                    description = "The battery temperature is at a critical ${batteryStatus.temperatureCelsius}°C. Stop charging, exit high-performance games, and cool the device.",
                    category = "BATTERY",
                    severity = "CRITICAL",
                    actionText = "View Diagnostics",
                    actionRoute = "dashboard"
                )
            )
        } else if (batteryStatus.levelPercent < 20 && !batteryStatus.isCharging) {
            recommendations.add(
                RecommendationItem(
                    id = "battery_low",
                    title = "Battery is low",
                    description = "Battery level is at ${batteryStatus.levelPercent}%. Enable Battery Saver mode and restrict background location trackers to prolong runtime.",
                    category = "BATTERY",
                    severity = "WARNING",
                    actionText = "Battery Optimizer",
                    actionRoute = "dashboard"
                )
            )
        }

        // 4. CPU checks
        if (cpuStatus.totalUsagePercentage > 75) {
            recommendations.add(
                RecommendationItem(
                    id = "cpu_warning",
                    title = "Spike in CPU load detected",
                    description = "CPU usage is elevated at ${cpuStatus.totalUsagePercentage}%. A heavy background operation is running. Limit background app counts.",
                    category = "CPU",
                    severity = "WARNING",
                    actionText = "Monitor CPU Cores",
                    actionRoute = "dashboard"
                )
            )
        }

        // 5. Apps checks
        val highDrainApps = installedApps.filter { it.estimatedBatteryDrainPercent > 8.0f }
        if (highDrainApps.isNotEmpty()) {
            recommendations.add(
                RecommendationItem(
                    id = "apps_drain_warning",
                    title = "High-drain apps detected",
                    description = "Found ${highDrainApps.size} apps with excessive background permissions (like fine location/camera) causing battery drain. Review and revoke access.",
                    category = "NOTIFICATIONS",
                    severity = "WARNING",
                    actionText = "Review App Permissions",
                    actionRoute = "apps"
                )
            )
        }

        // Add a general healthy advice if all metrics are perfect
        if (recommendations.isEmpty()) {
            recommendations.add(
                RecommendationItem(
                    id = "general_perfect",
                    title = "All systems are running great",
                    description = "Your device storage, RAM, battery temperature, and processor load are in optimal condition! Keep it up.",
                    category = "CPU",
                    severity = "INFO",
                    actionText = "View Analytics",
                    actionRoute = "analyzer"
                )
            )
        }

        return recommendations
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
