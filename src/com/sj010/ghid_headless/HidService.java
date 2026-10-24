package com.sj010.ghid_headless;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GhostHID Headless Transport Gateway
 * Runs in background, binds BluetoothHidDevice, and handles raw HID commands via ADB IPC.
 */
public class HidService extends Service {

    public static final String TAG = "GHOST_HID";
    private static final String CHANNEL_ID = "ghost_hid_gateway";

    public static final String ACTION_CMD = "com.sj010.ghid_headless.ACTION_CMD";
    public static final String ACTION_LOCK = "com.sj010.ghid_headless.ACTION_LOCK";
    public static final String ACTION_TYPE = "com.sj010.ghid_headless.ACTION_TYPE";

    private static final byte[] HID_KEYBOARD_DESCRIPTOR = new byte[] {
        // ---- Mouse collection (Report ID 2) ----
        (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
        (byte) 0x09, (byte) 0x02,        // Usage (Mouse)
        (byte) 0xA1, (byte) 0x01,        // Collection (Application)
        (byte) 0x85, (byte) 0x02,        //   Report ID (2)
        (byte) 0x09, (byte) 0x01,        //   Usage (Pointer)
        (byte) 0xA1, (byte) 0x00,        //   Collection (Physical)
        (byte) 0x05, (byte) 0x09,        //     Usage Page (Buttons)
        (byte) 0x19, (byte) 0x01,        //     Usage Minimum (1)
        (byte) 0x29, (byte) 0x03,        //     Usage Maximum (3)
        (byte) 0x15, (byte) 0x00,        //     Logical Minimum (0)
        (byte) 0x25, (byte) 0x01,        //     Logical Maximum (1)
        (byte) 0x95, (byte) 0x03,        //     Report Count (3)
        (byte) 0x75, (byte) 0x01,        //     Report Size (1)
        (byte) 0x81, (byte) 0x02,        //     Input (Data, Variable, Absolute)
        (byte) 0x95, (byte) 0x01,        //     Report Count (1)
        (byte) 0x75, (byte) 0x05,        //     Report Size (5)
        (byte) 0x81, (byte) 0x03,        //     Input (Constant, Variable, Absolute) - padding
        (byte) 0x05, (byte) 0x01,        //     Usage Page (Generic Desktop)
        (byte) 0x09, (byte) 0x30,        //     Usage (X)
        (byte) 0x09, (byte) 0x31,        //     Usage (Y)
        (byte) 0x09, (byte) 0x38,        //     Usage (Wheel)
        (byte) 0x15, (byte) 0x81,        //     Logical Minimum (-127)
        (byte) 0x25, (byte) 0x7F,        //     Logical Maximum (127)
        (byte) 0x75, (byte) 0x08,        //     Report Size (8)
        (byte) 0x95, (byte) 0x03,        //     Report Count (3)
        (byte) 0x81, (byte) 0x06,        //     Input (Data, Variable, Relative)
        (byte) 0xC0,                     //   End Collection (Physical)
        (byte) 0xC0,                     // End Collection (Application)

        // ---- Keyboard collection (Report ID 1) ----
        (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
        (byte) 0x09, (byte) 0x06,        // Usage (Keyboard)
        (byte) 0xA1, (byte) 0x01,        // Collection (Application)
        (byte) 0x85, (byte) 0x01,        //   Report ID (1)
        (byte) 0x05, (byte) 0x07,        //   Usage Page (Key Codes)
        (byte) 0x19, (byte) 0xE0,        //   Usage Minimum (224)
        (byte) 0x29, (byte) 0xE7,        //   Usage Maximum (231)
        (byte) 0x15, (byte) 0x00,        //   Logical Minimum (0)
        (byte) 0x25, (byte) 0x01,        //   Logical Maximum (1)
        (byte) 0x75, (byte) 0x01,        //   Report Size (1)
        (byte) 0x95, (byte) 0x08,        //   Report Count (8)
        (byte) 0x81, (byte) 0x02,        //   Input (Data, Variable, Absolute) - Modifiers
        (byte) 0x95, (byte) 0x01,        //   Report Count (1)
        (byte) 0x75, (byte) 0x08,        //   Report Size (8)
        (byte) 0x81, (byte) 0x01,        //   Input (Constant) - Reserved
        (byte) 0x95, (byte) 0x05,        //   Report Count (5)
        (byte) 0x75, (byte) 0x01,        //   Report Size (1)
        (byte) 0x05, (byte) 0x08,        //   Usage Page (LEDs)
        (byte) 0x19, (byte) 0x01,        //   Usage Minimum (1)
        (byte) 0x29, (byte) 0x05,        //   Usage Maximum (5)
        (byte) 0x91, (byte) 0x02,        //   Output (Data, Variable, Absolute) - LEDs
        (byte) 0x95, (byte) 0x01,        //   Report Count (1)
        (byte) 0x75, (byte) 0x03,        //   Report Size (3)
        (byte) 0x91, (byte) 0x01,        //   Output (Constant) - LED padding
        (byte) 0x95, (byte) 0x06,        //   Report Count (6)
        (byte) 0x75, (byte) 0x08,        //   Report Size (8)
        (byte) 0x15, (byte) 0x00,        //   Logical Minimum (0)
        (byte) 0x25, (byte) 0x65,        //   Logical Maximum (101)
        (byte) 0x05, (byte) 0x07,        //   Usage Page (Key Codes)
        (byte) 0x19, (byte) 0x00,        //   Usage Minimum (0)
        (byte) 0x29, (byte) 0x65,        //   Usage Maximum (101)
        (byte) 0x81, (byte) 0x00,        //   Input (Data, Array) - Keys
        (byte) 0xC0                      // End Collection
    };

    private BluetoothAdapter mAdapter;
    private BluetoothHidDevice mHidDevice;
    private BluetoothDevice mTargetDevice;
    private boolean mIsRegistered = false;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private ExecutorService mExecutor;
    private SimpleDateFormat mTimeFormat;
    private File mLogFile;

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutor = Executors.newSingleThreadExecutor();
        mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

        try {
            mLogFile = new File(getExternalFilesDir(null), "gateway.log");
        } catch (Exception ignored) {}

        createNotificationChannel();
        startForeground(1001, buildNotification("GhostHID Gateway: Ready"));

        log("GhostHID Gateway Service started (Headless Mode).");
        initBluetooth();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "GhostHID Gateway",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Maintains headless Bluetooth HID bridge");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
            .setContentTitle("GhostHID Gateway")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(1001, buildNotification(text));
        }
    }

