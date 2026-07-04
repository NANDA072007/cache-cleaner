package com.example.core.monitor

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.core.event.AppEvent
import com.example.core.event.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StorageMonitor(private val context: Context) {
    data class StorageState(
        val totalBytes: Long = 0,
        val freeBytes: Long = 0,
        val usedBytes: Long = 0,
        val usedPercentage: Int = 0
    )

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    init {
        update()
    }

    fun update() {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = total - free
            val percentage = if (total > 0) ((used * 100) / total).toInt() else 0

            val newState = StorageState(total, free, used, percentage)
            _state.value = newState

            // Emit to global event bus
            EventBus.tryEmit(AppEvent.StorageChanged(free, total))

            // Check for low storage condition (< 10% free or < 1.5GB)
            val freeGb = free / (1024.0 * 1024.0 * 1024.0)
            if (percentage > 90 || freeGb < 1.5) {
                EventBus.tryEmit(AppEvent.LowStorage)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
