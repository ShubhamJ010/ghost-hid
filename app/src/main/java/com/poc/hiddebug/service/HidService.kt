package com.poc.hiddebug.service

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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.poc.hiddebug.GhostHidApp
import com.poc.hiddebug.domain.model.ConnectionStatus
import com.poc.hiddebug.domain.model.HidStep
import com.poc.hiddebug.domain.model.LogEntry
import com.poc.hiddebug.domain.parser.ActionSequenceParser
import com.poc.hiddebug.domain.parser.KeyTokens
import com.poc.hiddebug.domain.pointer.MouseButton
import com.poc.hiddebug.domain.pointer.PointerMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

class HidService : Service() {

    inner class LocalBinder : Binder() {
        val service: HidService get() = this@HidService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var adapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var targetDevice: BluetoothDevice? = null

    private val _connectionState = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _targetDeviceState = MutableStateFlow<BluetoothDevice?>(null)
    val targetDeviceState: StateFlow<BluetoothDevice?> = _targetDeviceState.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var logFile: File? = null

    companion object {
        const val TAG = "GHOST_HID"
        const val CHANNEL_ID = "ghost_hid_gateway"
        const val ACTION_CMD = "com.poc.hiddebug.ACTION_CMD"
        const val ACTION_LOCK = "com.poc.hiddebug.ACTION_LOCK"
        const val ACTION_TYPE = "com.poc.hiddebug.ACTION_TYPE"

        const val MOUSE_REPORT_ID = 2
        const val KEYBOARD_REPORT_ID = 1
        private val RELEASE_REPORT = ByteArray(4)

        // A click must be long enough for every host stack to sample it, and
        // the two cycles must sit inside the OS double-click window (~500 ms).
        // The gaps are generous because some Bluetooth stacks coalesce reports
        // that arrive only a few milliseconds apart.
        private const val CLICK_PRESS_MS = 48L
        private const val CLICK_SETTLE_MS = 30L
        private const val CLICK_GAP_MS = 90L
        private const val WHEEL_SETTLE_MS = 14L

        private val HID_DESCRIPTOR = byteArrayOf(
            // Mouse collection (Report ID 2)
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), 0x02.toByte(), 0x09.toByte(), 0x01.toByte(), 0xA1.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(),
            0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(), 0x95.toByte(), 0x03.toByte(),
            0x75.toByte(), 0x01.toByte(), 0x81.toByte(), 0x02.toByte(), 0x95.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x05.toByte(), 0x81.toByte(), 0x01.toByte(), 0x05.toByte(), 0x01.toByte(),
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
            0x29.toByte(), 0x65.toByte(), 0x81.toByte(), 0x00.toByte(), 0xC0.toByte()
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            logFile = File(getExternalFilesDir(null), "gateway.log")
        } catch (ignored: Exception) {}

        createNotificationChannel()
        startForeground(1001, buildNotification("GhostHID Gateway: Ready"))

        log("GhostHID Service started.")
        initBluetooth()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GhostHID Gateway",
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
            .setContentTitle("GhostHID Gateway")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
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
                updateNotification("Connected to $name")
            } else {
                updateNotification("Gateway Status: $status")
            }
            writeStatusFile()
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            val reportSize = when (id.toInt()) {
                KEYBOARD_REPORT_ID -> 8
                MOUSE_REPORT_ID -> 4
                else -> bufferSize.coerceIn(0, 64)
            }
            hidDevice?.replyReport(device, type, id, ByteArray(reportSize))
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            hidDevice?.replyReport(device, type, id, ByteArray(0))
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
            val paired = a.bondedDevices
            if (paired != null) {
                for (d in paired) {
                    val name = d.name
                    val addr = d.address
                    if (specificAddress != null && specificAddress.equals(addr, ignoreCase = true)) {
                        targetDevice = d
                        _targetDeviceState.value = d
                        log("Target explicitly selected: $name [$addr]")
                        break
                    }
                    if (name != null && (name.startsWith("LP-") || name.contains("TZD") || addr.equals("38:FC:98:E8:E3:87", ignoreCase = true))) {
                        targetDevice = d
                        _targetDeviceState.value = d
                        log("Target matched: $name [$addr]")
                    }
                }
            }
        } catch (e: Exception) {
            log("refreshDevices error: ${e.message}")
        }
        writeStatusFile()
    }

    fun connectTarget(specificAddress: String? = null) {
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
        val target = targetDevice
        val hid = hidDevice
        if (hid != null && target != null) {
            log("Disconnecting from ${target.name}...")
            hid.disconnect(target)
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
                writeStatusFile()
            }
        } catch (e: Exception) {
            log("Unpair error: ${e.message}")
        }
    }

    fun sendRawReport(report: ByteArray) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            val ok = hid.sendReport(target, 1, report)
            log("sendRawReport -> $ok")
        }
    }

    fun sendWinL() {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            log(">>> Executing Win+L...")
            val down = byteArrayOf(0x08.toByte(), 0x00, 0x0F.toByte(), 0, 0, 0, 0, 0)
            val up = ByteArray(8)
            hid.sendReport(target, 1, down)
            try { Thread.sleep(70) } catch (ignored: Exception) {}
            hid.sendReport(target, 1, up)
        }
    }

    fun sendKey(modifier: Byte, keyCode: Byte, durationMs: Long = 40) {
        val hid = hidDevice ?: return
        val target = targetDevice ?: return
        executor.execute {
            val down = byteArrayOf(modifier, 0, keyCode, 0, 0, 0, 0, 0)
            val up = ByteArray(8)
            hid.sendReport(target, 1, down)
            try { Thread.sleep(if (durationMs > 0) durationMs else 40) } catch (ignored: Exception) {}
            hid.sendReport(target, 1, up)
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
                    hid.sendReport(target, 1, down)
                    try { Thread.sleep(25) } catch (ignored: Exception) {}
                    hid.sendReport(target, 1, up)
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
    private val pointerExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ghid-pointer").apply { isDaemon = true }
    }
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
     * Executes a full action sequence with AST steps, variable expansion, and delay handling.
     */
    fun executeCombo(rawOrName: String) {
        scope.launch {
            val combo = GhostHidApp.instance.comboRepository.find(rawOrName)
            val raw = combo?.keys ?: rawOrName
            val steps = ActionSequenceParser.parse(raw)
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
                    is HidStep.Variable -> {
                        val value = GhostHidApp.instance.snippetRepository.resolve(step.name)
                        if (!value.isNullOrEmpty()) {
                            typeText(value)
                            delay(value.length * 50L + 50L)
                        } else {
                            log("Variable $${step.name} empty or unresolved")
                        }
                    }
                    is HidStep.MouseAction -> {
                        sendMouse(step.action)
                        delay(100)
                    }
                    is HidStep.Directive -> {
                        when (step.name) {
                            "DISCONNECT" -> disconnectTarget()
                            "EXIT" -> stopSelf()
                        }
                    }
                }
            }
            log(">>> Sequence finished.")
        }
    }

    private fun writeStatusFile() {
        try {
            val f = File(getExternalFilesDir(null), "status.json")
            FileWriter(f).use { fw ->
                fw.write("{\n")
                fw.write("  \"registered\": ${_isRegistered.value},\n")
                fw.write("  \"connected\": ${_connectionState.value == ConnectionStatus.CONNECTED},\n")
                fw.write("  \"state\": \"${_connectionState.value}\",\n")
                fw.write("  \"target\": \"${targetDevice?.name ?: "none"}\",\n")
                fw.write("  \"address\": \"${targetDevice?.address ?: "none"}\",\n")
                fw.write("  \"updated\": \"${timeFormat.format(Date())}\"\n")
                fw.write("}\n")
            }
        } catch (ignored: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            val cmd = intent.getStringExtra("cmd")

            when {
                "stop".equals(cmd, ignoreCase = true) -> stopSelf()
                ACTION_LOCK == action || "lock".equals(cmd, ignoreCase = true) -> sendWinL()
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
        return START_STICKY
    }

    fun log(msg: String) {
        val line = "[${timeFormat.format(Date())}] $msg"
        Log.d(TAG, msg)
        val entry = LogEntry(timeFormat.format(Date()), msg)
        _logs.value = (_logs.value + entry).takeLast(100)

        logFile?.let { f ->
            executor.execute {
                try {
                    FileWriter(f, true).use { it.write(line + "\n") }
                } catch (ignored: Exception) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        hidDevice?.let { hid ->
            try { hid.unregisterApp() } catch (ignored: Exception) {}
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
        }
        executor.shutdownNow()
        pointerExecutor.shutdownNow()
        _connectionState.value = ConnectionStatus.DISCONNECTED
        writeStatusFile()
        log("GhostHID Service destroyed.")
    }
}
