package com.sj010.ghid.domain.model

import java.util.Locale

/**
 * Policy for scoping Most Used Combos and Payloads either by:
 * - [UsageTrackingScope.DEVICE_TYPE]: Grouped across all devices sharing the same operating system (e.g. all Macs)
 * - [UsageTrackingScope.DEVICE_NAME]: Isolated per distinct Bluetooth device name (e.g. "Mac A" vs "Mac B")
 */
object UsageScopingPolicy {

    const val KEY_GLOBAL_PREFIX = "usage:"
    const val KEY_TYPE_PREFIX = "usage_type:"
    const val KEY_DEVICE_PREFIX = "usage_device:"

    fun normalizeName(name: String): String = name.trim().lowercase(Locale.ROOT)

    fun normalizeDeviceName(deviceName: String?): String {
        if (deviceName.isNullOrBlank()) return ""
        return deviceName.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
    }

    fun buildGlobalKey(normalizedName: String): String = "$KEY_GLOBAL_PREFIX$normalizedName"

    fun buildTypeKey(os: TargetOs, normalizedName: String): String =
        "$KEY_TYPE_PREFIX${os.name.lowercase(Locale.ROOT)}:$normalizedName"

    fun buildDeviceKey(normalizedDeviceName: String, normalizedName: String): String =
        "$KEY_DEVICE_PREFIX$normalizedDeviceName:$normalizedName"

    fun resolveCounts(
        entries: Map<String, Int>,
        scope: UsageTrackingScope,
        os: TargetOs = TargetOs.UNKNOWN,
        deviceName: String? = null
    ): Map<String, Int> {
        return when (scope) {
            UsageTrackingScope.DEVICE_NAME -> {
                val normDevice = normalizeDeviceName(deviceName)
                if (normDevice.isNotEmpty()) {
                    val prefix = "$KEY_DEVICE_PREFIX$normDevice:"
                    entries.filterKeys { it.startsWith(prefix) }
                        .mapKeys { it.key.removePrefix(prefix) }
                } else {
                    resolveGlobalCounts(entries)
                }
            }
            UsageTrackingScope.DEVICE_TYPE -> {
                if (os.isKnown) {
                    val prefix = "$KEY_TYPE_PREFIX${os.name.lowercase(Locale.ROOT)}:"
                    val osCounts = entries.filterKeys { it.startsWith(prefix) }
                        .mapKeys { it.key.removePrefix(prefix) }
                    if (osCounts.isNotEmpty()) {
                        osCounts
                    } else {
                        // Fallback to global counts so legacy data prior to scoping isn't lost
                        resolveGlobalCounts(entries)
                    }
                } else {
                    resolveGlobalCounts(entries)
                }
            }
        }
    }

    fun resolveGlobalCounts(entries: Map<String, Int>): Map<String, Int> =
        entries.filterKeys { it.startsWith(KEY_GLOBAL_PREFIX) && !it.startsWith(KEY_TYPE_PREFIX) && !it.startsWith(KEY_DEVICE_PREFIX) }
            .mapKeys { it.key.removePrefix(KEY_GLOBAL_PREFIX) }
}
