# GhostHID Wire and Report Protocol Specification

This document specifies the binary report structures, Bluetooth L2CAP channel semantics, ADB broadcast intent formats, and filesystem state schemas used in GhostHID.

## 1. Bluetooth HID Layer

GhostHID operates over Bluetooth Classic (Basic Rate / Enhanced Data Rate) using the Human Interface Device (HID) Profile v1.1.

### L2CAP Channels
Communication between the Android phone and the host computer uses two Logical Link Control and Adaptation Protocol (L2CAP) channels:
- **Control Channel (PSM 0x0011):** Carries HID handshake requests, GET_REPORT, SET_REPORT, and connection state transitions.
- **Interrupt Channel (PSM 0x0013):** Transmits asynchronous input reports (keystrokes and mouse movements) from phone to target host.

```text
+-------------------------------------------------------------+
|                      Target Host OS                         |
|      (Windows kbdclass.sys / macOS AppleHIDKeyboard)        |
+-------------------------------------------------------------+
              ^                                ^
   Interrupt  | PSM 0x0013          Control    | PSM 0x0011
   Input Data | (Unidirectional)    Handshake  | (Bidirectional)
              v                                v
+-------------------------------------------------------------+
|               Android Bluetooth Stack (bluedroid)           |
|                BluetoothHidDevice HAL / Daemon              |
+-------------------------------------------------------------+
```

### Dual-Report Input Structures

GhostHID registers a composite SDP descriptor (`SUBCLASS1_COMBO`, 0x50) that multiplexes keyboard and mouse reports using 1-byte Report IDs.

#### Keyboard Input Report (Report ID 1)
Total payload size: 8 bytes.

```text
Byte 0: Modifier Bitmap
  Bit 0: Left Control   (0x01)
  Bit 1: Left Shift     (0x02)
  Bit 2: Left Alt       (0x04)
  Bit 3: Left GUI / Win (0x08)
  Bit 4: Right Control  (0x10)
  Bit 5: Right Shift    (0x20)
  Bit 6: Right Alt      (0x40)
  Bit 7: Right GUI      (0x80)

Byte 1: Reserved (Always 0x00)

Bytes 2 to 7: Keycode Array
  Byte 2: Keycode Slot 1 (0x00 = No key, 0x04-0x65 = HID Keycode)
  Byte 3: Keycode Slot 2
  Byte 4: Keycode Slot 3
  Byte 5: Keycode Slot 4
  Byte 6: Keycode Slot 5
  Byte 7: Keycode Slot 6
```

When no keys are pressed, an idle report consists of 8 zero bytes:
`00 00 00 00 00 00 00 00`

When `Win + L` is pressed, the report payload is:
`08 00 0F 00 00 00 00 00` (Modifier 0x08 = Left GUI, Keycode 0x0F = 'l').

#### Mouse Input Report (Report ID 2)
Total payload size: 4 bytes.

```text
Byte 0: Button Bitmap
  Bit 0: Button 1 (Left Click)    (0x01)
  Bit 1: Button 2 (Right Click)   (0x02)
  Bit 2: Button 3 (Middle Click)  (0x04)
  Bits 3 to 7: Constant padding   (0x00)

Byte 1: X Displacement (Signed 8-bit integer, -127 to +127 relative)
Byte 2: Y Displacement (Signed 8-bit integer, -127 to +127 relative)
Byte 3: Vertical Scroll Wheel (Signed 8-bit integer, -127 to +127 relative)
```

