package com.example.data.model

data class AppInfoItem(
    val packageName: String,
    val label: String,
    val apkSize: Long,
    val installDate: Long,
    val isSystemApp: Boolean,
    val permissions: List<String>,
    val launchFrequencyIndex: Int, // 1: Low, 2: Medium, 3: High
    val estimatedBatteryDrainPercent: Float // estimated daily drainage
)
