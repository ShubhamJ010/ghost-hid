# Prompt: Build Your Own GhostHID Headless Android App

Copy everything below the rule into an AI coding agent (or hand it to a developer).
It is the complete, self-contained contract the `ghid` CLI expects from a **headless**
Android gateway app — an app with no UI of its own that only talks Bluetooth HID and
answers ADB broadcasts.

This repository ships the **non-headless** app (`android/`, Jetpack Compose + Material 3
Expressive UI). The headless variant is intentionally *not* kept in this repo anymore; use
this prompt to code your own from scratch. The contract below is the only thing that must
match — everything else (language, project layout, state management) is your choice.

---

You are building **GhostHID Headless**, an Android app that turns the phone into a Bluetooth
HID keyboard + mouse driven over ADB by the `ghid` CLI. No launcher icon, no screens, no
Compose, no Material 3. A foreground service, a broadcast receiver, and files on disk —
that is the entire app.

Reference implementation (the non-headless app) lives in this repo under `android/app/src/main/java/com/sj010/ghid/`.
Protocol background: `docs/protocol-spec.md`, `docs/architecture.md`, `docs/flows.md`,
`docs/decisions/ADR-001-*`, `ADR-002-*`, `ADR-008-*`.

## 1. Identity (drop-in compatible — exact values, do not invent new ones)

| Item | Value |
|---|---|
| Application ID | `com.sj010.ghid` |
| Foreground service | `com.sj010.ghid/.service.HidService` |
| Broadcast receiver | `com.sj010.ghid/.service.HidCommandReceiver` |
| Command action | `com.sj010.ghid.ACTION_CMD` |
| Lock action | `com.sj010.ghid.ACTION_LOCK` |
| Type action | `com.sj010.ghid.ACTION_TYPE` |
| Boot action | `android.intent.action.BOOT_COMPLETED` |
| Public activity | `com.sj010.ghid/.MainActivity` (see §6) |
| minSdk / targetSdk | 28 / 35 |

There is **no** backward compatibility with the old `com.sj010.ghid_headless` package:
its package name, action strings, component names and file paths are all dead. Do not
mirror, translate, or fall back to them.

## 2. Manifest

