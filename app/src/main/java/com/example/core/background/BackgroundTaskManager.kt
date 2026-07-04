package com.example.core.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BackgroundTaskManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleAllTasks() {
        scheduleDailyScan()
        scheduleWeeklyReportJob()
    }

    private fun scheduleDailyScan() {
        val dailyScanRequest = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            24, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "SmartBoostDailyScan",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyScanRequest
        )
    }

    private fun scheduleWeeklyReportJob() {
        val weeklyReportRequest = PeriodicWorkRequestBuilder<ScheduledScanWorker>(
            7, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "SmartBoostWeeklyReport",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyReportRequest
        )
    }

    fun cancelAllTasks() {
        workManager.cancelAllWork()
    }
}
