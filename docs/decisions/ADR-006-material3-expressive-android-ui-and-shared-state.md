# ADR-006: Standalone Android Material 3 Expressive UI with Bi-directional Shared State

## Status
Accepted

## Date
2026-10-23

## Context
GhostHID originated as a headless Android service (`com.sj010.ghid_headless`) receiving commands over ADB from the macOS Go CLI (`ghid`). This topology worked well for desktop-bound automation but required a tethered or networked host computer to trigger even simple actions (such as locking a workstation or injecting credentials).

Users require the ability to use the phone as a fully autonomous, standalone hardware Bluetooth HID controller with on-device controls, while:
1. Retaining 100% backward compatibility with the Go CLI and ADB automation pipeline.
2. Adopting a modern, delightful, and expressive visual experience based on Google's Material 3 Expressive design system.
3. Maintaining shared state between the Mac workstation and Android smartphone for combo macros and snippets, including hardware-backed credential protection for secrets.
4. Preserving clean monorepo organization and maintainable build mechanics.

## Decision
1. **Material 3 Expressive Android Application**:
   - Introduce a standalone Android Activity (`MainActivity`) built on **Jetpack Compose** and `androidx.compose.material3`.
   - Implement dynamic color (Monet), tonal elevations, 8dp spacing grid, extra-rounded corner tokens, and physics-based spring motions.
   - Provide an adaptive layout scaffold with 5 dedicated screens: Dashboard, Quick Send, Combo Studio, Snippet Studio, and Virtual Mouse / Trackpad.

2. **Dual-Entry Service Architecture**:
   - Retain `HidService` as the unified Bluetooth HID engine.
   - Expose a `LocalBinder` with reactive Kotlin `StateFlow` primitives for the in-process Compose UI.
   - Retain `HidCommandReceiver` for external ADB broadcasts from the Mac CLI. Both local UI and remote ADB can control the service concurrently.

3. **Bi-directional Shared State & Secure Vault**:
   - Store combos in `/sdcard/Android/data/com.sj010.ghid_headless/files/combos.json` using the identical JSON schema as Mac `~/.config/ghid/combos.json`.
   - Store plaintext snippets in `snippets.json`.
   - Store sensitive secrets (such as `$PASSWORD`) in the hardware-backed Android Keystore via `EncryptedSharedPreferences` (AES-256-GCM MasterKey), matching the security posture of macOS Keychain on the Mac.
   - Add a `ghid sync` command to the Go CLI to pull/push macro and snippet updates between Mac and phone storage over ADB.

4. **Monorepo Structure & Build Evolution**:
   - Establish `android/` as the standard Android Gradle project root containing the modernized `app` module.
   - Update the root `build.sh` script to invoke Gradle and produce `build/ghost-hid.apk`, preserving the existing CLI build and deployment workflows (`./build.sh && ghid install-apk`).

## Alternatives considered

### 1. Web-based UI served from Android local HTTP server
- Pros: Multi-platform browser access.
- Cons: High latency for mouse trackpad events, no access to native Android Keystore without complex bridges, clunky UX compared to native apps, poor offline reliability.
- Rejected: Native Jetpack Compose provides superior tactile feedback, instant spring animations, and native Keystore integration.

### 2. Traditional XML Views with Android View Binding
- Pros: Simple build requirements.
- Cons: Google I/O 2026 Material 3 Expressive guidelines are Compose-first. XML lacks out-of-the-box support for the latest expressive components, shape morphing, and spring motion physics.
- Rejected: Compose is the future-proof, primary target for Material 3 Expressive.

### 3. Separate Repository for the Android App
- Pros: Isolates Go build from Gradle build.
- Cons: Fractures protocol definitions, leads to schema drift between Mac Go AST parser and Android HID engine, complicates CI/CD and release coordination.
- Rejected: Monorepo preserves tight cohesion and prevents protocol divergence.

## Consequences
- The Android app can now be used standalone directly from the phone screen without a Mac workstation present.
- The Go CLI and TUI continue working without changes when connected over ADB.
- Macros created on the phone are available to the Mac and vice versa via `ghid sync`.
- Passwords remain secure at rest in both operating systems (Keychain on macOS, Keystore on Android).
- The project adopts standard Gradle tooling for the Android subsystem, enabling linting, unit testing, and dependency management.
