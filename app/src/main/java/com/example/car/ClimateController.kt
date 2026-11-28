package com.example.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.data.LauncherPreferencesRepository
import com.example.model.ClimateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ClimateController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferencesRepository: LauncherPreferencesRepository
) {
    companion object {
        const val MIN_TEMP_C = 16.0f
        const val MAX_TEMP_C = 32.0f
        const val STEP_TEMP_C = 0.5f

        // Standard Android Automotive VehiclePropertyIds
        private const val PROP_HVAC_TEMPERATURE_SET = 358614275
        private const val PROP_INSIDE_TEMPERATURE = 291504647
        private const val AREA_DRIVER_ZONE = 1
    }

    private val _state = MutableStateFlow(ClimateState())
    val state: StateFlow<ClimateState> = _state.asStateFlow()

    private var receiverRegistered = false
    private var carInstance: Any? = null
    private var carPropertyManager: Any? = null

    private val hvacReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val extras = intent?.extras ?: return
            val targetRaw = extractTempExtra(extras, "temp", "driverTemp", "leftTemp", "temperature", "setTemp")
            val cabinRaw = extractTempExtra(extras, "cabinTemp", "insideTemp", "ambientTemp")
            val acState = if (extras.containsKey("ac")) extras.getBoolean("ac", _state.value.isAcOn) else null

            if (targetRaw != null || cabinRaw != null || acState != null) {
                _state.update { current ->
                    current.copy(
                        targetTempC = targetRaw?.coerceIn(MIN_TEMP_C, MAX_TEMP_C) ?: current.targetTempC,
                        cabinTempC = cabinRaw ?: current.cabinTempC,
                        isAcOn = acState ?: current.isAcOn,
                        isHardwareSynced = true,
                        statusLabel = "Синхр. с климатом Adayo"
                    )
                }
                if (targetRaw != null) {
                    scope.launch {
                        preferencesRepository.setClimateTempC(targetRaw.coerceIn(MIN_TEMP_C, MAX_TEMP_C))
                    }
                }
                if (acState != null) {
                    scope.launch {
                        preferencesRepository.setClimateAcOn(acState)
                    }
                }
            }
        }
    }

    init {
        scope.launch {
            val saved = preferencesRepository.settingsFlow.first()
            _state.update { current ->
                current.copy(
                    targetTempC = snapToHalfDegree(saved.climateTempC),
                    isAcOn = saved.climateAcOn
                )
            }
        }
    }

    fun start() {
        registerHvacBroadcasts()
        tryConnectCarService()
    }

    fun stop() {
        unregisterHvacBroadcasts()
        disconnectCarService()
    }

    fun increaseTemperature() {
        setTemperature(_state.value.targetTempC + STEP_TEMP_C)
    }

    fun decreaseTemperature() {
        setTemperature(_state.value.targetTempC - STEP_TEMP_C)
    }

    fun setTemperature(rawTempC: Float) {
        val next = snapToHalfDegree(rawTempC).coerceIn(MIN_TEMP_C, MAX_TEMP_C)
        val synced = trySendTemperatureToCar(next)
        _state.update { current ->
            current.copy(
                targetTempC = next,
                isHardwareSynced = current.isHardwareSynced || synced,
                statusLabel = if (current.isHardwareSynced || synced) {
                    "Синхр. с климатом"
                } else {
                    "Салон • Haval M6"
                }
            )
        }
        scope.launch {
            preferencesRepository.setClimateTempC(next)
        }
    }

    fun toggleAc() {
        val next = !_state.value.isAcOn
        _state.update { it.copy(isAcOn = next) }
        scope.launch {
            preferencesRepository.setClimateAcOn(next)
        }
    }

    private fun tryConnectCarService() {
        if (carPropertyManager != null) return
        runCatching {
            val carClass = Class.forName("android.car.Car")
            val createCar = carClass.getMethod("createCar", Context::class.java)
            val car = createCar.invoke(null, context) ?: return@runCatching
            carInstance = car

            val isConnected = (carClass.getMethod("isConnected").invoke(car) as? Boolean) ?: true
            if (!isConnected) {
                runCatching { carClass.getMethod("connect").invoke(car) }
            }

            val propMgr = runCatching {
                carClass.getMethod("getCarManager", String::class.java).invoke(car, "property")
            }.getOrNull() ?: return@runCatching

            carPropertyManager = propMgr
            val mgrClass = propMgr.javaClass

            val currentSet = runCatching {
                mgrClass.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(propMgr, PROP_HVAC_TEMPERATURE_SET, AREA_DRIVER_ZONE) as? Float
            }.getOrNull()

            val insideTemp = runCatching {
                mgrClass.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    .invoke(propMgr, PROP_INSIDE_TEMPERATURE, 0) as? Float
            }.getOrNull()

            if (currentSet != null || insideTemp != null) {
                _state.update { current ->
                    current.copy(
                        targetTempC = currentSet?.takeIf { it in 10f..40f }?.let(::snapToHalfDegree)
                            ?: current.targetTempC,
                        cabinTempC = insideTemp?.takeIf { it in -40f..65f },
                        isHardwareSynced = true,
                        statusLabel = "Синхр. с CarService"
                    )
                }
            }
        }
    }

    private fun trySendTemperatureToCar(tempC: Float): Boolean {
        val propMgr = carPropertyManager
        if (propMgr != null) {
            val setOk = runCatching {
                propMgr.javaClass
                    .getMethod(
                        "setFloatProperty",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType
                    )
                    .invoke(propMgr, PROP_HVAC_TEMPERATURE_SET, AREA_DRIVER_ZONE, tempC)
            }.isSuccess
            if (setOk) return true
        }
        return false
    }

    private fun disconnectCarService() {
        val car = carInstance ?: return
        runCatching {
            val carClass = Class.forName("android.car.Car")
            carClass.getMethod("disconnect").invoke(car)
        }
        carInstance = null
        carPropertyManager = null
    }

    private fun registerHvacBroadcasts() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction("com.adayo.action.hvac.tempchange")
            addAction("com.adayo.hvac.TEMP_CHANGE")
            addAction("com.adayo.ac.action.AC_INFO")
            addAction("com.adayo.action.ac.info")
        }
        ContextCompat.registerReceiver(
            context,
            hvacReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    private fun unregisterHvacBroadcasts() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(hvacReceiver) }
        receiverRegistered = false
    }

    private fun extractTempExtra(extras: android.os.Bundle, vararg keys: String): Float? {
        for (key in keys) {
            if (!extras.containsKey(key)) continue
            @Suppress("DEPRECATION")
            val raw = extras.get(key)
            val parsed = when (raw) {
                is Float -> raw
                is Double -> raw.toFloat()
                is Int -> if (raw > 60) raw / 10f else raw.toFloat()
                is String -> raw.toFloatOrNull()
                else -> null
            }
            if (parsed != null && parsed in 10f..45f) {
                return snapToHalfDegree(parsed)
            }
        }
        return null
    }

    private fun snapToHalfDegree(tempC: Float): Float =
        ((tempC * 2f).roundToInt() / 2f).coerceIn(MIN_TEMP_C, MAX_TEMP_C)
}
