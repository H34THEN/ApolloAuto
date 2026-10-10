# Android Auto Device Test Plan

**Target HU:** Sobalai 6.9″ wireless AA (VW Rabbit)  
**Phone package:** `com.apolloauto.player` `1.0.0-beta.2`  
**Branch:** `hotfix/android-auto-ux-p0`

## Lab substitutes (when car unavailable)

| Tool | Use |
|---|---|
| Desktop Head Unit (DHU) | Browse + Now Playing layout — see `ANDROID_AUTO_DHU.md` + `tools/aa-dhu/` |
| `./tools/aa-dhu/capture_review.sh` | Scripted screenshots → `aa-dhu-gallery/` |
| Media Controller Test | Available commands, queue size, skip |
| `adb logcat -s ApolloAASession` | `onConnect`, `expandSingle` |
| `adb shell dumpsys media_session` | Legacy PlaybackState actions |

## Checklist

### Discovery & modes
- [ ] Apollo appears in AA media apps / launcher
- [ ] Dashboard media pane shows Apollo when selected
- [ ] Opening Apollo from the AA app rail enters **full-app media** (browse + NP) — host-dependent
- [ ] Returning to AA home restores dashboard (Maps may reappear — host behavior)

### Browse
- [ ] Root shows: Car Favorites, Playlists, All Tracks, Artists, Albums, Recently Added, Recently Played, More
- [ ] Titles are short (no `(N)` suffixes on root)
- [ ] More contains Most Played, Unplayed, Folders, Phone Visualizers
- [ ] Back pops one level
- [ ] Search returns playable tracks
- [ ] Empty categories do not crash

### Now Playing / transport
- [ ] Play / Pause toggles and matches audio
- [ ] **Next** visible when queue has a following item; advances correctly
- [ ] Previous works (previous item or restart)
- [ ] Seek updates position without clearing queue
- [ ] Artwork + title + artist correct
- [ ] Shuffle (if used) does not reshuffle on every Next

### Queue
- [ ] Queue opens from Now Playing
- [ ] Queue lists expanded / playlist items
- [ ] Back returns to Now Playing without stopping playback

### Hardware keys (document actual mapping)
- [ ] HU Next / Previous (if sent as media keys)
- [ ] Volume knob (system)
- [ ] HU Back

### Explicit non-goals
- [ ] Maps pane cannot be removed by Apollo — **pass = documented**
- [ ] No projected shader visualizer — Immersive remains phone/native only

## Results log

| Date | Environment | Result | Notes |
|---|---|---|---|
| 2026-10-10 | Unit tests + signed APK install (phone) | PASS (lab) | Car retest pending owner |
| | Sobalai wireless AA | _pending_ | Owner photo baseline pre-fix |

**Do not mark physical-car verification PASS until the Sobalai session is executed.**
