#!/usr/bin/env bash
# Verify local machine is ready for Android Auto DHU testing.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

ok=0
warn=0

pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; ok=1; }
note() { echo "WARN  $*"; warn=1; }

pass "ANDROID_HOME=$ANDROID_HOME"

if [[ -x "$DHU_BIN" ]]; then
  pass "DHU binary: $DHU_BIN ($("$DHU_BIN" --version 2>&1 | head -1))"
else
  fail "DHU missing — run: tools/aa-dhu/install_dhu.sh"
fi

if command -v adb >/dev/null 2>&1; then
  pass "adb on PATH"
else
  fail "adb missing"
fi

devices="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -n "$devices" ]]; then
  pass "device(s): $(echo "$devices" | tr '\n' ' ')"
else
  fail "no adb device connected"
fi

if adb shell pm path com.apolloauto.player >/dev/null 2>&1; then
  ver="$(adb shell dumpsys package com.apolloauto.player | awk -F= '/versionName=/{print $2; exit}')"
  pass "Apollo release installed ($ver)"
else
  note "com.apolloauto.player not installed — install dist release APK before DHU review"
fi

if adb shell pm path com.google.android.projection.gearhead >/dev/null 2>&1; then
  pass "Android Auto (gearhead) installed"
else
  fail "Android Auto app missing — install from Play Store"
fi

# Head unit server listens on device port 5277 when started.
if adb shell 'toybox netstat -ltn 2>/dev/null || true; ss -ltn 2>/dev/null || true' 2>/dev/null \
    | grep -E ':5277\b' >/dev/null 2>&1; then
  pass "head unit server listening on :5277"
else
  note "head unit server NOT listening on :5277 — start it before run_dhu.sh"
fi

echo
echo "Manual phone steps (required once per session for --adb mode):"
echo "  1. Open Android Auto settings (Developer mode: tap Version 10×)"
echo "  2. Enable Unknown sources (for sideloaded Apollo)"
echo "  3. Overflow menu → Start head unit server (notification appears)"
echo "  4. Unlock phone screen"
echo "  5. Then: ./tools/aa-dhu/run_dhu.sh   or   ./tools/aa-dhu/capture_review.sh"
echo
if [[ "$ok" -ne 0 ]]; then
  echo "Prereq check FAILED"
  exit 1
fi
echo "Prereq check OK"
exit 0
