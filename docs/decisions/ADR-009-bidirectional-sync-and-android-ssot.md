# ADR-009: Bi-directional Synchronization and Android as Single Source of Truth (SSOT)

## Status
Accepted

## Date
2026-10-25

## Context
With the introduction of the Material 3 Compose Android application (ADR-006), users can create, edit, pin, and delete combos and snippets directly on the Android smartphone. Previously, the macOS Go CLI (`ghid`) loaded from its own local files (`~/.config/ghid/combos.json` and `snippets.json`) and seeded hardcoded defaults on startup, causing:
1. Divergence between phone and workstation stores.
2. Local CLI reviving combos or snippets that were intentionally deleted on the phone.
3. Plaintext updates made on phone (such as `$EMAIL`) not being recognized by the CLI without manual file copying.
4. App in-memory caches on Android not reloading when files on external flash storage changed, unless the app was cleanly restarted.
5. Inability to execute safe synchronization natively within Go without relying on external scripting environments.

## Decision

1. **Android as Single Source of Truth (SSOT)**:
   - The Android filesystem (`/sdcard/Android/data/com.sj010.ghid/files/`) is designated as the primary authority for all combos and snippets.
   - When the CLI connects to an Android device, remote state overrides local state.
   - Local disk files in `~/.config/ghid/` serve strictly as an offline cache and storage for local OS secrets (macOS Keychain).
   - Hardcoded local default seeding (`EnsureDefaults`) on CLI startup is disabled when an active device is available; the phone's items are used directly.

2. **Native Go Syncer Package (`internal/syncer`)**:
   - Implemented natively in Go without external Python, Bash, or third-party runtime dependencies.
   - **`Pull(mgr, cStore, sStore)`**: Pulls phone JSON via ADB `cat`. Replaces local combos list with the phone's combos. Updates local snippets list while matching local macOS Keychain secrets for secure snippets (`Secure == true`).
   - **`Push(mgr, cStore, sStore)`**: Pushes local combos to phone. Pushes sanitized snippets (`Value = ""` when `Secure == true`) to phone flash storage, protecting hardware Keystore integrity. Restarts the app and triggers an automatic Bluetooth reconnect broadcast.
   - **`Sync(mgr, cStore, sStore)`**: Executes full cycle: Pull -> Push -> Restart -> Reconnect.

3. **Lifecycle Synchronization Triggers**:
   - **On CLI Startup (`AutoPullIfConnected`)**: In `<15ms`, the CLI checks if an ADB device is connected in the `device` state and quietly pulls fresh combos and snippets before executing any command (`combo`, `snippets`, `quick`, `interactive`).
   - **On Local Mutations (`AutoPushIfConnected`)**: When modifying state via CLI (`ghid combo save`, `ghid combo fav`, `ghid combo delete`, `ghid snippets set`, `ghid snippets delete`), changes are saved locally and immediately pushed to Android, followed by app restart and reconnect.

4. **Safe App Restart and Reconnection (`RestartAppAndReconnect`)**:
   - Invokes `am force-stop com.sj010.ghid`.
   - Starts main activity: `am start -n com.sj010.ghid/.MainActivity`.
   - Waits 1.5 seconds for Jetpack Compose runtime and `HidService` initialization.
   - Broadcasts `am broadcast -a com.sj010.ghid.ACTION_CMD -n com.sj010.ghid/.service.HidCommandReceiver --es cmd connect`, restoring the Bluetooth connection to the active target laptop automatically.

5. **Security Boundaries & Secret Hygiene**:
   - Android Keystore manages encrypted credentials on phone (`AndroidKeystoreVault.kt`).
   - macOS Keychain manages credentials on Mac (`internal/vault`).
   - Wire transfer over ADB and disk storage on `/sdcard` strictly scrub secrets (`Value = ""`). Secrets never cross the transport in plaintext.

## Consequences

- The user can configure combos and snippets on either phone or laptop; changes propagate seamlessly.
- Deletions made on the phone are respected by the CLI.
- No external Python dependencies required for synchronization.
- Bluetooth HID connection to target computer drops for only ~1.5s during restart and automatically re-establishes without user intervention.
- Machine-readable `--json` output available for automated agent workflows via `ghid sync --json`.
