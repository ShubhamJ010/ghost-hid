# ADR-003: Charm TUI with Structured JSON API and Native Credential Vault

## Status
Accepted

## Date
2026-03-28

## Context
Operators interact with GhostHID in two distinct environments:
1. Human operators sitting at their workstations needing a fast terminal console with visual feedback, single-key action triggering, and an interactive combo editor.
2. Automation scripts and AI agents executing scheduled unlock routines, running headless diagnostics, or managing combo libraries without terminal access.

Additionally, automated login requires storing sensitive Windows user credentials without storing plaintext passwords in local files or Git repositories.

## Decision
Build a single static Go binary (`ghid`) implementing both interaction patterns and secure credential handling:
1. **Interactive TUI:** Built with Charm (Bubble Tea, Lip Gloss, Bubbles). Offers a full-screen terminal interface for live gateway status, action shortcuts, combo management, and hardware diagnostics.
2. **Machine-Readable API:** Every command supports a `--json` flag. When `--json` is passed, interactive prompts are bypassed, outputs are formatted as valid JSON, errors produce structured error envelopes, and exit codes communicate status directly (0 for success, 1 for error or disconnected state).
3. **OS Credential Vault:** Store credentials in the macOS Keychain using the service name `ghost-hid` and account name matching the Windows username. Provide an in-memory vault fallback for non-macOS platforms or testing environments.
4. **Deep Module Separation:** Separate concerns into focused internal packages:
   - `internal/adb`: Process execution and ADB protocol management.
   - `internal/gateway`: Core gateway control interface and orchestration.
   - `internal/hid`: Token parsing, sequence validation, and keycode translation.
   - `internal/combos`: Disk persistence and grammar validation for combo definitions.
   - `internal/vault`: Secure credential storage abstractions.
   - `internal/tui`: Terminal UI views, styles, and event handling.
   - `internal/cli`: Argument routing and command handlers.

## Alternatives considered

### Python CLI toolchain
- Pros: Rapid prototyping.
- Cons: Requires a Python runtime, virtual environment setup, and dependency management (`pip install ...`). Distributing to non-technical environments introduces version conflicts.
- Rejected: Go compiles to a single, zero-dependency static executable.

### Storing credentials in a plaintext JSON or YAML configuration file
- Pros: Simple to inspect and edit.
- Cons: Severe security risk. Plaintext passwords sit exposed on disk and can be accidentally committed to version control.
- Rejected: Native OS keychains provide encrypted, hardware-backed credential storage with access control.

## Consequences
- Single binary distribution: `ghid` can be linked to `~/.local/bin` and run immediately.
- Shell scripts and AI agents parse clean JSON responses without scraping ANSI terminal escapes.
- Passwords remain encrypted in the macOS Keychain; the CLI retrieves them only at the moment of injection.
