# Automation and Agent Integration Guide

This guide details how AI agents, CI runners, and automated scripts interface with GhostHID without human intervention.

---

## 1. Integration Principles

GhostHID provides a non-interactive interface designed for programmatic execution:
- **`--json` Flag:** When passed to any command, interactive terminal prompts and ANSI escape codes are disabled. Output is emitted strictly as valid JSON to standard output.
- **Exit Codes:**
  - `0`: Operation succeeded or condition verified.
  - `1`: Operation failed, device disconnected, validation error, or resource unavailable.
- **Standard Error Isolation:** Fatal process diagnostics or ADB stderr messages are isolated to stderr so JSON parsers on stdout remain intact.

---

## 2. Complete JSON Schemas

### `ghid status --json`
Inspects the live connection, separating the ADB gateway from the Bluetooth HID link.

#### Response
```json
{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "LP-TZD-0X114650",
  "address": "38:FC:98:E8:E3:87",
  "os": "windows",
  "os_source": "hid_fingerprint",
  "os_confidence": "high",
  "detection_method": "apple_oui",
  "updated": "2026-10-23 01:15:30.122",
  "can_send": true,
  "secure_payloads": "device_only",
  "gateway": {
    "adb_connected": true,
    "serial": "192.168.1.80:38211"
  },
  "hid_link": {
    "registered": true,
    "connected": true,
    "state": "CONNECTED",
    "target": "LP-TZD-0X114650",
    "address": "38:FC:98:E8:E3:87"
  }
}
```

- `gateway.adb_connected` answers whether the CLI can reach the phone over ADB at all;
  `gateway.serial` is the attached transport. A missing phone makes the whole status
  object report `adb_connected: false` instead of a bare error.
- `hid_link` mirrors the phone's `status.json` (Bluetooth HID registration and target).
- `can_send` is the one boolean to gate on: ADB reachable **and** HID link connected.
- `os` comes from the passive HID fingerprint when available. Otherwise it is derived
  from the selected target device in `~/.config/ghid/config.json` and `os_source` is
  `selected_device`. `os_source` is `hid_fingerprint`, `selected_device`, or `none`.
- `secure_payloads` is always `device_only`: secure payload values never cross between
  the CLI and the Android app.
- `os_confidence` is `high`, `medium`, `low`, or `none`; `detection_method` is one of
  `apple_oui`, `apple_sdp_uuid`, `hid_led_handshake`, `name_heuristic`, `fallback`,
  or `none`. Older Android builds omit both; treat them as absent, not an error.

Exit code: `0` if `connected == true`, `1` if the HID link is not connected. The JSON
body is emitted in both cases, so parse it rather than relying on the exit code alone.

---

### `ghid doctor --json`
Runs a complete hardware, transport, and credentials health check.

#### Response
```json
{
  "version": "0.1.0",
  "adb_path": "/opt/homebrew/bin/adb",
  "adb_available": true,
  "device_connected": true,
  "device_model": "KB2001",
  "device_name": "SJ's OnePlus 8T",
  "device_serial": "192.168.1.80:41839",
  "apk_installed": true,
  "bluetooth_on": true,
  "gateway_registered": true,
  "gateway_connected": true,
  "target_laptop": "LP-TZD-0X114650",
  "target_address": "38:FC:98:E8:E3:87",
  "selected_device": {
    "id": "dev-1",
    "name": "LP-TZD-0X114650",
    "address": "38:FC:98:E8:E3:87",
    "platform": "windows",
    "selected": true
  },
  "registered_devices": 1,
  "keychain_available": true,
  "cached_user": "Administrator",
  "has_cached_password": true,
  "payloads_count": 7
}
```

`apk_installed` reflects package `com.sj010.ghid`. Exit code: `0` whenever a report is
produced (judge health from the booleans), `2` if the ADB gateway cannot be constructed — then
stdout carries `{"error": "..."}` instead. The human-readable `ghid doctor` run prints the
per-check list; the JSON form carries the same facts as flat booleans.

---

### `ghid combo schema --json`
Returns the grammar plus the complete token inventory.

