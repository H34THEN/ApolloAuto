# ST01 / ST03 Repair Report

**Date:** 2026-10-10  
**Branch:** `release/apollo-ii-v1-beta`  
**Implementation:** Compose Canvas interpretations (not GLSL/AGSL ports)

## Sources inspected

| ID | Shadertoy | Title | Author |
|---|---|---|---|
| ST01 | [Xs3GWj](https://www.shadertoy.com/view/Xs3GWj) | GrafEq | davidar |
| ST02 | [flGyWc](https://www.shadertoy.com/view/flGyWc) | lunchbreak | nabr |
| ST03 | [Wt33Wf](https://www.shadertoy.com/view/Wt33Wf) | Cyber Fuji 2020 | kaiware007 |

## Defects found

### ST01 — mostly blank / not reactive
**Root cause:** Sparse boolean cell sampling of implicit equations rarely lit pixels; pattern index used `phase * 0.008` against `warpClock` (~ms×0.12), cycling equation families too quickly; several equation ranges barely intersected the sampled plane.

**Repair:** Dense soft-field CRT grid (64×40) with height-normalized UVs, slow pattern dwell, audio-driven zoom/pan/hue/beat rings, spiral fallback if undersampled.

### ST03 — “doesn’t work” (missing Fuji / broken layout)
**Root cause:** Vertical layout used `immersiveViewportDim` (max edge). On landscape phones `span ≈ width`, so `fujiTop = horizon - span*0.28` went **above the viewport** — mountain/sun effectively off-screen.

**Repair:** Height-normalized layout matching Shadertoy `uv = (2*frag-res)/res.y`. Sun, Fuji, snow, and perspective grid use `height` fractions. Audio maps to “battery”, grid scroll, stripe thickness, horizon glow.

### ST02
Not reported broken; left as-is (minkowski shells). Regression-checked at compile time.

## Verification status

| Check | ST01 | ST03 |
|---|---|---|
| Compiles | Yes | Yes |
| Installed on device (debug `1.0.0-beta.1-cyber`) | Yes | Yes |
| Screenshot captured in `docs/screenshots/` | Pending device capture | Pending |
| 10-minute soak | Not completed this session | Not completed |
| Device-verified as READY | **Partial** — install + code fix; visual QA by owner | **Partial** |

## Files touched
- `app/.../ui/ShadertoyMilkLabScenes.kt`
