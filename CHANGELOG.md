# Changelog

All notable changes to GhostHID are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.1.1] - 2026-10-28

### Added
- **Full expressive app settings screen.** New Settings tab on the dashboard with collapsible categories covering trackpad & pointer sensitivity, HID transmission timing, host OS override (auto-detect / force Windows / force macOS / force Linux), auto-connect on launch, LED handshake detection window, theme mode (system / dark / light), dynamic color, keep screen awake, high refresh rate, and QuickSend history cap and haptics. Backed by a new `AppSettings` model, `SettingsRepository`, and persistent `SharedPreferences` + `kotlinx.serialization` storage with reactive `StateFlow`.
- **Dashboard settings shortcut.** "Settings & Tuning" card placed directly above the Quit App button on the dashboard, opening the new settings screen without leaving the gateway flow.
- **Interactive gateway notification.** Notification now carries a disconnect/connect action button that toggles the HID link and a "Stop gateway" action that tears down the service *and* quits the app through the activity's full quit path, avoiding a half-stopped state where the service is gone but the activity remains bound.
- **Notification action icons.** New `ic_notification_connect` and `ic_notification_stop` vector drawables used by the interactive notification buttons.
- **Dashboard settings navigation.** `Screen.Settings` entry added to the navigation enum and `MainAppScaffold` renders the new `SettingsScreen` with a back handler and a hidden bottom bar while in settings.
- **Theme and display toggles wired to settings.** Theme mode (system/dark/light), dynamic color, keep-screen-awake, and high-refresh-rate preferences are now read from `AppSettings` and applied live through `DisposableEffect`/`LaunchedEffect` in `MainActivity`.
- **Settings repository and model.** New `SettingsRepository` persists the full `AppSettings` payload via JSON in `SharedPreferences` with an update/reset API, and `AppSettings` is a serializable data class with typed enums (`OsOverrideMode`, `ThemeMode`) and defaults for all preference groups.
- **QuickSend history limit from settings.** `QuickSendHistoryRepository` now reads `quickSendHistoryLimit` from the live settings instead of a hardcoded constant, and `QuickSendEntry`/`QuickSendHistoryPolicy` accept an explicit `maxEntries` parameter.

### Changed
- **Lock combo migrated to Windows-native syntax.** Built-in "Lock" combo changed from `{mod+l}` to `{win+l}` with the description "Instant Windows lock screen (Win + L)". Migration in `ComboRepository` now normalises any legacy `mod+l` / `mod+ctrl+q` / `mod+alt+esc` references to the new Windows/macOS native tokens.
- **macOS combos migrated to Cmd syntax.** "MacSleep" changed from `{mod+ctrl+q}{500ms}{Escape}` to `{cmd+ctrl+q}{500ms}{Escape}` and "ForceQuit" from `{mod+alt+esc}{400ms}{Enter}{300ms}{Enter}` to `{cmd+alt+esc}{400ms}{Enter}{300ms}{Enter}`.
- **Agent integration docs updated to Win/Cmd tokens.** `docs/agent-integration.md` examples, JSON responses, and automation snippets now use `win+r`, `win+l`, and `cmd+ctrl+q` throughout instead of the legacy `mod` token.
- **README advertising raw HID injection.** README headline and features list now mention "raw payload injector", and the "Key features" section explicitly advertises raw HID payload injection across all four report descriptors with automatic report-ID inference and hold/release semantics.
- **README IPC security wording.** The "Native background service and IPC" section now describes the IPC receiver as protected by `android.permission.DUMP` so only authorised scripts/CLI can inject commands.
- **README raw HID usage examples.** New "Raw HID payload injection" subsection documenting Report ID 1–4 payloads, automatic inference, and `ghid raw` CLI examples (volume up hold, system sleep, Win+A chord).
- **Notification status line enrichment.** Notification body now appends the resolved host OS label ("· Windows"/"· macOS") while connected, and the foreground service registers with `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` on API 29+.

### Fixed
- **Lock combo mapping.** The built-in lock shortcut now emits the platform-native lock sequence (Win+L on Windows, Cmd+Ctrl+Q on macOS) instead of a generic modifier binding, matching what the host OS actually interprets as a lock command.
- **Quit-from-notification lifecycle.** "Stop gateway" from the notification now goes through the activity's full quit path so the app is fully closed instead of leaving the activity bound to a torn-down service.

## [0.1.0] - 2026-10-23

### Added
- Initial public release of GhostHID: native Android Bluetooth Classic HID controller, trackpad, and full-bleed macro deck with Material 3 Expressive design.
- Native `BluetoothHidDevice` SDP registration and L2CAP transport, without host-side drivers or companion agents.
- Multi-touch trackpad and mouse via standard relative pointer reports (Report ID 2), including single-tap left click, two-finger right click, edge scrolling, and middle click.
- QuickSend text typing and clipboard-to-host dispatch.
- Combo Studio shortcut editor with OS-specific glyphs for Windows, macOS, and Linux.
- Landscape stream deck with hardware action slots, custom button shapes, and a payload drawer.
- Hardware-encrypted payload vault backed by Android Keystore.
- ADB/CLI companion (`ghid-cli`) for terminal and agent automation.

[0.1.1]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.1.1
[0.1.0]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.1.0
