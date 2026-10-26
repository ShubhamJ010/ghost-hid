# ADR-002: Dual-Report HID Descriptor for Keyboard and Mouse

## Status
Accepted

## Date
2026-03-20

## Context
Initial prototypes of GhostHID only transmitted keyboard keystrokes. To support automated unlocking, window manipulation, and complex UI interaction (such as clicking specific buttons or scrolling lists), the system needed pointer input in addition to keyboard chords.

The Bluetooth HID Device profile allows registering either separate HID applications or a single composite descriptor using Report IDs.

## Decision
Register a single unified HID application descriptor with `BluetoothHidDevice.SUBCLASS1_COMBO` containing two distinct report collections:
1. **Report ID 1 (Keyboard):** Standard 8-byte input report (1 byte modifier bitmap, 1 byte reserved, 6 keycode slots) and 1-byte output report for LED state (Num Lock, Caps Lock, Scroll Lock).
2. **Report ID 2 (Mouse):** 4-byte input report (1 byte button bitmap with 3 buttons and 5 padding bits, 1 byte relative X movement, 1 byte relative Y movement, and 1 byte relative vertical wheel tick).

When dispatching events:
- Keystrokes, chords, and text typing dispatch through `BluetoothHidDevice.sendReport(device, 1, data)`.
- Clicks, double-clicks, and wheel scrolls dispatch through `BluetoothHidDevice.sendReport(device, 2, data)`.

## Alternatives considered

### Separate Bluetooth HID profile instances
- Pros: Strict separation between keyboard and pointer subsystems.
- Cons: Android's `BluetoothHidDevice` API only supports one active SDP registration per application context. Registering two instances triggers resource conflicts in the Bluetooth stack daemon.
- Rejected: Android framework limitation.

### Bluetooth Low Energy (BLE) HID over GATT (HOGP)
- Pros: Lower idle power consumption.
- Cons: Windows host pairing for BLE HID requires manual GATT service discovery, which often fails during lock screen or pre-boot environments. Bluetooth Classic HID (L2CAP PSM 0x11 control and 0x13 interrupt channels) provides immediate native driver binding in Windows without custom drivers.
- Rejected: Bluetooth Classic HID provides superior compatibility across host operating systems.

## Consequences
- The paired target operating system registers a single combo device ("GhostHID Keyboard & Mouse") with two input interfaces.
- Mouse gestures and keyboard chords can interleave with sub-millisecond switching.
- Changing report collections requires removing and re-pairing the device on the target host so the host OS re-queries and caches the updated SDP descriptor.
