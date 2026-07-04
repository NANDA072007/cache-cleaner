package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_score_logs")
data class DeviceScoreLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val overallScore: Int,
    val storageScore: Int,
    val memoryScore: Int,
    val batteryScore: Int,
    val cpuScore: Int
)
