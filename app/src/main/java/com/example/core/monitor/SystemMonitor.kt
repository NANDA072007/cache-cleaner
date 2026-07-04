package com.example.core.monitor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SystemMonitor(context: Context) {
    private val storageMonitor = StorageMonitor(context)
    private val memoryMonitor = MemoryMonitor(context)
    private val batteryMonitor = BatteryMonitor(context)
    private val cpuMonitor = CpuMonitor(context)

    val storageState: StateFlow<StorageMonitor.StorageState> = storageMonitor.state
    val memoryState: StateFlow<MemoryMonitor.MemoryState> = memoryMonitor.state
    val batteryState: StateFlow<BatteryMonitor.BatteryState> = batteryMonitor.state
    val cpuState: StateFlow<CpuMonitor.CpuState> = cpuMonitor.state

    private var monitorJob: Job? = null
    private val monitorScope = CoroutineScope(Dispatchers.IO)

    fun start(intervalMs: Long = 3000) {
        if (monitorJob != null) return
        monitorJob = monitorScope.launch {
            while (true) {
                // Update all metrics on background threads
                storageMonitor.update()
                memoryMonitor.update()
                batteryMonitor.update()
                cpuMonitor.update()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    fun forceRefresh() {
        storageMonitor.update()
        memoryMonitor.update()
        batteryMonitor.update()
        cpuMonitor.update()
    }
}
