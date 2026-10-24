# Standalone Android Toolchain (Zero Gradle)

GhostHID builds its Android APK using a direct command-line pipeline without Gradle, Groovy, Kotlin scripts, or Android Studio daemons.

---

## 1. Why Zero Gradle

Standard Android builds use Gradle, which incurs high overhead:
- JVM daemon warmup and background memory consumption (~1 GB RAM).
- Dependency resolution scans and lock file management.
- Multi-minute initial configuration and build phases.

GhostHID has zero external Java dependencies; it relies only on Android framework APIs (`android.bluetooth.*`, `android.app.*`, `android.content.*`). Building the APK directly with Google's standalone command-line tools takes approximately 1.8 seconds from clean state and produces a ~20 KB executable.

```text
build.sh Execution Timeline:
+--------------------------------------------------------------+
| aapt2 compile | aapt2 link | javac | d8 | zip | zipalign | sign |
+--------------------------------------------------------------+
0.0s            0.3s         0.7s    1.1s 1.3s  1.5s       1.8s
```

---

## 2. Compilation Stages in `build.sh`

The build pipeline in [`build.sh`](file:///Users/sj/Desktop/ws/ghost-hid/build.sh) runs in seven discrete steps:

### Stage 1: Resource Compilation (`aapt2 compile`)
Compiles XML files in `res/` into intermediate binary flat archives:
```bash
$BUILD_TOOLS/aapt2 compile --dir res -o build/resources.zip
```

### Stage 2: Resource Linking (`aapt2 link`)
Links compiled resources against the target Android platform stubs (`android.jar`), generates the `gen/com.sj010/ghid_headless/R.java` symbol file, and constructs the initial APK skeleton with compiled `AndroidManifest.xml`:
```bash
$BUILD_TOOLS/aapt2 link -I "$PLATFORM" \
    --manifest AndroidManifest.xml \
    -o build/unaligned.apk \
    --java gen \
    --auto-add-overlay \
    build/resources.zip
```

### Stage 3: Java Bytecode Compilation (`javac`)
Compiles both generated source (`gen/`) and project source (`src/`) targeting Java 17 bytecode:
```bash
javac -source 17 -target 17 \
    -classpath "$PLATFORM" \
    -d bin \
    $(find gen src -name "*.java")
```

### Stage 4: Dalvik Executable Conversion (`d8`)
Converts JVM `.class` bytecode files into Dalvik/ART executable bytecode (`classes.dex`):
```bash
$BUILD_TOOLS/d8 --lib "$PLATFORM" --output bin $(find bin -name "*.class")
```

### Stage 5: Archive Injection (`zip`)
Injects `classes.dex` into the root of `build/unaligned.apk`:
```bash
cd bin
zip -u ../build/unaligned.apk classes.dex
cd ..
```

### Stage 6: 4-Byte Memory Alignment (`zipalign`)
Aligns all uncompressed data (such as shared assets and raw resources) on 4-byte boundaries. This allows the Android operating system to memory-map resources directly from flash storage using `mmap()` without allocating extra RAM:
```bash
$BUILD_TOOLS/zipalign -f -p 4 build/unaligned.apk build/ghost-hid.apk
```

### Stage 7: Cryptographic Signing (`apksigner`)
If `debug.keystore` does not exist, `build.sh` generates an RSA-2048 keypair. It then applies Android APK Signature Scheme v2 and v3:
```bash
$BUILD_TOOLS/apksigner sign \
    --ks debug.keystore \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    build/ghost-hid.apk
```

---

## 3. Environment Prerequisites

To run [`build.sh`](file:///Users/sj/Desktop/ws/ghost-hid/build.sh):
1. **JDK 17:** OpenJDK 17 installed and `JAVA_HOME` configured.
2. **Android Command-Line Tools:**
   - `build-tools/35.0.0` or newer containing `aapt2`, `d8`, `zipalign`, and `apksigner`.
   - `platforms/android-34/android.jar` platform stub.

On macOS with Homebrew:
```bash
brew install openjdk@17 android-commandlinetools
```

Set paths in your shell profile:
```bash
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
```

Once installed, build and deploy in a single command:
```bash
./build.sh && ghid install-apk
```