#### Response
```json
{
  "grammar": "text outside braces typed literally; {chord}, {delay}, {key}, {consumer}, {PAYLOAD}",
  "step_types": ["keyboard chord: ctrl+shift+esc", "mouse gesture: click-left, scroll-up", "consumer key: {VolumeUp}", "payload injection: {USERNAME}, {WIN_SLEEP}", "typed text: plain text outside braces"],
  "keys": ["enter", "esc", "tab", "space", "backspace", "del", "f1", "...", "a", "0"],
  "modifiers": ["mod", "win", "ctrl", "alt", "shift"],
  "consumer": ["volumeup", "volumedown", "mute", "playpause"],
  "mac_aliases": ["cmd", "command", "super", "meta", "option", "opt"],
  "mouse_actions": ["click-left", "click-right", "click-middle", "double-click", "scroll-up", "scroll-down"],
  "fkey_range": "f1..f24",
  "aliases": {"return": "enter", "escape": "esc", "control": "ctrl", "opt": "alt", "cmd": "win"},
  "payload_convention": "Any braced identifier that is not a key, modifier, mouse action, delay, or raw hex report is a payload reference. Payload names are case-insensitive.",
  "notes": ["mod resolves to Command on macOS and Ctrl on Windows/Linux."],
  "os_tags": ["windows", "macos", "linux"],
  "default_os": "windows"
}
```

`ghid combo keys --json` returns the same token fields (`keys`, `modifiers`, `consumer`,
`mac_aliases`, `mouse_actions`, `fkey_range`, `aliases`, `payload_convention`, `notes`)
without the schema wrapper. `ghid combo keys <sequence>` keeps the historical dispatch
alias and sends that sequence.

Token facts agents hit in practice:
- F-keys are `f1`..`f24`. `{F13}`..`{F24}` are real HID usages, not payloads.
- `{shift}` alone is a valid modifier-only keyboard step on both CLI and Android.
- `playpause` is the media-key token. `play_pause` is the **payload** name
  (`PLAY_PAUSE` default raw report `cd00`), so `{PLAY_PAUSE}` resolves as a payload.
- Consumer keys cannot carry modifiers: `{ctrl+volumeup}` is rejected with a hint.

---

### `ghid combo validate "<sequence|combo name>" [--os ...] [--allow-unresolved] --json`
Parses a sequence for the target grammar **and resolves every payload reference**.

#### Success Response
```json
{
  "valid": true,
  "sequence": "{mod+r}{500ms}notepad{300ms}{Enter}",
  "os": "windows",
  "os_source": "selected_device",
  "steps": [
    {"type": "keyboard", "value": "mod+r", "delay_ms": 500, "modifier": 1, "keycode": 21, "report_hex": "0100150000000000"},
    {"type": "text", "value": "notepad", "delay_ms": 300},
    {"type": "keyboard", "value": "enter", "delay_ms": 0, "keycode": 40, "report_hex": "0000280000000000"}
  ],
  "payloads": ["PASSWORD"],
  "resolved_payloads": ["PASSWORD"],
  "unresolved_payloads": [],
  "empty_payloads": []
}
```

Grammar selection without `--os`, in order: the saved combo's OS when the argument is a
saved combo name; the selected target device's `platform`; Windows. `os_source` reports
`flag`, `combo`, `selected_device`, or `default`.

#### Error Response
```json
{
  "valid": false,
  "sequence": "{cmd+s}",
  "os": "windows",
  "os_source": "selected_device",
  "error": "\"cmd\" is a macOS key name; use a Windows key such as win, ctrl or alt"
}
```

Unresolved or valueless payload references make `valid` false and exit `1`, listing
`unresolved_payloads` / `empty_payloads` with a hint. `--allow-unresolved` downgrades
that to `valid: true` plus a `warning` (needed for secure payloads that exist only on
the phone).

Exit code: `0` if valid, `1` if invalid.

---

### `ghid combo show <name> --json`
Reads a saved combo back without touching `combos.json` by hand.

```json
{
  "status": "success",
  "combo": {"name": "Open Terminal", "sequence": "{mod+r}", "os": "windows", "favorite": true, "created_at": "2026-10-23T01:00:00Z"},
  "steps": [{"type": "keyboard", "value": "mod+r", "delay_ms": 0, "modifier": 1, "keycode": 21, "report_hex": "0100150000000000"}],
  "payloads": [],
  "resolved_payloads": [],
  "unresolved_payloads": [],
  "empty_payloads": []
}
```

