package com.example.core.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.model.OptimizationLog
import com.example.core.model.DeviceScoreLog
import com.example.core.monitor.SystemMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.optimizerDao()

            // Perform system diagnostics update
            val monitor = SystemMonitor(applicationContext)
            monitor.forceRefresh()

            val storage = monitor.storageState.value
            val memory = monitor.memoryState.value
            val battery = monitor.batteryState.value
            val cpu = monitor.cpuState.value

            // Insert a scheduled run entry in the Room DB
            dao.insertOptimizationLog(
                OptimizationLog(
                    type = "SCHEDULED_SCAN",
                    bytesFreed = 0L,
                    description = "Automated system background diagnostics completed successfully."
                )
            )

            // Re-calculate and insert latest Device Score
            val storageScore = (storage.freeBytes.toFloat() / (storage.totalBytes.coerceAtLeast(1L)).toFloat() * 100).toInt().coerceIn(0, 100)
            val memoryScore = (100 - memory.usedPercentage).coerceIn(0, 100)
            val batteryScore = if (battery.temperatureCelsius > 40f) 50 else 90
            val cpuScore = (100 - cpu.totalUsagePercentage).coerceIn(0, 100)
            val overall = ((storageScore + memoryScore + batteryScore + cpuScore) / 4)

            dao.insertDeviceScoreLog(
                DeviceScoreLog(
                    overallScore = overall,
                    storageScore = storageScore,
                    memoryScore = memoryScore,
                    batteryScore = batteryScore,
                    cpuScore = cpuScore
                )
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
