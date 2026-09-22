package com.example.core.engine.offline

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

enum class ThermalState(val displayName: String, val allowsInference: Boolean) {
    NORMAL("Normal", true),
    LIGHT("Light Warmth", true),
    MODERATE("Moderate Heat", true),
    SEVERE("Severe Heat - Throttling", true),
    CRITICAL("Critical - Inference Halted", false);

    val isThrottling: Boolean get() = this == SEVERE || this == CRITICAL
    val description: String get() = displayName
}

class ThermalManager(private val context: Context) {

    private val _currentThermalState = MutableStateFlow(ThermalState.NORMAL)
    val currentThermalState: StateFlow<ThermalState> = _currentThermalState.asStateFlow()

    private var thermalListener: Any? = null

    init {
        registerThermalListener()
    }

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val initialStatus = powerManager.currentThermalStatus
            _currentThermalState.value = mapPowerManagerStatus(initialStatus)

            val executor = Executors.newSingleThreadExecutor()
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                _currentThermalState.value = mapPowerManagerStatus(status)
            }
            powerManager.addThermalStatusListener(executor, listener)
            thermalListener = listener
        }
    }

    private fun mapPowerManagerStatus(status: Int): ThermalState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (status) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                else -> ThermalState.NORMAL
            }
        } else {
            ThermalState.NORMAL
        }
    }

    fun adaptThreadCount(requestedThreads: Int): Int {
        return when (_currentThermalState.value) {
            ThermalState.NORMAL -> requestedThreads
            ThermalState.LIGHT -> requestedThreads
            ThermalState.MODERATE -> requestedThreads.coerceAtMost(4)
            ThermalState.SEVERE -> requestedThreads.coerceAtMost(2)
            ThermalState.CRITICAL -> 0 // Halt
        }
    }

    fun shouldHaltInference(): Boolean {
        return _currentThermalState.value == ThermalState.CRITICAL
    }
}
