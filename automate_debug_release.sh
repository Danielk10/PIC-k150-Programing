#!/usr/bin/env bash
set -euo pipefail

# Log to a fresh file
LOG_FILE="/tmp/automation_debug_release.log"
exec > "$LOG_FILE" 2>&1

echo "[$(date)] Phase 1: SDK Setup..."
chmod +x setup-sdk.sh
./setup-sdk.sh

echo "[$(date)] Phase 2: Debug Compilation (assembleDebug)..."
./gradlew assembleDebug

echo "[$(date)] Phase 3: Identify Artifact..."
# The build output is redirected to /tmp/k150 in build.gradle
APK_PATH="/tmp/k150/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK_PATH" ]]; then
    echo "ERROR: Artifact not found at $APK_PATH"
    exit 1
fi

echo "[$(date)] Phase 4: Identify Version and Tag..."
VERSION=$(grep "versionName" app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
TIMESTAMP=$(date +%Y%m%d%H%M)
TAG="v${VERSION}-debug-pre-${TIMESTAMP}"

echo "[$(date)] Phase 5: GitHub Pre-release creation..."
gh release create "$TAG" "$APK_PATH" \
    --title "Debug Pre-release ${TAG}" \
    --notes "Automated background debug build and pre-release." \
    --prerelease

echo "[$(date)] SUCCESS: Automation pipeline finished."
