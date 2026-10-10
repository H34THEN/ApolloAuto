#!/usr/bin/env bash
# Full pre-release DHU capture: prereqs → walkthrough script → summarize gallery.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/aa-dhu/lib/env.sh"

"$ROOT/tools/aa-dhu/check_prereqs.sh"

mkdir -p "$AA_DHU_GALLERY_DIR"
rm -f "$AA_DHU_GALLERY_DIR"/*.png 2>/dev/null || true

echo "==> Launching scripted walkthrough (leave phone unlocked; head unit server ON)"
set +e
"$ROOT/tools/aa-dhu/run_dhu.sh" --script walkthrough_media.txt
rc=$?
set -e

echo
echo "==> Gallery: $AA_DHU_GALLERY_DIR"
ls -la "$AA_DHU_GALLERY_DIR" 2>/dev/null || true
count="$(ls -1 "$AA_DHU_GALLERY_DIR"/*.png 2>/dev/null | wc -l | tr -d ' ')"
echo "==> Captured $count PNG(s)"
echo "Review screenshots, then fill docs/release/aa-dhu-gallery/REVIEW.md"

if [[ "$count" -eq 0 ]]; then
  echo "No screenshots captured. Is the head unit server running?" >&2
  exit 1
fi
exit "$rc"
