# Apollo V5.1 — SYNTH SMOKE Renderer

**Date:** 2026-10-09  
**Branch:** `fix/apollo-smoke-immersive-controls-v5`

## Root cause

Prior `drawSynthSmokeScene` in `TranceReactorScenes.kt` stroked sine/cosine `Path` layers (`Stroke` widths 6–14). Still frames looked like wavy lines, not vapor.

## Implementation

New file: `ui/SynthSmokeScene.kt`

Technique (Compose Canvas, no AGSL dependency):

- Hashed value-noise **fBm** (3–4 octaves)
- Domain warp for curl-like displacement
- Layered soft **radial density plumes** with alpha falloff
- Kick/flux soft shock wash (not hard rings)
- Quality-tier cell/layer caps for performance

### Audio mapping

| Band | Effect |
|---|---|
| Sub-bass | Large plume radius / vertical compression |
| Bass | Pressure displacement + layer scale |
| Low mids | Main plume drift |
| Upper mids | Warp / tendril twist |
| Treble | Fine wisp offsets |
| Onset / flux | Localized soft burst |

Silence keeps gentle drift (`playing=false` drive ≈ 0.45).

### Color presets

Neon Vapor, Toxic Dreams, Hades Ash, Rainbow Ether, Ghost Smoke, Blacklight Plume. Theme/album palette modes still routed through `TrancePalette`.

### Controls (ThemeStore)

Density, turbulence, flow speed, bass pressure, softness, glow, saturation, plume size, audio sensitivity, color preset.

## Fallback

Lower `VizQualityTier` reduces layers/cells — still density plumes, never stroked waveforms.

## Verification status

| Check | Status |
|---|---|
| Unit: presets/controls | Passed |
| Compile | Passed |
| Still-frame smoke look | **Passed** on device Car HUD (`car_synth_smoke_v2.png`) |
| 10-minute continuity soak | Pending |
| Bass/treble distinct response | Pending (needs library playback) |
