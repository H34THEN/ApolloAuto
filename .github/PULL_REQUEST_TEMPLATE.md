## Summary
<!-- 1–3 bullets -->

## Test plan
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] Phone smoke (library / playback / Immersive)
- [ ] **Android Auto DHU** (if media/AA touched):
  - [ ] `./tools/aa-dhu/check_prereqs.sh`
  - [ ] `./tools/aa-dhu/capture_review.sh` or interactive `run_dhu.sh`
  - [ ] Fill `docs/release/aa-dhu-gallery/REVIEW.md` (Approve / Change)
- [ ] Sobalai / car retest when AA transport or browse changed

## Docs
- [ ] Updated `docs/release/*` if release-facing
