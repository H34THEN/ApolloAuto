# Apollo V5.2 — POLY Widescreen

## Root cause
`drawXr3PolyScene` used `min(width, height)` so landscape spirals were square-inscribed and left horizontal margins unused.

## Fix
- `SpiralFillMode.FILL` (default) → `max(w,h)` via `spiralViewportDim`
- `SpiralFillMode.FIT` → previous `min` behavior
- `spiralScale` default **1.35x** (range 0.5–2.5)
- Optional center X/Y offsets
- **Audio mapping unchanged** (spectrum × 0.35 + rms × 0.08 + onset stroke)

## Settings
Visualizer Settings → POLY SPIRAL when POLY is active.
