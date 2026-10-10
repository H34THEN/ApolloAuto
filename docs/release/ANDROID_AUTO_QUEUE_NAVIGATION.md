# Android Auto Queue Navigation

## Host-owned queue UI

Classic MediaLibraryService integrations do **not** draw a Compose queue on the head unit. Android Auto provides:

1. **Now Playing** — transport + metadata + artwork  
2. **Queue** — button on Now Playing opens the player timeline  
3. **Back** — returns to Now Playing (or browse, depending on host stack)

Apollo’s job is to keep a **stable multi-item timeline** so Queue is meaningful and dismissible via the host Back affordance.

## Intended path

```
NOW PLAYING → (Queue) → QUEUE LIST → (Back) → NOW PLAYING
```

The browse root must **not** permanently expand a queue-like dump of every track. Queue lives under Now Playing only.

## Apollo behavior

| Entry | Timeline |
|---|---|
| Play one track from browse | Expanded contextual playlist (`MediaQueueExpand`) |
| Play playlist / Car Favorites | Playlist order preserved |
| Next / Previous | Walk the same timeline; no reshuffle |
| Phone UI | Shares the same Media3 session / ExoPlayer |

## What we do not do

- Custom draggable AA overlays  
- Fake “collapse” panels  
- Regenerating the playlist on every screen change  
- Injecting visualizers into the Queue template
