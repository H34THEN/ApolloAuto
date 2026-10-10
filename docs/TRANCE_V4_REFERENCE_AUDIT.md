# Trance Reactor V4 — Reference Audit

**Date:** 2026-10-09  
**Rule:** Public demos are not redistributable source. Apollo builds original Compose Canvas scenes.

---

## A — Audible Visuals (Sonia Boller)

| Field | Detail |
|---|---|
| Demo | https://soniaboller.github.io/audible-visuals/ |
| Source | https://github.com/soniaboller/audible-visuals |
| License | **Apache-2.0** (GitHub API / repo metadata) |
| Visual behavior | Archimedean spiral variations; color schemes; mic or file input (Web Audio + THREE.js) |
| Audio analysis | Web Audio API FFT / waveform |
| Rendering | THREE.js / WebGL in browser |
| Suitable for Apollo | Spiral geometry ideas, multi-variant switching, rainbow color layering |
| Recreate independently | All Compose spiral math + Apollo session FFT mapping |
| Unverifiable | Exact runtime parameter names from demo UI without running it |

License note: `docs/licenses/SONIABOLLER_AUDIBLE_VISUALS_APACHE2.txt`

---

## B — Fluctus (Jordan Machado)

| Field | Detail |
|---|---|
| Demo | https://jojo.ninja/fluctus/ |
| Source | No clearly redistributable engine confirmed in this audit pass |
| License | **Unverified** — treat as inspiration-only |
| Visual behavior | Frequency-driven 3D wave sculpture / fluid membranes (from public description) |
| Audio analysis | Unknown without source |
| Rendering | WebGL experiment (browser) |
| Suitable for Apollo | Spatial ribbons, perspective deformation, onset propagation |
| Recreate independently | Full scene (TR10 FLUCTUS_FLOW) as original Compose projection |
| Unverifiable | License, exact mesh algorithm, audio pipeline |

License note: `docs/licenses/FLUCTUS_INSPIRATION_NOTE.txt`

---

## C — Wave (Matt DesLauriers / codevember)

| Field | Detail |
|---|---|
| Demo | https://mattdesl.github.io/codevember/3.html |
| Source | https://github.com/mattdesl/codevember |
| License | **MIT** (GitHub API) |
| Visual behavior | Flowing wave geometry, procedural curves, WebGL motion |
| Audio analysis | Demo-specific (codevember experiments vary) |
| Rendering | WebGL / JS |
| Suitable for Apollo | Layered wave ribbons, depth, chromatic trails |
| Recreate independently | TR11 WAVEFORM_VOID Compose landscape waves |
| Unverifiable | Whether day-3 HTML depends on non-MIT assets |

License note: `docs/licenses/MATTDESL_CODEVEMBER_MIT.txt`

---

## D — Obsidian (xplsv / mrdoob temp host)

| Field | Detail |
|---|---|
| Demo | https://mrdoob.com/files/temp/xplsv_obsidian/ |
| Source | Demo-hosted; no separate Apollo-vendored copy |
| License | **Unverified** — inspiration-only; do not copy artwork/music/code |
| Visual behavior | Dark procedural geometry, deformation, cinematic lighting |
| Audio analysis | Unknown |
| Rendering | WebGL |
| Suitable for Apollo | Faceted crystal look, edge glow, kick shock |
| Recreate independently | TR12 OBSIDIAN_REACTOR |
| Unverifiable | License, source availability, asset rights |

License note: `docs/licenses/XPLSV_OBSIDIAN_INSPIRATION_NOTE.txt`

---

## E — Vissonance (Tariq Soliman)

| Field | Detail |
|---|---|
| Demo | https://tariqksoliman.github.io/Vissonance/ |
| Source | https://github.com/tariqksoliman/Vissonance |
| License | **MIT** (LICENSE file verified) |
| Visual behavior | Multi-scene WebGL spectral geometry / particles / orbits |
| Audio analysis | Web Audio style band mapping |
| Rendering | WebGL |
| Suitable for Apollo | Multi-submode matrix (architecture / particles / orbits / field) |
| Recreate independently | TR13 VISSONANCE_MATRIX Compose submodes; do not embed the web app |
| Unverifiable | Asset licenses beyond code LICENSE (if any media bundled) |

License note: `docs/licenses/TARIQKSOLIMAN_VISSONANCE_MIT.txt`

---

## Architectural idea sources (not scene ports)

| Project | Use in Apollo |
|---|---|
| willianjusten/awesome-audio-visualization | Catalog of techniques |
| wizgrav/clubber | Spectral mapping ideas |
| hvianna/audioMotion-analyzer | Band labeling ideas |
| meyda/meyda | Feature vocabulary (flux, centroid, etc.) |

Apollo does **not** vendor these libraries. Chromagram / tempo / beat confidence remain **Planned**, not claimed complete.

---

## Rendering policy for V4

All TR01–TR13 scenes are original **Jetpack Compose Canvas** implementations using Apollo’s shared `AudioVisualizerController` (Media3 session Visualizer). No WebGL embedding. OpenGL ES / AGSL engine: Planned (not this pass).
