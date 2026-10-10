# Trance V4.2 — Release Report

**Branch:** `feature/apollo-trance-v4-2-finalization`  
**Baseline tag:** `apollo-pre-trance-v4-2` → `689a0c6`  
**Date:** 2026-10-09

## Delivered

1. **Reset fix** — monotonic animation clock (root cause: 3200ms Restart warpClock)
2. **Capture stability** — soft pause, stale recovery, diagnostics overlay
3. **Background engine** — AUTO / BLACK / ALBUM_ART with persistence + UI
4. **Auto DJ reliability** — interval/track effects no longer keyed on playing flicker
5. Unit tests for background policy + scene runtime
6. Documentation set listed in handoff §19

## Acceptance (honest)

| # | Criterion | Status |
|---|---|---|
| 1–5 | Background modes + persist + no scene restart on BG change | Code complete; device QA pending |
| 6–8 | No unexpected reset / stable lifetime / track change | Root cause fixed in code; long soak pending |
| 9–14 | Capture freshness / band response | Pipeline hardened; band unit tests PASS; device A/B pending |
| 15 | Auto DJ unexpected resets | Interval keying fixed |
| 16–17 | Landscape + art clash defaults | AUTO→BLACK for rainbow; screenshots pending |
| 18–22 | Preserve player features | No Media3/queue changes; regression not fully re-run |
| 23 | Real-device performance | **Not measured** |
| 24 | APK install | Run in ship step |
| 25 | Rollback | Tag + docs updated |

## Non-goals respected

- No new visualizer modes
- No OpenGL/AGSL rewrite
- No AA fullscreen shaders
- No merge to master
