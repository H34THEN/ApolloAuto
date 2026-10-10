# Trance V4.2 — Audio Capture Pipeline

## Chain

```
Media3 ExoPlayer audioSessionId
  → android.media.audiofx.Visualizer (playback session)
  → waveform + FFT callbacks
  → SpectralBandExtractor (subBass…treble, flux)
  → AudioFeatures StateFlow
  → TranceResponseProfile multipliers
  → ApolloVisualizerSurface / scene drawers
```

## Capture source

| Source | Status |
|---|---|
| Playback-session Visualizer | **Active path** (`CaptureSource.PLAYBACK_SESSION`) |
| Decorative idle | When no session / attach failed |
| Microphone | **Not implemented** (RECORD_AUDIO is required by Visualizer API on many devices, but we do not open a mic AudioRecord) |

## Freshness

- `lastFrameUptimeMs` updated on every FFT/wave callback
- `frameAgeMs()` / `isFrameStale(750ms)` exposed
- Immersive runs a 1s recovery loop calling `recoverIfStale(session)` (re-enable or reattach; does not recreate scene)

## Stability rules

- Mode changes never rebuild the Visualizer
- Attach rebuilds only when `audioSessionId` changes
- Pause soft-decays features; capture object remains

## Automated tests

- `AudioFeaturesBandsTest` — band separation / flux / profile
- Physical bass-only vs treble-only visual A/B: **pending device**
