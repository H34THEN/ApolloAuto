# Android Auto Queue Fix — Multi-track Next

**Version:** `1.0.0-beta.3`  
**Status:** Engineering fix landed; re-verify with DHU USB capture

## Root cause (DHU 04–07)

Selecting a track from a large playlist called `onSetMediaItems` / `onAddMediaItems` with **one** `MediaItem`.  
`MediaQueueExpand` then inferred siblings from **album/folder/artist** metadata. Many playlist tracks are unique albums → expansion collapsed to **1 item** → Media3 hid `SEEK_TO_NEXT` and Next did nothing.

## Fix

1. **`BrowsePlayContext`** — on every `onGetChildren` that returns playables, remember `(parentId → mediaIds)`.
2. On single-item play, **prefer that browsed list** (playlist/album/folder order) before album heuristics.
3. Applied in both `onAddMediaItems` and `onSetMediaItems`.

## Labels (same cut)

| Before | After |
|---|---|
| Car Favorites | Favorites |
| Playlists (N) | Playlists |
| All Tracks | Songs |
| Recently Added | New |
| Recently Played | Recent |
| Most Played | Top Played |

## Verification

```bash
./tools/aa-dhu/run_capture_usb.sh
# Logcat: ApolloAASession — onSetMediaItems expand -> N items (browse=playlist:…)
```

Expect: Next visible, advances track, Queue lists multiple items.
