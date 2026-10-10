# Apollo V5.2 — Scene Reactivity Audit

**Date:** 2026-10-09  
**Branch:** `fix/apollo-v5-2-visualizer-reactivity`  
**Baseline:** `bb5a667` (V5.1 checkpoint)

## Strong reference (user-confirmed)

| Display | ID | Renderer | Why strong |
|---|---|---|---|
| LIQUID | LIQUID_SPECTRUM | `drawLiquidSpectrumScene` | FFT + bandBoost → large path height (geometry) |
| SPIRAL | AUDIBLE_SPIRALS | `drawAudibleSpiralsScene` | Multi-band radius warp + FFT ±18px |
| POLY | XR3_POLY | `drawXr3PolyScene` | Spectrum teeth on spiral radius; stroke onset |

## Weak → repaired (V5.2)

| Display | ID | Pre-V5.2 weakness | Fix |
|---|---|---|---|
| STAR | STAR_FIELD | Speed/alpha only | Radial expand, kick shockwave, mid orbit, treble twinkle |
| STAR2 | STARS2 | Single-bin + heavy EMA | 3 depth layers, radial breath, distinct from STAR |
| SIER | XR3_SIERPINSKI | Tiny scale / alpha | Bass breath, mid subdivision wobble, treble edge glow |
| RAIN | PRISM_RAIN | Constant fall + rare splash | Event density, kick drops, onset ripples |
| SMOKE | SYNTH_SMOKE | Low contrast / small deltas | Density contrast, kick pressure, chromatic pulse |

## Classification (evidence-based)

| Scene | Pre | Post (code) | Device music |
|---|---|---|---|
| LIQUID / SPIRAL / POLY | Strong | Strong (preserved) | Pending playback |
| STAR / STAR2 / SIER / RAIN / SMOKE | Weak | Strong mappings | Pending playback |
| Other registry (~45 modes) | Mixed | Audited; not all rewritten | See matrix |

Full mode→draw map: `VisualizerCanvas.kt` dispatch. Shared mapper: `VisualResponseEngine.kt`.
