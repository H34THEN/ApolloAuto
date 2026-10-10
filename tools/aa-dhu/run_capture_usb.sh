#!/usr/bin/env bash
# Full DHU capture over USB AOA. Accepts phone "Welcome / Continue" prompts via adb.
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

accept_phone_prompts() {
  # Best-effort while USB accessory may flap adb.
  for _ in $(seq 1 40); do
    if ! adb devices 2>/dev/null | grep -q $'\tdevice$'; then
      sleep 1
      continue
    fi
    adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
    adb shell uiautomator dump /sdcard/uidump.xml >/dev/null 2>&1 || true
    adb pull /sdcard/uidump.xml /tmp/aa-prompt.xml >/dev/null 2>&1 || true
    [[ -f /tmp/aa-prompt.xml ]] || { sleep 1; continue; }
    # Tap common affirmative buttons by text bounds
    python3 - <<'PY' || true
import re, subprocess
xml=open("/tmp/aa-prompt.xml").read()
targets=("Continue","Allow","While using the app","Only this time","OK","Got it","Accept","Start")
for m in re.finditer(r'text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    t=m.group(1).replace("&amp;","&")
    if t in targets or t.startswith("While using"):
        x1,y1,x2,y2=map(int,m.groups()[1:])
        x,y=(x1+x2)//2,(y1+y2)//2
        print(f"tap {t!r} @({x},{y})")
        subprocess.run(["adb","shell","input","tap",str(x),str(y)], check=False)
PY
    sleep 1
  done
}

rm -f /tmp/dhu-cmd.fifo /tmp/dhu-usb.log
mkfifo /tmp/dhu-cmd.fifo

"$DHU_BIN" --usb --config="$CONFIG" --input=touch \
  < /tmp/dhu-cmd.fifo > /tmp/dhu-usb.log 2>&1 &
DHU_PID=$!
echo "$DHU_PID" > /tmp/dhu.pid
exec 9>/tmp/dhu-cmd.fifo
send() { printf '%s\n' "$1" >&9; }

cleanup() {
  send "quit" || true
  exec 9>&- || true
  wait "$DHU_PID" 2>/dev/null || true
  adb start-server >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> Waiting for USB accessory attach…"
for i in $(seq 1 40); do
  if grep -qE 'Attached!|SSL negotiation finished' /tmp/dhu-usb.log 2>/dev/null; then
    echo "==> Link up"
    break
  fi
  if ! kill -0 "$DHU_PID" 2>/dev/null; then
    echo "DHU exited during attach:" >&2
    cat /tmp/dhu-usb.log >&2
    exit 1
  fi
  sleep 1
done

echo "==> Accepting phone prompts (Continue / Allow)…"
accept_phone_prompts &
PROMPT_PID=$!

# Give user/phone time to finish TOS / permissions
sleep 25
kill "$PROMPT_PID" 2>/dev/null || true
wait "$PROMPT_PID" 2>/dev/null || true

if ! kill -0 "$DHU_PID" 2>/dev/null; then
  echo "DHU died during prompts:" >&2
  cat /tmp/dhu-usb.log >&2
  exit 1
fi

send "focus video on"
send "focus audio on"
sleep 2
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

adb start-server >/dev/null 2>&1 || true
adb exec-out screencap -p > "$GALLERY/phone_during_dhu.png" 2>/dev/null || true

echo "==> Log:"
tail -60 /tmp/dhu-usb.log || true
echo "==> Gallery:"
ls -la "$GALLERY"/0*.png 2>/dev/null || true
count="$(ls -1 "$GALLERY"/0*.png 2>/dev/null | wc -l | tr -d ' ')"
echo "==> Captured $count frames"
# Fail only if still stuck on the "check your phone" gate (tiny/black frames still count as files)
if [[ "$count" -eq 0 ]]; then
  exit 2
fi
