# Apollo II — Kenney HUD Asset Manifest

**Source archive:** `/Users/Gengar/Downloads/kenney_ui-pack-space-expansion.zip`  
**Pack identity:** Kenney UI Pack: Sci-fi (2.0)  
**License:** Creative Commons Zero (CC0) — see `docs/licenses/KENNEY_UI_PACK_SCI_FI_CC0.txt`  
**Extracted (gitignored):** `third_party/kenney/ui-pack-sci-fi/`  
**Inventory:** `docs/KENNEY_ASSET_INVENTORY.json` (1114 files)

## Selected runtime assets

Only a small curated set is copied into the APK (`app/src/main/res/drawable-nodpi/`).

| Resource | Source | Dims | Format | Role | Color treatment |
|---|---|---|---|---|---|
| `apollo_btn_blade` | `PNG/Blue/Default/button_square_header_blade_rectangle.png` | 192×64 | PNG | Wide HUD button | Theme tint (`SrcAtop`) |
| `apollo_btn_notch` | `PNG/Blue/Default/button_square_header_notch_square.png` | 64×64 | PNG | Icon button | Theme tint |
| `apollo_btn_small` | `PNG/Blue/Default/button_square_header_small_rectangle.png` | 192×64 | PNG | Compact / tab button | Theme tint |
| `apollo_btn_square` | `PNG/Blue/Default/button_square_header_large_square.png` | 64×64 | PNG | Square control | Theme tint |
| `apollo_bar_round` | `PNG/Blue/Default/bar_round_gloss_large.png` | 96×24 | PNG | Progress decoration | Theme tint |
| `apollo_bar_round_l/m/r` | segmented `_l/_m/_r` | 12/24/12 × 24 | PNG | Stretchable timeline | Theme tint |
| `apollo_bar_square` | `PNG/Blue/Default/bar_square_small.png` | 96×16 | PNG | Compact meter | Theme tint |
| `apollo_crosshair_a` | `PNG/Blue/Default/crosshair_color_a.png` | 30×30 | PNG | Artwork frame corner | Theme tint (sparing) |
| `apollo_crosshair_c` | `PNG/Blue/Default/crosshair_color_c.png` | 30×30 | PNG | Visualizer ornament | Theme tint |
| `kenney_future` | `Font/Kenney Future.ttf` | n/a | TTF | HUD headings / labels | Theme text color |
| `kenney_future_narrow` | `Font/Kenney Future Narrow.ttf` | n/a | TTF | Compact tech labels | Theme text color |

**Not bundled:** remaining ~1100 pack files stay in gitignored extract for tooling only.

## Typography policy

Kenney Future fonts are used for HUD chrome labels only. Track titles / artists use sans-serif fallback for Unicode coverage.
