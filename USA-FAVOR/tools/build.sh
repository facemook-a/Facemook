#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :Admin:assembleDebug :User:assembleDebug :Admin:testDebugUnitTest :User:testDebugUnitTest
mkdir -p dist
cp USA-Admin/app/build/outputs/apk/debug/Admin-debug.apk dist/USA-Admin.apk
cp USA-Favor/app/build/outputs/apk/debug/User-debug.apk dist/USA-Favor.apk
sha256sum dist/USA-Admin.apk dist/USA-Favor.apk > dist/SHA256SUMS
