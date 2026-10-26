# ADR-014: Raw Snippets, Strict Security Boundary, and Material 3 Expressive Controls

## Status
Accepted

## Date
2026-10-25

## Context
Snippets were previously restricted to string-only text types, requiring all low-level ACPI or consumer control actions to be hard-coded into macro sequences. Furthermore, credential syncing between the CLI and Android phone lacked clear directional boundaries, and the UI presented quick presets that cluttered the form without supporting raw binary payload entry.

## Decision

1. **Snippet Types (Text vs. Raw HID Report)**:
   - Snippets now support two distinct types: `"type"` (text typing) and `"raw"` (raw HID report bytes).
   - In Go, `Snippet.Type` defaults to `"type"`. If set to `"raw"`, `Snippet.IsRaw()` returns `true`.
   - In Kotlin, `Snippet.type` defaults to `"type"`, with convenience property `Snippet.isRaw`.
   - Raw snippets contain hexadecimal representations of raw HID reports (e.g., `02` for ACPI sleep, `cd00` for consumer play/pause).

2. **Strict Security Isolation**:
   - Secure snippets (`secure: true`) are strictly device-local and isolated:
     - On the CLI, secure snippet values are saved exclusively in the native OS Keychain (macOS) / Secret Service (Linux) and are NEVER saved or pushed to the Android phone.
     - On Android, secure snippets are stored in the Hardware Keystore backed EncryptedSharedPreferences.
     - Synchronization routines (`syncer.Push`) explicitly filter out secure snippets from transit payloads. Local secure snippets are never overwritten during pulls.
   - Non-secure snippets (`secure: false`) maintain their exact plaintext values across CLI and phone sync in both directions and are never hidden or masked.

3. **Built-in Combos & Raw Snippet Integration**:
   - Default built-in combos `Sleep` (Windows) and `PlayPause` (macOS) reference raw snippets:
     - `Sleep`: `$SLEEP_WINDOWS ->{0.06} raw:00` (referencing raw snippet `$SLEEP_WINDOWS` = `02`).
     - `PlayPause`: `$PLAY_PAUSE ->{0.06} raw:0000` (referencing raw snippet `$PLAY_PAUSE` = `cd00`).
   - Added a default secure text snippet `$EMAIL` (`secure: true`, `type: "type"`).
   - Both Go and Android execution engines dynamically resolve `$NAME` variables: if a variable resolves to a raw snippet or hex report, it is dispatched directly via raw HID report transmission rather than keystroke typing.

4. **Android Material 3 Expressive UI Updates**:
   - **M3ExpressiveSnippetCard**:
     - Removed the old "Type to PC" button.
     - Replaced action button with dynamic "Send" (for raw snippets, using `Icons.AutoMirrored.Filled.Send`) and "Type" (for text snippets, using `Icons.Default.Bolt`).
     - Non-secure snippets display plaintext values without obscure masks. Secure snippets display password masks with reveal toggles.
     - Added a snippet type badge (`RAW` vs `TEXT`).
   - **SnippetEditorPage**:
     - Removed "QUICK PRESETS".
     - Added a Material 3 Expressive segmented pill toggle between "Type (Text)" and "Send (Raw HID)".
     - Added a Material 3 Expressive "Paste from Clipboard" button (`FilledTonalButton`, `secondaryContainer`) directly beneath the value field.

## Consequences
- Clean separation between sensitive enclave secrets and portable configuration.
- Flexible hardware and consumer macro definitions using variable references.
- Cohesive Material 3 Expressive mobile design language without legacy presets.
