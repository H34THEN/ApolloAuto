# Known Issues — Apollo II public beta

## Projected Android Auto

- **Debug + release both installed:** AA may bind `com.apolloauto.player.debug` and hide Next. Uninstall the debug package before HU/DHU testing.
- **Next / single-item queue:** Fixed in **beta.3** (browse-context + expand). DHU USB: Next visible and advances after multi-track queue. Re-select a playlist track on Sobalai after upgrade.
- **Browse label truncation:** Short titles in beta.3 (`Favorites`, `Songs`, `New`, `Recent`). Host may still ellipsize on very narrow panes.
- **Maps dashboard split:** Host-owned; use AA full-app media from the rail.
- **No projected Immersive visualizers:** By platform policy — use Native Car Mode / Immersive on the phone.

## Native Car Mode

- Viz-first default in beta.3; queue drawer inside viz-only is still via FULL HUD → QUEUE (dedicated overlay TBD).
- Immersive visualizers are for parked / passenger use — not a driving distraction aid.

## Library

- OEM MediaStore may mis-tag system tones as music — filtered by path/duration.
- Album art for SAF libraries fills via background enrich after scan.

## Hardware

- Sobalai wireless AA is projection-only; do not assume APK install on the head unit.
