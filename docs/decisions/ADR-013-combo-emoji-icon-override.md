# ADR-013: Combo Emoji Icon Override

## Status
Accepted

## Date
2026-10-25

## Context
ADR-007 removed the per-combo emoji from ADR-005 and made the target OS tag the only
combo identity: every Windows combo showed the same Windows logo, and terminal
listings showed the same `[win]` / `[mac]` badge. With a library of a dozen
shortcuts that is not enough signal — users cannot tell "Open RustDesk" from
"Lock" at a glance, and there was no way to give a combo its own icon from the
Combo Studio editor.

## Decision
Reintroduce an optional `emoji` field on `combos.Combo` as *icon metadata*, not a
replacement for the OS tag:

- JSON key is `emoji`, omitted when empty. Both the Go store and the Android
  `Combo` model read/write the same field, so ADB sync carries it unchanged.
- Validation lives in `combos.ValidateEmoji` and mirrors the Kotlin
  `ComboEmojiPolicy`: trimmed, at most 12 code points, no whitespace, and no
  ASCII letters or digits. Plain text such as `rocket` is rejected.
- Android: the editor shows a compact `ComboIconSlot` next to `Name` that opens a
  full-screen picker (search + category chips over a generated Unicode catalog,
  plus a "use the OS logo" reset). `ComboGlyph(combo, size, tint)` renders the
  emoji when present and falls back to `OsGlyph` otherwise. Every combo identity
  surface (studio card, dashboard collage, most-used list, favorite picker) uses it.
- The picker catalog is generated from Unicode's `emoji-test.txt` by
  `scripts/generate_emoji_catalog.py` and committed, so the app ships the full
  emoji set without a runtime download.
- CLI/TUI: `combo save` accepts `--emoji <e>`; listings print `Combo.Badge()`,
  which is the emoji when set and the bracketed OS tag otherwise.
- Built-in combos ship icons: Login `🔑`, Lock `🔒`, Sleep `😴`, MacSleep `🌙`,
  ForceQuit `⚡`, PlayPause `⏯️`. `EnsureDefaults` backfills them for libraries
  saved before this field existed.
- An update that omits `--emoji` keeps the stored emoji, matching how the OS tag
  and description behave.

## Consequences
- Combos remain OS-scoped and filterable; the emoji only overrides how one combo
  is drawn, so no filters or dashboards change shape.
- The OS logo is still the default, so libraries without icons look unchanged.
- Emoji are user data: sync, `--json` payloads and the phone file must round-trip
  them, which the shared `emoji` key guarantees.
