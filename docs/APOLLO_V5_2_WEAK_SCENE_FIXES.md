# Apollo V5.2 — Weak Scene Fixes

## STAR (`drawStarFieldScene`)
- Kick shockwave rings
- Bass expansion of projective field
- Midrange orbital nudge
- Treble twinkle on subset of stars
- Trails on strong kick

## STAR2 (`Stars2Scene.kt`)
- Three spectral depth layers (far/mid/near)
- Faster velocity chase (55/45 vs 72/28)
- Radial breathing + tangential drift
- Fixed seed for stable field
- Distinct from STAR tunnel aesthetic

## SIER (`drawXr3SierpinskiScene`)
- Bass breathing up to ~22% scale
- Midrange subdivision midpoint wobble
- Treble/edge accent strokes
- Faster rotation coupling to presence/kick
- Recursion depth 3–6 from rms+lowMid

## RAIN (`drawPrismRainScene`)
- Drop count driven by rms/kick/sparkle
- Kick → long thick drops
- Treble → fine precipitation speed
- Onset/kick impact ripples on ground line

## LIQUID / SPIRAL
- Untouched (regression-sensitive)
- Unit anchors in `VisualResponseEngineTest`
