# Apollo V5 — ECHOHUNT Scaffold

**Status:** Interfaces + local credential store only. Not a working identifier yet.

## Packages

- `echohunt/EchoHuntModels.kt`
- `echohunt/EchoHuntContracts.kt` (Chromaprint / AcoustID / MusicBrainz seams)
- `echohunt/EchoHuntCredentialStore.kt` (AcoustID **application API key** in private prefs)

## Explicit non-goals (this pass)

- No native Chromaprint `.so` yet
- No desktop `fpcalc` bundled in APK
- No MusicBrainz login password storage
- No microphone capture UI yet (must be opt-in later)

## Operator note

Rotate any MusicBrainz password that was shared outside a password manager. Hosted AcoustID expects an application API key from https://acoustid.org/webservice — configure via future Settings UI into `EchoHuntCredentialStore`.
