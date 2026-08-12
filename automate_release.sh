#!/usr/bin/env bash
set -euo pipefail

# Log everything to /tmp
exec > /tmp/release_automation.log 2>&1

echo "Starting SDK setup..."
./setup-sdk.sh

echo "Starting compilation..."
./gradlew assembleRelease

echo "Identifying version..."
VERSION=$(grep "versionName" app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
TAG="v${VERSION}-pre-$(date +%Y%m%d%H%M)"

echo "Creating GitHub pre-release ${TAG}..."
gh release create "$TAG" /tmp/k150/outputs/apk/release/app-release.apk --title "Pre-release $TAG" --notes "Automated background pre-release build." --prerelease

echo "Process completed successfully."
