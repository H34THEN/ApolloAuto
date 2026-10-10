# Apollo V5.2 — Audio Response Engine

**File:** `media/VisualResponseEngine.kt`

## Design

Does **not** create a second capture pipeline. Maps existing `AudioFeatures` from `AudioVisualizerController` into band drives used by weak-scene repairs.

### Components

| Helper | Role |
|---|---|
| `VisualResponseMapper.expand` | Gamma lift for soft signals |
| `VisualResponseMapper.bandDrive` | Per-band drives × `VisualIntensity` |
| `kickPressure` | Sub+bass+onset for large geometry |
| `sparkle` | Treble+presence+flux for fine detail |
| `ResponseEnvelope` | Attack/release for stateful chase |
| `safePulse` | Photosensitivity luminance cap |
| `spiralViewportDim` | FIT=min / FILL=max for POLY |

### Principle (from LIQUID/SPIRAL/POLY)

Successful scenes change **path geometry** by tens of percent of the viewport. Weak scenes previously changed only alpha or scroll speed. V5.2 repairs follow the geometry-first pattern.

### Intensity presets

Calm 0.65 · Balanced 1.0 · Reactive 1.35 (default) · Hyper 1.75  
Stored in ThemeStore `visual_intensity`.
