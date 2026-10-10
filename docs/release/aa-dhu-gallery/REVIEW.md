# Android Auto DHU Visual Review

**Build:** `1.0.0-beta.2` (versionCode 103)  
**Config:** `tools/aa-dhu/config/sobalai_6_9.ini` (USB AOA)  
**Capture:** `./tools/aa-dhu/run_capture_usb.sh`  
**Date:** 2026-10-10  

Mark each row **Approve** or **Change**.

| # | Screen / moment | File | Verdict | Notes |
|---|---|---|---|---|
| 1 | Full-app Now Playing (art + metadata) | `01_home.png` | | Art OK; Pause+Prev; **Next missing**; Queue pill present |
| 2 | Browse / Playlists tabs | `02_media_surface.png` | | Tabs truncated (`Car Favori…`); counts in titles |
| 3 | Playlist track list | `03_media_night.png` | | No row thumbnails (host list); pink NP FAB OK |
| 4 | After play/pause | `04_after_play_pause.png` | | Playing; **Next still missing** |
| 5 | After media_next key | `05_after_next.png` | | Track did **not** advance (same song) |
| 6 | After media_previous | `06_after_previous.png` | | Restarted same track |
| 7 | Final | `07_final.png` | | Shuffle icon appeared; Next still absent |

## Observed vs goals

| Goal | DHU result |
|---|---|
| Full-app media (not Maps split) | **PASS** — Apollo fills main pane + left rail |
| Queue accessible / dismissible | **PASS** — Queue pill on NP |
| Play/Pause | **PASS** |
| Next / multi-item queue | **FAIL** — Next hidden; skip did not change track |
| Shallow root labels | **PARTIAL** — still seeing truncated titles / `(N)` counts (AA cache or tab chrome) |
| Projected visualizer | N/A (correctly absent) |

## Likely follow-ups (engineering)

1. Confirm `MediaQueueExpand` runs on AA play (`ApolloAASession` log) and timeline size > 1.  
2. Force-stop Android Auto + Apollo after install so browse tree refreshes.  
3. Prefer `./tools/aa-dhu/run_capture_usb.sh` over ADB tunnel (video focus).  
4. On first USB connect, tap **Continue** on phone Welcome dialog.

## Sign-off

- [ ] Blocking Change items resolved  
- [ ] Re-capture after Next fix  
- [ ] Ready for public beta APK
