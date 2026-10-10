# Music Source Manager (V2.1)

## Model
- `MusicSource` / `MusicSourceKind` — phone MediaStore, phone SAF folder, USB SSD, SD card, other
- `MusicSourceStore` — persists multiple roots, merges catalogs into `LibraryStore`
- `SafLibraryScanner` — recursive SAF walk (audio + M3U discovery)
- `LibraryScanCoordinator` — scans all enabled sources without blocking playback UI thread (IO dispatcher)

## UI
- **Settings → Library → APOLLO // STORAGE MATRIX**
  - CHANGE PHONE FOLDER (SAF tree picker → labeled **PHONE FOLDER**, not USB)
  - + ADD SOURCE (USB/SD/other SAF)
  - RESCAN ALL / per-source ENABLE, RESCAN, DEFAULT, REMOVE
- Home row: ALL / PHONE / USB filters + PHONE FOLDER / LINK USB

## Migration
Legacy single `usb_tree_uri` + `LibrarySource` becomes:
1. Always-present `phone_mediastore` source
2. Optional `src_legacy_usb` if a tree URI existed

Removing a source clears Apollo’s index only — files are never deleted.

## Gate
A music folder anywhere accessible via SAF can be selected and scanned without pretending it is a USB SSD.