    private void initBluetooth() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            log("ERROR: Bluetooth adapter not available!");
            return;
        }
        log("Bluetooth enabled=" + mAdapter.isEnabled() + ", LocalName=" + mAdapter.getName());
        mAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HID_DEVICE);
    }

    private final BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                mHidDevice = (BluetoothHidDevice) proxy;
                log("BluetoothHidDevice proxy connected.");
                registerHidApp();
                refreshDevices(null);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                mHidDevice = null;
                mIsRegistered = false;
                log("BluetoothHidDevice proxy disconnected.");
                writeStatusFile();
            }
        }
    };

    public void registerHidApp() {
        if (mHidDevice == null) return;
        BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
            "GhostHID Keyboard & Mouse",
            "Virtual HID Keyboard + Mouse Gateway",
            "Android",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HID_KEYBOARD_DESCRIPTOR
        );
        boolean ok = mHidDevice.registerApp(sdp, null, null, mExecutor, mHidCallback);
        log("registerApp() returned: " + ok);
    }

    private final BluetoothHidDevice.Callback mHidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            mIsRegistered = registered;
            log("onAppStatusChanged: registered=" + registered);
            writeStatusFile();
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            mConnectionState = state;
            String name = (device != null) ? device.getName() : "Unknown";
            log("onConnectionStateChanged: [" + name + "] -> " + stateToString(state));
            if (state == BluetoothProfile.STATE_CONNECTED) {
                mTargetDevice = device;
                updateNotification("Connected to " + name);
            } else {
                updateNotification("Gateway Status: " + stateToString(state));
            }
            writeStatusFile();
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            log("onGetReport: type=" + type + ", id=" + id);
            if (mHidDevice != null) {
                if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                    int size = (id == 2) ? 4 : 8;
                    mHidDevice.replyReport(device, type, id, new byte[size]);
                } else {
                    mHidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                }
            }
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            log("onSetReport: type=" + type + ", id=" + id);
            if (mHidDevice != null) {
                mHidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
            }
        }
    };

    public void refreshDevices(String specificAddress) {
        if (mAdapter == null) return;
        try {
            Set<BluetoothDevice> paired = mAdapter.getBondedDevices();
            if (paired != null) {
                for (BluetoothDevice d : paired) {
                    String name = d.getName();
                    String addr = d.getAddress();
                    if (specificAddress != null && specificAddress.equalsIgnoreCase(addr)) {
                        mTargetDevice = d;
                        log("Target explicitly selected: " + name + " [" + addr + "]");
                        break;
                    }
                    if (name != null && (name.startsWith("LP-") || name.contains("TZD") || addr.equalsIgnoreCase("38:FC:98:E8:E3:87"))) {
                        mTargetDevice = d;
                        log("Target laptop matched: " + name + " [" + addr + "]");
                    }
                }
            }
        } catch (Exception e) {
            log("refreshDevices error: " + e.getMessage());
        }
        if (mTargetDevice == null && specificAddress != null && !specificAddress.isEmpty() && !"none".equalsIgnoreCase(specificAddress)) {
            try {
                BluetoothDevice dev = mAdapter.getRemoteDevice(specificAddress);
                if (dev != null) {
                    log("Target not in bonded devices. Requesting createBond() with " + specificAddress + "...");
                    dev.createBond();
                }
            } catch (Exception e) {
                log("createBond error: " + e.getMessage());
            }
        }
        writeStatusFile();
    }

    public void connectTarget(String specificAddress) {
        if (specificAddress != null || mTargetDevice == null) {
            refreshDevices(specificAddress);
        }
        if (mHidDevice != null && mTargetDevice != null) {
            log("Connecting to " + mTargetDevice.getName() + " [" + mTargetDevice.getAddress() + "]...");
            boolean ok = mHidDevice.connect(mTargetDevice);
            log("connect() returned: " + ok);
        } else {
            log("connect() failed: target device not found");
        }
    }

    public void disconnectTarget() {
        if (mHidDevice != null && mTargetDevice != null) {
            log("Disconnecting from " + mTargetDevice.getName() + "...");
            mHidDevice.disconnect(mTargetDevice);
        }
    }

    public void sendRawReport(byte[] report) {
        if (mHidDevice == null || mTargetDevice == null || report == null) {
            log("Cannot send raw report: device not ready");
            return;
        }
        mExecutor.execute(() -> {
            boolean ok = mHidDevice.sendReport(mTargetDevice, 1, report);
            log("sendRawReport(" + bytesToHex(report) + ") -> " + ok);
        });
    }

    public void sendWinL() {
        if (mHidDevice == null || mTargetDevice == null) {
            log("Cannot send Win+L: device not ready");
            return;
        }
        mExecutor.execute(() -> {
            log(">>> Executing Win+L sequence...");
            byte[] down = new byte[] { (byte) 0x08, 0x00, (byte) 0x0F, 0, 0, 0, 0, 0 };
            byte[] up = new byte[8];
            boolean r1 = mHidDevice.sendReport(mTargetDevice, 1, down);
            try { Thread.sleep(70); } catch (Exception ignored) {}
            boolean r2 = mHidDevice.sendReport(mTargetDevice, 1, up);
            log("Win+L down=" + r1 + ", up=" + r2);
        });
    }

    public void sendKey(byte modifier, byte keyCode, int durationMs) {
        if (mHidDevice == null || mTargetDevice == null) return;
        mExecutor.execute(() -> {
            byte[] down = new byte[] { modifier, 0, keyCode, 0, 0, 0, 0, 0 };
            byte[] up = new byte[8];
            mHidDevice.sendReport(mTargetDevice, 1, down);
            try { Thread.sleep(durationMs > 0 ? durationMs : 40); } catch (Exception ignored) {}
            mHidDevice.sendReport(mTargetDevice, 1, up);
            try { Thread.sleep(20); } catch (Exception ignored) {}
        });
    }

    public void typeText(String text) {
        if (mHidDevice == null || mTargetDevice == null || text == null) return;
        mExecutor.execute(() -> {
            log(">>> Typing string (" + text.length() + " chars)");
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                byte[] key = keyForChar(c);
                byte modifier = key[0];
                byte code = key[1];

                if (code != 0) {
                    byte[] down = new byte[] { modifier, 0, code, 0, 0, 0, 0, 0 };
                    byte[] up = new byte[8];
                    mHidDevice.sendReport(mTargetDevice, 1, down);
                    try { Thread.sleep(25); } catch (Exception ignored) {}
                    mHidDevice.sendReport(mTargetDevice, 1, up);
                    try { Thread.sleep(25); } catch (Exception ignored) {}
                } else {
                    log("Cannot type character: U+" + String.format("%04X", (int) c));
                }
            }
            log(">>> Typing complete.");
        });
    }

    /**
     * Maps printable US-layout ASCII to a HID modifier byte and keycode.
     * Returns {0, 0} for characters the keyboard cannot produce.
     */
    private static byte[] keyForChar(char c) {
        byte mod = 0x00;
        byte code = 0x00;

        if (c >= 'a' && c <= 'z') code = (byte) (0x04 + (c - 'a'));
        else if (c >= 'A' && c <= 'Z') { mod = 0x02; code = (byte) (0x04 + (c - 'A')); }
        else if (c >= '1' && c <= '9') code = (byte) (0x1E + (c - '1'));
        else if (c == '0') code = 0x27;
        else if (c == ' ') code = 0x2C;
        else if (c == '\n' || c == '\r') code = 0x28;
        else if (c == '\t') code = 0x2B;
        else if (c == '.') code = 0x37;
        else if (c == ',') code = 0x36;
        else if (c == '-') code = 0x2D;
        else if (c == '=') code = 0x2E;
        else if (c == ';') code = 0x33;
        else if (c == '\'') code = 0x34;
        else if (c == '[') code = 0x2F;
        else if (c == ']') code = 0x30;
        else if (c == '\\') code = 0x31;
        else if (c == '`') code = 0x35;
        else if (c == '/') code = 0x38;
        else if (c == '!') { mod = 0x02; code = 0x1E; }
        else if (c == '@') { mod = 0x02; code = 0x1F; }
        else if (c == '#') { mod = 0x02; code = 0x20; }
        else if (c == '$') { mod = 0x02; code = 0x21; }
        else if (c == '%') { mod = 0x02; code = 0x22; }
        else if (c == '^') { mod = 0x02; code = 0x23; }
        else if (c == '&') { mod = 0x02; code = 0x24; }
        else if (c == '*') { mod = 0x02; code = 0x25; }
        else if (c == '(') { mod = 0x02; code = 0x26; }
        else if (c == ')') { mod = 0x02; code = 0x27; }
        else if (c == '_') { mod = 0x02; code = 0x2D; }
        else if (c == '+') { mod = 0x02; code = 0x2E; }
        else if (c == '{') { mod = 0x02; code = 0x2F; }
        else if (c == '}') { mod = 0x02; code = 0x30; }
        else if (c == '|') { mod = 0x02; code = 0x31; }
        else if (c == ':') { mod = 0x02; code = 0x33; }
        else if (c == '"') { mod = 0x02; code = 0x34; }
        else if (c == '~') { mod = 0x02; code = 0x35; }
        else if (c == '<') { mod = 0x02; code = 0x36; }
        else if (c == '>') { mod = 0x02; code = 0x37; }
        else if (c == '?') { mod = 0x02; code = 0x38; }

        return new byte[] { mod, code };
    }

    public void sendCombo(String combo) {
        if (mHidDevice == null || mTargetDevice == null || combo == null) return;
        mExecutor.execute(() -> {
            log(">>> Executing combo: " + combo);
            String[] parts = combo.toLowerCase(Locale.ROOT).split("\\+");
            byte modifier = 0;
            byte key = 0;

            for (String p : parts) {
                p = p.trim();
                if (p.equals("win") || p.equals("gui") || p.equals("cmd") || p.equals("super") || p.equals("meta")) modifier |= 0x08;
                else if (p.equals("ctrl") || p.equals("control")) modifier |= 0x01;
                else if (p.equals("alt") || p.equals("opt") || p.equals("option")) modifier |= 0x04;
                else if (p.equals("shift")) modifier |= 0x02;
                else if (p.equals("del") || p.equals("delete")) key = 0x4C;
                else if (p.equals("backspace")) key = 0x2A;
                else if (p.equals("esc") || p.equals("escape")) key = 0x29;
                else if (p.equals("enter") || p.equals("return")) key = 0x28;
                else if (p.equals("tab")) key = 0x2B;
                else if (p.equals("space")) key = 0x2C;
                else if (p.equals("up")) key = 0x52;
                else if (p.equals("down")) key = 0x51;
                else if (p.equals("left")) key = 0x50;
                else if (p.equals("right")) key = 0x4F;
                else if (p.equals("home")) key = 0x4A;
                else if (p.equals("end")) key = 0x4D;
                else if (p.equals("pageup") || p.equals("pgup")) key = 0x4B;
                else if (p.equals("pagedown") || p.equals("pgdn")) key = 0x4E;
                else if (p.equals("insert")) key = 0x49;
                else if (p.equals("capslock") || p.equals("caps")) key = 0x39;
                else if (p.equals("numlock")) key = 0x53;
                else if (p.equals("scrolllock") || p.equals("scroll")) key = 0x47;
                else if (p.equals("printscreen") || p.equals("prtsc") || p.equals("print")) key = 0x46;
                else if (p.equals("pause") || p.equals("break")) key = 0x48;
                else if (p.equals("menu") || p.equals("apps") || p.equals("context")) key = 0x65;
                else if (p.equals("minus")) key = 0x2D;
                else if (p.equals("equal") || p.equals("equals")) key = 0x2E;
                else if (p.equals("leftbracket") || p.equals("lbracket")) key = 0x2F;
                else if (p.equals("rightbracket") || p.equals("rbracket")) key = 0x30;
                else if (p.equals("backslash")) key = 0x31;
                else if (p.equals("semicolon")) key = 0x33;
                else if (p.equals("quote") || p.equals("apostrophe")) key = 0x34;
                else if (p.equals("grave") || p.equals("backtick") || p.equals("tilde")) key = 0x35;
                else if (p.equals("comma")) key = 0x36;
                else if (p.equals("period") || p.equals("dot")) key = 0x37;
                else if (p.equals("slash") || p.equals("forwardslash")) key = 0x38;
                else if (p.startsWith("f") && p.length() >= 2) {
                    try {
                        int fNum = Integer.parseInt(p.substring(1));
                        if (fNum >= 1 && fNum <= 12) {
                            key = (byte) (0x3A + (fNum - 1));
                        }
                    } catch (Exception ignored) {}
                }
                else if (p.length() == 1) {
                    char c = p.charAt(0);
                    if (c >= 'a' && c <= 'z') key = (byte) (0x04 + (c - 'a'));
                    else if (c >= '1' && c <= '9') key = (byte) (0x1E + (c - '1'));
                    else if (c == '0') key = 0x27;
                }
            }

            byte[] down = new byte[] { modifier, 0, key, 0, 0, 0, 0, 0 };
            byte[] up = new byte[8];
            mHidDevice.sendReport(mTargetDevice, 1, down);
            try { Thread.sleep(60); } catch (Exception ignored) {}
            mHidDevice.sendReport(mTargetDevice, 1, up);
            log("Combo sent: mod=" + modifier + ", key=" + key);
        });
    }

    /**
     * Sends a real HID mouse gesture (buttons or wheel) using report ID 2.
     * Called from the Go CLI as: cmd=mouse, action=click-left|click-right|
     * click-middle|double-click|scroll-up|scroll-down.
     */
    public void sendMouse(String action) {
        if (mHidDevice == null || mTargetDevice == null || action == null) {
            log("Cannot send mouse: device not ready");
            return;
        }
        final String a = action.trim().toLowerCase(Locale.ROOT);
        mExecutor.execute(() -> {
            byte buttons = 0;
            int wheel = 0;
            boolean doubleClick = false;

            switch (a) {
                case "click-left":
                case "left-click":
                case "leftclick":
                case "lclick":
                    buttons = 0x01;
                    break;
                case "click-right":
                case "right-click":
                case "rightclick":
                case "rclick":
                    buttons = 0x02;
                    break;
                case "click-middle":
                case "middle-click":
                case "middleclick":
                case "mclick":
                    buttons = 0x04;
                    break;
                case "double-click":
                case "doubleclick":
                case "dblclick":
                    buttons = 0x01;
                    doubleClick = true;
                    break;
                case "scroll-up":
                case "scrollup":
                case "wheel-up":
                    wheel = 1;
                    break;
                case "scroll-down":
                case "scrolldown":
                case "wheel-down":
                    wheel = -1;
                    break;
                default:
                    log("Unknown mouse action: " + action);
                    return;
            }

            sendMouseReport(buttons, wheel);
            if (doubleClick) {
                try { Thread.sleep(80); } catch (Exception ignored) {}
                sendMouseReport(buttons, wheel);
            }
            log(">>> Mouse sent: " + a + " (buttons=" + buttons + ", wheel=" + wheel + ")");
        });
    }

    private void sendMouseReport(byte buttons, int wheel) {
        byte[] down = new byte[] { buttons, 0x00, 0x00, (byte) wheel };
        byte[] up = new byte[4];
        boolean r1 = mHidDevice.sendReport(mTargetDevice, 2, down);
        try { Thread.sleep(45); } catch (Exception ignored) {}
        boolean r2 = mHidDevice.sendReport(mTargetDevice, 2, up);
        if (!r1 || !r2) {
            log("sendMouseReport failed: down=" + r1 + ", up=" + r2);
        }
    }

    public void unpairTarget(String targetAddr) {
        if (mAdapter == null) return;
        try {
            Set<BluetoothDevice> paired = mAdapter.getBondedDevices();
            if (paired != null) {
                for (BluetoothDevice d : paired) {
                    if (targetAddr == null || targetAddr.isEmpty() || d.getAddress().equalsIgnoreCase(targetAddr)) {
                        log("Unpairing " + d.getName() + " [" + d.getAddress() + "]...");
                        Method m = d.getClass().getMethod("removeBond");
                        m.invoke(d);
                    }
                }
            }
            if (mTargetDevice != null && (targetAddr == null || targetAddr.isEmpty() || mTargetDevice.getAddress().equalsIgnoreCase(targetAddr))) {
                mTargetDevice = null;
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                writeStatusFile();
            }
        } catch (Exception e) {
            log("Unpair error: " + e.getMessage());
        }
    }

    private void writeStatusFile() {
        try {
            File f = new File(getExternalFilesDir(null), "status.json");
            FileWriter fw = new FileWriter(f);
            fw.write("{\n");
            fw.write("  \"registered\": " + mIsRegistered + ",\n");
            fw.write("  \"connected\": " + (mConnectionState == BluetoothProfile.STATE_CONNECTED) + ",\n");
            fw.write("  \"state\": \"" + stateToString(mConnectionState) + "\",\n");
            fw.write("  \"target\": \"" + (mTargetDevice != null ? mTargetDevice.getName() : "none") + "\",\n");
            fw.write("  \"address\": \"" + (mTargetDevice != null ? mTargetDevice.getAddress() : "none") + "\",\n");
            fw.write("  \"updated\": \"" + mTimeFormat.format(new Date()) + "\"\n");
            fw.write("}\n");
            fw.close();
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("cmd");

            if ("stop".equalsIgnoreCase(cmd)) {
                stopServiceCompletely();
                return START_NOT_STICKY;
            } else if (ACTION_LOCK.equals(action) || "lock".equalsIgnoreCase(cmd)) {
                sendWinL();
            } else if (ACTION_TYPE.equals(action) || "type".equalsIgnoreCase(cmd)) {
                typeText(intent.getStringExtra("text"));
            } else if ("combo".equalsIgnoreCase(cmd)) {
                sendCombo(intent.getStringExtra("keys"));
            } else if ("mouse".equalsIgnoreCase(cmd)) {
                sendMouse(intent.getStringExtra("action"));
            } else if ("raw".equalsIgnoreCase(cmd)) {
                String hex = intent.getStringExtra("hex");
                if (hex != null && hex.length() == 16) {
                    sendRawReport(hexToBytes(hex));
                }
            } else if ("connect".equalsIgnoreCase(cmd)) {
                connectTarget(intent.getStringExtra("address"));
            } else if ("disconnect".equalsIgnoreCase(cmd)) {
                disconnectTarget();
            } else if ("unpair".equalsIgnoreCase(cmd)) {
                unpairTarget(intent.getStringExtra("address"));
            } else if ("register".equalsIgnoreCase(cmd)) {
                registerHidApp();
            } else if ("status".equalsIgnoreCase(cmd)) {
                log("STATUS: Registered=" + mIsRegistered + ", State=" + stateToString(mConnectionState) + ", Target=" + (mTargetDevice != null ? mTargetDevice.getName() : "none"));
                writeStatusFile();
            }
        }
        return START_STICKY;
    }

    public void stopServiceCompletely() {
        log("Stopping GhostHID Gateway service and freeing Bluetooth resources...");
        disconnectTarget();
        if (mHidDevice != null) {
            try { mHidDevice.unregisterApp(); } catch (Exception ignored) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    public String stateToString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED: return "DISCONNECTED";
            case BluetoothProfile.STATE_CONNECTING: return "CONNECTING...";
            case BluetoothProfile.STATE_CONNECTED: return "CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTING: return "DISCONNECTING...";
            default: return "UNKNOWN(" + state + ")";
        }
    }

    public void log(String msg) {
        final String line = "[" + mTimeFormat.format(new Date()) + "] " + msg;
        Log.e(TAG, msg);
        if (mLogFile != null) {
            mExecutor.execute(() -> {
                try (FileWriter fw = new FileWriter(mLogFile, true)) {
                    fw.write(line + "\n");
                } catch (Exception ignored) {}
            });
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            b[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return b;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Headless service, no binding needed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHidDevice != null) {
            try { mHidDevice.unregisterApp(); } catch (Exception ignored) {}
            if (mAdapter != null) {
                mAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, mHidDevice);
            }
            mHidDevice = null;
        }
        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        writeStatusFile();
        log("GhostHID Gateway destroyed.");
    }
}
