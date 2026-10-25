package com.sj010.ghid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.R
import com.sj010.ghid.data.DeviceOsCache
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.model.HidStep
import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.LogEntry
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.os.HostOsClassifier
import com.sj010.ghid.domain.parser.ActionSequenceParser
import com.sj010.ghid.domain.parser.KeyTokens
import com.sj010.ghid.domain.pointer.MouseButton
import com.sj010.ghid.domain.pointer.PointerMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class HidService : Service() {

    inner class LocalBinder : Binder() {
        val service: HidService get() = this@HidService
    }

    private val binder = LocalBinder()
    @Volatile
    private var isTornDown = false
    private val scope = CoroutineScope(Dispatchers.Default)
    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(),
        { runnable -> Thread(runnable, "ghid-worker").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var adapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var targetDevice: BluetoothDevice? = null

    private val _connectionState = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _targetDeviceState = MutableStateFlow<BluetoothDevice?>(null)
    val targetDeviceState: StateFlow<BluetoothDevice?> = _targetDeviceState.asStateFlow()

    private val _targetSelectionReady = MutableStateFlow(false)
    val targetSelectionReady: StateFlow<Boolean> = _targetSelectionReady.asStateFlow()

    private val _needsInitialTargetSelection = MutableStateFlow(false)
    val needsInitialTargetSelection: StateFlow<Boolean> = _needsInitialTargetSelection.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Passive host OS fingerprint (ADR-007). UNKNOWN until a host connects.
    private val _targetOs = MutableStateFlow(HostOsFingerprint.Unknown)
    val targetOs: StateFlow<HostOsFingerprint> = _targetOs.asStateFlow()

    // Effective target OS resolved across fingerprint, cache, OUI, and device name
    private val _effectiveTargetOs = MutableStateFlow(TargetOs.WINDOWS)
    val effectiveTargetOs: StateFlow<TargetOs> = _effectiveTargetOs.asStateFlow()

    private val deviceOsCache: DeviceOsCache
        get() = GhostHidApp.instance.deviceOsCache

    private var logFile: File? = null
    private var targetSwitchJob: Job? = null
    private var osDetectionJob: Job? = null
    private var ledHandshakeSeen = false

    companion object {
        var instance: HidService? = null
            private set

        const val TAG = "GHOST_HID"
        const val CHANNEL_ID = "ghost_hid_gateway"
        const val ACTION_CMD = "com.sj010.ghid.ACTION_CMD"
        const val ACTION_LOCK = "com.sj010.ghid.ACTION_LOCK"
        const val ACTION_TYPE = "com.sj010.ghid.ACTION_TYPE"
        private const val TARGET_PREFERENCES = "ghosthid_target"
        private const val KEY_LAST_CONNECTED_ADDRESS = "last_connected_address"
        private const val KEY_SELECTED_ADDRESS = "selected_address"
        private const val KEY_TARGET_SETUP_COMPLETE = "target_setup_complete"

        const val KEYBOARD_REPORT_ID = 1
        const val MOUSE_REPORT_ID = 2
        const val SYSTEM_REPORT_ID = 3
        const val CONSUMER_REPORT_ID = 4
        private val RELEASE_REPORT = ByteArray(4)

        // A click must be long enough for every host stack to sample it, and
        // the two cycles must sit inside the OS double-click window (~500 ms).
        // The gaps are generous because some Bluetooth stacks coalesce reports
        // aggressively when the radio is busy.
        private const val CLICK_PRESS_MS = 28L
        private const val CLICK_SETTLE_MS = 36L
        private const val CLICK_GAP_MS = 90L
        private const val WHEEL_SETTLE_MS = 14L
        private const val MOVE_CHUNK_GAP_MS = 6L
        private const val TARGET_SWITCH_SETTLE_MS = 350L

        private val HID_DESCRIPTOR = byteArrayOf(
            // Mouse collection (Report ID 2)
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), 0x02.toByte(), 0x09.toByte(), 0x01.toByte(), 0xA1.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(),
            0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(), 0x95.toByte(), 0x03.toByte(),
            0x75.toByte(), 0x01.toByte(), 0x81.toByte(), 0x02.toByte(), 0x95.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x05.toByte(), 0x81.toByte(), 0x03.toByte(), 0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x30.toByte(), 0x09.toByte(), 0x31.toByte(), 0x09.toByte(), 0x38.toByte(),
            0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(), 0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x03.toByte(), 0x81.toByte(), 0x06.toByte(), 0xC0.toByte(), 0xC0.toByte(),

            // Keyboard collection (Report ID 1)
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x06.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0xE0.toByte(),
            0x29.toByte(), 0xE7.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x08.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x08.toByte(), 0x81.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x05.toByte(), 0x75.toByte(), 0x01.toByte(), 0x05.toByte(), 0x08.toByte(),
            0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x05.toByte(), 0x91.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x03.toByte(), 0x91.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x06.toByte(), 0x75.toByte(), 0x08.toByte(), 0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x65.toByte(), 0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0x00.toByte(),
            0x29.toByte(), 0x65.toByte(), 0x81.toByte(), 0x00.toByte(), 0xC0.toByte(),

            // System Control collection (Report ID 3)
            0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
            0x09.toByte(), 0x80.toByte(),        // Usage (System Control)
            0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
            0x85.toByte(), 0x03.toByte(),        //   Report ID (3)
            0x19.toByte(), 0x81.toByte(),        //   Usage Minimum (System Power Down)
            0x29.toByte(), 0x83.toByte(),        //   Usage Maximum (System Wake Up)
            0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
            0x75.toByte(), 0x01.toByte(),        //   Report Size (1 bit)
            0x95.toByte(), 0x03.toByte(),        //   Report Count (3)
            0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute)
            0x75.toByte(), 0x05.toByte(),        //   Report Size (5 bits) - padding
            0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
            0x81.toByte(), 0x01.toByte(),        //   Input (Constant)
            0xC0.toByte(),                       // End Collection

            // Consumer Control collection (Report ID 4)
            0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
            0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
            0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
            0x85.toByte(), 0x04.toByte(),        //   Report ID (4)
            0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
            0x26.toByte(), 0x9C.toByte(), 0x02.toByte(), // Logical Maximum (0x029C)
            0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
            0x2A.toByte(), 0x9C.toByte(), 0x02.toByte(), // Usage Maximum (0x029C)
            0x75.toByte(), 0x10.toByte(),        //   Report Size (16 bits)
            0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
            0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array, Absolute)
            0xC0.toByte()                        // End Collection
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            logFile = File(getExternalFilesDir(null), "gateway.log")
        } catch (ignored: Exception) {}

        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1001,
                    buildNotification("Ready"),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(1001, buildNotification("Ready"))
            }
        } catch (e: SecurityException) {
            log("Deferred foreground start: missing connectedDevice permission")
        } catch (e: Exception) {
            try {
                startForeground(1001, buildNotification("Ready"))
            } catch (ignored: Exception) {}
        }

        log("GhostHID Service started.")
        initBluetooth()
    }

    fun resolveEffectiveOs(): TargetOs {
        val fp = _targetOs.value
        if (fp.isKnown) return fp.os

        val device = targetDevice
        if (device != null) {
            val cached = device.address?.let { deviceOsCache.get(it) }
            if (cached?.isKnown == true) return cached.os
            if (HostOsClassifier.isAppleOui(device.address)) return TargetOs.MACOS
            val byName = HostOsClassifier.fromName(safeDeviceName(device))
            if (byName?.isKnown == true) return byName.os
        }
        return TargetOs.WINDOWS
    }

    fun updateEffectiveTargetOs() {
        _effectiveTargetOs.value = resolveEffectiveOs()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GhostHID",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains Bluetooth HID bridge"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("GhostHID")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_ghost)
            .setOngoing(false)
            .build()
    }

    private fun updateNotification(text: String) {
        if (isTornDown) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(1001, buildNotification(text))
    }

    private fun initBluetooth() {
        adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            log("ERROR: Bluetooth adapter not available!")
            return
        }
        log("Bluetooth enabled=${adapter?.isEnabled}, LocalName=${adapter?.name}")
        restoreTargetSelection()
        adapter?.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                log("BluetoothHidDevice proxy connected.")
                registerHidApp()
                refreshDevices(null)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isRegistered.value = false
                log("BluetoothHidDevice proxy disconnected.")
                writeStatusFile()
            }
        }
    }

    fun registerHidApp() {
        val hid = hidDevice ?: return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "GhostHID Keyboard & Mouse",
            "Virtual HID Keyboard + Mouse Gateway",
            "Android",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HID_DESCRIPTOR
        )
        val ok = hid.registerApp(sdp, null, null, executor, hidCallback)
        log("registerApp() returned: $ok")
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _isRegistered.value = registered
            log("onAppStatusChanged: registered=$registered")
            writeStatusFile()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val status = ConnectionStatus.fromInt(state)
            _connectionState.value = status
            val name = device?.name ?: "Unknown"
            log("onConnectionStateChanged: [$name] -> $status")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                targetDevice = device
                _targetDeviceState.value = device
                device?.address?.let { address ->
                    targetPreferences().edit()
                        .putString(KEY_LAST_CONNECTED_ADDRESS, address)
                        .putString(KEY_SELECTED_ADDRESS, address)
                        .putBoolean(KEY_TARGET_SETUP_COMPLETE, true)
                        .apply()
                }
                _needsInitialTargetSelection.value = false
                updateNotification("Connected to $name")
                startOsDetection(device)
            } else {
                stopOsDetection()
                updateNotification("Status: $status")
            }
            updateEffectiveTargetOs()
            writeStatusFile()
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            log("onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                val reportSize = when (id.toInt()) {
                    KEYBOARD_REPORT_ID -> 8
                    MOUSE_REPORT_ID -> 4
                    SYSTEM_REPORT_ID -> 1
                    CONSUMER_REPORT_ID -> 2
                    else -> null
                }
                if (reportSize != null) {
                    hidDevice?.replyReport(device, type, id, ByteArray(reportSize))
                } else {
                    hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                }
            } else {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            log("onSetReport: type=$type, id=$id")
            // Windows pushes the initial keyboard LED state (report ID 1) right
            // after enumeration. macOS and BlueZ only send it on user lock-key
            // changes, so an immediate report is a conclusive Windows signal.
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id == KEYBOARD_REPORT_ID.toByte()) {
                maybeRecordLedHandshake(device)
            }
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        val a = adapter ?: return emptyList()
        return try {
            a.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refreshDevices(specificAddress: String?) {
        val a = adapter ?: return
        try {
            if (specificAddress == null) {
                restoreTargetSelection()
            } else {
                val device = a.bondedDevices?.firstOrNull {
                    it.address.equals(specificAddress, ignoreCase = true)
                }
                targetDevice = device
                _targetDeviceState.value = device
                if (device != null) {
                    targetPreferences().edit()
                        .putString(KEY_SELECTED_ADDRESS, device.address)
                        .putBoolean(KEY_TARGET_SETUP_COMPLETE, true)
                        .apply()
                    _needsInitialTargetSelection.value = false
                    log("Target explicitly selected: ${device.name} [${device.address}]")
                }
            }
        } catch (e: Exception) {
            log("refreshDevices error: ${e.message}")
        }
        updateEffectiveTargetOs()
        writeStatusFile()
    }

    fun markTargetSelectionComplete() {
        targetPreferences().edit().putBoolean(KEY_TARGET_SETUP_COMPLETE, true).apply()
        _needsInitialTargetSelection.value = false
    }

    private fun targetPreferences() = getSharedPreferences(TARGET_PREFERENCES, Context.MODE_PRIVATE)

    private fun restoreTargetSelection() {
        val preferences = targetPreferences()
        try {
            val preferredAddresses = listOfNotNull(
                preferences.getString(KEY_LAST_CONNECTED_ADDRESS, null),
                preferences.getString(KEY_SELECTED_ADDRESS, null)
            ).distinct()
            val pairedDevices = adapter?.bondedDevices.orEmpty()
            val device = preferredAddresses.firstNotNullOfOrNull { address ->
                pairedDevices.firstOrNull { it.address.equals(address, ignoreCase = true) }
            }
            targetDevice = device
            _targetDeviceState.value = device
            _needsInitialTargetSelection.value = device == null &&
                !preferences.getBoolean(KEY_TARGET_SETUP_COMPLETE, false)
            _targetSelectionReady.value = true
        } catch (e: Exception) {
            log("restoreTargetSelection error: ${e.message}")
            _targetSelectionReady.value = true
        }
        updateEffectiveTargetOs()
    }

    fun connectTarget(specificAddress: String? = null) {
        val previous = targetDevice
        val hid = hidDevice
        val isSwitch = specificAddress != null && previous != null &&
            !previous.address.equals(specificAddress, ignoreCase = true)
        val wasLinked = _connectionState.value == ConnectionStatus.CONNECTED ||
            _connectionState.value == ConnectionStatus.CONNECTING

        if (isSwitch && hid != null && wasLinked) {
            val oldTarget = previous ?: return
            log("Switching target: disconnecting ${oldTarget.name} [${oldTarget.address}] first...")
            targetSwitchJob?.cancel()
            try {
                hid.disconnect(oldTarget)
            } catch (e: Exception) {
                log("disconnect old target error: ${e.message}")
            }
            // Let the stack finish tearing down the old link before asking for the new one.
            targetSwitchJob = scope.launch {
                delay(TARGET_SWITCH_SETTLE_MS)
                connectToTarget(specificAddress)
            }
            return
        }

        connectToTarget(specificAddress)
    }

    private fun connectToTarget(specificAddress: String?) {
        if (specificAddress != null || targetDevice == null) {
            refreshDevices(specificAddress)
        }
        val target = targetDevice
        val hid = hidDevice
        if (hid != null && target != null) {
            log("Connecting to ${target.name} [${target.address}]...")
            val ok = hid.connect(target)
            log("connect() returned: $ok")
        } else {
            log("connect() failed: target device not found")
        }
    }

    fun disconnectTarget() {
        val hid = hidDevice
        if (hid != null) {
            try {
                val connected = hid.connectedDevices
                if (!connected.isNullOrEmpty()) {
                    for (dev in connected) {
                        log("Disconnecting from ${dev.name ?: dev.address}...")
                        try {
                            hid.disconnect(dev)
                        } catch (e: Exception) {
                            log("disconnectTarget error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                log("getConnectedDevices error: ${e.message}")
            }
        }
        val target = targetDevice
        if (hid != null && target != null) {
            try {
                log("Disconnecting from ${target.name}...")
                hid.disconnect(target)
            } catch (e: Exception) {
                log("disconnectTarget error: ${e.message}")
            }
        }
    }

    fun unpairTarget(targetAddr: String?) {
        val a = adapter ?: return
        try {
            val paired = a.bondedDevices
            if (paired != null) {
                for (d in paired) {
                    if (targetAddr == null || targetAddr.isEmpty() || d.address.equals(targetAddr, ignoreCase = true)) {
                        log("Unpairing ${d.name} [${d.address}]...")
                        val m = d.javaClass.getMethod("removeBond")
                        m.invoke(d)
                    }
                }
            }
            if (targetDevice != null && (targetAddr == null || targetAddr.isEmpty() || targetDevice?.address.equals(targetAddr, ignoreCase = true))) {
                targetDevice = null
                _targetDeviceState.value = null
                _connectionState.value = ConnectionStatus.DISCONNECTED
                stopOsDetection()
                updateEffectiveTargetOs()
                writeStatusFile()
            }
        } catch (e: Exception) {
            log("unpairTarget error: ${e.message}")
        }
    }

    fun sendRawReport(bytes: ByteArray) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing Raw Report (${bytes.size} bytes)...")
            // Map common payload lengths to their standard report IDs:
            // 1 byte  -> System Control (0x03)
            // 2 bytes -> Consumer Control (0x04)
            // 4 bytes -> Mouse (0x02)
            // 8 bytes -> Keyboard (0x01)
            val reportId = when (bytes.size) {
                1 -> SYSTEM_REPORT_ID
                2 -> CONSUMER_REPORT_ID
                4 -> MOUSE_REPORT_ID
                8 -> KEYBOARD_REPORT_ID
                else -> KEYBOARD_REPORT_ID
            }
            sendRawReport(reportId, bytes)
        }
    }

    fun sendRawReport(reportId: Int, bytes: ByteArray) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing Raw Report ID $reportId (${bytes.size} bytes)...")
            val ok = hid.sendReport(target, reportId, bytes)
            log("sendReport(id=$reportId) returned: $ok")
        }
    }

    fun sendSystemSleep() {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing System Sleep (Report $SYSTEM_REPORT_ID)...")
            val down = byteArrayOf(0x02) // Bit 1 is System Sleep (0x02)
            val up = byteArrayOf(0x00)
            val r1 = hid.sendReport(target, SYSTEM_REPORT_ID, down)
            try { Thread.sleep(60) } catch (ignored: Exception) {}
            val r2 = hid.sendReport(target, SYSTEM_REPORT_ID, up)
            log("System Sleep sent: down=$r1, up=$r2")
        }
    }

    fun sendConsumerKey(usageCode: Int) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing Consumer Key 0x${usageCode.toString(16)} (Report $CONSUMER_REPORT_ID)...")
            val down = byteArrayOf((usageCode and 0xFF).toByte(), ((usageCode shr 8) and 0xFF).toByte())
            val up = byteArrayOf(0x00, 0x00)
            val r1 = hid.sendReport(target, CONSUMER_REPORT_ID, down)
            try { Thread.sleep(60) } catch (ignored: Exception) {}
            val r2 = hid.sendReport(target, CONSUMER_REPORT_ID, up)
            log("Consumer Key sent: down=$r1, up=$r2")
        }
    }

    fun sendLock() {
        val os = resolveEffectiveOs()
        if (os == TargetOs.MACOS) {
            sendMacLock()
        } else {
            sendWinL()
        }
    }

    fun sendMacLock() {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing macOS Lock (Cmd+Ctrl+Q)...")
            val down = byteArrayOf(0x09.toByte(), 0x00, 0x14.toByte(), 0, 0, 0, 0, 0)
            val up = ByteArray(8)
            hid.sendReport(target, KEYBOARD_REPORT_ID, down)
            try { Thread.sleep(70) } catch (ignored: Exception) {}
            hid.sendReport(target, KEYBOARD_REPORT_ID, up)
        }
    }

    fun sendWinL() {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing Win+L...")
            val down = byteArrayOf(0x08.toByte(), 0x00, 0x0F.toByte(), 0, 0, 0, 0, 0)
            val up = ByteArray(8)
            hid.sendReport(target, KEYBOARD_REPORT_ID, down)
            try { Thread.sleep(70) } catch (ignored: Exception) {}
            hid.sendReport(target, KEYBOARD_REPORT_ID, up)
        }
    }

    fun sendKey(modifier: Byte, keyCode: Byte, durationMs: Long = 40) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            val down = byteArrayOf(modifier, 0, keyCode, 0, 0, 0, 0, 0)
            val up = ByteArray(8)
            hid.sendReport(target, KEYBOARD_REPORT_ID, down)
            try { Thread.sleep(if (durationMs > 0) durationMs else 40) } catch (ignored: Exception) {}
            hid.sendReport(target, KEYBOARD_REPORT_ID, up)
            try { Thread.sleep(20) } catch (ignored: Exception) {}
        }
    }

    fun typeText(text: String?) {
        if (text.isNullOrEmpty()) return
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Typing string (${text.length} chars)")
            for (i in text.indices) {
                val c = text[i]
                val (modifier, code) = KeyTokens.keyForChar(c)
                if (code != 0.toByte()) {
                    val down = byteArrayOf(modifier, 0, code, 0, 0, 0, 0, 0)
                    val up = ByteArray(8)
                    hid.sendReport(target, KEYBOARD_REPORT_ID, down)
                    try { Thread.sleep(25) } catch (ignored: Exception) {}
                    hid.sendReport(target, KEYBOARD_REPORT_ID, up)
                    try { Thread.sleep(25) } catch (ignored: Exception) {}
                }
            }
            log(">>> Typing complete.")
        }
    }

    // ---------------------------------------------------------------------
    // Pointer transport
    //
    // Every pointer report goes through one thread so press/release pairs can
    // never overtake each other. Relative motion is coalesced and split into
    // HID-sized chunks, which keeps fast drags smooth and never blocks the
    // caller (the trackpad runs on the UI thread).
    // ---------------------------------------------------------------------

    private val pointerLock = Any()
    private val pointerExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(),
        { runnable -> Thread(runnable, "ghid-pointer").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardPolicy()
    )
    private var pendingDx = 0
    private var pendingDy = 0
    private var pointerFlushScheduled = false

    /**
     * Emits a real press/release cycle. Multiple clicks are emitted as
     * separate cycles so Windows and macOS both interpret them natively
     * instead of relying on a vendor-specific "double click" report.
     */
    fun sendMouseButton(button: MouseButton, clicks: Int = 1) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        val count = clicks.coerceAtLeast(1)

        runOnPointerThread {
            repeat(count) { index ->
                if (index > 0) sleep(CLICK_GAP_MS)
                hid.sendReport(target, MOUSE_REPORT_ID, mouseReport(button.mask, 0, 0, 0))
                sleep(CLICK_PRESS_MS)
                hid.sendReport(target, MOUSE_REPORT_ID, RELEASE_REPORT)
                sleep(CLICK_SETTLE_MS)
            }
            log(">>> Mouse ${button.name.lowercase(Locale.ROOT)} x$count")
        }
    }

    /**
     * Relative pointer motion, and/or wheel notches when [wheel] is non-zero.
     * Motion larger than one report is transmitted in full instead of being
     * clipped to 127 counts.
     */
    fun sendMouseMove(dx: Int, dy: Int, wheel: Int = 0) {
        if (dx == 0 && dy == 0 && wheel == 0) return
        if (hidDevice == null || targetDevice == null) return

        if (wheel != 0) {
            runOnPointerThread { emitWheel(wheel) }
            return
        }

        synchronized(pointerLock) {
            pendingDx += dx
            pendingDy += dy
            if (!pointerFlushScheduled) {
                pointerFlushScheduled = true
                runOnPointerThread(::flushPointerMoves)
            }
        }
    }

    /** CLI and combo entry point. Tokens stay in sync with the Go grammar. */
    fun sendMouse(action: String?) {
        val normalized = action?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isEmpty()) return

        when (normalized) {
            "double-click", "doubleclick", "dblclick" -> sendMouseButton(MouseButton.LEFT, clicks = 2)
            "scroll-up", "scrollup", "wheel-up" -> sendMouseMove(0, 0, wheel = 1)
            "scroll-down", "scrolldown", "wheel-down" -> sendMouseMove(0, 0, wheel = -1)
            else -> {
                val button = MouseButton.fromToken(normalized)
                if (button != null) sendMouseButton(button) else log("Unknown mouse action: $action")
            }
        }
    }

    private fun runOnPointerThread(block: () -> Unit) {
        if (pointerExecutor.isShutdown) return
        try {
            pointerExecutor.execute(block)
        } catch (ignored: RejectedExecutionException) {
            // The service is shutting down; drop the report instead of crashing.
        }
    }

    private fun flushPointerMoves() {
        while (true) {
            val stepX: Int
            val stepY: Int
            synchronized(pointerLock) {
                if (pendingDx == 0 && pendingDy == 0) {
                    pointerFlushScheduled = false
                    return
                }
                stepX = pendingDx
                stepY = pendingDy
                pendingDx = 0
                pendingDy = 0
            }
            emitMove(stepX, stepY)
        }
    }

    private fun emitMove(dx: Int, dy: Int) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        val xChunks = PointerMath.chunks(dx)
        val yChunks = PointerMath.chunks(dy)
        val reports = maxOf(xChunks.size, yChunks.size)

        for (index in 0 until reports) {
            val report = mouseReport(
                buttons = 0,
                dx = xChunks.getOrElse(index) { 0 },
                dy = yChunks.getOrElse(index) { 0 },
                wheel = 0
            )
            hid.sendReport(target, MOUSE_REPORT_ID, report)
            if (reports > 1 && index < reports - 1) {
                sleep(MOVE_CHUNK_GAP_MS)
            }
        }
    }

    private fun emitWheel(notches: Int) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        for (chunk in PointerMath.chunks(notches)) {
            hid.sendReport(target, MOUSE_REPORT_ID, mouseReport(0, 0, 0, chunk))
            sleep(WHEEL_SETTLE_MS)
        }
    }

    private fun mouseReport(buttons: Byte, dx: Int, dy: Int, wheel: Int): ByteArray = byteArrayOf(
        buttons,
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte()
    )

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Executes a full action sequence with AST steps, payload expansion, and delay handling.
     */
    fun executeCombo(rawOrName: String) {
        scope.launch {
            val app = GhostHidApp.instance
            val combo = app.comboRepository.find(rawOrName)
                ?: app.comboRepository.combos.value.firstOrNull { it.sequence == rawOrName || it.effectiveSequence == rawOrName }
            val raw = combo?.effectiveSequence ?: rawOrName
            val steps = ActionSequenceParser.parse(raw, _effectiveTargetOs.value)
            combo?.let { app.comboUsageRepository.recordUse(it.name) }
            log(">>> Executing sequence ($raw)")

            for (step in steps) {
                when (step) {
                    is HidStep.Delay -> {
                        delay(step.millis)
                    }
                    is HidStep.KeyboardChord -> {
                        sendKey(step.modifier, step.keyCode)
                        delay(50)
                    }
                    is HidStep.TypeText -> {
                        typeText(step.text)
                        delay(step.text.length * 50L + 50L)
                    }
                    is HidStep.PayloadReference -> {
                        val pRepo = GhostHidApp.instance.payloadRepository
                        val p = pRepo.get(step.name)
                        val value = pRepo.resolve(step.name)
                        if (!value.isNullOrEmpty()) {
                            val cleanVal = value.trim()
                            val cleanHex = (if (cleanVal.startsWith("raw:", ignoreCase = true)) cleanVal.substring(4) else cleanVal)
                                .replace(" ", "")
                            val isHexMatch = cleanHex.length in listOf(2, 4, 8, 16) && cleanHex.all { it in "0123456789abcdefABCDEF" }
                            if (p?.isRaw == true || cleanVal.startsWith("raw:", ignoreCase = true) || isHexMatch) {
                                if (isHexMatch) {
                                    val bytes = ByteArray(cleanHex.length / 2) { i ->
                                        cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                                    }
                                    sendRawReport(bytes)
                                    delay(20)
                                } else {
                                    log("Invalid raw report hex in payload {${step.name}}: $value")
                                }
                            } else {
                                typeText(value)
                                delay(value.length * 50L + 50L)
                            }
                        } else {
                            log("Payload {${step.name}} empty or unresolved")
                        }
                    }
                    is HidStep.MouseAction -> {
                        sendMouse(step.action)
                        delay(100)
                    }
                    is HidStep.RawReport -> {
                        sendRawReport(step.bytes)
                        delay(20)
                    }
                }
            }
            log(">>> Sequence finished.")
        }
    }

    private fun writeStatusFile() {
        try {
            val fingerprint = _targetOs.value
            val f = File(getExternalFilesDir(null), "status.json")
            FileWriter(f).use { fw ->
                fw.write("{\n")
                fw.write("  \"registered\": ${_isRegistered.value},\n")
                fw.write("  \"connected\": ${_connectionState.value == ConnectionStatus.CONNECTED},\n")
                fw.write("  \"state\": \"${_connectionState.value}\",\n")
                fw.write("  \"target\": \"${targetDevice?.name ?: "none"}\",\n")
                fw.write("  \"address\": \"${targetDevice?.address ?: "none"}\",\n")
                fw.write("  \"os\": \"${fingerprint.os.token}\",\n")
                fw.write("  \"os_confidence\": \"${fingerprint.confidence.token}\",\n")
                fw.write("  \"detection_method\": \"${fingerprint.method}\",\n")
                fw.write("  \"updated\": \"${timeFormat.format(Date())}\"\n")
                fw.write("}\n")
            }
        } catch (ignored: Exception) {}
    }

    // ---------------------------------------------------------------------
    // Passive host OS fingerprinting (ADR-007)
    // ---------------------------------------------------------------------

    private fun maybeRecordLedHandshake(device: BluetoothDevice?) {
        val target = targetDevice ?: return
        val fromTarget = device == null || device.address.equals(target.address, ignoreCase = true)
        if (!fromTarget) return

        ledHandshakeSeen = true
        if (osDetectionJob != null) {
            osDetectionJob?.cancel()
            osDetectionJob = null
            val classified = HostOsClassifier.classify(
                address = target.address,
                name = safeDeviceName(target),
                serviceUuids = deviceServiceUuids(target),
                ledHandshakeReceived = true
            )
            publishFingerprint(classified, target.address)
            writeStatusFile()
        }
    }

    private fun startOsDetection(device: BluetoothDevice?) {
        osDetectionJob?.cancel()
        osDetectionJob = null
        ledHandshakeSeen = false

        val address = device?.address
        val cached = deviceOsCache.get(address)
        if (cached != null && cached.isKnown) {
            log(
                "Using cached OS for ${safeDeviceName(device)} [$address]: " +
                    "${cached.os.label} (${cached.confidence.token} via ${cached.method})"
            )
            publishFingerprint(cached, address)
            return
        }

        val uuids = deviceServiceUuids(device)
        val apple = HostOsClassifier.appleSignal(address, uuids)
        if (apple != null) {
            publishFingerprint(apple, address)
            return
        }

        // Unknown while the LED window is open; the timer or a SET_REPORT
        // resolves it within 500 ms.
        _targetOs.value = HostOsFingerprint.Unknown
        updateEffectiveTargetOs()
        osDetectionJob = scope.launch {
            delay(HostOsClassifier.LED_HANDSHAKE_WINDOW_MS)
            val classified = HostOsClassifier.classify(
                address = address,
                name = safeDeviceName(device),
                serviceUuids = uuids,
                ledHandshakeReceived = ledHandshakeSeen
            )
            publishFingerprint(classified, address)
            writeStatusFile()
        }
    }

    private fun stopOsDetection() {
        osDetectionJob?.cancel()
        osDetectionJob = null
        ledHandshakeSeen = false
        _targetOs.value = HostOsFingerprint.Unknown
        updateEffectiveTargetOs()
    }

    private fun publishFingerprint(
        fingerprint: HostOsFingerprint,
        deviceAddress: String? = targetDevice?.address
    ) {
        _targetOs.value = fingerprint
        if (fingerprint.isKnown && deviceAddress != null) {
            deviceOsCache.put(deviceAddress, fingerprint)
        }
        updateEffectiveTargetOs()
        log(
            "Host OS: ${fingerprint.os.label} " +
                "(${fingerprint.confidence.token} confidence via ${fingerprint.method})"
        )
    }

    private fun safeDeviceName(device: BluetoothDevice?): String? = try {
        device?.name
    } catch (e: SecurityException) {
        null
    }

    private fun deviceServiceUuids(device: BluetoothDevice?): List<String>? = try {
        device?.uuids?.mapNotNull { it?.uuid?.toString() }
    } catch (e: Exception) {
        null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isTornDown) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent != null) {
            handleIntent(intent)
        }
        return START_NOT_STICKY
    }

    fun handleIntent(intent: Intent) {
        val action = intent.action
        val cmd = intent.getStringExtra("cmd")

        when {
            "stop".equals(cmd, ignoreCase = true) -> quit()
            ACTION_LOCK == action || "lock".equals(cmd, ignoreCase = true) -> sendLock()
            "sleep".equals(cmd, ignoreCase = true) -> {
                if (resolveEffectiveOs() == TargetOs.MACOS) {
                    executeCombo("cmd+ctrl+q ->{0.5} esc")
                } else {
                    sendSystemSleep()
                }
            }
            "raw".equals(cmd, ignoreCase = true) -> {
                val hex = intent.getStringExtra("hex")
                val hasExplicitId = intent.hasExtra("id")
                if (!hex.isNullOrEmpty()) {
                    val cleanHex = hex.replace(" ", "")
                    if (cleanHex.length % 2 == 0) {
                        val bytes = ByteArray(cleanHex.length / 2) { i ->
                            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                        }
                        if (hasExplicitId) {
                            sendRawReport(intent.getIntExtra("id", KEYBOARD_REPORT_ID), bytes)
                        } else {
                            sendRawReport(bytes)
                        }
                    }
                }
            }
            "media".equals(cmd, ignoreCase = true) -> {
                val key = intent.getStringExtra("key")?.lowercase(Locale.ROOT)
                when (key) {
                    "play", "pause", "playpause" -> sendConsumerKey(0x00CD)
                    "next", "next-track" -> sendConsumerKey(0x00B5)
                    "prev", "prev-track" -> sendConsumerKey(0x00B6)
                    "volup", "volume-up" -> sendConsumerKey(0x00E9)
                    "voldown", "volume-down" -> sendConsumerKey(0x00EA)
                    "mute" -> sendConsumerKey(0x00E2)
                }
            }
            ACTION_TYPE == action || "type".equals(cmd, ignoreCase = true) -> typeText(intent.getStringExtra("text"))
            "combo".equals(cmd, ignoreCase = true) -> {
                val keys = intent.getStringExtra("keys")
                if (!keys.isNullOrEmpty()) executeCombo(keys)
            }
            "mouse".equals(cmd, ignoreCase = true) -> sendMouse(intent.getStringExtra("action"))
            "connect".equals(cmd, ignoreCase = true) -> connectTarget(intent.getStringExtra("address"))
            "disconnect".equals(cmd, ignoreCase = true) -> disconnectTarget()
            "unpair".equals(cmd, ignoreCase = true) -> unpairTarget(intent.getStringExtra("address"))
            "register".equals(cmd, ignoreCase = true) -> registerHidApp()
            "status".equals(cmd, ignoreCase = true) -> writeStatusFile()
        }
    }

    fun log(msg: String) {
        val line = "[${timeFormat.format(Date())}] $msg"
        Log.d(TAG, msg)
        val entry = LogEntry(timeFormat.format(Date()), msg)
        _logs.value = (_logs.value + entry).takeLast(100)

        logFile?.let { f ->
            if (!executor.isShutdown) {
                try {
                    executor.execute {
                        try {
                            FileWriter(f, true).use { it.write(line + "\n") }
                        } catch (ignored: Exception) {}
                    }
                } catch (ignored: RejectedExecutionException) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun teardown() {
        if (isTornDown) return
        isTornDown = true

        log("Tearing down GhostHID Service...")
        targetSwitchJob?.cancel()
        osDetectionJob?.cancel()
        try {
            scope.cancel()
        } catch (ignored: Exception) {}

        disconnectTarget()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(1001)
            nm?.cancelAll()
        } catch (ignored: Exception) {}

        val hid = hidDevice
        if (hid != null) {
            try {
                hid.unregisterApp()
            } catch (e: Exception) {
                log("unregisterApp error: ${e.message}")
            }
            try {
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            } catch (e: Exception) {
                log("closeProfileProxy error: ${e.message}")
            }
            hidDevice = null
        }

        targetDevice = null
        _targetDeviceState.value = null
        _connectionState.value = ConnectionStatus.DISCONNECTED
        _isRegistered.value = false
        stopOsDetection()
        writeStatusFile()

        try {
            executor.shutdown()
        } catch (ignored: Exception) {}
        try {
            pointerExecutor.shutdown()
        } catch (ignored: Exception) {}

        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(1001)
            nm?.cancelAll()
        } catch (ignored: Exception) {}
    }

    fun quit() {
        log("Quit requested.")
        teardown()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        teardown()
        log("GhostHID Service destroyed.")
    }
}
