#!/usr/bin/env bash
# Launch Desktop Head Unit against the connected phone.
# Usage:
#   tools/aa-dhu/run_dhu.sh                 # interactive, sobalai_6_9 config
#   tools/aa-dhu/run_dhu.sh --compact       # 800x480
#   tools/aa-dhu/run_dhu.sh --script walkthrough_media.txt
#   tools/aa-dhu/run_dhu.sh --usb           # AOA USB transport (DHU 2.x)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

CONFIG="$AA_DHU_CONFIG_DIR/sobalai_6_9.ini"
SCRIPT=""
TRANSPORT="adb" # adb|usb

while [[ $# -gt 0 ]]; do
  case "$1" in
    --compact) CONFIG="$AA_DHU_CONFIG_DIR/compact_800x480.ini"; shift ;;
    --config) CONFIG="$2"; shift 2 ;;
    --script) SCRIPT="$2"; shift 2 ;;
    --usb) TRANSPORT="usb"; shift ;;
    --adb) TRANSPORT="adb"; shift ;;
    -h|--help)
      sed -n '2,10p' "$0"
      exit 0
      ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [[ ! -x "$DHU_BIN" ]]; then
  echo "DHU not installed. Run tools/aa-dhu/install_dhu.sh" >&2
  exit 1
fi

mkdir -p "$AA_DHU_GALLERY_DIR"

if [[ "$TRANSPORT" == "adb" ]]; then
  adb forward tcp:5277 tcp:5277
  echo "==> ADB forward tcp:5277 (ensure head unit server is running on phone)"
  ARGS=(--adb=5277)
else
  ARGS=(--usb)
fi

ARGS+=(--config="$CONFIG" --input=touch)
echo "==> Starting DHU config=$CONFIG"
echo "==> Screenshots → $AA_DHU_GALLERY_DIR"
echo "==> Phone must be unlocked; Android Auto → Start head unit server"

# Screenshots are written relative to CWD.
cd "$AA_DHU_GALLERY_DIR"

if [[ -n "$SCRIPT" ]]; then
  SCRIPT_PATH="$SCRIPT"
  [[ "$SCRIPT_PATH" = /* ]] || SCRIPT_PATH="$AA_DHU_SCRIPTS_DIR/$SCRIPT"
  echo "==> Scripted: $SCRIPT_PATH"
  "$DHU_BIN" "${ARGS[@]}" < "$SCRIPT_PATH"
else
  "$DHU_BIN" "${ARGS[@]}"
fi
