#!/usr/bin/env bash
# Wait until the phone head-unit server is up, then run the gallery capture.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

MAX_WAIT="${1:-180}"
echo "Waiting up to ${MAX_WAIT}s for head unit server on device :5277 …"
echo "On phone: Android Auto overflow → Start head unit server (keep unlocked)."

adb shell am start -n com.google.android.projection.gearhead/.companion.settings.DefaultSettingsActivity >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true

elapsed=0
while (( elapsed < MAX_WAIT )); do
  if adb shell 'toybox netstat -ltn 2>/dev/null; ss -ltn 2>/dev/null' 2>/dev/null | rg -q ':5277'; then
    echo "Head unit server detected."
    exec "$ROOT/tools/aa-dhu/capture_review.sh"
  fi
  sleep 3
  elapsed=$((elapsed + 3))
  printf '.'
done
echo
echo "Timed out waiting for :5277. Start the head unit server, then re-run:" >&2
echo "  ./tools/aa-dhu/wait_and_capture.sh" >&2
exit 1
