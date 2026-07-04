package com.example.core.monitor

import android.app.ActivityManager
import android.content.Context
import com.example.core.event.AppEvent
import com.example.core.event.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryMonitor(private val context: Context) {
    data class MemoryState(
        val totalRamBytes: Long = 0,
        val availableRamBytes: Long = 0,
        val usedRamBytes: Long = 0,
        val lowMemoryThresholdBytes: Long = 0,
        val isSystemLowMemory: Boolean = false,
        val usedPercentage: Int = 0
    )

    private val _state = MutableStateFlow(MemoryState())
    val state: StateFlow<MemoryState> = _state.asStateFlow()

    init {
        update()
    }

    fun update() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val total = memoryInfo.totalMem
            val avail = memoryInfo.availMem
            val threshold = memoryInfo.threshold
            val isLow = memoryInfo.lowMemory
            val used = total - avail
            val percentage = if (total > 0) ((used * 100) / total).toInt() else 0

            val newState = MemoryState(total, avail, used, threshold, isLow, percentage)
            _state.value = newState

            if (isLow) {
                EventBus.tryEmit(AppEvent.LowMemory)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
