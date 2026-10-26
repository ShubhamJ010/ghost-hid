# ADR-012: Single Non-Headless Android App and Release-Only Deployment

## Status
Accepted

## Date
2026-10-25

## Context
GhostHID carried two Android apps at once:

1. **Legacy headless gateway** — `com.sj010.ghid_headless`, plain Java under `src/`, resources in
   root `res/` plus root `AndroidManifest.xml`, compiled by a zero-Gradle
   `aapt2`/`javac`/`d8`/`zipalign`/`apksigner` pipeline that `build.sh` fell back to whenever
   `android/gradlew` was missing.
2. **Material 3 Expressive app** — `com.sj010.ghid`, Jetpack Compose under `android/`, built by
   Gradle, with the full dashboard, Combo Studio, trackpad and Quick Send (ADR-006 … ADR-011).

Both answered the same ADB broadcast contract, which left the Go side half-migrated: the
receiver and action constants already pointed at `com.sj010.ghid`, while `PackageName`,
`ServiceName` and `StatusFileRemote` still pointed at `com.sj010.ghid_headless`, and
`GetStatus()` probed both `status.json` paths as a fallback. `ghid doctor` reported a package
nobody shipped, `ghid build` produced a debug APK from a script nobody ran, and every doc
described a toolchain the repo no longer used.

## Decision

1. **Delete the headless app.** Removed `src/`, `res/`, the root `AndroidManifest.xml`, and the
   legacy fallback branch in `build.sh`. The root of the repo now contains no Android sources.
2. **One package, no compatibility layer.** `com.sj010.ghid` is the only identity in Go, docs,
   and the manifest. `adb` constants were unified:
   `PackageName = com.sj010.ghid`,
   `ServiceName = com.sj010.ghid/.service.HidService`,
   `StatusFileRemote = /sdcard/Android/data/com.sj010.ghid/files/status.json`.
   `GetStatus()` reads exactly one path and returns the offline fallback if it is missing or
   malformed — no probing of legacy locations. The duplicate `AppPackageName` constant was
   folded into `PackageName`.
3. **Release-only deployment.** `build.sh` runs `android/gradlew :app:assembleRelease` and
   publishes `build/ghost-hid.apk`; `ghid build` invokes the same script; `ghid install-apk`
   resolves `build/ghost-hid.apk`, then the raw Gradle release output. Debug APKs are no longer
   part of the install path. Release and debug share `debug.keystore`, so `adb install -r`
   upgrades in place and existing Bluetooth pairings survive.
4. **Headless stays available as a contract, not as code.** `docs/prompts/headless-android-app.md`
   is the self-contained prompt/spec for coding a UI-less gateway from scratch: package and
   component names, every broadcast command and extra, the `status.json` schema, `gateway.log`
   format, logcat tag, HID report IDs, and the acceptance checklist against the real device.
   Anything built from it must use `com.sj010.ghid` to drop in over the shipped app.

## Consequences

- **Deterministic diagnostics.** `ghid doctor`, `ghid status`, `ghid sync` and the TUI all read
  the same package and the same single `status.json`.
- **One build artifact.** What `ghid build` produces is what `ghid install-apk` deploys, and it
  is R8-minified — manifest-referenced components (`MainActivity`, `HidService`,
  `HidCommandReceiver`) must be kept by ProGuard rules.
- **Docs match the repo.** README, man page, `AGENTS.md`, architecture/flows/protocol/toolchain
  docs, and the doctor samples were rewritten in the same change; ADR-001 and ADR-006 keep their
  headless wording as historical record of the original design.
- **Re-pairing rules are unchanged:** descriptor, report ID, or subclass edits still require
  unpairing and re-pairing on Windows; Kotlin-only changes need only install + restart.