---

### `ghid combo send <name> --dry-run --json`
Resolves the sequence into concrete HID actions without dispatching anything. Secure
values stay masked. The same flag works on a one-shot `ghid combo <sequence> --dry-run`.

```json
{
  "dry_run": true,
  "valid": true,
  "name": "Toggle Media",
  "sequence": "{shift}{PLAY_PAUSE}{500ms}",
  "os": "macos",
  "steps": [
    {"type": "keyboard", "token": "shift", "modifier": 2, "report_hex": "0200000000000000"},
    {"type": "raw", "payload": "PLAY_PAUSE", "payload_type": "raw", "report_id": 4, "raw_hex": "cd00", "payload_present": true},
    {"type": "delay", "delay_ms": 500}
  ],
  "payloads": ["PLAY_PAUSE"],
  "resolved_payloads": ["PLAY_PAUSE"],
  "unresolved_payloads": [],
  "empty_payloads": [],
  "dispatched": false
}
```

Exit code: `0` if every step and payload resolves, `1` otherwise (nothing is sent).

---

### `ghid combo list --json`
Returns all saved combos in the local library (`~/.config/ghid/combos.json`).

#### Response
```json
[
  {
    "name": "Login",
    "sequence": "{ctrl+alt+del}{1s}{Escape}{800ms}{USERNAME}{800ms}{Tab}{800ms}{PASSWORD}{800ms}{Enter}",
    "os": "windows",
    "emoji": "🔑",
    "description": "Windows unlock & credential injection",
    "favorite": true,
    "created_at": "2026-10-23T01:00:00Z"
  },
  {
    "name": "Lock",
    "sequence": "{mod+l}",
    "os": "windows",
    "emoji": "🔒",
    "description": "Instant Windows lock screen (Win + L)",
    "favorite": true,
    "created_at": "2026-10-23T01:00:00Z"
  },
  {
    "name": "Sleep",
    "sequence": "{WIN_SLEEP}",
    "os": "windows",
    "emoji": "😴",
    "description": "Hardware ACPI sleep sequence",
    "favorite": true,
    "created_at": "2026-10-23T01:00:00Z"
  }
]
```

Filter the library by target OS with `ghid combo list --os macos --json`.
Combos saved before OS tagging existed report `os: "windows"`. Tag a combo at
save time with `--os windows|macos|linux`; without the flag, updates keep the
existing tag. The same `os` value selects the matching filters and profile in
the Android app.

The optional `emoji` string is combo icon metadata. When present it overrides the
OS logo on the Android card and replaces the `[win]` / `[mac]` / `[linux]` badge
in `ghid combo list` and the TUI. Set it at save time with `--emoji <e>`; without
the flag, updates keep the existing emoji. Values must be an emoji (no ASCII
letters, digits or spaces, at most 12 code points) or the save fails with
`{"status":"error","error":"..."}` and exit 1.

#### `ghid combo save` payload and sync semantics
`combo save` resolves every `{NAME}` reference before writing. Missing names or
payloads with no value fail the save with the offending references and a hint.
Pass `--allow-unresolved` for a secure payload that only exists on the phone
(secure values never sync to the CLI).

The response states whether a push actually happened instead of assuming it:

```json
{
  "status": "saved",
  "updated": false,
  "synced": false,
  "combo": {"name": "Open Terminal", "sequence": "{mod+r}", "os": "macos"}
}
```

`synced` is true only when an attached phone was pushed successfully. Without a phone
the combo is saved locally and `synced` is false; a failed push adds `sync_error`.
The human output mirrors this with `(synced to Android)` or
`(local only; no Android device attached, not synced)`.

#### `ghid sync --push --json`
```json
{
  "status": "success",
  "mode": "push",
  "device": "192.168.1.80:38211",
  "report": {
    "combos_pulled": 0,
    "combos_pushed": 8,
    "payloads_pulled": 0,
    "payloads_pushed": 4,
    "payloads_skipped_secure": 3,
    "app_restarted": true,
    "reconnected": true
  }
}
```

---

### `ghid combo fav <name> --json`
Toggles the pinned favorite status (`❤️`) of a combo.

#### Response
```json
{
  "status": "success",
  "name": "Open Terminal",
  "favorite": true
}
```

