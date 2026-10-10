# Android Auto DHU Visual Review

**Build:** `1.0.0-beta.2` (versionCode 103)  
**Config:** `tools/aa-dhu/config/sobalai_6_9.ini`  
**Reviewer:**  
**Date:**  

Mark each row **Approve** or **Change** (with notes). Re-run `./tools/aa-dhu/capture_review.sh` after fixes.

| # | Screen / moment | File | Verdict | Notes |
|---|---|---|---|---|
| 1 | AA home / launcher | `01_home.png` | | |
| 2 | Media surface (Apollo or picker) | `02_media_surface.png` | | |
| 3 | Media night mode | `03_media_night.png` | | |
| 4 | After play/pause | `04_after_play_pause.png` | | |
| 5 | After next | `05_after_next.png` | | |
| 6 | After previous | `06_after_previous.png` | | |
| 7 | Final / idle | `07_final.png` | | |

## Manual click-through (interactive DHU)

Do these once per pre-release when AA UX changes. Add extra PNGs as `08_*.png` …

| Step | Expected | Verdict | Notes |
|---|---|---|---|
| Open Apollo from app rail (full-app media) | Larger browse than dashboard pane | | |
| Root browse labels | Short titles; Car Favorites → … → More | | |
| Open More | Most Played, Unplayed, Folders, Phone Visualizers | | |
| Play track from Artists/Albums | Now Playing shows art + metadata | | |
| Next visible & advances | Not missing; queue has following item | | |
| Queue → Back | Returns to Now Playing; playback continues | | |
| Phone Visualizers note | Affects phone Immersive only | | |

## Platform limitations (do not file as Apollo bugs)

- Maps / dashboard split is Android Auto host-owned.  
- No projected Immersive visualizer.  

## Sign-off

- [ ] Gallery screenshots attached / committed for this cut  
- [ ] Blocking Change items resolved  
- [ ] Ready for public beta APK
