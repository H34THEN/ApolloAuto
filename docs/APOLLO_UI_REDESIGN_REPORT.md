# Apollo II — Phase D.2 UI Redesign Report

**Branch:** `feature/apollo-hud-ux-refinement`  
**Parent Kenney tip:** `57875824a3a5b7709fbe21018941905fc06ce8d4`  
**Checkpoint WIP:** `2868fe2122d5bee76f8077cbcb9fb4123b1c1e65`  
**Package:** `com.apolloauto.player.debug`  
**Device QA:** Metroid A024 (`00021157P002695`)  
**Date:** 2026-10-09

## Summary

Phase D.2 reconstructs phone home and Car Mode for readable typography, denser library visibility, compact chrome, and a responsive landscape car deck. Playback, playlists, Car Favorites, themes, EQ, and sleep timer are preserved.

## Components modified

| Area | Files |
|---|---|
| Spacing / type tokens | `theme/ApolloSpacing.kt` |
| Typography policy | `theme/ApolloFonts.kt`, `ui/hud/KenneyHudComponents.kt` (buttons/tabs → body face) |
| Artwork fallback | `ui/components/ArtworkThumb.kt` |
| Home chrome | `ui/home/HomeChrome.kt`, `ui/home/NowPlayingCard.kt` |
| Phone shell | `ui/MainActivity.kt` |
| Car Mode | `ui/CarModeActivity.kt` |
| Playback meta | `media/PlayerController.kt` (`album` field) |
| Rollback / status | `docs/UI_REDESIGN_ROLLBACK.md`, this report, `APOLLO_IMPLEMENTATION_STATUS.md` |

## Typography changes

- Decorative HUD face (E1D1C / Kenney) limited to logotype and selective headers.
- Interactive labels (buttons, tabs, nav, search, track rows, timers) use `LocalBodyFont` (system sans).
- Type scale tokens in `ApolloTypeScale` (logo 28sp, track 17sp, body/meta 12–14sp).

## Layout changes

- Screen padding 16dp; panel gap 12dp; row height ~68dp; touch targets ≥48dp primary.
- Compact header ~72dp: logo + theme line + source pill + activity dot + settings.
- Bottom navigation: Home / Library / Lists / Car / Settings.
- Oversized neon PHONE/USB/CAR blocks replaced with compact chips + SCAN/LINK.
- Now Playing card: art (96dp) + meta + thin timeline + primary transport + secondary toolbar.
- Library rows: art thumb, title/artist/album hierarchy, overflow menu (playlist / car favorite).
- Artwork placeholder: grid + AII mark + “NO ART” (never empty black square).

## Car Mode

- Dedicated landscape `BoxWithConstraints` layout.
- Zones: artwork (140–200dp square HUD frame) + playback + browse panel.
- Browse tabs: CAR ★ / ALL / QUEUE with compact empty states.
- Transport and SAVE TO CAR kept above system insets; no oversized circular orb art.

## Kenney asset mapping

| Role | Asset / component |
|---|---|
| Progress framing | `apollo_bar_round_*` via `ApolloHudProgress` |
| Artwork frame | `ApolloArtworkFrame` + crosshair corners |
| Tab / chip chrome | Themed borders (Kenney small buttons remain available) |
| Blade buttons | Reserved for Settings / Classic-adjacent actions; home uses readable chips |

Theme colors continue to flow from `ThemeStore` / `HudPalette` (six presets + custom).

## Screenshots

| Capture | Path |
|---|---|
| Home portrait | `docs/screenshots/d2/home_portrait.png` |
| Car Mode landscape | `docs/screenshots/d2/car_mode.png` |

### Visual QA notes (device)

- Header vertical footprint reduced; library list visible on first viewport.
- Bottom nav readable; Home selection highlighted.
- Now Playing primary transport centered; secondary actions on separate row.
- Car Mode empty favorites no longer fills panel with a single centered paragraph.
- SAVE TO CAR / EXIT remain on-screen in landscape.
- Remaining polish: decorative logotype still dense at small sizes; full theme screenshot matrix (all 6 + custom) pending extended session.

## Functional regression (smoke)

| Check | Result |
|---|---|
| Install + launch | Pass |
| Phone library list (276 tracks observed) | Pass |
| Bottom nav destinations | Pass (Settings opens activity) |
| Car Mode open / EXIT | Pass |
| Build `assembleDebug` | Pass |
| Playback / EQ / sleep / AA | Not re-exercised end-to-end this pass (code paths unchanged) |

## Known limitations

- Albums / Artists / Folders category views not yet separate (Tracks / Car / Lists preserved).
- Queue tab in Car Mode is a from-current-track library slice, not a full Media3 queue editor.
- Screenshot matrix incomplete vs handoff §15 (home + car captured; Settings/EQ/themes pending).
- Android Auto DHU not retested this phase.

## Rollback

See `docs/UI_REDESIGN_ROLLBACK.md`.
