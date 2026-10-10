# Apollo II — Queue & Shuffle Behavior

**Branch:** `fix/apollo-metadata-car-controls-shuffle`  
**Owner:** Shared ExoPlayer in `ApolloPlaybackService` via Media3 session  
**Coordinator:** `StablePlaybackQueue` (process-scoped on `ApolloApp`)

## Queue ownership

- The authoritative playlist is the ExoPlayer / MediaController media-item timeline.
- Phone UI, Car Mode, notifications, Bluetooth, and Android Auto all command the same session.
- `StablePlaybackQueue` records the unshuffled `original` list for the session and whether shuffle is active.

## Shuffle activation

1. Preserve the current track and playback position.
2. Build a new order: **current item first**, remaining items shuffled once (`Random(generation)`).
3. Call `setMediaItems(newOrder, startIndex = 0, positionMs)` once.
4. Force `shuffleModeEnabled = false` on Media3 so ExoPlayer does not apply a second shuffle layer.
5. UI `shuffle` flag is `StablePlaybackQueue.shuffleEnabled`.

Ordinary NEXT / PREVIOUS / track-end advancement never call `setMediaItems` and never reshuffle.

## Next / previous

- NEXT → `seekToNextMediaItem()` when available; at end of queue with no next item, wraps to index 0
- PREVIOUS → `seekToPreviousMediaItem()` when available; otherwise wraps to last item
- Queue tap → `seekTo(index, 0)` via `PlayerController.seekToQueueIndex`
- Home “preview” list is UI-capped (40 rows) but play always queues the full filtered library

## Repeat

Uses Media3 `Player.repeatMode` (OFF / ALL / ONE) independently of the stable order.

## Manual edits

- New `playTracks(...)` replaces the session queue (and re-applies shuffle projection if shuffle is already on).
- Add-to-playlist / Car Favorites do **not** rebuild the playback timeline.

## Persistence

- While the process lives, order lives in the player timeline + `StablePlaybackQueue.original`.
- Process death: Media3 / system may restore the last media items; full durable shuffle-order persistence across kill is a known limitation (see D3 report).

## Android Auto

AA browse/play still resolves through `ApolloPlaybackService`. Playback commands mutate the shared player; no separate shuffle RNG for AA.
