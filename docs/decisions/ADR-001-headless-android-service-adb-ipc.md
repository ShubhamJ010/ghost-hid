# ADR-001: Headless Android Service with BroadcastReceiver IPC over ADB

## Status
Accepted (mechanics still current; package and paths superseded by [ADR-012](ADR-012-single-android-app-and-release-only-deployment.md))

## Date
2026-03-15

> **Amended 2026-10-25:** the foreground-service + `BroadcastReceiver` over ADB design stands,
> but the app it describes was renamed and rewritten as the Material 3 app, and the original
> `com.sj010.ghid_headless` sources were deleted. Current values: action
> `com.sj010.ghid.ACTION_CMD`, component `com.sj010.ghid/.service.HidCommandReceiver`, service
> `com.sj010.ghid/.service.HidService`, state file
> `/sdcard/Android/data/com.sj010.ghid/files/status.json`. Nothing falls back to the old paths.

## Context
GhostHID requires a mechanism to transmit keyboard and mouse HID reports from a host workstation (macOS, Linux, or CI agent) to a target computer (such as a Windows laptop) using an Android smartphone as the physical transmitter.

We needed an IPC channel between the host control software and the Android runtime that satisfied four requirements:
1. Works on unmodified stock Android firmware (no root permissions required).
2. Supports execution when the phone screen is off or locked.
3. Operates over both physical USB cables and local Wi-Fi networks.
4. Requires minimal latency and zero third-party background daemons running on the phone.

## Decision
Implement the Android component as a Foreground Service (`HidService`) paired with an explicit `BroadcastReceiver` (`HidCommandReceiver`), driven over ADB (`adb shell am broadcast` and `adb shell am start-foreground-service`).

Communication mechanics:
- Commands pass from the host CLI (`ghid`) to the phone via ADB command line arguments (`-a com.sj010.ghid_headless.ACTION_CMD --es cmd <command>`).
- State synchronizes back to the host through an explicit JSON file written to application external storage (`/sdcard/Android/data/com.sj010.ghid_headless/files/status.json`), which the host reads with `adb shell cat`.
- The Android service runs with `startForeground` and displays a persistent notification channel with `IMPORTANCE_LOW` to prevent Android's low-memory killer (OOM killer) from killing the service during long sessions.

## Alternatives considered

### Root-level Linux /dev/hidg0 gadget driver
- Pros: Direct kernel-level report injection without Android framework overhead.
- Cons: Requires custom kernel compilation, USB gadget mode support, and rooted devices. Stock consumer phones cannot run it.
- Rejected: Breaks portability across standard consumer phones.

### Local TCP socket server inside the Android app
- Pros: Bi-directional streaming connection over `adb forward tcp:...`.
- Cons: Requires port management, socket lifecycle handling on network drops, and reconnect logic when the phone enters deep sleep.
- Rejected: Broadcast intents provide stateless, decoupled command dispatch that survives process suspends without maintaining open sockets.

### ADB input commands (`adb shell input text`)
- Pros: No Android app required.
- Cons: Injects events into the phone itself, not into an external Bluetooth-paired host. Cannot act as a hardware HID peripheral to a remote PC.
- Rejected: Does not solve the external Bluetooth HID transmission requirement.

## Consequences
- No root access is required. Any Android 9+ (API 28+) device with Bluetooth HID Device profile support works out of the box.
- The phone can operate with its display turned off in headless mode.
- Host communication depends on ADB availability (USB or wireless debugging on port 5555 or dynamic pairing ports).
- Status inspection relies on disk writes in external storage; latency per status poll is bounded by ADB roundtrip time (~15-30 ms).
