package com.example.core.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.core.model.AppInfoItem
import java.io.File

class AppManager(private val context: Context) {

    fun getInstalledApps(): List<AppInfoItem> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppInfoItem>()

        for (appInfo in apps) {
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            val packageName = appInfo.packageName
            val appLabel = pm.getApplicationLabel(appInfo).toString()
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            // App size estimate
            val sizeBytes = try {
                val file = File(appInfo.sourceDir)
                if (file.exists()) file.length() else 1024L * 1024L * 15 // Standard fallback
            } catch (e: Exception) {
                1024L * 1024L * 15
            }

            // Retrieve install and update dates
            var installDate = System.currentTimeMillis()
            var updateDate = System.currentTimeMillis()
            try {
                val packageInfo = pm.getPackageInfo(packageName, 0)
                installDate = packageInfo.firstInstallTime
                updateDate = packageInfo.lastUpdateTime
            } catch (e: Exception) {
                // Ignore
            }

            // Retrieve requested permissions list
            val permissions = mutableListOf<String>()
            try {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                packageInfo.requestedPermissions?.forEach {
                    permissions.add(it)
                }
            } catch (e: Exception) {
                // Ignore
            }

            // Battery drain estimation based on permissions count & system profile
            val estimatedBattery = calculateBatteryDrainEstimate(permissions)

            // Frequency index estimate (1: Low, 2: Med, 3: High)
            val frequency = when {
                packageName.contains("com.google") || packageName.contains("android") -> 3
                permissions.contains(android.Manifest.permission.RECEIVE_BOOT_COMPLETED) -> 2
                else -> 1
            }

            result.add(
                AppInfoItem(
                    packageName = packageName,
                    label = appLabel,
                    icon = icon,
                    isSystemApp = isSystem,
                    apkSize = sizeBytes,
                    estimatedBatteryDrainPercent = estimatedBattery,
                    installDate = installDate,
                    lastUpdateDate = updateDate,
                    permissions = permissions,
                    launchFrequencyIndex = frequency
                )
            )
        }

        return result.sortedByDescending { it.apkSize }
    }

    private fun calculateBatteryDrainEstimate(permissions: List<String>): Float {
        var base = 1.2f
        if (permissions.contains(android.Manifest.permission.ACCESS_FINE_LOCATION)) base += 2.5f
        if (permissions.contains(android.Manifest.permission.CAMERA)) base += 1.8f
        if (permissions.contains(android.Manifest.permission.RECORD_AUDIO)) base += 1.5f
        if (permissions.contains(android.Manifest.permission.RECEIVE_BOOT_COMPLETED)) base += 1.2f
        return base
    }
}
