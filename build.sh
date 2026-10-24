#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f "android/gradlew" ]; then
    echo "=== Building GhostHID with Gradle & Jetpack Compose (Material 3 Expressive) ==="
    cd android
    ./gradlew :app:assembleDebug
    cd ..
    mkdir -p build
    cp android/app/build/outputs/apk/debug/app-debug.apk build/ghost-hid.apk
    echo "=== SUCCESS: build/ghost-hid.apk ready! ==="
    exit 0
fi

# Fallback manual build if no gradlew present
SDK_DIR="/opt/homebrew/share/android-commandlinetools"
BUILD_TOOLS="$SDK_DIR/build-tools/35.0.0"
PLATFORM="$SDK_DIR/platforms/android-34/android.jar"
JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

echo "=== Building GhostHID Legacy APK ==="
rm -rf build gen bin *.apk
mkdir -p build gen bin

"$BUILD_TOOLS/aapt2" compile --dir res -o build/resources.zip
"$BUILD_TOOLS/aapt2" link -I "$PLATFORM" \
    --manifest AndroidManifest.xml \
    -o build/unaligned.apk \
    --java gen \
    --auto-add-overlay \
    build/resources.zip

javac -source 17 -target 17 \
    -classpath "$PLATFORM" \
    -d bin \
    $(find gen src -name "*.java")

"$BUILD_TOOLS/d8" --lib "$PLATFORM" --output bin $(find bin -name "*.class")

cd bin
zip -u ../build/unaligned.apk classes.dex
cd ..

"$BUILD_TOOLS/zipalign" -f -p 4 build/unaligned.apk build/ghost-hid.apk

if [ ! -f debug.keystore ]; then
    keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=GhostHID,O=GhostHID,C=US"
fi

"$BUILD_TOOLS/apksigner" sign --ks debug.keystore --ks-pass pass:android --ks-key-alias androiddebugkey build/ghost-hid.apk
echo "=== SUCCESS: build/ghost-hid.apk ready! ==="
