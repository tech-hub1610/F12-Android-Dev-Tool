#!/usr/bin/env bash
set -e

# ==============================================================
# F12 Browser — Automated Build & Upgrade Script
# Usage: ./build_apk.sh [versionName] [versionCode]
# Example: ./build_apk.sh 1.1.0 2
# ==============================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

VERSION_NAME="${1:-1.0.0}"
VERSION_CODE="${2:-1}"

echo "=========================================================="
echo " Building F12 Browser APK"
echo " Version Name: $VERSION_NAME"
echo " Version Code: $VERSION_CODE"
echo "=========================================================="

# Update versionName and versionCode if supplied
if [ "$#" -ge 1 ]; then
    sed -i "s/versionCode [0-9]\+/versionCode $VERSION_CODE/" app/build.gradle
    sed -i "s/versionName \"[^\"]\+\"/versionName \"$VERSION_NAME\"/" app/build.gradle
fi

echo "Running tests..."
/usr/local/bin/gradle test

echo "Compiling and packaging APKs (Debug & Release)..."
/usr/local/bin/gradle assemble

echo ""
echo "=========================================================="
echo " BUILD SUCCESSFUL! Generated APKs located in ./output/:"
echo "=========================================================="
ls -lh output/

echo ""
echo "Debug APK:   output/F12-Browser-v${VERSION_NAME}-debug.apk"
echo "Release APK: output/F12-Browser-v${VERSION_NAME}-release.apk"
