#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}"

mkdir -p "${SDK_ROOT}/cmdline-tools"

if [[ ! -x "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" ]]; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "${tmp_dir}"' EXIT

  curl -fsSL "${CMDLINE_TOOLS_URL}" -o "${tmp_dir}/${CMDLINE_TOOLS_ZIP}"
  unzip -q -o "${tmp_dir}/${CMDLINE_TOOLS_ZIP}" -d "${SDK_ROOT}/cmdline-tools"
  rm -rf "${SDK_ROOT}/cmdline-tools/latest"
  mv "${SDK_ROOT}/cmdline-tools/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
fi

export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

set +o pipefail
yes | sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null
set -o pipefail

sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" \
  "platform-tools" \
  "platforms;android-23" \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "cmake;3.22.1" \
  "ndk;28.2.13676358"

printf 'sdk.dir=%s\n' "${ANDROID_SDK_ROOT}" > "${PROJECT_ROOT}/local.properties"
chmod +x "${PROJECT_ROOT}/gradlew"

echo "Android SDK/NDK configurado en ${ANDROID_SDK_ROOT}"
echo "local.properties generado en ${PROJECT_ROOT}/local.properties"
echo "Gradle wrapper habilitado: ${PROJECT_ROOT}/gradlew"
