# Android Auto Control Fixes

**Version:** `1.0.0-beta.2`  
**Primary file:** `app/src/main/java/com/apolloauto/player/media/ApolloPlaybackService.kt`  
**Helper:** `MediaQueueExpand.kt`

## Problem

Owner Now Playing showed Pause + Previous + Queue but **no Next**.

## Cause

Per Media3 guidance ([androidx/media#1778](https://github.com/androidx/media/issues/1778)): `Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM` is unavailable when the timeline has only one item. Android Auto browse often calls `onSetMediaItems` with a single track.

## Fix

1. **`MediaQueueExpand`** — When a single playable is set, expand to:
   - Same album (+ artist when known), else
   - Same folder, else
   - Same artist, else
   - Full library catalog  
   Start index = selected track. Multi-item playlists (Playlists / Car Favorites) are left unchanged.
2. **`onConnect`** — Explicitly advertise `DEFAULT_PLAYER_COMMANDS` (play/pause, seek, next/previous when the player allows).
3. **Custom actions** — On automotive packages (`projection.gearhead`, etc.), only keep `CMD_SAVE_CAR_FAV` in available session commands. Phone Immersive customs (`CMD_NEXT_VISUAL`, `CMD_TOGGLE_DRIVE`) stay for non-auto controllers. No `setCustomLayout` crowding the compact NP row.

## Expected controls

| Action | Behavior |
|---|---|
| Play / Pause | Toggles ExoPlayer; state reflected by host |
| Previous | Previous media item, or beginning-of-track per Media3/ExoPlayer rules |
| Next | Next item in the expanded / playlist queue |
| Seek | Scrub current item; queue unchanged |
| Queue | Host queue screen from player timeline (Back returns to Now Playing) |

Next is **not** advertised as available when the expanded queue still has no next item (true single-track libraries).

## Verification

- Unit: `MediaQueueExpandTest`
- Device: Media Controller Test / AA — play from Artists/Albums/All Tracks and confirm Next appears and advances without reshuffling
- Logcat tag: `ApolloAASession` (`expandSingle -> N items`)
