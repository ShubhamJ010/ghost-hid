# GhostHID Documentation Index

Complete architectural, operational, protocol, and decision documentation for GhostHID and its companion tools:
- **[ghid-cli](https://github.com/ShubhamJ010/ghid-cli)**: Command-line companion and terminal UI.
- **[ghost-hid](https://github.com/ShubhamJ010/ghost-hid)**: Android Bluetooth Classic HID controller and app.

---

## Core Specifications

- [System Architecture Specification](architecture.md): Subsystem topology, foreground service lifecycle, dual-report HID descriptor bytecode, and security model.
- [Execution and Lifecycle Flows](flows.md): Sequence diagrams for login sequences, combo execution, Bluetooth SDP handshakes, and ACPI sleep teardown.
- [Wire and Report Protocol Specification](protocol-spec.md): Binary report payloads, L2CAP channels, USB HID scancode translation table, and ADB intent extras.
- [Troubleshooting and Operational Runbook](troubleshooting.md): Diagnosis for ADB transport drops, Windows driver binding issues, Bluetooth desync, and Keychain access over SSH.
- [Automation and Agent Integration Guide](agent-integration.md): Machine-readable `--json` schemas, exit codes, and integration recipes.
- [Android Build Toolchain](build-and-toolchain.md): Gradle release pipeline, R8 optimization, and APK signing.
- [Headless Android App Prompt](prompts/headless-android-app.md): Autonomous agent prompt to build a UI-less Bluetooth HID gateway from scratch.

---

## Architecture Decision Records (ADRs)

- [ADR-001: Headless Android Service with BroadcastReceiver IPC over ADB](decisions/ADR-001-headless-android-service-adb-ipc.md)
- [ADR-002: Dual-Report HID Descriptor for Keyboard and Mouse](decisions/ADR-002-dual-report-descriptor-for-keyboard-and-mouse.md)
- [ADR-003: Charm TUI with Structured JSON API and Native Credential Vault](decisions/ADR-003-charm-tui-with-json-agent-api-and-keychain-vault.md)
- [ADR-004: Snippets Subsystem, Dynamic Combo Variables, and Unified Quick Send](decisions/ADR-004-snippets-subsystem-and-quick-send.md)
- [ADR-005: Combo Metadata and Unified Stepper Engine](decisions/ADR-005-combo-metadata-and-unified-stepper.md)
- [ADR-006: Standalone Android Material 3 Expressive UI with Bi-directional Shared State](decisions/ADR-006-material3-expressive-android-ui-and-shared-state.md)
- [ADR-007: Passive Target Host OS Fingerprinting](decisions/ADR-007-host-os-fingerprinting.md)
- [ADR-008: Composite HID Report Descriptor and Auto-Inferred Raw Dispatch](decisions/ADR-008-composite-descriptor-and-auto-inferred-raw-dispatch.md)
- [ADR-009: Bi-directional Synchronization and Android as Single Source of Truth](decisions/ADR-009-bidirectional-sync-and-android-ssot.md)
- [ADR-010: macOS and Cross-Platform Gestures Adaptation](decisions/ADR-010-macos-and-cross-platform-gesture-adaptation.md)
- [ADR-011: Quick Send Unified Iconography and Device-Driven Host Coupling](decisions/ADR-011-quick-send-unified-iconography-and-host-coupling.md)
- [ADR-012: Single Non-Headless Android App and Release-Only Deployment](decisions/ADR-012-single-android-app-and-release-only-deployment.md)
- [ADR-013: Combo Emoji Icon Override](decisions/ADR-013-combo-emoji-icon-override.md)
- [ADR-014: Raw Snippets, Security Boundary, and Material 3 Expressive Controls](decisions/ADR-014-raw-snippets-security-boundary-and-m3-expressive-controls.md)
- [ADR-015: Unified Bracket Sequence DSL and Payload Subsystem](decisions/ADR-015-unified-bracket-sequence-dsl-and-payloads.md)
