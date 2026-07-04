package com.example.core.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.core.event.AppEvent
import com.example.core.event.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(private val context: Context) {
    data class BatteryState(
        val levelPercent: Int = 50,
        val statusText: String = "Unknown",
        val healthText: String = "Good",
        val temperatureCelsius: Float = 25.0f,
        val voltageMilliVolts: Int = 3800,
        val designCapacityMah: Int = 4000,
        val isCharging: Boolean = false
    )

    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    private var lastChargingState: Boolean? = null

    init {
        update()
    }

    fun update() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatusIntent = context.registerReceiver(null, filter) ?: return

            val level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 50

            val status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            val health = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Good"
            }

            val temp = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            val voltage = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

            // Estimate physical capacity from profiles
            val capacity = getBatteryCapacity(context)

            val newState = BatteryState(
                levelPercent = pct,
                statusText = statusStr,
                healthText = healthStr,
                temperatureCelsius = temp,
                voltageMilliVolts = voltage,
                designCapacityMah = capacity,
                isCharging = isCharging
            )
            _state.value = newState

            // Check if charging status flipped to trigger EventBus
            if (lastChargingState != isCharging) {
                if (lastChargingState != null) {
                    if (isCharging) {
                        EventBus.tryEmit(AppEvent.ChargingStarted)
                    } else {
                        EventBus.tryEmit(AppEvent.ChargingStopped)
                    }
                }
                lastChargingState = isCharging
            }

            EventBus.tryEmit(AppEvent.BatteryChanged(pct, isCharging, healthStr))
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun getBatteryCapacity(ctx: Context): Int {
        return try {
            val powerProfileClass = "com.android.internal.os.PowerProfile"
            val mPowerProfile = Class.forName(powerProfileClass)
                .getConstructor(Context::class.java)
                .newInstance(ctx)
            val batteryCapacity = Class.forName(powerProfileClass)
                .getMethod("getBatteryCapacity")
                .invoke(mPowerProfile) as Double
            batteryCapacity.toInt()
        } catch (e: Exception) {
            4500 // Solid default fallback
        }
    }
}
