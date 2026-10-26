# Android Build Toolchain (Gradle Release Pipeline)

GhostHID builds its Android APK with the Gradle wrapper using the root wrapper (`./gradlew`) — Android Gradle Plugin 9.4.1 with built-in Kotlin, Gradle 9.6.0, Jetpack Compose. The build always produces a **release** artifact; there is no debug build in the deployment path.

The former zero-Gradle pipeline (`aapt2` → `javac` → `d8` → `zipalign` → `apksigner`) and the legacy `com.sj010.ghid_headless` sources it compiled were removed along with the headless app. Nothing in the repo invokes those tools anymore.

---

## 1. Entry points

| Command | What it runs | Output |
|---|---|---|
| `./build.sh` | `./gradlew :app:assembleRelease` | `build/ghost-hid.apk` |
| `ghid build` | `bash build.sh` (falls back to `gradlew` if `build.sh` is absent) | `build/ghost-hid.apk` |
| `ghid install-apk` | `adb install -r` of `build/ghost-hid.apk` | APK on the phone |

`ghid install-apk` resolves the artifact in this order:

1. `build/ghost-hid.apk` (repo root, published by `build.sh`)
2. `app/build/outputs/apk/release/app-release.apk` (raw Gradle output)
3. `ghost-hid.apk` (explicit path passed as an argument)

An explicit path always wins: `ghid install-apk /path/to/app.apk`.

---

## 2. What Gradle does

`app/build.gradle.kts`:

- **Application ID** `com.sj010.ghid`, `minSdk 28`, `targetSdk 35`, `compileSdk 37`.
- **Release build type:** `isMinifyEnabled = true` (R8 code shrinking), `isShrinkResources = true`,
  `proguard-android-optimize.txt` plus `app/proguard-rules.pro` (which currently only
  silences AndroidX security-annotation warnings).
- **Signing:** both debug and release sign with the repo's `debug.keystore`
  (`androiddebugkey`, password `android`), so a release APK installs over a debug install
  without re-pairing Bluetooth.
- **Compose:** `buildFeatures.compose` plus the Compose compiler plugin, the Compose BOM
  (`2026.09.00`) and Material 3 `1.5.0-beta02`. That Material 3 line is what ships the
  official [MaterialShapes](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialShapes)
  catalogue, which `ui/theme/ExpressiveShapes.kt` converts to Compose `Shape`s. It also
  forces `compileSdk 37` and AGP 9.x through `aar-metadata`.
- **Kotlin:** supplied by AGP's built-in Kotlin support (no `org.jetbrains.kotlin.android`
  plugin); the Compose and kotlinx.serialization compiler plugins stay explicit.
- **Targets the release artifact only.** `assembleDebug` still exists as a local development
  convenience, but it is never published to `build/` and never installed by `ghid`.

Build stages:

```text
:app:compileReleaseKotlin → :app:compileReleaseJavaWithJavac
    → :app:minifyReleaseWithR8 → :app:shrinkReleaseResources
    → :app:packageRelease → :app:signRelease
    → app/build/outputs/apk/release/app-release.apk
    → cp build/ghost-hid.apk
```

---

## 3. Environment prerequisites

1. **JDK 17** (`JAVA_HOME` pointing at it).
2. **Android SDK** with platform `android-37.0` and build-tools `37.0.0`; `local.properties`
   must contain `sdk.dir=...` (the file is gitignored — create it once). Install them with
   `sdkmanager "platforms;android-37.0" "build-tools;37.0.0"`.
3. **Network access on the first build** so Gradle can resolve AGP, the Compose compiler, and AndroidX
   artifacts; subsequent builds use the wrapper caches in `.gradle` and
   `~/.gradle`.

On macOS with Homebrew:

```bash
brew install openjdk@17 android-commandlinetools
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
```

Build and deploy in a single command:

```bash
./build.sh && ghid install-apk
```

After installing, restart the gateway so the new package state is picked up:

```bash
ghid restart && ghid connect && ghid status
```

---

## 4. Why release, not debug

- R8 strips unused Compose/AndroidX code, so the shipped APK stays small despite the UI stack.
- One artifact: what `ghid build` produces is byte-for-byte what `ghid install-apk` deploys,
  which keeps `ghid doctor --json` results reproducible across machines.
- The signed release build keeps the same package identity (`com.sj010.ghid`), so
  `adb install -r` upgrades in place and the existing Windows Bluetooth pairing survives.

> Changing the HID descriptor, report IDs, or HID subclasses still invalidates a cached
> Windows pairing — that is a protocol change, not a build change. Re-pair GhostHID on
> Windows after any descriptor edit.
