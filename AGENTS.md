# AGENTS.md

GhostHID = Android phone as Bluetooth HID keyboard + mouse.
`ghid` (Go, this Mac) → ADB → `com.sj010.ghid_headless` on phone → Bluetooth HID → target PC.

Status: alpha. Big refactors welcome. Run tests, update docs, keep JSON contract stable in same change.

## Repo map

| Path | Job |
|---|---|
| `cmd/ghid` | binary entry |
| `internal/cli` | arg parsing, commands, JSON output, TUI launch |
| `internal/gateway` | `Controller` interface, validation, ADB calls |
| `internal/adb` | adb transport, broadcasts, `FakeRunner` for tests |
| `internal/hid` | key tokens, combo/mouse/text parsers, step descriptions |
| `internal/combos` | combo library at `~/.config/ghid/combos.json` |
| `internal/tui` | bubbletea dashboard + Combo Studio |
| `internal/config`, `vault`, `keychain` | config, credential storage |
| `src/com.sj010/ghid_headless` | Android `HidService` + `HidCommandReceiver` |
| `res/`, `AndroidManifest.xml`, `build.sh` | APK resources + zero-Gradle build |
| `docs/` | architecture, flows, protocol, agent integration, ADRs |

Layer rule: `cli → gateway → adb → broadcast → Java`. No direct `adb` calls from CLI/TUI. No business logic in `cmd/ghid`.

## Core commands

```bash
cd /Users/sj/Desktop/ws/ghost-hid

gofmt -w cmd internal
go vet ./...
go test ./... -count=1
./build.sh          # APK only
```

## Update CLI on this Mac

```bash
cd /Users/sj/Desktop/ws/ghost-hid
gofmt -w cmd internal
go test ./... -count=1
go build -o ghid ./cmd/ghid
go install ./cmd/ghid
./ghid install-bin
hash -r
ghid --version
```

Two install paths exist:

- `go install` → `~/go/bin/ghid` (copy)
- `./ghid install-bin` → `~/.local/bin/ghid` (symlink to repo binary) + man page

`~/go/bin` usually wins in PATH. Update both, or `rm ~/go/bin/ghid` once to use symlink only.
Resolve with `which ghid`.

## Update APK on this Mac

Phone must be visible to ADB first: `ghid doctor --json`.

```bash
cd /Users/sj/Desktop/ws/ghost-hid
./build.sh
ghid install-apk
ghid restart
sleep 2
ghid connect
ghid status
```

Want `SDP Registered: true` and `Connection: CONNECTED`.

If HID descriptor, report IDs, subclass, or mouse support changed:
**unpair GhostHID on Windows, then pair again.** Old pairing caches old descriptor. Java-only changes need install + restart, not re-pair.

## Update both

```bash
cd /Users/sj/Desktop/ws/ghost-hid
gofmt -w cmd internal
go test ./... -count=1
./build.sh
ghid install-apk
ghid restart
go build -o ghid ./cmd/ghid
go install ./cmd/ghid
./ghid install-bin
sleep 2
ghid connect
ghid status
```

## Agent JSON contract

`--json` = non-interactive. No prompts, no ANSI, JSON on stdout, diagnostics on stderr.

- Exit `0` success, `1` error.
- Combo errors: `{"status":"error","error":"..."}` on stdout + exit 1.
- `combo schema --json` = grammar + every valid token. Agents discover, no hard-code.
- `combo validate <keys> --json` = parse without send/save.
- `combo save` = upsert, returns `updated: true|false`.
- `combo delete --json` = no confirmation.

```bash
ghid combo schema --json
ghid combo validate 'win+s ->{0.7} type:rustdesk ->{0.4} enter' --json
ghid combo save "Open RustDesk" 'win+s ->{0.7} type:rustdesk ->{0.4} enter' --json
ghid combo list --json
ghid combo send "Open RustDesk" --json
ghid combo delete "Open RustDesk" --json
```

Combo grammar: `step ->{seconds} step`.
Steps: keyboard chord, mouse gesture, `type:<text>` / `text:<text>`.
Windows only. Reject `cmd`, `super`, `option`.
Typed text = printable US-ASCII. Go validator + Java `keyForChar` must stay in sync.

## Engineering rules

- New behavior → test first. Table-driven where possible.
- Tests use `adb.FakeRunner` + `vault.MemoryVault`; never touch real phone/ADB.
- One source of truth per concept. Tokens in `hid`, storage in `combos`, transport in `adb`.
- Prefer pure parsers/validators. Side effects at edges.
- Keep `gateway.Controller` small. Add method only when CLI truly needs it.
- New command → update `printHelp`, `man/ghid.1`, `README.md`, and `docs/agent-integration.md` if JSON shape changes.
- Preserve one-shot `ghid combo "win+d"`.
- Commit style: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`.
- Alpha means large refactors OK. Do not leave half-migrated layers, dead code, or duplicate parsers.

## Android app design & iteration rules

- **Super new app & fast evolution**: Actively listen to user feedback and iterate immediately on the go.
- **Zero backward compatibility constraints for Android UI**: Do not preserve deprecated UI elements or hold onto legacy screens when the user asks to remove or change them (e.g. gateway terminal log completely removed from dashboard).
- **Material 3 Expressive shapes & styling**: Use official Google M3 Expressive shape catalog (4-petal clovers, scallops, soft diamonds, arches, soft triangles, hearts, pentagons, concave squircles) and vibrant high-contrast cyberpunk dark palettes.
- **Always verify on real connected device**: When APK is deployed to ADB device, take screenshots, review visual alignment, and polish touch targets directly.

## Gotchas

- Flags stripped globally (`--json`, `-s`). Text containing those tokens can break.
- Combo timestamps stored UTC. Local clock may show behind.
- `build.sh` hard-codes Homebrew Android SDK + JDK 17 paths. Check before editing.
- TUI is one bubbletea app. `esc` returns to dashboard, `ctrl+c` exits.
- Mouse needs report ID `2`; keyboard report ID `1`. Wrong descriptor = no mouse.
- `ghid combo` bare is interactive only on a TTY. Non-TTY falls back to list.

## Done checklist

- [ ] `gofmt` clean
- [ ] `go test ./... -count=1` green
- [ ] CLI rebuilt + installed in both paths
- [ ] APK rebuilt + installed if Java/res changed
- [ ] service restarted + reconnected
- [ ] docs/man/help updated
- [ ] JSON output still parseable, exit codes unchanged
