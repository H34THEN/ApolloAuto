# Trance V4.2 — Performance

## Measurements this pass

| Metric | Result |
|---|---|
| Device FPS (Eco/Balanced/High) | **Not profiled** |
| 10-minute single-scene soak | **Not run** |
| 30-minute Auto DJ soak | **Not run** |
| Audio underruns | **Not observed** (no device session logged) |

## Engineering notes

- Compose Canvas retained; no GPU rewrite in V4.2 (per priority: fix reset/audio first).
- Quality tiers still scale particle/mesh density.
- Monotonic clock uses `withFrameNanos` with dt clamped to 48ms to avoid spiral after jank.

Re-run physical profiling before marking High-quality GPU work.
