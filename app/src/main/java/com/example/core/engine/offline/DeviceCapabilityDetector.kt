package com.example.core.engine.offline

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlin.math.roundToInt

data class DeviceCapability(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val usedRamPercent: Int,
    val cpuCores: Int,
    val thermalStatus: String,
    val batteryTemperatureC: Float,
    val recommendedTier: ModelTier,
    val isMemorySafeForInference: Boolean,
    val safetyWarning: String?
)

enum class ModelTier {
    LOW_QUANTIZED,   // <= 1.5B (TinyLlama, Qwen-Coder-1.5B)
    BALANCED_MEDIUM, // 2B - 3.8B (Gemma-2B, Phi-3-Mini)
    HIGH_CAPACITY    // 7B - 8B (Mistral-7B, Llama-3-8B)
}

enum class PerformanceProfile(val displayName: String, val threads: Int, val maxTokens: Int) {
    PERFORMANCE("Maximum Performance", 6, 4096),
    BALANCED("Balanced Workstation", 4, 2048),
    COOL_MODE("Cool & Thermal Throttled", 2, 1024),
    BATTERY_SAVER("Battery Saver", 1, 512)
}

object DeviceCapabilityDetector {

    fun getDeviceCapability(context: Context): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availableRamMb = memoryInfo.availMem / (1024 * 1024)
        val usedRamMb = totalRamMb - availableRamMb
        val usedPercent = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb) * 100).roundToInt() else 50
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Battery temperature
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = tempRaw / 10.0f

        // Thermal status
        val thermalStatus = getThermalStatus(context, batteryTempC)

        // Tier evaluation
        val recommendedTier = when {
            availableRamMb < 2500 -> ModelTier.LOW_QUANTIZED
            availableRamMb in 2500..5500 -> ModelTier.BALANCED_MEDIUM
            else -> ModelTier.HIGH_CAPACITY
        }

        val isSafe = availableRamMb > 600 && !memoryInfo.lowMemory
        val warning = when {
            memoryInfo.lowMemory || availableRamMb < 600 -> "Critical RAM level! Unload background tasks to avoid crash."
            thermalStatus == "Critical" || batteryTempC > 46f -> "High thermal status detected. Inference throttled to Cool Mode."
            else -> null
        }

        return DeviceCapability(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            usedRamPercent = usedPercent,
            cpuCores = cpuCores,
            thermalStatus = thermalStatus,
            batteryTemperatureC = batteryTempC,
            recommendedTier = recommendedTier,
            isMemorySafeForInference = isSafe,
            safetyWarning = warning
        )
    }

    private fun getThermalStatus(context: Context, batteryTempC: Float): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val status = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            return when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Heat"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Heat"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> if (batteryTempC > 42f) "Warm" else "Normal"
            }
        }
        return if (batteryTempC > 43f) "Moderate Heat" else if (batteryTempC > 47f) "Severe Heat" else "Normal"
    }
}