---

### `ghid click [left|right|middle|double] [--double] [--count <n>] --json`
Dispatches an in-place mouse button click at the target system cursor's current resting position.

#### Architectural Notice for Autonomous AI Agents
> [!IMPORTANT]
> **No Pointer Coordinates or Cursor Movement:** GhostHID operates strictly at the USB HID hardware peripheral layer without knowledge of host screen geometry, window layout, or cursor coordinates. `ghid click` does not accept coordinates or relative offsets (`move x, y` / `dx, dy`). It only clicks wherever the cursor currently happens to be resting on screen.
>
> If an agent attempts to pass coordinates (e.g. `ghid click 500 300` or `ghid click --x 100`), GhostHID immediately rejects the invocation with exit code 1 and returns structured JSON with error code `COORDINATES_NOT_SUPPORTED`.

#### Success Response
```json
{
  "status": "success",
  "command": "click",
  "button": "left",
  "clicks": 1,
  "action": "click-left",
  "report_id": 2,
  "report_hex": "01000000",
  "coordinates_supported": false,
  "cursor_context": "resting_position",
  "note": "Clicked at resting cursor position. No pointer coordinates or cursor movement (move x, y / relative dx, dy) are supported by GhostHID."
}
```

#### Double-Click Response (`ghid click --double --json` or `ghid click double --json`)
```json
{
  "status": "success",
  "command": "click",
  "button": "left",
  "clicks": 2,
  "action": "double-click",
  "report_id": 2,
  "report_hex": "01000000",
  "coordinates_supported": false,
  "cursor_context": "resting_position",
  "note": "Clicked at resting cursor position. No pointer coordinates or cursor movement (move x, y / relative dx, dy) are supported by GhostHID."
}
```

#### Coordinate Attempt Error Response (`ghid click 100 200 --json`)
```json
{
  "status": "error",
  "error": "No pointer coordinates or cursor movement (move x, y / relative dx, dy) are supported. GhostHID operates at the USB HID hardware peripheral layer without screen geometry or cursor coordinate awareness. 'ghid click' only clicks wherever the cursor currently happens to be resting on screen. (coordinate value \"100\" is not supported)",
  "code": "COORDINATES_NOT_SUPPORTED",
  "coordinates_supported": false,
  "cursor_movement_supported": false,
  "usage": "ghid click [left|right|middle|double] [--double] [--count <n>] [--json]"
}
```

---

### `ghid quick <text...> [--raw] --json`
Unified ad-hoc transmission. Types text strings through the virtual keyboard by default, or transmits raw 8-byte HID reports when `--raw` is specified.

> **Note:** Replaces legacy root `ghid type` and `ghid raw` commands (which have been removed without backward-compatibility aliases).

#### Default Type Mode
```bash
ghid quick "Hello World" --json
```
```json
{
  "status": "success",
  "mode": "type",
  "typed": "Hello World"
}
```

#### Raw 16-Hex HID Report Mode
```bash
ghid quick --raw "08000f0000000000" --json
```
```json
{
  "status": "success",
  "mode": "raw",
  "raw": "08000f0000000000"
}
```

---

### `ghid payload <action> --json`
Inspects and manages text variables and encrypted secrets stored in
`~/.config/ghid/payloads.json` (and the macOS Keychain).

Every read returns the same shape: `name`, `type`, `secure`, `source` (`json` or
`keychain`), `present`, `has_value`, and `value` only for non-secure payloads.

#### List Payloads
```bash
ghid payload list --json
```
```json
[
  {
    "name": "username",
    "type": "type",
    "secure": false,
    "source": "json",
    "present": true,
    "has_value": true,
    "value": "Administrator",
    "created_at": "2026-10-23T01:00:00Z",
    "updated_at": "2026-10-23T01:00:00Z"
  },
  {
    "name": "password",
    "type": "type",
    "secure": true,
    "source": "keychain",
    "present": false,
    "has_value": false,
    "keychain_service": "ghost-hid",
    "keychain_account": "ghid_payload_password",
    "error": "secure value not found in Keychain"
  }
]
```

`present` is `false` when a secure payload's Keychain item is missing; the definition
still exists, but its value is unreachable. Secure entries never include `value`.

