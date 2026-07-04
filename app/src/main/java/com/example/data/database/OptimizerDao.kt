package com.example.data.database

import androidx.room.*
import com.example.data.model.OptimizationLog
import com.example.data.model.DeviceScoreLog
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizerDao {
    @Query("SELECT * FROM optimization_logs ORDER BY timestamp DESC")
    fun getAllOptimizationLogs(): Flow<List<OptimizationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationLog(log: OptimizationLog)

    @Query("DELETE FROM optimization_logs")
    suspend fun clearAllOptimizationLogs()

    @Query("SELECT * FROM device_score_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentDeviceScores(): Flow<List<DeviceScoreLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceScoreLog(log: DeviceScoreLog)
}
