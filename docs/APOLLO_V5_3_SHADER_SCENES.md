# Apollo V5.3 — Shadertoy + MILK//LAB Scenes

## Shadertoy ST01–ST03

| Mode | Gallery | Source | Author | Visual identity | Adaptation |
|---|---|---|---|---|---|
| `ST01_GRAF_EQ` | ST01 | [Xs3GWj](https://www.shadertoy.com/view/Xs3GWj) | davidar | GrafEq — CRT implicit equation graphs cycling patterns | Compose cell sampling + scanlines; audio selects pattern / hue / beat ring |
| `ST02_LUNCHBREAK` | ST02 | [flGyWc](https://www.shadertoy.com/view/flGyWc) | nabr | lunchbreak — minkowski SDF reflective solids | Superellipse shells + specular bloom; bass expands, mids rotate |
| `ST03_CYBER_FUJI` | ST03 | [Wt33Wf](https://www.shadertoy.com/view/Wt33Wf) | kaiware007 | Cyber Fuji 2020 — sun, Fuji, synth grid | Perspective grid + sun stripes + Fuji fill; bass drives “battery” |

These are **Compose Canvas interpretations**, not GLSL ports. Shadertoy default licensing applies to originals; Apollo ships original geometry inspired by inspected behavior.

## MILK//LAB

XorDev vs MilkDrop2077 pack ([milkdrop3.com/XorDev](https://milkdrop3.com/XorDev/)) is a **commercial** collection (no redistrib). Full preset catalog could not be legally inspected/ported.

Apollo ships **10 original** scenes in that aesthetic vein:

| Mode | Short | Notes |
|---|---|---|
| `ML_BLACK_HOLE` | HOLE | Inspired by publicly cited name `XorDev - MilkDrop2077 - Black hole.milk` (forum), not a `.milk` port |
| `ML_EVENT_HORIZON` | HORIZON | Lensed warp rings |
| `ML_XOR_PLASMA` | XPLASMA | Plasma field |
| `ML_INFERNAL_MESH` | INFERNAL | Spiral mesh |
| `ML_ASTRAL_COLLAPSE` | ASTRAL | Particle collapse |
| `ML_NEON_RITUAL` | RITUAL | Neon polygons |
| `ML_LIQUID_WARP` | LIQUIDW | Waveform liquid layers |
| `ML_GLITCH_CORE` | GLITCH | Horizontal glitch bands |
| `ML_CHAOS_TUNNEL` | CHAOS | Jagged tunnel |
| `ML_FRACTAL_BITE` | BITE | Recursive triangles |

Implementation: `app/.../ui/ShadertoyMilkLabScenes.kt`  
Gallery categories: `shadertoy // ST`, `milk // lab`
