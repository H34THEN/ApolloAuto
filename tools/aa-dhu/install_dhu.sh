#!/usr/bin/env bash
# Install Android Auto Desktop Head Unit into ANDROID_HOME.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

echo "==> ANDROID_HOME=$ANDROID_HOME"
if [[ -x "$DHU_BIN" ]]; then
  echo "==> DHU already installed: $DHU_BIN"
  "$DHU_BIN" --version || true
  exit 0
fi

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager not found. Install Android cmdline-tools and add them to PATH." >&2
  exit 1
fi

yes | sdkmanager --sdk_root="$ANDROID_HOME" "extras;google;auto"
chmod +x "$DHU_BIN"
echo "==> Installed: $DHU_BIN"
"$DHU_BIN" --version
