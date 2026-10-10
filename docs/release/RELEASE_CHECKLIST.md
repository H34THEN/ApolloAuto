# Apollo II — Public Beta Release Checklist

## Android Auto UX gate (`1.0.0-beta.2`)

- [x] MediaLibrarySession discovery preserved
- [x] Next/Previous via multi-item queue expansion
- [x] Play/Pause via DEFAULT_PLAYER_COMMANDS
- [x] Queue = host timeline; Back dismisses
- [x] Shallow browse + More secondary categories
- [x] Maps / fullscreen limitation documented
- [x] No projected visualizer bypass attempted
- [x] Native Immersive / Viz-Only unchanged by AA browse (Phone Visualizers under More)
- [x] Unit tests: browse tree + queue expand
- [ ] Owner Sobalai retest (photos / Next visible)
- [ ] Signed APK + SHA-256 published

## Prior beta.1 gates (carry forward)

- [x] ST01 / ST03 repair notes
- [x] Security / license audit docs
- [x] Privacy + README
- [x] Signing via local-release.properties (not in git)
- [ ] Re-run secret scan on APK before GitHub Release
- [ ] GitHub Release assets for beta.2

## Phone regression smoke

- [ ] Library scan (ringtone filter + art enrich)
- [ ] Playback / queue / playlists / Car Favorites
- [ ] EQ / sleep timer / notification
- [ ] Immersive + Viz-Only + background modes
- [ ] Settings persistence
