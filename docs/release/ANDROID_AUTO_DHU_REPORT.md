# Android Auto DHU Report — Car Experience Cut

**App:** Apollo II `1.0.0-beta.3` (versionCode 104)  
**Harness:** `./tools/aa-dhu/run_capture_usb.sh`  
**Gallery:** `docs/release/aa-dhu-gallery/`  
**Commit:** `456ecf5` (+ follow-up docs)

## Critical setup finding

With **both** `com.apolloauto.player` and `com.apolloauto.player.debug` installed, Android Auto bound the **debug** session (beta.1-cyber, queue size 1).  
**Mitigation:** `adb uninstall --user 0 com.apolloauto.player.debug` before AA/DHU validation.

## Pre-fix (debug / single-item)

| Check | Result |
|---|---|
| Next visible | FAIL |
| Next advances | FAIL |
| Queue multi-track | FAIL (size=1) |

## Post-fix (release + multi-track library play)

Seed: phone library tap → `PlayerController.playTracks` → media session **queue size=3706**, actions include skip-next.

| Check | Result |
|---|---|
| Next visible | **PASS** (`02_media_surface.png`) |
| Next advances | **PASS** — GOOD MORNING SUNSHINE → Closer (`05_after_next.png`) |
| Previous returns | **PASS** — back to GOOD MORNING SUNSHINE (`06_after_previous.png`) |
| Queue action present | PASS |
| Full-app Now Playing | PASS (no Maps split in media surface frames) |
| Browse short labels | Code landed; tab visual not re-shot this run |
| Sobalai vehicle | Owner retest required |

## Engineering

- `BrowsePlayContext` + `MediaQueueExpand` browse siblings for AA single-item browse taps
- Catalog/album/folder fallbacks remain for when browse context is cold

## Claim boundary

DHU results do **not** certify Sobalai wireless AA.
