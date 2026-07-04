package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "optimization_logs")
data class OptimizationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "JUNK_CLEAN", "MEMORY_BOOST", "BATTERY_OPTIMIZE"
    val bytesFreed: Long,
    val description: String
)
