package com.example.core.model

import android.graphics.drawable.Drawable

data class AppInfoItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val apkSize: Long,
    val estimatedBatteryDrainPercent: Float,
    val installDate: Long,
    val lastUpdateDate: Long,
    val permissions: List<String>,
    val launchFrequencyIndex: Int // 1: Low, 2: Med, 3: High
)
