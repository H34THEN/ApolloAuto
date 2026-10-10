# Native Car Mode — Visualizer-First Design

**Experience A** in the Apollo dual-car model (native APK UI — not projected Android Auto).

## Default destination (`1.0.0-beta.3`)

`CarModeActivity` opens in **`CarDeckMode.VISUALIZER`** (`VisualizerOnlyDeck`):

- Fullscreen visualizer + background layer (AUTO / BLACK / ALBUM / SCENE)
- Chrome auto-hides after ~3.5s of inactivity
- Tap toggles Previous / Play-Pause / Next / title / scene chips / Layers
- Side library list starts **collapsed** (FULL HUD toggle reveals classic car deck)

## Explicitly not projected AA

Immersive shaders stay on the phone / native Car Mode. Android Auto uses host media templates only.

## Interaction model

| Action | Result |
|---|---|
| Enter Car Mode | Viz-first; playback continues if already playing |
| Tap canvas | Show/hide HUD |
| FULL HUD | Classic list + transport layout |
| IMMERSIVE | Launch `ImmersiveVisualizerActivity` (full Immersive UX) |
| Layers | Background sheet without restarting music |

## Follow-ups (not blocking beta.3 queue fix)

- Dedicated queue bottom-sheet inside viz-only (today: FULL HUD → QUEUE tab)
- Compact library drawer without leaving viz
- Eco/Balanced/High render tiers surfaced in Car Mode
