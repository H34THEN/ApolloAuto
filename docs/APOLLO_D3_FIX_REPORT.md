# Apollo II — Phase D.3 Fix Report

**Branch:** `fix/apollo-metadata-car-controls-shuffle`  
**Parent:** `feature/apollo-hud-ux-refinement` @ `046b3ff`  
**Date:** 2026-10-09

## Root causes

### 1) Duplicate artist / album lines
- Phone MediaStore often stores identical ARTIST and ALBUM (e.g. both `Carpe Dolor`).
- USB scanner previously set **both** artist and album to the folder name.
- UI always rendered title + artist + album without deduplication.

**Fix:** `TrackMetadataFormatter` presentation layer; USB artist fallback set to `Unknown Artist` (album remains folder). Stored Track / Media3 metadata unchanged for session tags.

### 2) Large SAVE TO CAR control
- Car Mode used a wide text chip under transport.

**Fix:** Compact heart (`Favorite` / `FavoriteBorder`) with 48dp touch target, accent when in Car Favorites, TalkBack labels.

### 3) Shuffle “queue changes on every skip”
- Car Mode QUEUE was `library.drop(currentIndex)`, so every NEXT changed the displayed sequence.
- Media3 `shuffleModeEnabled` also does not expose a UI-friendly stable timeline order.

**Fix:** `StablePlaybackQueue` physically reorders the playlist once on shuffle enable (current track first). NEXT only seeks. QUEUE UI reads `playback.queue` from the player timeline.

## Files changed (primary)

- `data/TrackMetadataFormatter.kt` (+ unit tests)
- `media/StablePlaybackQueue.kt` (+ unit tests)
- `media/PlayerController.kt`
- `ApolloApp.kt` (shared queue coordinator)
- `usb/UsbMusicScanner.kt`
- `ui/MainActivity.kt`, `ui/home/NowPlayingCard.kt`, `ui/NowPlayingActivity.kt`
- `ui/CarModeActivity.kt`
- Docs: `APOLLO_QUEUE_BEHAVIOR.md`, this report, status update

## Test results

| Suite | Result |
|---|---|
| `TrackMetadataFormatterTest` | Run via `./gradlew :app:testDebugUnitTest` |
| `StablePlaybackQueueTest` | Same |
| Device install | Pending this report’s release step |
| Android Auto NEXT | **Not verified** (no DHU this session) |
| Process-death shuffle restore | **Limitation** — not fully persisted |

## Remaining limitations

- Durable shuffle-order persistence across process kill not implemented.
- Queue edit APIs (play next / reorder / remove) beyond `playTracks` replacement are not expanded this phase.
- Full AA command matrix not exercised.
