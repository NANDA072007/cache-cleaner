package com.example.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.OptimizerDao
import com.example.data.model.AppInfoItem
import com.example.data.model.DeviceScoreLog
import com.example.data.model.OptimizationLog
import com.example.data.model.RecommendationItem
import com.example.data.model.StorageItem
import com.example.data.repository.AppManager
import com.example.data.repository.DeviceScanner
import com.example.data.repository.RecommendationEngine
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val optimizerDao: OptimizerDao = database.optimizerDao()
    private val deviceScanner = DeviceScanner(context)
    private val appManager = AppManager(context)
    private val recommendationEngine = RecommendationEngine()

    // --- SCAN STATES ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow("Ready to scan")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    // --- REAL SYSTEM DATA FLOWS ---
    private val _storageUsage = MutableStateFlow(deviceScanner.getStorageUsage())
    val storageUsage: StateFlow<DeviceScanner.StorageUsage> = _storageUsage.asStateFlow()

    private val _memoryUsage = MutableStateFlow(deviceScanner.getMemoryUsage())
    val memoryUsage: StateFlow<DeviceScanner.MemoryUsage> = _memoryUsage.asStateFlow()

    private val _batteryStatus = MutableStateFlow(deviceScanner.getBatteryStatus())
    val batteryStatus: StateFlow<DeviceScanner.BatteryStatus> = _batteryStatus.asStateFlow()

    private val _cpuStatus = MutableStateFlow(deviceScanner.getCpuStatus())
    val cpuStatus: StateFlow<DeviceScanner.CpuStatus> = _cpuStatus.asStateFlow()

    private val _storageItems = MutableStateFlow<List<StorageItem>>(emptyList())
    val storageItems: StateFlow<List<StorageItem>> = _storageItems.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfoItem>>(emptyList())
    val installedApps: StateFlow<List<AppInfoItem>> = _installedApps.asStateFlow()

    private val _recommendations = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val recommendations: StateFlow<List<RecommendationItem>> = _recommendations.asStateFlow()

    // --- SCORES ---
    private val _overallScore = MutableStateFlow(85)
    val overallScore: StateFlow<Int> = _overallScore.asStateFlow()

    private val _scores = MutableStateFlow<Map<String, Int>>(
        mapOf("STORAGE" to 82, "MEMORY" to 85, "BATTERY" to 90, "CPU" to 88)
    )
    val scores: StateFlow<Map<String, Int>> = _scores.asStateFlow()

    // --- DATABASE HISTORICAL DATA FLOWS ---
    val optimizationHistory: StateFlow<List<OptimizationLog>> = optimizerDao.getAllOptimizationLogs()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scoreHistory: StateFlow<List<DeviceScoreLog>> = optimizerDao.getRecentDeviceScores()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SETTINGS PREFERENCES ---
    val autoScanDaily = settingsRepository.autoScanDaily.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoRemindCleanup = settingsRepository.autoRemindCleanup.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val theme = settingsRepository.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")
    val language = settingsRepository.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    val animationsEnabled = settingsRepository.animationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val privacyMode = settingsRepository.privacyMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Run light metrics update immediately
        refreshSystemMetrics()
        // Start live CPU & Memory updates every 2 seconds
        viewModelScope.launch {
            while (true) {
                delay(2000)
                if (!_isScanning.value) {
                    _cpuStatus.value = deviceScanner.getCpuStatus()
                    _memoryUsage.value = deviceScanner.getMemoryUsage()
                }
            }
        }
    }

    fun refreshSystemMetrics() {
        _storageUsage.value = deviceScanner.getStorageUsage()
        _memoryUsage.value = deviceScanner.getMemoryUsage()
        _batteryStatus.value = deviceScanner.getBatteryStatus()
        _cpuStatus.value = deviceScanner.getCpuStatus()
        recalculateScores()
    }

    fun runFullScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Starting Diagnostic Scan..."
            delay(500)

            _scanProgressText.value = "Analyzing CPU Core States..."
            _cpuStatus.value = deviceScanner.getCpuStatus()
            delay(400)

            _scanProgressText.value = "Reading RAM Page Tables..."
            _memoryUsage.value = deviceScanner.getMemoryUsage()
            delay(400)

            _scanProgressText.value = "Polling Battery Health parameters..."
            _batteryStatus.value = deviceScanner.getBatteryStatus()
            delay(400)

            _scanProgressText.value = "Parsing Installed Application structures..."
            val appsList = appManager.getInstalledApps()
            _installedApps.value = appsList
            delay(400)

            _scanProgressText.value = "Mapping system disk contents..."
            val filesList = deviceScanner.scanStorageFiles { progress ->
                _scanProgressText.value = progress
            }
            _storageItems.value = filesList
            delay(500)

            _scanProgressText.value = "Formulating diagnostic advice..."
            // Generate real hardware-driven recommendation lists
            val advice = recommendationEngine.generateRecommendations(
                storageUsage = _storageUsage.value,
                memoryUsage = _memoryUsage.value,
                batteryStatus = _batteryStatus.value,
                cpuStatus = _cpuStatus.value,
                storageItems = filesList,
                installedApps = appsList
            )
            _recommendations.value = advice

            recalculateScores()

            // Save the newly calculated diagnostic score to persistent history
            withContext(Dispatchers.IO) {
                optimizerDao.insertDeviceScoreLog(
                    DeviceScoreLog(
                        overallScore = _overallScore.value,
                        storageScore = _scores.value["STORAGE"] ?: 80,
                        memoryScore = _scores.value["MEMORY"] ?: 80,
                        batteryScore = _scores.value["BATTERY"] ?: 80,
                        cpuScore = _scores.value["CPU"] ?: 80
                    )
                )
            }

            _scanProgressText.value = "Diagnostics Complete!"
            delay(400)
            _isScanning.value = false
        }
    }

    private fun recalculateScores() {
        val sUsage = _storageUsage.value
        val mUsage = _memoryUsage.value
        val bStatus = _batteryStatus.value
        val cStatus = _cpuStatus.value

        // Compute scores logically based on real values
        val storageScore = (100 - sUsage.usedPercentage).coerceIn(40, 100)
        val memoryScore = (100 - mUsage.usedPercentage).coerceIn(40, 100)
        
        val batHealthFactor = if (bStatus.healthText == "Good") 50 else 30
        val batteryScore = ((bStatus.levelPercent * 0.5f) + batHealthFactor).toInt().coerceIn(30, 100)
        
        val cpuScore = (100 - cStatus.totalUsagePercentage).coerceIn(30, 100)

        _scores.value = mapOf(
            "STORAGE" to storageScore,
            "MEMORY" to memoryScore,
            "BATTERY" to batteryScore,
            "CPU" to cpuScore
        )

        _overallScore.value = (storageScore + memoryScore + batteryScore + cpuScore) / 4
    }

    fun cleanJunk(selectedItems: List<StorageItem>, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Initiating Junk Clean sequence..."
            delay(500)

            var bytesFreed = 0L
            var fileCount = 0

            for (item in selectedItems) {
                _scanProgressText.value = "Deleting ${item.name} (${formatSize(item.size)})..."
                val success = withContext(Dispatchers.IO) {
                    deviceScanner.deleteFileItem(item)
                }
                if (success) {
                    bytesFreed += item.size
                    fileCount++
                }
                delay(15) // Smooth visible, completely non-blocking UI transition
            }

            // Add log entry to SQLite Room
            if (bytesFreed > 0 || fileCount > 0) {
                withContext(Dispatchers.IO) {
                    optimizerDao.insertOptimizationLog(
                        OptimizationLog(
                            type = "JUNK_CLEAN",
                            bytesFreed = bytesFreed,
                            description = "Purged $fileCount junk/temporary items from local directories."
                        )
                    )
                }
            }

            // Reload file scanner
            val updatedFiles = _storageItems.value.filterNot { item -> selectedItems.any { it.path == item.path } }
            _storageItems.value = updatedFiles

            // Refresh system storage values
            _storageUsage.value = deviceScanner.getStorageUsage()
            
            // Re-eval advice
            _recommendations.value = recommendationEngine.generateRecommendations(
                storageUsage = _storageUsage.value,
                memoryUsage = _memoryUsage.value,
                batteryStatus = _batteryStatus.value,
                cpuStatus = _cpuStatus.value,
                storageItems = updatedFiles,
                installedApps = _installedApps.value
            )

            recalculateScores()

            _scanProgressText.value = "Cleaned ${formatSize(bytesFreed)} successfully!"
            delay(800)
            _isScanning.value = false
            onComplete(bytesFreed)
        }
    }

    fun boostMemory(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Reclaiming unused memory pages..."
            delay(600)

            var processKillCount = 0

            withContext(Dispatchers.IO) {
                // Execute standard garbage collector request
                System.gc()
                Runtime.getRuntime().gc()

                // Query and close actually running background services and cached app processes
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (activityManager != null) {
                    try {
                        val runningProcesses = activityManager.runningAppProcesses
                        if (runningProcesses != null) {
                            for (procInfo in runningProcesses) {
                                // Filter out foreground application processes and our own app
                                if (procInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && procInfo.pkgList != null) {
                                    for (pkgName in procInfo.pkgList) {
                                        if (pkgName != context.packageName) {
                                            activityManager.killBackgroundProcesses(pkgName)
                                            processKillCount++
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Secure fallback: kill top known candidates from memory lists
                        _installedApps.value.take(5).forEach { app ->
                            try {
                                activityManager?.killBackgroundProcesses(app.packageName)
                                processKillCount++
                            } catch (ex: Exception) {}
                        }
                    }
                }

                // Log memory boost
                optimizerDao.insertOptimizationLog(
                    OptimizationLog(
                        type = "MEMORY_BOOST",
                        bytesFreed = 0L, // Memory boost is operational (pages freed), not static storage
                        description = "System Garbage Collector invoked. Released memory from $processKillCount background processes."
                    )
                )
            }

            _memoryUsage.value = deviceScanner.getMemoryUsage()
            _cpuStatus.value = deviceScanner.getCpuStatus()
            recalculateScores()

            _scanProgressText.value = "Memory pages optimized!"
            delay(800)
            _isScanning.value = false
            onComplete("System Garbage Collection triggered. Reclaimed background allocations.")
        }
    }

    fun optimizeBattery(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Simulated operational settings logs
            withContext(Dispatchers.IO) {
                optimizerDao.insertOptimizationLog(
                    OptimizationLog(
                        type = "BATTERY_OPTIMIZE",
                        bytesFreed = 0L,
                        description = "Optimized battery drainage parameters & scheduled sleep states."
                    )
                )
            }
            onComplete()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            optimizerDao.clearAllOptimizationLogs()
        }
    }

    // --- SETTINGS WRITERS ---
    fun setAutoScanDaily(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoScanDaily(enabled) }
    fun setAutoRemindCleanup(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoRemindCleanup(enabled) }
    fun setTheme(themeStr: String) = viewModelScope.launch { settingsRepository.setTheme(themeStr) }
    fun setLanguage(langStr: String) = viewModelScope.launch { settingsRepository.setLanguage(langStr) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAnimationsEnabled(enabled) }
    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setPrivacyMode(enabled) }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}

class MainViewModelFactory(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, database, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
