# Trance V4.2 — Reset Analysis

**Date:** 2026-10-09  
**Branch:** `feature/apollo-trance-v4-2-finalization`  
**Baseline:** `689a0c6` / tag `apollo-pre-trance-v4-2`

## Symptom

Visualizer geometry appeared to snap / restart every few seconds during continuous playback.

## Root cause (confirmed in code)

In [`VisualizerCanvas.kt`](../app/src/main/java/com/apolloauto/player/ui/VisualizerCanvas.kt), `warpClock` was driven by:

```kotlin
animateFloat(0f → 1000f, tween(3200), RepeatMode.Restart)
```

`RepeatMode.Restart` forced the phase back to `0` every **~3.2 seconds**. Nearly all TR/cosmic scenes used `fastPhase = warpClock` as absolute phase, producing a hard visual snap that looked like a scene reset.

This was **not** scene object recreation, Auto DJ, or Media3 queue rebuild.

## Contributing factors (secondary)

1. **Immersive `setEnabled(false)` on pause** hard-disabled the Android `Visualizer`, which could make analysis appear frozen / decorative after resume until reattach.
2. **Auto DJ `LaunchedEffect` keyed on `playback.playing` / `autoDjInterval`** restarted the interval timer on those changes (could cause unexpected early scene changes when Drive Mode / randomizer was on — distinct from the 3.2s snap).
3. Album-art full-bleed behind every immersive scene made rainbow modes harder to read (UX, not a reset).

## Fix

1. Replace restarting `warpClock` with a **monotonic** `withFrameNanos` accumulator (`monoMs`), never snapped to zero.
2. Keep `Visualizer` attached across pause; soft-decay features instead of tearing down capture.
3. Auto DJ interval effect keyed only on randomizer enable; track-boundary effect primed to ignore first mediaId.
4. `VisualizerSceneRuntime` tracks intentional mode changes vs unexpected resets; same-mode reselect does not recreate.

## Verification

| Check | Result |
|---|---|
| Unit: scene reselect does not increment unexpected reset | PASS (`VisualizerSceneRuntimeTest`) |
| Code review: no Restart phase for scene time | PASS |
| Physical 10-minute continuous scene session | **Not run this pass** — mark Verified after device QA |

## Instrumentation

Enable Settings → VISUALIZER → **Capture diagnostics** for immersive overlay (`creates`, `resetsUnexpected`, `frameAgeMs`, `autodj` counts).
