#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building GhostHID release APK (Gradle + Jetpack Compose, Material 3 Expressive) ==="
./gradlew :app:assembleRelease

mkdir -p build
cp app/build/outputs/apk/release/app-release.apk build/ghost-hid.apk
echo "=== SUCCESS: build/ghost-hid.apk (release, package com.sj010.ghid) ready! ==="
