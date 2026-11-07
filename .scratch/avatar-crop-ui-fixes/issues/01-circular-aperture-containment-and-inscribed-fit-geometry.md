# 01: Circular Aperture Containment & Inscribed Fit Geometry

**What to build:**
Ensure that when a user crops a photo in `AvatarCropDialog`, the image is strictly contained within the circular aperture reticle with zero visual bleed onto outer areas. Fix "Fit whole image" geometry so that toggling it scales rectangular images to fit entirely within the circular mask without clipping corners, and updates the canvas and exported PNG consistently.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] `AvatarCropGeometry.calculateBaseScale` when `fitWholeImage = true` calculates the scale required to inscribe the rectangular bitmap completely within the circular aperture (diameter = `viewportSize`), ensuring no corners are clipped by the circle.
- [x] Toggling `fitWholeImage` re-centers zoom to 1f and offsets to (0, 0) while clamping within valid bounds.
- [x] In `AvatarCropDialog`, photo drawing is clipped strictly to the circular aperture path (`clipPath`), ensuring no image pixels render outside the circle reticle.
- [x] Export rendering in `renderCroppedAvatarPng` matches the inscribed circular geometry and `#FFFFFF` background filling.
- [x] Automated unit tests in `AvatarStorageAndCropTest` verify inscribed scale calculations across wide (16:9), tall (9:16), and square (1:1) aspect ratios.

