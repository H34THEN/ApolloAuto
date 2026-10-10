# Android Auto UX Audit — Sobalai / VW Rabbit

**Date:** 2026-10-10  
**Branch:** `hotfix/android-auto-ux-p0`  
**App:** Apollo II `1.0.0-beta.2` (versionCode 103)  
**Vehicle / HU:** 2007 VW Rabbit Mk5 · Sobalai 6.9″ wireless Android Auto  
**Integration:** Media3 `MediaLibraryService` (classic media browse) — not Car App Library templates

## Observed (owner photographs)

Photos were described by the owner (not checked into the repo). Documented from that description:

| Photo | Observation |
|---|---|
| 1 — Now Playing | Art, title/artist, scrubber, large pause, previous, queue; **no visible next**; Maps on the right of the AA dashboard |
| 2 — Browse | Horizontal/root categories: Car Favorites, Playlists, Recently Added, More, Unplayed, Recently Played, Most Played, Artists, Albums (+ search). **Labels truncated** on the compact HUD |
| 3 — Dashboard | App rail left, Apollo browse center, Maps right — standard AA split layout |

## Platform ownership (non-negotiable)

| Surface | Controlled by |
|---|---|
| Dashboard split / Maps pane size | Android Auto host |
| Media pane chrome, transport layout | Android Auto host |
| Browse list styling / truncation rules | Android Auto host |
| Full-app media vs dashboard media | User + AA (tap Apollo launcher / media app) |
| Shader / immersive visualizer projection | **Not supported** for third-party media apps |

Apollo **cannot** remove Maps from the dashboard, resize host panels, or inject unrestricted animated visualizers into the projected UI without violating AA driver-distraction rules.

## Root-cause findings (pre-fix)

1. **Missing Next** — Media3 only exposes `COMMAND_SEEK_TO_NEXT*` when the player timeline has a next item. Browse taps often set a **single** `MediaItem`, so Next (and a meaningful Queue) disappear. Previous still appears (seek-to-start / previous media).
2. **Crowded browse root** — Eleven top-level folders with parenthetical counts truncated on a 6.9″ landscape media pane.
3. **Visualizer expectation** — “Phone Visualizers” browse nodes change **phone** Immersive mode only; they never render on the Sobalai projection.
4. **Custom session commands** — Extra customs (viz / drive) can compete for scarce Now Playing action slots on compact HUDs.

## Supported product split

| Mode | Priority |
|---|---|
| **Projected Android Auto** | Playback, metadata, artwork, transport, shallow browse, queue via host |
| **Native Immersive / Viz-Only / Car Mode** | Fullscreen visualizers, shaders, Trance Reactor, backgrounds |

## Car App Library templates (feasibility)

MediaPlaybackTemplate / Car App Library media templates could improve header/queue presentation on some AAOS targets. For this wireless phone→HU projection stack, classic `MediaLibraryService` already discovers and plays. A mid-beta migration risks browse/playback regressions without guaranteeing Maps-free fullscreen. **Decision: stay on MediaLibraryService for beta.2**; revisit templates only if DHU/car gaps remain after control + browse fixes.
