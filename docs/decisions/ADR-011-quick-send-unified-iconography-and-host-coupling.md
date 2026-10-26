# ADR-011: Quick Send Unified Iconography and Device-Driven Host Coupling

## Status
Accepted

## Date
2026-10-25

## Context
In previous iterations of the Quick Send modal sheet:
1. **Redundant & Cluttered Top-Right Controls**: The sheet header displayed two adjacent elements: an OS selector pill (`(Apple/Windows) [macOS]`) and a separate 44dp clover container with a lightning bolt icon (`⚡`). This cluttered the header and introduced contradictory affordances.
2. **Disconnected Manual OS Switching**: The user was given manual controls (`clickable(onClick = onToggleOs)`) to flip the operating system in Quick Send independently of the connected device. This broke consistency with the rest of the application (Dashboard, Trackpad, Combos), where the target OS is strictly driven by the active or last-connected Bluetooth device.
3. **No-Device State**: When no target PC was selected, the UI defaulted arbitrarily to Windows without communicating that no target machine was bound, obscuring whether dispatches would route to any host.

## Decision

1. **Merged Single Icon Badge**:
   - Eliminated the redundant OS pill and the lightning bolt icon in `QuickSendSheetHeader`.
   - Merged both into a single 44dp 4-petal clover badge (`ExpressiveShapes.Clover4Leaf`) on the right side of the header.
   - When a device is selected (`targetDevice != null`), the badge displays the canonical host OS vector glyph via `OsGlyph(os = currentOs)` (`ic_os_apple`, `ic_os_windows`, `ic_os_linux`).
   - When no device is selected (`targetDevice == null`), the badge displays the Ghost mascot icon (`painterResource(R.drawable.ic_notification_ghost)`).
   - Applied the identical clover badge and device-driven icon to `QuickSendCollapsedButton` when the composer dock is scrolled down.

2. **Device-Driven Host OS Coupling (Non-Switchable)**:
   - Removed manual OS toggle interactions from Quick Send.
   - The effective OS is coupled directly to `service?.effectiveTargetOs` and `service?.targetDeviceState` (or the last-connected cached device target restored on app launch).
   - The header subtitle dynamically communicates host state:
     - Target selected: `"$historyCount recent sends · ${targetOs.label}"`
     - No target: `"$historyCount recent sends · Ghost"`

3. **Unrestricted History Access**:
   - Quick Send history is always displayed in full via `historyRepository.entries`, regardless of whether a device is currently connected.
   - In fresh installs where no device has yet been connected, the history gracefully displays the empty state with example sequences.

## Consequences

- **Header Elegance**: Clean single-badge indicator matching the Material 3 Expressive aesthetic without duplicated buttons or labels.
- **System Consistency**: Quick Send behavior now perfectly matches the Dashboard, Combos, and Trackpad screens—all derived from the single source of truth in `HidService`.
- **Zero Confusion**: Users immediately see which host OS commands are parsed against and when no PC is selected (Ghost icon).
