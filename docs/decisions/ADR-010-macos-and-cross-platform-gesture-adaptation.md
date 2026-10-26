# ADR-010: macOS & Cross-Platform Gestures Adaptation, Vector Iconography, and Inset-Aware Shade Docking

## Status
Accepted

## Date
2026-10-25

## Context
When controlling a target host (such as a MacBook Air or Windows workstation) via the virtual trackpad in GhostHID, multi-finger gestures and quick-send actions must match the host operating system's native shortcuts and conventions:
1. **Shortcut Incompatibility**: Multi-finger swipe gestures hardcoded to Windows shortcuts (`Win+Tab`, `Alt+Tab`, `Win+D`) failed or performed unwanted actions when connected to macOS targets (`Mission Control`, `Cmd+F3`, spaces navigation).
2. **Icon Ambiguity**: Generic Material icons (`Icons.Default.Laptop`, `Icons.Default.DesktopWindows`) were being used interchangeably for OS selection chips, causing visual confusion and lacking brand-accurate vector silhouettes for Apple, Windows, and Linux.
3. **Trackpad Gestures Shade Clipping & Overlap**:
   - In closed state, the top gestures dock had an internal status-bar padding inside a 56dp height container, pushing the "gestures" title and drag handle off-screen.
   - In open state, `PullDownShade` expanded without subtracting the Android system navigation bar insets, causing the bottom dock controls to render behind and overlap with the Android navigation pill.
   - The morphing dock had distorted controls (such as a flattened diamond toggle) rather than symmetrical, accessible Material 3 Expressive surfaces.

## Decision

1. **Host-Aware Dynamic Gesture Mapping (`TrackpadGesture.kt`)**:
   - `TrackpadGesture` provides dual-platform semantics and key combos:
     - `actionNameFor(os: TargetOs)`
     - `descriptionFor(os: TargetOs)`
     - `shortcutFor(os: TargetOs)`
     - `comboFor(os: TargetOs)`
   - **macOS Gestures & Shortcuts**:
     - **3-Finger Swipe Up**: Mission Control (`Ctrl + ↑`, `ctrl+up`)
     - **3-Finger Swipe Down**: Show Desktop (`Cmd + F3`, `cmd+f3`)
     - **3-Finger Swipe Left / Right**: Previous Space / Next Space (`Ctrl + ←` / `Ctrl + →`, `ctrl+left` / `ctrl+right`)
     - **4-Finger Swipe Up**: App Exposé (`Ctrl + ↓`, `ctrl+down`)
     - **4-Finger Swipe Down**: Minimize All (`Cmd + Opt + M`, `cmd+opt+m`)
     - **4-Finger Swipe Left / Right**: Previous Space / Next Space (`Ctrl + ←` / `Ctrl + →`)
   - **Windows Gestures & Shortcuts**:
     - **3-Finger Swipe Up**: Task View (`Win + Tab`)
     - **3-Finger Swipe Down**: Show Desktop (`Win + D`)
     - **3-Finger Swipe Left / Right**: Previous App / Next App (`Alt + Shift + Tab` / `Alt + Tab`)
     - **4-Finger Swipe Up**: Action Center (`Win + A`)
     - **4-Finger Swipe Down**: Minimize Windows (`Win + M`)
     - **4-Finger Swipe Left / Right**: Previous Desktop / Next Desktop (`Ctrl + Win + Left` / `Ctrl + Win + Right`)

2. **Standardized Vector Iconography (`OsGlyph.kt`)**:
   - Replaced all generic Material icons with canonical vector silhouettes:
     - `ic_os_apple.xml` (Apple logo)
     - `ic_os_windows.xml` (Windows 4-pane logo)
     - `ic_os_linux.xml` (Linux penguin)
   - Encapsulated within `com.sj010.ghid.ui.common.os.OsGlyph(os: TargetOs, modifier, tint)`.
   - Used consistently across Quick Send modal header, Gestures Shade morphing dock, and Dashboard Pinned items.

3. **Inset-Aware Shade Docking (`PullDownShade.kt` & `GesturesShade.kt`)**:
   - **Navigation Bar Inset Accounting**:
     `availableHeight` in `PullDownShade.kt` now explicitly subtracts `WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()`. The morphing surface terminates cleanly above the system navigation bar on gesture-nav and 3-button Android devices.
   - **Closed Dock Geometry**:
     Removed `.statusBarsPadding()` from inside `GestureMorphingDock`. When closed, the drag handle and "gestures" title sit cleanly centered at the top of the trackpad viewport.
   - **Symmetrical Dock Controls**:
     When open, the bottom dock displays:
     - Left: Circular `44.dp` OS toggle button with active vector silhouette (`OsGlyph`).
     - Center: Morphing drag handle (smoothly animated above the title) and "Gestures" text.
     - Right: Circular `44.dp` close button (`Icons.Default.Close`).

## Consequences

- Full gesture support when targeting macOS (MacBook Air/Pro) without manual keycode remapping.
- Cards in the Gestures Gallery lead with the swipe the user performs (`Swipe Up`) and one platform-accurate action label (`Mission Control` vs `Task View`). Keyboard shortcut notation and prose descriptions are never rendered — the gallery only needs to answer "what does this swipe do".
- Zero visual clipping or overlap on phones running edge-to-edge Android gesture navigation (e.g. OnePlus, Pixel, Samsung).
- High-fidelity vector logos for macOS, Windows, and Linux across all application surfaces.
