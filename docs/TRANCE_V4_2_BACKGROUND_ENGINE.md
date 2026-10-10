# Trance V4.2 — Background Engine

## Modes (user-facing)

| Mode | Behavior |
|---|---|
| AUTO | Scene preference: BLACK for rainbow/spectral; SCENE_DEFINED atmospheric worlds (no album art) |
| BLACK | Pure `#000000`; suppress opaque scene void fills |
| ALBUM_ART | Full-bleed cover + blur/darkness/saturation; geometry composites on top |

## Policy

Implemented in `VisualizerBackgroundPolicy` / `VisualizerBackgroundMode.kt`.

AUTO → BLACK includes: Signal six, Chromatic Phi, Ectoplasm, Circ Infinity, Prism Rain, Note Swarm, Mandala, Liquid Spectrum, Audible Spirals, Vissonance Matrix, plus SPECTR/MV2/DNA/DROP/XR3 instruments.

AUTO → SCENE_DEFINED includes: Hyperdrive, Nebula, Rave Core, Ocean, Plasma, Fracture, Synth Smoke, Fluctus, Waveform Void, Obsidian.

## Layering

1. `VisualizerBackgroundLayer` (black or processed album art)
2. Scene geometry (`clearBackdrop` when required)
3. HUD / gallery / diagnostics

## Persistence (ThemeStore SharedPreferences)

- `viz_background_mode`
- `album_bg_blur` (default 0.55)
- `album_bg_darkness` (default 0.62)
- `album_bg_saturation` (default 0.55)

Changing background mode does **not** recreate scene runtime or the analyzer.
