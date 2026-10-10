# DHU Gallery Review — `1.0.0-beta.3`

**Capture:** `./tools/aa-dhu/run_capture_usb.sh`  
**Phone:** Metroid A024 · Apollo **release** `com.apolloauto.player` `1.0.0-beta.3`  
**Note:** Uninstall `com.apolloauto.player.debug` before DHU — AA prefers the debug session when both are present.

## Pre-fix (stale single-item / debug session)

| Frame | Result |
|---|---|
| Now Playing | Next absent; queue size 1 |
| Next keycode | No track change |

## Post-fix (multi-track queue seeded from phone library → 3706 items)

| Frame | Observation | Verdict |
|---|---|---|
| `01_home.png` / `02_media_surface.png` | **GOOD MORNING SUNSHINE** · Previous + **Next** visible · Queue action present | PASS |
| `04_after_play_pause.png` | Pause ↔ Play | PASS |
| `05_after_next.png` | Advanced to **Closer** / The Tiny · Next still visible | **PASS — blocker cleared** |
| `06_after_previous.png` | Returned to **GOOD MORNING SUNSHINE** | PASS |

## Still for owner / follow-up

- Browse-label truncation on Sobalai (code: short titles) — re-check tabs on vehicle
- AA browse-tap expand path (BrowsePlayContext) — unit tested; re-select a playlist track on HU after upgrade
- DHU ≠ vehicle certification
