# Changelog

All notable changes to GhostHID are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.2.2] - 2026-11-04

### Added
- **Most Used Payloads pill carousel on Dashboard.**
  - **3-Row staggered capsule carousel:** New `MostUsedPayloadsSection` rendering a 3-row horizontally scrolling grid of 58dp capsule pills directly below Most Used Combos, placing raw HID reports and typed text snippets one tap away without opening the landscape stream deck.
  - **Dynamic interlocking widths & leading insets:** Pure policy (`PayloadPillsPolicy.calculatePillWidth`) computes irregular stepped widths (118..252dp) seeded from title and subtitle lengths with per-(row, column) stagger rhythm, coupled with per-row leading offsets (0/44/18dp) for a signature staggered brickwork layout with full edge bleed under screen gutters.
  - **Visual & tactile feedback:** Leading 38dp circular art badge with icon type indicators (Lock for secure vault items, Memory for raw hex reports, TextFields for keystrokes), tactile spring morph on press (29dp stadium down to 18dp squarish pill on long-press haptic), and 8 curated M3 Expressive tonal palettes.
  - **One-tap payload dispatch:** Standalone `PayloadDispatcher` executes typed strings or validated raw HID byte packets and logs usage immediately.
- **Scoped usage tracking for Combos & Payloads.**
  - **Configurable history scope:** New `UsageTrackingScope` preference under Settings > Host options ("Most Used History Scope") supporting `Device Type` (OS-wide grouping across all Macs/PCs) and `Device Name` (isolated per Bluetooth host name, e.g. Mac A vs Mac B).
  - **Multi-tier key scheme & migration safety:** `UsageScopingPolicy` maintains global (`usage:`), per-OS (`usage_type:<os>:`), and per-device (`usage_device:<device>:`) counters on every use, with graceful fallback ensuring pre-existing usage counts survive without data loss.
  - **Scope-aware basis indicators:** Header subtitles on both Most Used Combos and Most Used Payloads dynamically reflect the active tracking basis ("Based on <OS> history" or "Based on <device> history").
  - **Optimized dashboard resolution:** Pre-filtered count maps memoized with `remember` on the dashboard, and `HidService` combo execution plus stream deck payload paths updated to pass effective target OS and device name.

## [0.2.1] - 2026-11-03

### Added
- **Interactive Mode typing preview enhancements.**
  - **Letter chaining & typing buffer:** Fast consecutive keystrokes from the software IME keyboard remain grouped into a single preview text block during active typing with a 1.2s cooldown rather than spamming individual single-letter chips.
  - **Live backspace editing:** Pressing backspace dynamically trims characters from the active typing chunk in the preview HUD.
  - **Keypress repetition counter:** Consecutive identical non-character keypresses (e.g. arrow keys, backspace, volume keys) collapse into a single chip with an `xN` count badge.
  - **Dedicated key and media icons:** Navigation arrows, media keys (volume up/down, mute, play/pause, prev/next track, stop), tab, enter, and backspace display vector icons with compact labels.
  - **Auto-clearing preview slate:** The preview slate automatically fades out and resets after 4 seconds of inactivity.
- **Hardware volume buttons mapping.**
  - Physical volume up/down keys are intercepted while Interactive Mode is active.
  - Configurable mapping options under Settings: **Volume** (host HID volume ±), **Right / Left Arrow** (presentation slide navigation / cursor stepping), and **Next / Previous Track** (media playback track skipping).
  - Keyboard island status subtext and center prompt dynamically indicate the active volume button mode.
- **Interactive Mode settings section.** Added an "Interactive Mode" settings card in the Settings tab with an interactive choice group for volume key behavior.

### Changed
- **Default display sleep behavior.** Default `keepScreenAwake` preference changed to `false` so the device respects standard Android display sleep settings out of the box, with clarified description in Settings.

## [0.2.0] - 2026-11-02

### Added
- **Interactive keyboard mode.** Full-screen interactive mode turning the mobile software keyboard into an instantaneous live Bluetooth HID keyboard forwarder. Supports character-by-character typing directly to the paired host PC/Mac, live text capture, and automatic focus management.
- **Configurable 4-slot Key Mapper.** Top utility strip with 4 user-customizable key slots providing sticky modifier chaining (Shift, Ctrl, Alt, Meta/Cmd/Win) and fast access to frequently used keys.
- **Pull-down Key Drawer shade.** Collapsible bottom sheet drawer organizing system modifiers, navigation arrow keys, editing cluster (Home, End, PageUp, PageDown, Delete, Insert), full function row (F1–F12), media playback controls, and system actions.
- **Integrated trackpad in Interactive Mode.** When the software keyboard is dismissed, an integrated gesture trackpad appears automatically, allowing seamless transitions between typing and pointer manipulation.
- **Virtual trackpad window hold-to-drag & grab.**
  - Double-tap and hold gesture on trackpad surface to grab windows, move files, or drag-select text without releasing mouse button state.
  - Persistent held button state in `HidService` (`setMouseButton()`, `releaseAllMouseButtons()`, and `heldButtons` mask) maintained across relative mouse motion and scroll wheel reports.
  - Two-thumb / two-finger deck drag: hold the "Left" action key on the bottom click deck with thumb while moving cursor with index finger on the trackpad.
  - Visual and haptic feedback during drag operations: 2.5dp accent border, intensified touch glow, "HOLD & DRAG" HUD pill banner, and vibration on grab start / release drop.

### Changed
- **Mouse report engine.** Relative mouse movement (`emitMove`) and scroll wheel (`emitWheel`) now carry the persistent `heldButtons` mask instead of hardcoded 0, allowing sustained drag operations across high-frequency pointer movements.
- **Bluetooth disconnect safety.** Disconnecting from host or stopping HID service automatically releases all held mouse buttons and resets state to prevent stuck button inputs on the host.
- **Click deck interaction.** Replaced standard click detectors on the trackpad action deck with precise pointer press/release tracking to support holding buttons down across multiple fingers.

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

[0.2.2]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.2.2
[0.2.1]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.2.1
[0.2.0]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.2.0
[0.1.1]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.1.1
[0.1.0]: https://github.com/ShubhamJ010/ghost-hid/releases/tag/v0.1.0
[Unreleased]: https://github.com/ShubhamJ010/ghost-hid/compare/v0.2.2...HEAD
