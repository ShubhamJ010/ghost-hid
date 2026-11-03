package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.model.UsageScopingPolicy
import com.sj010.ghid.domain.model.UsageTrackingScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ComboUsageRepository(context: Context) {

    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _allUsage = MutableStateFlow(readAllUsage())
    val allUsage: StateFlow<Map<String, Int>> = _allUsage.asStateFlow()

    // Backward-compatible global counts flow
    private val _useCounts = MutableStateFlow(UsageScopingPolicy.resolveGlobalCounts(_allUsage.value))
    val useCounts: StateFlow<Map<String, Int>> = _useCounts.asStateFlow()

    fun recordUse(name: String, os: TargetOs? = null, deviceName: String? = null) {
        val normalizedName = UsageScopingPolicy.normalizeName(name)
        if (normalizedName.isEmpty()) return

        synchronized(lock) {
            val currentMap = _allUsage.value.toMutableMap()
            val editor = preferences.edit()

            // 1. Always record global usage
            val globalKey = UsageScopingPolicy.buildGlobalKey(normalizedName)
            val nextGlobal = (currentMap[globalKey] ?: 0) + 1
            editor.putInt(globalKey, nextGlobal)
            currentMap[globalKey] = nextGlobal

            // 2. Record device type (OS) usage if host OS is known
            if (os != null && os.isKnown) {
                val osKey = UsageScopingPolicy.buildTypeKey(os, normalizedName)
                val nextOs = (currentMap[osKey] ?: 0) + 1
                editor.putInt(osKey, nextOs)
                currentMap[osKey] = nextOs
            }

            // 3. Record device name usage if device name is provided
            val normDevice = UsageScopingPolicy.normalizeDeviceName(deviceName)
            if (normDevice.isNotEmpty()) {
                val deviceKey = UsageScopingPolicy.buildDeviceKey(normDevice, normalizedName)
                val nextDevice = (currentMap[deviceKey] ?: 0) + 1
                editor.putInt(deviceKey, nextDevice)
                currentMap[deviceKey] = nextDevice
            }

            editor.apply()
            _allUsage.value = currentMap
            _useCounts.value = UsageScopingPolicy.resolveGlobalCounts(currentMap)
        }
    }

    /**
     * Resolves effective combo counts according to the chosen [scope].
     *
     * - Under [UsageTrackingScope.DEVICE_NAME]: Returns counts specifically for [deviceName].
     *   If [deviceName] is blank or null, falls back to global counts.
     * - Under [UsageTrackingScope.DEVICE_TYPE]: Returns counts for [os] (shared by all devices with that OS).
     *   If [os] is unknown, falls back to global counts.
     */
    fun resolveCounts(
        scope: UsageTrackingScope,
        os: TargetOs = TargetOs.UNKNOWN,
        deviceName: String? = null
    ): Map<String, Int> {
        return UsageScopingPolicy.resolveCounts(
            entries = _allUsage.value,
            scope = scope,
            os = os,
            deviceName = deviceName
        )
    }

    private fun readAllUsage(): Map<String, Int> = preferences.all
        .mapNotNull { (key, value) ->
            if (value is Int) key to value else null
        }
        .toMap()

    private companion object {
        const val FILE_NAME = "combo_usage"
    }
}
