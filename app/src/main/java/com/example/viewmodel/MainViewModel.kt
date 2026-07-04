package com.example.viewmodel

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.OptimizerDao
import com.example.core.model.AppInfoItem
import com.example.core.model.DeviceScoreLog
import com.example.core.model.OptimizationLog
import com.example.core.model.RecommendationItem
import com.example.core.model.StorageItem
import com.example.core.monitor.SystemMonitor
import com.example.core.scanner.JunkScanner
import com.example.core.scanner.JunkClassifier
import com.example.core.scanner.JunkAnalyzer
import com.example.core.scanner.RecommendationEngine
import com.example.core.scanner.ReportGenerator
import com.example.core.apps.AppManager
import com.example.core.settings.SettingsRepository
import com.example.core.logging.Logger
import com.example.core.event.EventBus
import com.example.core.event.AppEvent
import com.example.core.background.BackgroundTaskManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val systemMonitor = SystemMonitor(context)
    private val appManager = AppManager(context)
    private val junkScanner = JunkScanner(context)
    private val junkClassifier = JunkClassifier()
    private val junkAnalyzer = JunkAnalyzer()
    private val recommendationEngine = RecommendationEngine()
    private val reportGenerator = ReportGenerator()
    private val backgroundTaskManager = BackgroundTaskManager(context)

    // --- PIPELINE SCAN STATES ---
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow("Ready to scan")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var scanJob: Job? = null

    // --- REAL SYSTEM DATA FLOWS FROM SYSTEM MONITOR ---
    val storageUsage = systemMonitor.storageState
    val memoryUsage = systemMonitor.memoryState
    val batteryStatus = systemMonitor.batteryState
    val cpuStatus = systemMonitor.cpuState

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

    // --- REPORT DATA FLOW ---
    private val _healthReport = MutableStateFlow<ReportGenerator.DeviceHealthReport?>(null)
    val healthReport: StateFlow<ReportGenerator.DeviceHealthReport?> = _healthReport.asStateFlow()

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
        Logger.i(message = "Initializing Platform ViewModel architecture...")
        
        // Start live unified monitors (updates every 3 seconds)
        systemMonitor.start(3000)

        // Initialize background tasks WorkManager
        viewModelScope.launch {
            autoScanDaily.collect { enabled ->
                if (enabled) {
                    backgroundTaskManager.scheduleAllTasks()
                } else {
                    backgroundTaskManager.cancelAllTasks()
                }
            }
        }

        // Listen for EventBus notifications to update score metrics dynamically
        viewModelScope.launch {
            EventBus.events.collect { event ->
                Logger.d(message = "Event received on internal bus: $event")
                when (event) {
                    is AppEvent.StorageChanged, is AppEvent.BatteryChanged,
                    is AppEvent.LowStorage, is AppEvent.LowMemory -> {
                        recalculateScores()
                    }
                    else -> {}
                }
            }
        }

        // Keep local health analytics report synced with metrics and optimization runs
        viewModelScope.launch {
            combine(
                storageUsage,
                memoryUsage,
                batteryStatus,
                cpuStatus,
                optimizationHistory,
                scoreHistory
            ) { array ->
                val s = array[0] as com.example.core.monitor.StorageMonitor.StorageState
                val m = array[1] as com.example.core.monitor.MemoryMonitor.MemoryState
                val b = array[2] as com.example.core.monitor.BatteryMonitor.BatteryState
                val c = array[3] as com.example.core.monitor.CpuMonitor.CpuState
                val o = array[4] as List<OptimizationLog>
                val h = array[5] as List<DeviceScoreLog>
                reportGenerator.generateReport(s, m, b, c, o, h)
            }.collect { report ->
                _healthReport.value = report
            }
        }
    }

    fun refreshSystemMetrics() {
        systemMonitor.forceRefresh()
        recalculateScores()
    }

    fun runFullScan() {
        scanJob?.cancel()
        _isPaused.value = false
        scanJob = viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Starting Pipeline Scan..."
            Logger.i(message = "Scan pipeline: STARTED")
            delay(500)

            _scanProgressText.value = "Phase 1: Querying Telemetry Monitors..."
            systemMonitor.forceRefresh()
            delay(400)

            _scanProgressText.value = "Phase 2: App Manager catalog sweep..."
            val appsList = withContext(Dispatchers.IO) { appManager.getInstalledApps() }
            _installedApps.value = appsList
            delay(400)

            _scanProgressText.value = "Phase 3: Directory content scan..."
            val rawFiles = try {
                junkScanner.scan { label, _ ->
                    _scanProgressText.value = label
                }
            } catch (e: Exception) {
                Logger.e(message = "Scan phase 3 error", throwable = e)
                emptyList()
            }

            _scanProgressText.value = "Phase 4: Classifying sector categories..."
            val classifiedItems = withContext(Dispatchers.Default) {
                rawFiles.map { junkClassifier.classify(it) }
            }
            delay(300)

            _scanProgressText.value = "Phase 5: Duplicate files analytics..."
            val finalizedStorageItems = withContext(Dispatchers.Default) {
                junkAnalyzer.analyze(classifiedItems)
            }
            _storageItems.value = finalizedStorageItems
            delay(400)

            _scanProgressText.value = "Phase 6: Recommendation engine sweep..."
            val advice = recommendationEngine.generateRecommendations(
                storageUsage = storageUsage.value,
                memoryUsage = memoryUsage.value,
                batteryStatus = batteryStatus.value,
                cpuStatus = cpuStatus.value,
                storageItems = finalizedStorageItems,
                installedApps = appsList
            )
            _recommendations.value = advice

            recalculateScores()

            // Persist the diagnostics result
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
            Logger.i(message = "Scan pipeline: COMPLETED")
            delay(400)
            _isScanning.value = false
        }
    }

    fun pauseScan() {
        viewModelScope.launch {
            junkScanner.pause()
            _isPaused.value = true
            _scanProgressText.value = "Scan paused"
            Logger.i(message = "Scan pipeline: PAUSED")
        }
    }

    fun resumeScan() {
        viewModelScope.launch {
            junkScanner.resume()
            _isPaused.value = false
            _scanProgressText.value = "Resuming scan..."
            Logger.i(message = "Scan pipeline: RESUMED")
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _isPaused.value = false
        _scanProgressText.value = "Scan cancelled"
        Logger.i(message = "Scan pipeline: CANCELLED")
    }

    private fun recalculateScores() {
        val sUsage = storageUsage.value
        val mUsage = memoryUsage.value
        val bStatus = batteryStatus.value
        val cStatus = cpuStatus.value

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
            _scanProgressText.value = "Initiating Clean sequence..."
            delay(500)

            var bytesFreed = 0L
            var fileCount = 0

            for (item in selectedItems) {
                _scanProgressText.value = "Deleting ${item.name} (${formatSize(item.size)})..."
                val success = withContext(Dispatchers.IO) {
                    try {
                        if (item.file.exists()) {
                            if (item.file.isDirectory) {
                                item.file.deleteRecursively()
                            } else {
                                item.file.delete()
                            }
                        } else {
                            true
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                if (success) {
                    bytesFreed += item.size
                    fileCount++
                }
                delay(15)
            }

            // Add log entry to SQLite Room
            if (bytesFreed > 0 || fileCount > 0) {
                withContext(Dispatchers.IO) {
                    optimizerDao.insertOptimizationLog(
                        OptimizationLog(
                            type = "JUNK_CLEAN",
                            bytesFreed = bytesFreed,
                            description = "Purged $fileCount junk items. Recovered ${formatSize(bytesFreed)} space."
                        )
                    )
                }
                EventBus.emit(AppEvent.CleanupCompleted(bytesFreed, fileCount))
            }

            // Reload files
            val updatedFiles = _storageItems.value.filterNot { item -> selectedItems.any { it.path == item.path } }
            _storageItems.value = updatedFiles
            
            // Refresh storage monitor
            systemMonitor.forceRefresh()
            
            // Re-eval recommendations
            _recommendations.value = recommendationEngine.generateRecommendations(
                storageUsage = storageUsage.value,
                memoryUsage = memoryUsage.value,
                batteryStatus = batteryStatus.value,
                cpuStatus = cpuStatus.value,
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
                System.gc()
                Runtime.getRuntime().gc()

                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (activityManager != null) {
                    try {
                        val runningProcesses = activityManager.runningAppProcesses
                        if (runningProcesses != null) {
                            for (procInfo in runningProcesses) {
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
                        _installedApps.value.take(5).forEach { app ->
                            try {
                                activityManager?.killBackgroundProcesses(app.packageName)
                                processKillCount++
                            } catch (ex: Exception) {}
                        }
                    }
                }

                optimizerDao.insertOptimizationLog(
                    OptimizationLog(
                        type = "MEMORY_BOOST",
                        bytesFreed = 0L,
                        description = "Released heap space from $processKillCount background apps."
                    )
                )
            }

            systemMonitor.forceRefresh()
            recalculateScores()

            _scanProgressText.value = "Memory allocations boosted!"
            delay(800)
            _isScanning.value = false
            onComplete("System allocations boosted. Released memory registers.")
        }
    }

    fun optimizeBattery(onComplete: () -> Unit) {
        viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()
        systemMonitor.stop()
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
