package com.example.core.event

import com.example.core.model.StorageItem

sealed class AppEvent {
    data class StorageChanged(val freeBytes: Long, val totalBytes: Long) : AppEvent()
    data class BatteryChanged(val levelPercent: Int, val isCharging: Boolean, val health: String) : AppEvent()
    data class PackageInstalled(val packageName: String) : AppEvent()
    data class PackageRemoved(val packageName: String) : AppEvent()
    object ChargingStarted : AppEvent()
    object ChargingStopped : AppEvent()
    object LowStorage : AppEvent()
    object LowMemory : AppEvent()
    data class CleanupCompleted(val bytesFreed: Long, val filesDeleted: Int) : AppEvent()
}