Permissions: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`,
`BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`,
`RECEIVE_BOOT_COMPLETED`.

- `HidService`: `exported="true"`, `android:foregroundServiceType="connectedDevice"`.
- `HidCommandReceiver`: `exported="true"`, intent filters for `ACTION_CMD`, `ACTION_LOCK`,
  `ACTION_TYPE`, `BOOT_COMPLETED`.
- The service must call `startForeground(1001, notification, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)`
  inside `onCreate()` on API ≥ 29, and must tolerate a `SecurityException` (fall back to the
  plain notification) instead of crashing.
- `adb shell` cannot grant runtime Bluetooth permissions, so the app must be usable after a
  single manual launch that grants them; keep requesting them defensively at every entry point.

## 3. Component lifecycle

### 3.1 `HidService` (the whole app)

- On `onCreate()`: create the notification channel, start foreground, open the
  `BluetoothHidDevice` profile proxy, build the descriptor, call `registerApp()`, start
  writing `status.json`, and set the log file to
  `/sdcard/Android/data/com.sj010.ghid/files/gateway.log`.
- `onStartCommand` must accept an intent with **no action and no extras** — `ghid start`
  is literally `adb shell am start-foreground-service -n com.sj010.ghid/.service.HidService`.
  A null/blank intent means "start up, register SDP, write status", nothing more.
- `onDestroy`: `unregisterApp()`, close the profile proxy, write a final `status.json`, stop
  foreground with `STOP_FOREGROUND_REMOVE`.
- Keep a static `instance` field. `HidCommandReceiver` forwards to it when the service is
  alive, and only starts the foreground service when it is not.

### 3.2 `HidCommandReceiver`

Forwards each intent to `HidService.instance` if present; otherwise starts
`HidService` as a foreground service (API ≥ 26) carrying the same action and extras, with a
`startService` fallback.

## 4. Broadcast command contract

Every command arrives as `am broadcast -n com.sj010.ghid/.service.HidCommandReceiver -a com.sj010.ghid.ACTION_CMD --es cmd <name> [--es <key> <value>]`.
Commands are matched case-insensitively.

| `cmd` | Extras | Required behavior |
|---|---|---|
| `connect` | `address` (optional) | Connect to the bonded target; with no address, the last/selected device. Persist selection. |
| `disconnect` | — | Disconnect the HID link. |
| `unpair` | `address` (optional) | Remove the Bluetooth bond. |
| `stop` | — | `stopSelf()`; service tears down and writes final status. |
| `combo` | `keys` | Execute a chord or one-shot key, e.g. `win+s`, `ctrl+alt+del`, `enter`. |
| `type` | `text` | Type a printable ASCII string (also triggered by action `ACTION_TYPE`). |
| `mouse` | `action` | One of `click-left`, `click-right`, `click-middle`, `double-click`, `scroll-up`, `scroll-down`. |
| `raw` | `hex`, optional `id` | Send the hex bytes as a raw report. With `id`, use that Report ID; without it, infer: 1 byte → 3 (system control), 2 → 4 (consumer), 4 → 2 (mouse), 8 → 1 (keyboard). |
| `media` | `key` | `play`/`pause`/`playpause` → 0x00CD, `next` → 0x00B5, `prev` → 0x00B6, `volup` → 0x00E9, `voldown` → 0x00EA, `mute` → 0x00E2 (consumer page). |
| `sleep` | — | macOS target: run `cmd+ctrl+q ->{0.5} esc`; otherwise System Control Sleep (report ID 3, usage 0x82). |
| `lock` | — | Windows: `Win+L`; macOS: `cmd+ctrl+q`. Also triggered by action `ACTION_LOCK` with no extras. |
| `register` | — | Re-run `registerApp()` (SDP re-registration). |
| `status` | — | Rewrite `status.json` immediately. |

Unrecognized commands are ignored silently — never crash a receiver.

## 5. Files the CLI reads (exact paths and schemas)

All under `/sdcard/Android/data/com.sj010.ghid/files/`:

### `status.json` — written on every state change, atomically enough to be cat-able

```json
{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "LAPTOP-TOP",
  "address": "38:FC:98:E8:E3:87",
  "os": "windows",
  "os_confidence": "high",
  "detection_method": "led",
  "updated": "2026-10-25 09:12:44.104"
}
```

- `state` ∈ `DISCONNECTED` / `CONNECTING` / `CONNECTED` / `DISCONNECTING` / `UNKNOWN`.
- `os` ∈ `windows` / `macos` / `linux` / `unknown`; `os_confidence` ∈ `high` / `medium` /
  `low` / `none`; `detection_method` ∈ `apple_oui` / `apple_sdp_uuid` /
  `hid_led_handshake` / `name_heuristic` / `fallback` / `none`.
- `updated` is formatted `yyyy-MM-dd HH:mm:ss.SSS`.
- The CLI reads this with `adb shell cat`; a file that does not start with `{` is treated as
  an offline gateway. Never leave a partially written file behind.

### `gateway.log` — appended, never truncated at runtime

Line format: `[yyyy-MM-dd HH:mm:ss.SSS] message` (the app's `timeFormat`). The CLI dumps it with `ghid filelog` and tails
logcat with `ghid logs`, which greps for the logcat tag **`GHOST_HID`** — so log every
significant event through `Log.d("GHOST_HID", ...)` *and* to this file (service start/destroy,
SDP registered, connect/disconnect/unpair, each command received, HID send failures).

### `combos.json` and `snippets.json`

The host is the source of truth for these: `ghid sync` pushes/pulls them with `adb push`/`cat`
and then force-stops and restarts the app to reload. Your app must:

- tolerate both files being absent, empty, or rewritten while running;
- treat their contents as opaque JSON if it does not itself execute saved combos;
- reload them on the next service start after a restart;
- **never** write secrets: `snippets` marked secure are scrubbed by the host before pushing.

## 6. `MainActivity` (required, but headless)

There is no launcher icon: do **not** add `category.LAUNCHER`. Ship an exported
`com.sj010.ghid/.MainActivity` whose entire job is to start `HidService` and `finish()`.
`ghid sync` restarts the app with `adb shell am start -n com.sj010.ghid/.MainActivity`, so
this component must exist and must work from a cold, force-stopped process.

## 7. Bluetooth HID requirements

- Use `BluetoothHidDevice` (API 28+) and register with
  `BluetoothHidDeviceAppSdpSettings("GhostHID Keyboard & Mouse", "Virtual HID Keyboard + Mouse Gateway", "Android", BluetoothHidDevice.SUBCLASS1_COMBO, descriptor)` —
  the combo subclass (`0xC0`) advertises keyboard **and** mouse, which is what makes Windows
  bind both drivers.
- One **composite descriptor** carrying four report IDs:

| Report ID | Report | Bytes |
|---|---|---|
| 1 | Keyboard: modifier byte, reserved, 6× keycode | 8 |
| 2 | Mouse: buttons, X, Y, wheel | 4 |
| 3 | System control: usage page 0x01, usage 0x82 (sleep) | 1 |
| 4 | Consumer: 16-bit usage (media keys) | 2 |

- Send keyboard reports as **press then release** pairs; mouse clicks as button-down then an
  all-zero release report; wheel ticks as ±1 on the wheel byte.
- Output-report LED handling (report ID 1, output reports) must be consumed or Windows will
  warn about an unhandled HID report.
- Changing the descriptor, report IDs, or subclasses invalidates an existing Windows pairing:
  the app should log `SDP registered` when `onAppStatusChanged(plugged, true)` fires, because
  `ghid status --json` keys off `registered`.
- Passive host OS fingerprinting (optional but expected by `status.json`): classify from the
  target's OUI, name, service UUIDs, and the caps-lock LED handshake, then persist into
  `os` / `os_confidence` / `detection_method`.

## 8. Build & install

- Plain Gradle Kotlin/Java project; the CLI ships a release build. Keep
  `applicationId com.sj010.ghid`, `versionCode` monotonic, and sign with the same key the
  non-headless app uses so `adb install -r` upgrades in place.
- `ghid install-apk` installs `build/ghost-hid.apk` from the repo root; to be installable
  that way, publish your artifact there, or simply install yours with
  `adb install -r <your.apk>`.
- No ProGuard surprises: if you minify, keep `HidService` and `HidCommandReceiver` (they are
  referenced from the manifest) and verify with an install.

## 9. Acceptance checklist — verify on the real phone, not in a unit test

```bash
ghid doctor --json          # "GhostHID APK" check must pass for com.sj010.ghid
ghid status --json          # reads status.json, exit 0 when connected
ghid start && ghid status   # service starts from a blank intent, SDP registered: true
ghid combo "win+r"          # combo broadcast reaches the phone and the PC
ghid quick "hello" --json   # type command
ghid quick --raw "cd00"     # consumer report via inferred report ID 4
ghid logs -n 50             # logcat lines tagged GHOST_HID
ghid filelog                # gateway.log content
ghid sync --json            # push/pull combos + restart + reconnect
ghid stop                   # clean shutdown, status.json shows offline
```

Then on the target PC: pair once, confirm it binds as a Bluetooth **keyboard and mouse**
(not an audio device), and that typing, clicking, and scrolling all work.

## 10. Out of scope

Compose UI, Material 3 theming, on-device combo/snippet editors, encrypted preferences, and
anything `com.sj010.ghid_headless` used to do. The host CLI owns interaction; the phone owns
radio and keystrokes.
