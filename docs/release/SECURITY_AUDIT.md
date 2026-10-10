# Security Audit — v1.0.0-beta.1

## Scope
- Application source under `app/`
- Docs
- Gradle config
- Release APK certificate inspection
- `.gitignore` for keystores / local signing props

## Tools
- Ripgrep pattern scan for tokens/passwords/private keys
- Manual review of ECHOHUNT credential store
- `apksigner verify --print-certs` on release APK

## Findings
| Item | Result |
|---|---|
| Hardcoded MusicBrainz passwords | **Not found** |
| Hardcoded AcoustID keys | **Not found** (prefs-only store) |
| GitHub tokens in tree | **Not found** |
| Keystores in git | **Excluded** (`*.jks`, `local-release.properties`) |
| Release signing | Local keystore at `~/.apollo-signing/` (outside repo) |
| Media3 SessionToken matches | Benign (MediaSession API) |

## Remediation / owner actions
- Rotate any MusicBrainz password previously shared in chat if not already rotated.
- Do not commit `local-release.properties` or `~/.apollo-signing/*.jks`.
- Treat AcoustID application API keys as local Settings data only.

## Remaining risks
- Sideloaded APKs are reverse-engineerable; never embed privileged secrets.
- Local beta keystore password is for development machines only — replace for production distribution.
- Gitleaks CI not yet wired; recommend adding when Actions secrets are available.

## Verdict
**No privileged secrets detected in source or packaged APK for this beta.** Safe to publish APK as a GitHub Release asset from a **private** repo; do not flip public without a second scan.
