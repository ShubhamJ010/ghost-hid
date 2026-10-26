# GhostHID Troubleshooting and Operational Runbook

This guide provides recovery procedures for connection, pairing, power management, and credential issues encountered when running GhostHID.

---

## 1. ADB Connection and Authorization Issues

### Symptoms
- `ghid` exits with error: `no android devices found`
- `adb devices` shows `<device_id> unauthorized` or `<device_id> offline`

### Root Cause
1. USB debugging prompt on the phone was dismissed or RSA key expired.
2. Android wireless debugging port changed after phone reboot or Wi-Fi reconnect.
3. ADB server state became stale.

### Recovery Steps

#### If Device is Unauthorized
1. Look at the phone screen.
2. Accept the prompt: **"Allow USB debugging?"** and check **"Always allow from this computer"**.
3. Re-run:
   ```bash
   adb devices
   ```

#### If Device is Offline or Stale
Restart the ADB host server:
```bash
adb kill-server
adb start-server
adb devices
```

#### Reconnecting over Wi-Fi
If using Wireless Debugging on Android 11+:
1. Open phone **Settings > Developer options > Wireless debugging**.
2. Note the IP address and port (for example, `192.168.1.80:41839`).
3. If connecting for the first time, tap **Pair device with pairing code** and run:
   ```bash
   adb pair 192.168.1.80:<pairing_port> <pairing_code>
   ```
4. Connect the main debugging channel:
   ```bash
   adb connect 192.168.1.80:<connect_port>
   ```
5. Update your `ghid` default wireless endpoint in `~/.config/ghid/config.json`:
   ```json
   {
     "wireless_adb_address": "192.168.1.80:41839"
   }
   ```

---

## 2. Windows Pairs Phone as Audio Device or Trackpad/Mouse Fails on Windows Laptop

### Symptoms
- In Windows Settings, the phone appears under "Other devices" or "Audio" instead of "Input" (Mouse, keyboard, & pen).
- Keystrokes sent by `ghid` have no effect on the target computer.
- Keyboard works on Windows, but the virtual **Trackpad / Mouse pointer** does not move, even though it works on macOS.

### Root Cause
1. **Windows Bluetooth SDP and HID Descriptor Caching**: Unlike macOS (which re-enumerates HID collections on every connection), Windows strictly caches the Bluetooth SDP records and HID report descriptors by MAC address under `HKLM\System\CurrentControlSet\Enum\BTHENUM`. If the phone was paired before GhostHID registered both Mouse (Report ID 2) and Keyboard (Report ID 1) collections, Windows only loads `kbdclass.sys` and permanently ignores mouse input packets.
2. **HID Control Handshake Protocol**: On Windows host connection, Windows immediately issues a `SET_REPORT` for keyboard LEDs and queries `GET_REPORT` for Feature reports. Replying with data packets instead of an explicit HID `HANDSHAKE` token or returning dummy data for non-input report types causes the Windows `bthhid.sys` driver stack to enter error state (Code 10), disabling the secondary mouse collection.

### Recovery Steps

#### Step 1: Remove Existing Windows Pairing
1. On the Windows laptop, open **Settings > Bluetooth & devices**.
2. Find the phone name in the device list.
3. Click the three dots menu and select **Remove device**.

#### Step 2: Unpair on the Phone
In the GhostHID Android app, unpair the Windows laptop, or run through CLI:
```bash
ghid unpair
```

#### Step 3: Register HID SDP Record Before Pairing
Ensure GhostHID is actively running with both keyboard and mouse descriptors registered:
```bash
ghid start
ghid status --json
```
Confirm that `"registered": true` is returned.

#### Step 4: Pair with Advanced Discovery
1. In Windows **Settings > Bluetooth & devices**, scroll down to **Bluetooth devices discovery**.
2. Change the dropdown from **Default** to **Advanced**.
3. Click **Add device > Bluetooth**.
4. Select the phone from the list.
5. Windows will query the active combo SDP record, identify `GhostHID Keyboard & Mouse`, and bind both `kbdclass.sys` and `mouclass.sys` drivers under Device Manager (`Human Interface Devices` > `Bluetooth Low Energy / Bluetooth HID Device`).

---

## 3. Bluetooth State Desynchronization

### Symptoms
- `status.json` reports `connected: true`, but keystrokes are ignored.
- Or, Windows displays the phone as connected, but `ghid` reports `DISCONNECTED`.

### Root Cause
L2CAP control and interrupt channels dropped due to physical range, interference, or host sleep, but one side failed to receive the disconnect frame.

### Recovery Steps
1. Cycle the Bluetooth connection from `ghid`:
   ```bash
   ghid reconnect
   ```
2. If the connection fails to recover, restart the Android gateway service:
   ```bash
   ghid stop
   sleep 1
   ghid start
   ```
3. Inspect the live gateway logs to verify L2CAP handshake:
   ```bash
   ghid logs -n 30
   ```
   Look for:
   `onConnectionStateChanged: [target] -> CONNECTED`

---

## 4. Target Computer Fails to Wake on Keystroke (ACPI Sleep)

### Symptoms
- `ghid login` is executed while the laptop lid is closed or asleep, but the laptop does not wake up.

### Root Cause
Modern PC laptops gate power to internal Intel or Qualcomm Bluetooth PCIe/USB controllers during ACPI S3 sleep and Modern Standby (S0ix). The Bluetooth controller cannot process incoming L2CAP connection requests when unpowered.

### Operational Rule
GhostHID is a controller, not a physical power injector:
1. Tap the laptop power button or built-in keyboard spacebar to power on the Bluetooth controller.
2. The phone will reconnect to the laptop within 1 to 2 seconds.
3. Run `ghid login` to unlock the computer.

---

## 5. Android Battery Optimization and Process Termination

### Symptoms
- GhostHID works immediately after launch, but stops responding after the phone screen has been off for 15 minutes.

### Root Cause
Android Doze mode and manufacturer-specific battery managers (such as OnePlus OxygenOS, Xiaomi MIUI, or Samsung OneUI) suspend background execution threads.

### Prevention Steps
GhostHID implements an ongoing foreground notification (`IMPORTANCE_LOW`) to prevent termination. To ensure zero throttling when the phone display is turned off:
1. Open phone **Settings > Apps > GhostHID**.
2. Tap **Battery** or **App battery usage**.
3. Select **Unrestricted** (prevents background CPU throttling and socket freezes).
4. In Developer Options, enable **Stay awake** (screen stays on while charging) if the phone is permanently plugged into a workstation.

---

## 6. macOS Keychain Access in Headless and SSH Sessions

### Symptoms
- `ghid login` fails with: `security: SecKeychainSearchCopyNext: The specified item could not be found in the keychain` or prompts for password interactively.
- Occurs when running `ghid` over SSH or in automated cron scripts.

### Root Cause
macOS locks the user keychain when there is no active GUI login session, or requires unlocking before a non-interactive shell can access stored secrets.

### Recovery Steps
Unlock the login keychain in your script or SSH profile before invoking `ghid`:
```bash
security unlock-keychain -p "<your_macos_login_password>" ~/Library/Keychains/login.keychain-db
```

Verify credential availability:
```bash
ghid creds status --json
```

If you do not want to use macOS Keychain, provide credentials directly at execution time:
```bash
ghid login -u "Administrator" -p "SecretPassword123"
```
Direct command-line passwords bypass Keychain retrieval.
