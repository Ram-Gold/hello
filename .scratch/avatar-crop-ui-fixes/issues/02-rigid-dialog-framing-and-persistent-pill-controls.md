# 02: Rigid Dialog Framing & Persistent Pill Controls

**What to build:**
Re-architect the `AvatarCropDialog` UI so that the top action header and bottom controls reside in solid, elevated background bars layered strictly above the interactive canvas, with `clipToBounds()` preventing enlarged images from overlapping buttons or headers. Make the "Reset" button permanent (non-removing) with clear active/inactive visual states and give all controls cohesive pill backgrounds.

**Blocked by:** 01: Circular Aperture Containment & Inscribed Fit Geometry

**Status:** done

- [x] Top action header (`Cancel`, `Crop Photo`, `Confirm`) sits in a permanent surface container above the viewport canvas with a solid `#1A1817` background and safe padding.
- [x] Crop viewport container applies `Modifier.clipToBounds()` so transformed canvas drawing cannot render into header or footer spaces under any zoom level.
- [x] Bottom controls container sits in a permanent surface with solid background and generous touch targets.
- [x] "Reset" button is always present in the bottom row (never conditionally added/removed) with distinct enabled/disabled states (clickable and colored when `zoom > 1.01f || offsetX != 0f || offsetY != 0f`, muted and non-clickable when at baseline).
- [x] Both "Fit whole image" and "Reset" use styled pill container backgrounds (`CircleShape` or `RoundedCornerShape(20.dp)`) with distinct active and inactive color treatments.
- [x] Visual regression test or Compose preview verifies header, circular aperture, and bottom controls remain intact when zoom is at `MAX_ZOOM`.

