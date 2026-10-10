#!/usr/bin/env bash
# One-shot DHU capture with a persistent stdin writer (avoids EOF killing DHU).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

GALLERY="$AA_DHU_GALLERY_DIR"
CONFIG="${1:-$AA_DHU_CONFIG_DIR/sobalai_6_9.ini}"
mkdir -p "$GALLERY"
cd "$GALLERY"

pkill -f 'desktop-head-unit' 2>/dev/null || true
sleep 1

adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
adb forward --remove-all >/dev/null 2>&1 || true
adb forward tcp:5277 tcp:5277 >/dev/null

rm -f /tmp/dhu-cmd.fifo /tmp/dhu-review.log
mkfifo /tmp/dhu-cmd.fifo

"$DHU_BIN" \
  --adb=127.0.0.1:5277 \
  --config="$CONFIG" \
  --input=touch \
  < /tmp/dhu-cmd.fifo > /tmp/dhu-review.log 2>&1 &
DHU_PID=$!
echo "$DHU_PID" > /tmp/dhu.pid

# Persistent writer in this process
exec 9>/tmp/dhu-cmd.fifo
send() { printf '%s\n' "$1" >&9; }

cleanup() {
  send "quit" || true
  exec 9>&- || true
  wait "$DHU_PID" 2>/dev/null || true
}
trap cleanup EXIT

sleep 4
if ! kill -0 "$DHU_PID" 2>/dev/null; then
  echo "DHU exited early:" >&2
  cat /tmp/dhu-review.log >&2
  exit 1
fi
echo "==> DHU running pid=$DHU_PID"

send "focus video on"
send "focus audio on"
sleep 3
send "day"
sleep 1
send "screenshot 01_home.png"
sleep 1
send "keycode media"
sleep 5
send "screenshot 02_media_surface.png"
sleep 1
send "night"
sleep 2
send "screenshot 03_media_night.png"
sleep 1
send "day"
sleep 1
send "keycode media_play_pause"
sleep 4
send "screenshot 04_after_play_pause.png"
sleep 1
send "keycode media_next"
sleep 4
send "screenshot 05_after_next.png"
sleep 1
send "keycode media_previous"
sleep 4
send "screenshot 06_after_previous.png"
sleep 1
send "screenshot 07_final.png"
sleep 2

# Also grab phone surface for correlation
adb exec-out screencap -p > "$GALLERY/phone_during_dhu.png" || true

echo "==> DHU log (tail):"
tail -40 /tmp/dhu-review.log || true
echo "==> Gallery:"
ls -la "$GALLERY"/*.png 2>/dev/null || echo "(no pngs)"

# Count real DHU screenshots (exclude phone_/mac_)
count=0
for f in "$GALLERY"/0*.png; do
  [[ -f "$f" ]] || continue
  count=$((count + 1))
done
echo "==> Captured $count scripted frames"
if [[ "$count" -eq 0 ]]; then
  echo "No DHU screenshots — video focus may still be denied. Check phone for AA prompts." >&2
  exit 2
fi
