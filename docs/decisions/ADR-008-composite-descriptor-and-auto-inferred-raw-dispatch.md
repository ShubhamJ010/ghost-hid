# ADR-008: Composite 4-Report HID Descriptor and Auto-Inferred Length-Based Raw Dispatch

## Status
Accepted

## Date
2026-10-24

## Context
GhostHID originally registered a dual-report HID SDP descriptor ([ADR-002](ADR-002-dual-report-descriptor-for-keyboard-and-mouse.md)) multiplexing Keyboard (Report ID 1, 8 bytes) and Mouse (Report ID 2, 4 bytes). While this supported basic typing, chords, and pointer actions, it suffered from several architectural constraints:
1. **Sleep and Wake Fragility:** Putting Windows hosts to sleep required brittle OS keystroke sequences (`Win+X -> u -> s`), which fail when lock screens or modal dialogs are active. macOS and Linux have no equivalent universal key sequence.
2. **Media and Consumer Controls:** Controlling media playback (play/pause, volume, tracks) required either platform-specific shortcut chords or was unsupported over the standard keyboard descriptor.
3. **Report Routing Complexity:** Raw HID dispatch was previously locked to 16 hex characters (8-byte keyboard reports) or required callers to remember and specify internal numeric Report IDs (e.g., `<report_id>:<hex>`).
4. **Platform Specificity:** Combos rejected macOS modifier tokens (`cmd`, `command`, `opt`, `option`) because the validation was hardcoded to Windows key names.

## Decision

### 1. Composite 4-Report HID Descriptor
Extend the Android Bluetooth HID SDP record to include four top-level application collections with dedicated Report IDs:

- **Report ID 1: Keyboard (`0x05, 0x01, 0x09, 0x06`)**
  - Payload: 8 bytes (1 modifier byte, 1 reserved byte, 6 keycode bytes).
- **Report ID 2: Mouse (`0x05, 0x01, 0x09, 0x02`)**
  - Payload: 4 bytes (1 button bitmap, 1 X relative, 1 Y relative, 1 scroll wheel relative).
- **Report ID 3: System Control (`0x05, 0x01, 0x09, 0x80`)**
  - Payload: 1 byte (bit 0 = Power Down `0x01`, bit 1 = Sleep `0x02`, bit 2 = Wake Up `0x04`).
  - Enables instant, hardware-level ACPI sleep without UI dependency.
- **Report ID 4: Consumer Control (`0x05, 0x0C, 0x09, 0x01`)**
  - Payload: 2 bytes (16-bit little-endian consumer usage code).
  - Common codes: Play/Pause `0x00CD` (`raw:cd00`), Next Track `0x00B5`, Prev Track `0x00B6`, Volume Up `0x00E9`, Volume Down `0x00EA`, Mute `0x00E2`, Release `0x0000`.

### 2. Auto-Inferred Length-Based Raw Dispatch
Rather than forcing users or calling agents to specify `<report_id>`, the gateway and CLI automatically infer the target HID Report ID directly from the payload byte length:

| Payload Byte Length | Hex Length | Target Report ID | HID Collection | Example |
|---|---|---|---|---|
| **1 byte** | 2 hex chars | **3** | System Control | `raw:02 ->{0.06} raw:00` (Instant Sleep) |
| **2 bytes** | 4 hex chars | **4** | Consumer Control | `raw:cd00 ->{0.06} raw:0000` (Play/Pause) |
| **4 bytes** | 8 hex chars | **2** | Mouse Pointer | `raw:01000000` (Left Click Press) |
| **8 bytes** | 16 hex chars | **1** | Keyboard | `raw:08000f0000000000` (Win/Cmd + L) |

Any payload not matching 1, 2, 4, or 8 bytes is rejected at validation time before transmission.

### 3. Action Sequence Grammar Extension
Extend the action sequence grammar across Go and Kotlin engines:
- Support `raw:<hex>` tokens directly in sequences alongside chords, text, variables, and delays:
  ```text
  raw:cd00 ->{0.06} raw:0000
  ```
- Support macOS modifier aliases:
  - `cmd`, `command` $\rightarrow$ maps to GUI mask `0x08`.
  - `opt`, `option` $\rightarrow$ maps to Alt mask `0x04`.
- OS-aware validation:
  - Combos saved with `--os macos` permit `cmd` and `opt`.
  - Combos saved with `--os windows` (default) warn against accidental macOS key usage, providing clear error guidance.

## Alternatives Considered

### Explicit `<report_id>:<hex>` Prefix
- Pros: Leaves no ambiguity if two reports have identical lengths.
- Cons: All four standard HID reports have distinct lengths (1, 2, 4, 8 bytes). Requiring `<report_id>` puts internal protocol plumbing onto users and automation scripts.
- Decision: Infer from length automatically; preserve optional `--id` override for low-level diagnostic tooling.

### Discrete High-Level Media Commands Only
- Pros: Simple CLI flag like `ghid media play`.
- Cons: Cannot be combined inside custom composite sequences (`ghid combo save`), cannot be timed with delays, and prevents sending arbitrary vendor-specific consumer usages.
- Decision: Implemented length-inferred `raw:<hex>` reports as a universal primitive usable in both one-shot commands and complex multi-step sequences.

## Consequences

- Full cross-platform support: macOS, Windows, and Linux can be controlled using native modifiers (`cmd` vs `win`) and standard media controls.
- Zero-touch report dispatch: agents and users simply provide raw hex; the gateway routes to the correct USB HID endpoint.
- Hardware-grade ACPI sleep support across Windows and macOS hosts via Report 3.
- Backwards compatibility: Existing 16-hex keyboard reports continue to route to Report ID 1 transparently.
