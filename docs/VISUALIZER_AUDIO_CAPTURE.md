# Visualizer Audio Capture Audit (V2.3)

## Actual source (verified in code)

Apollo uses **`android.media.audiofx.Visualizer` bound to ExoPlayer’s `audioSessionId`**.

| Claim | Reality |
|--------|---------|
| Playback-session FFT/waveform | **Yes** — `AudioVisualizerController.attach(sessionId)` |
| Microphone `AudioRecord` capture | **No** — not implemented |
| `RECORD_AUDIO` permission | Platform **gate** for session Visualizer on many devices; does **not** mean mic recording |
| Decorative idle | Used when capture inactive / attach fails |

## CaptureSource labels
`CaptureSource.PLAYBACK_SESSION` when Visualizer attach succeeds; `CaptureSource.DECORATIVE` otherwise. UI shows `captureSource.shortLabel()`.

## Shared features
`AudioFeatures`: bass / mid / high / rms / onset estimate / silence — derived from session FFT + waveform with attack/release smoothing. Not a precise BPM detector.

## Policy
- No mic audio written to disk or transmitted
- Capture failure never stops Media3 playback
- Mic-reactive mode is **not** offered; if added later it must be explicit opt-in with a visible indicator
