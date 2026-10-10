#!/usr/bin/env bash
# Shared env for Apollo AA DHU tools.

export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "${ANDROID_HOME}" ]]; then
  for candidate in \
    "$HOME/Library/Android/sdk" \
    /opt/homebrew/share/android-commandlinetools \
    "$HOME/Android/Sdk"; do
    if [[ -d "$candidate" ]]; then
      export ANDROID_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "${ANDROID_HOME}" ]]; then
  echo "ANDROID_HOME is not set and no SDK was found." >&2
  exit 1
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
export DHU_DIR="$ANDROID_HOME/extras/google/auto"
export DHU_BIN="$DHU_DIR/desktop-head-unit"

AA_DHU_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export AA_DHU_ROOT
export AA_DHU_CONFIG_DIR="$AA_DHU_ROOT/config"
export AA_DHU_SCRIPTS_DIR="$AA_DHU_ROOT/scripts"
export AA_DHU_GALLERY_DIR="${AA_DHU_GALLERY_DIR:-$AA_DHU_ROOT/../../docs/release/aa-dhu-gallery}"