Examples:
- Left click press: `01 00 00 00`
- Right click press: `02 00 00 00`
- Scroll wheel up: `00 00 00 01`
- Scroll wheel down: `00 00 00 FF` (two's complement -1)
- Release all mouse buttons: `00 00 00 00`

#### Keyboard LED Output Report
The target host transmits 1-byte output reports to report lock key statuses:
- Bit 0: Num Lock (0x01)
- Bit 1: Caps Lock (0x02)
- Bit 2: Scroll Lock (0x04)
- Bit 3: Compose (0x08)
- Bit 4: Kana (0x10)

---

## 2. HID Keycode Translation Table

GhostHID maps US-ASCII characters and token names to USB HID Usage Page 0x07 (Keyboard/Keypad).

### Alphanumeric Mappings
| Token / Character | Modifier Byte | HID Usage Code (Hex) | Description |
|:------------------|:--------------|:---------------------|:------------|
| `a` through `z`   | `0x00`        | `0x04` to `0x1D`     | Letters a to z |
| `A` through `Z`   | `0x02` (Shift)| `0x04` to `0x1D`     | Capital letters A to Z |
| `1` through `9`   | `0x00`        | `0x1E` to `0x26`     | Numbers 1 to 9 |
| `0`               | `0x00`        | `0x27`               | Number 0 |

### Control and Navigation Mappings
| Token | Modifier Byte | HID Usage Code (Hex) | Description |
|:------|:--------------|:---------------------|:------------|
| `enter` / `return`| `0x00`     | `0x28`               | Return / Enter |
| `esc` / `escape`  | `0x00`     | `0x29`               | Escape |
| `backspace`       | `0x00`     | `0x2A`               | Backspace |
| `tab`             | `0x00`     | `0x2B`               | Horizontal Tab |
| `space`           | `0x00`     | `0x2C`               | Spacebar |
| `capslock`        | `0x00`     | `0x39`               | Caps Lock |
| `f1` to `f12`     | `0x00`     | `0x3A` to `0x45`     | Function Keys F1-F12 |
| `printscreen`     | `0x00`     | `0x46`               | Print Screen |
| `scrolllock`      | `0x00`     | `0x47`               | Scroll Lock |
| `pause` / `break` | `0x00`     | `0x48`               | Pause |
| `insert`          | `0x00`     | `0x49`               | Insert |
| `home`            | `0x00`     | `0x4A`               | Home |
| `pgup` / `pageup` | `0x00`     | `0x4B`               | Page Up |
| `del` / `delete`  | `0x00`     | `0x4C`               | Delete Forward |
| `end`             | `0x00`     | `0x4D`               | End |
| `pgdn` / `pagedown`| `0x00`    | `0x4E`               | Page Down |
| `right`           | `0x00`     | `0x4F`               | Right Arrow |
| `left`            | `0x00`     | `0x50`               | Left Arrow |
| `down`            | `0x00`     | `0x51`               | Down Arrow |
| `up`              | `0x00`     | `0x52`               | Up Arrow |
| `numlock`         | `0x00`     | `0x53`               | Num Lock |
| `menu` / `apps`   | `0x00`     | `0x65`               | Windows Context Menu Key |

### Symbol Mappings
| Character | Token | Modifier | Keycode |
|:----------|:------|:---------|:--------|
| `-` | `minus` | `0x00` | `0x2D` |
| `_` | | `0x02` (Shift) | `0x2D` |
| `=` | `equal` | `0x00` | `0x2E` |
| `+` | | `0x02` (Shift) | `0x2E` |
| `[` | `leftbracket` | `0x00` | `0x2F` |
| `{` | | `0x02` (Shift) | `0x2F` |
| `]` | `rightbracket`| `0x00` | `0x30` |
| `}` | | `0x02` (Shift) | `0x30` |
| `\` | `backslash` | `0x00` | `0x31` |
| `\|` | | `0x02` (Shift) | `0x31` |
| `;` | `semicolon` | `0x00` | `0x33` |
| `:` | | `0x02` (Shift) | `0x33` |
| `'` | `quote` | `0x00` | `0x34` |
| `"` | | `0x02` (Shift) | `0x34` |
| `` ` `` | `grave` | `0x00` | `0x35` |
| `~` | | `0x02` (Shift) | `0x35` |
| `,` | `comma` | `0x00` | `0x36` |
| `<` | | `0x02` (Shift) | `0x36` |
| `.` | `period` | `0x00` | `0x37` |
| `>` | | `0x02` (Shift) | `0x37` |
| `/` | `slash` | `0x00` | `0x38` |
| `?` | | `0x02` (Shift) | `0x38` |
| `!` | | `0x02` (Shift) | `0x1E` |
| `@` | | `0x02` (Shift) | `0x1F` |
| `#` | | `0x02` (Shift) | `0x20` |
| `$` | | `0x02` (Shift) | `0x21` |
| `%` | | `0x02` (Shift) | `0x22` |
| `^` | | `0x02` (Shift) | `0x23` |
| `&` | | `0x02` (Shift) | `0x24` |
| `*` | | `0x02` (Shift) | `0x25` |
| `(` | | `0x02` (Shift) | `0x26` |
| `)` | | `0x02` (Shift) | `0x27` |

---

## 3. ADB IPC Broadcast Protocol

The host triggers commands by dispatching Android broadcast intents via `adb shell am broadcast`:

```bash
adb shell am broadcast \
  -a com.sj010.ghid_headless.ACTION_CMD \
  -n com.sj010.ghid_headless/.HidCommandReceiver \
  --es cmd "<command>" \
  [optional arguments]
```

### Command Specifications

#### `connect`
Initiates an L2CAP connection to a bonded target device.
- Extras: `--es address <MAC>` (optional, defaults to matched target)
- Behavior: Looks up bonded devices matching address. If not bonded, triggers `createBond()`.

#### `disconnect`
Closes L2CAP interrupt and control channels.
- Extras: none
- Behavior: Invokes `BluetoothHidDevice.disconnect(target)`.

#### `type`
Types an ASCII string with automatic key down and key up delays.
- Extras: `--es text "<string>"`
- Behavior: Iterates through characters, maps to modifier + keycode, sleeps 25 ms down, sleeps 25 ms up.

#### `combo`
Executes a single keyboard chord.
- Extras: `--es keys "<modifiers+keys>"`
- Example: `--es keys "ctrl+shift+esc"`
- Behavior: Sends down report, sleeps 60 ms, sends release report.

#### `mouse`
Transmits a pointer gesture using Report ID 2.
- Extras: `--es action "<action_token>"`
- Supported actions: `click-left`, `click-right`, `click-middle`, `double-click`, `scroll-up`, `scroll-down`.
- Behavior: Sends a 4-byte button report, holds it for 48 ms, then sends a release report. `double-click` is two complete press/release cycles separated by a 90 ms gap, so Windows and macOS both synthesize a native double click instead of receiving a vendor-specific gesture. Relative motion is coalesced and split into ≤127-count reports, so fast drags are never clipped by the 8-bit HID field.

#### `raw`
Transmits an explicit 8-byte hexadecimal report.
- Extras: `--es hex "<16_hex_characters>"`
- Example: `--es hex "08000F0000000000"` (Win + L)
- Behavior: Parses byte array and dispatches directly via `BluetoothHidDevice.sendReport(target, 1, bytes)`.

#### `stop`
Gracefully cleans up Bluetooth resources and kills the Android foreground service.
- Extras: none
- Behavior: Disconnects target, unregisters SDP app, removes foreground notification, calls `stopSelf()`.

---

## 4. Filesystem State and Logging Protocol

The Android gateway writes state and log files directly to external application storage (`/sdcard/Android/data/com.sj010.ghid_headless/files/`).

### Status File (`status.json`)
The service overwrites this file on every state transition (SDP registration, connection change, disconnect).

```json
{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "LP-TZD-0X114650",
  "address": "38:FC:98:E8:E3:87",
  "updated": "2026-10-23 01:15:30.122"
}
```

Field definitions:
- `registered` (bool): `true` when Android SDP record is registered with the Bluetooth stack.
- `connected` (bool): `true` when L2CAP channels to target are active (`STATE_CONNECTED`).
- `state` (string): Current state representation: `DISCONNECTED`, `CONNECTING...`, `CONNECTED`, or `DISCONNECTING...`.
- `target` (string): Bluetooth friendly name of connected host.
- `address` (string): MAC address of target host.
- `updated` (string): Timestamp formatted as `yyyy-MM-dd HH:mm:ss.SSS`.

### Gateway Log File (`gateway.log`)
Appended line by line on each service operation.

Example content:
```text
[2026-10-23 01:15:20.001] GhostHID Gateway Service started (Headless Mode).
[2026-10-23 01:15:20.015] Bluetooth enabled=true, LocalName=KB2001
[2026-10-23 01:15:20.040] BluetoothHidDevice proxy connected.
[2026-10-23 01:15:20.052] registerApp() returned: true
[2026-10-23 01:15:20.089] onAppStatusChanged: registered=true
[2026-10-23 01:15:21.100] Target laptop matched: LP-TZD-0X114650 [38:FC:98:E8:E3:87]
[2026-10-23 01:15:21.105] Connecting to LP-TZD-0X114650 [38:FC:98:E8:E3:87]...
[2026-10-23 01:15:21.112] connect() returned: true
[2026-10-23 01:15:22.340] onConnectionStateChanged: [LP-TZD-0X114650] -> CONNECTED
[2026-10-23 01:15:25.410] >>> Executing combo: ctrl+alt+del
[2026-10-23 01:15:25.480] Combo sent: mod=5, key=76
```