#### Get Payload
```bash
ghid payload get username --json
```
```json
{
  "name": "username",
  "type": "type",
  "secure": false,
  "source": "json",
  "present": true,
  "has_value": true,
  "value": "Administrator"
}
```

`ghid payload get` on a secure payload whose Keychain item is missing prints the view
with `present: false` and exits `1`; the human form prints the Keychain service and
account to inspect.

Keychain coordinates for secure payloads: service `ghost-hid`, account
`ghid_payload_<lowercase-name>` (for example `ghid_payload_password`). Manage them with
`/usr/bin/security` if needed.

#### Set Payload
```bash
ghid payload set api_token "secret_abc123" --secure --json
```

Flags are accepted in any position. `--type raw` and `--type=raw` are equivalent;
unknown flags are errors instead of being folded into the value. The JSON response uses
the same read shape as `get`, so it never echoes a secure value.

#### Delete Payload
```bash
ghid payload delete api_token --json
```
```json
{
  "status": "deleted",
  "name": "api_token",
  "synced": false
}
```

#### Secure payload synchronization
Secure payload values never sync in either direction: `sync --push` sends secure
entries with `Value` scrubbed (ADR-009) so phone-side references resolve, counts
them as `payloads_skipped_secure`, and keeps the secret in the local Keychain
only. Create phone-only secrets on the phone. `ghid status --json` always
reports `secure_payloads: "device_only"`.

---

### `ghid devices scan --json`
Queries the phone directly over ADB (`dumpsys bluetooth_manager`) for bonded Bluetooth devices. Enables zero-touch registration without needing to manually transcribe MAC addresses.

#### Response
```json
[
  {
    "address": "A0:9A:8E:57:36:DA",
    "name": "Shubham’s MacBook Air",
    "type": "DUAL"
  },
  {
    "address": "38:FC:98:E8:E3:87",
    "name": "LP-TZD-0X114650",
    "type": "BR/EDR"
  }
]
```

---

### `ghid devices list --json`
Lists all configured target hosts.

#### Response
```json
[
  {
    "id": "dev-1",
    "name": "LP-TZD-0X114650",
    "address": "38:FC:98:E8:E3:87",
    "platform": "windows",
    "selected": true
  },
  {
    "id": "dev-2",
    "name": "MacBook Pro M3",
    "address": "F8:FF:C2:11:22:33",
    "platform": "macos",
    "selected": false
  }
]
```

---

## 3. Automation Recipes

### Bash: Safe Pre-Flight and Keystroke Injection
```bash
#!/usr/bin/env bash
set -euo pipefail

# 1. Verify connection status
if ! ghid status --json >/dev/null 2>&1; then
    echo "Host disconnected. Attempting reconnect..."
    ghid reconnect || {
        echo "Failed to connect to target host" >&2
        exit 1
    }
    sleep 2
fi

# 2. Inject command
ghid combo "{mod+r}{500ms}cmd{300ms}{Enter}"
```

### Python: Automated Diagnostic and Action Dispatch
```python
import json
import subprocess
import sys

def run_ghid(cmd: list[str]) -> dict:
    result = subprocess.run(["ghid", *cmd, "--json"], capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"Command failed (exit {result.returncode}): {result.stderr or result.stdout}")
    return json.loads(result.stdout)

# Run health check
doctor = run_ghid(["doctor"])
if not doctor.get("gateway_connected"):
    print("Gateway not connected, attempting reconnection...")
    subprocess.run(["ghid", "reconnect"], check=True)

# Dispatch sequence
print("Dispatching combo sequence...")
subprocess.run(["ghid", "combo", "{ctrl+alt+del}{1s}{Escape}"], check=True)
```

### Node.js: Agent Tool Definition
```typescript
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const exec = promisify(execFile);

export async function sendHostCombo(keys: string): Promise<{ success: boolean; output: string }> {
  // 1. Validate combo grammar
  const { stdout: validationOut } = await exec("ghid", ["combo", "validate", keys, "--json"]);
  const validation = JSON.parse(validationOut);
  if (!validation.valid) {
    throw new Error(`Invalid combo syntax: ${validation.error}`);
  }

  // 2. Dispatch sequence
  const { stdout } = await exec("ghid", ["combo", keys]);
  return { success: true, output: stdout.trim() };
}
```
