# Apollo II — HUD Visual QA

**Branch:** `feature/kenney-cyberpunk-hud`  
**Baseline tag:** `apollo-pre-kenney-hud` (`7632dc8`)

## Screenshot locations

Capture manually after install into:
`docs/kenney_previews/screenshots/` (optional; gitignored PNGs OK)

Suggested captures:
- Main (Apollo Prime / Hades / Eidolon / Custom)
- Now Playing — Tactical / Immersive / Terminal
- Settings Display / Audio / Playback
- Car Mode
- Playlist membership sheet

## Checklist

| Screen | Themes | Insets | Contrast | Touch targets | Notes |
|---|---|---|---|---|---|
| Main dashboard | pending device review | status/nav padding added | pending | pending | Kenney chrome partial (status + NP entry) |
| Now Playing | pending | yes | pending | Kenney icon buttons 52dp+ | 3 layouts |
| Settings | pending | yes | pending | pending | Interface style toggle |
| EQ / Timer | pending | via Settings | pending | native sliders | Function preserved |
| Car Mode | not yet Kenney-skinned | landscape | pending | large controls | Phase K5 remaining |
| Android Auto | n/a phone HUD | host UI | n/a | n/a | Untested DHU |

## Accessibility

- Kenney Future used for HUD labels only; metadata uses sans-serif.
- Classic interface style available as fallback.
- Motion profiles: Full / Reduced / Static / System.

## Known defects

- Full library/playlist row Kenney migration incomplete (K5).
- Visualizer requires `RECORD_AUDIO`; without grant shows NO CAPTURE (not fake-as-live).
- EQ listening verification still outstanding.
- Sleep timer full-duration verification still outstanding.
