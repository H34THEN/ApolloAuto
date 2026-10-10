# Apollo V5.1 — Intermediate Test Report (Visualizer Repair)

**Date:** 2026-10-09  
**Branch:** `fix/apollo-smoke-immersive-controls-v5`  
**Baseline tag:** `apollo-pre-v5-1` → `3003d61`  
**APK:** `/Users/Gengar/Desktop/local-github/ApolloAuto/app/build/outputs/apk/debug/app-debug.apk`  
**Package:** `com.apolloauto.player.debug`

## Milestone: P0–P3 visualizer repair (intermediate)

This is **not** the full V5 music-player / ECHOHUNT release.

### Root cause (Smoke)

SYNTH SMOKE previously drew stroked sine paths. Replaced with fBm density plumes in `SynthSmokeScene.kt`.

### Files changed (primary)

- `ui/SynthSmokeScene.kt` (new)
- `ui/TranceReactorScenes.kt` (removed sine smoke)
- `ui/VisualizerCanvas.kt`
- `ui/VisualBackgroundSheet.kt` (new)
- `ui/ImmersiveVisualizerActivity.kt`
- `ui/CarModeActivity.kt`
- `ui/VisualizerGallerySheet.kt`
- `ui/VisualizerSettingsPanel.kt`
- `ui/VisualizerModeSelector.kt`
- `media/VisualizerBackgroundMode.kt` (+ SCENE)
- `theme/ThemeStore.kt`
- Tests + docs under `docs/APOLLO_V5_1_*`

### Tests executed

| Test | Result |
|---|---|
| `:app:compileDebugKotlin` | Passed |
| `VisualizerBackgroundPolicyTest` | Passed |
| `SynthSmokeNoiseTest` | Passed |
| `:app:assembleDebug` | Passed |
| `adb install -r` on `00021157P002695` | Passed |
| Landscape Car SYNTH_SMOKE still-frame | **Passed** — vapor field (not wavy lines) in `docs/qa/v5_1/car_synth_smoke_v2.png` |
| Full Immersive/Viz-Only BG sheet matrix | **Partial** — Immersive not adb-exported; manual remaining |
| 10-minute Smoke soak | **Not executed** |
| 30-minute mixed soak / FPS / thermal | **Not executed** |
| Android Auto regression | **Not executed** |
| Audio-reactive smoke with music | **Not executed** (library empty: 0 tracks) |

### Credentials

MusicBrainz / AcoustID passwords must **not** be stored in the repo. Rotate any password shared in chat. Future ECHOHUNT will use a Settings-held AcoustID application API key only.

### Known limitations

- Smoke is Compose Canvas fBm plumes (not Navier–Stokes / AGSL). Looks cloud-like; GPU shader path can be evaluated later if needed.
- Physical screenshot inspection and soak metrics still required for full P3 sign-off.
- Full V5 sources/M3U/ECHOHUNT not claimed complete in this intermediate build.

### Next

1. Device visual confirmation + screencaps  
2. Audit existing V5 library/source features (P4)  
3. ECHOHUNT scaffold without secrets (P5)
