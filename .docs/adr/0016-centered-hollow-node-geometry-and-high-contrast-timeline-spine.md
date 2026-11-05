# ADR-0016: Centered Hollow Node Geometry and High-Contrast Living Timeline Spine

- **Status:** Accepted
- **Date:** 2026-11-05
- **Context:** Refining the visual presentation of chronological encounters on the Living Timeline (`Timeline Tab`) and `Contact Profile Screen` (`LivingTimeline.kt`).

---

## 1. Problem & Motivation

Previously, past encounter nodes on the Living Timeline featured a solid primary/accent circular dot placed at the top-left of each encounter row, alongside a faint 50% opacity outline variant gray vertical line. This produced two visual shortcomings:
1. **Misalignment with Asymmetric Cards:** Because encounter cards expand dynamically with notes, badges, and location tags, pinning the node dot to the top header made the dot feel detached from the card's visual mass.
2. **Visual Hierarchy & Contrast:** The faint gray spine lacked the crisp, ink-like clarity of Hello's warm editorial typography and tactile aesthetic, while the solid dot competed with the colored contact avatars.

---

## 2. Decisions

### Decision 1: High-Contrast Dynamic Spine Line
- The vertical connecting line (spine) for past encounters is rendered with a 2dp continuous stroke using high-contrast surface coloring:
  - **Light Mode:** Deep Ink/Black (`MaterialTheme.colorScheme.onSurface`, `#252321` or `#000000`).
  - **Dark Mode:** High-contrast crisp white/light gray (`MaterialTheme.colorScheme.onSurface`).
- Faint low-contrast gray spines are deprecated for past encounters.

### Decision 2: Hollow Center Node Dot
- The timeline node dot is updated from a solid filled disc to a hollow node dot:
  - **Diameter:** 12.dp.
  - **Border:** 2.dp crisp stroke matching the spine color (`MaterialTheme.colorScheme.onSurface`).
  - **Fill:** High-contrast surface background (`MaterialTheme.colorScheme.surface` or pure white in light mode, dark surface in dark mode).

### Decision 3: Card-Midpoint Vertical Alignment
- The timeline node dot is vertically centered relative to the **entire Encounter Card height** (`Alignment.CenterVertically` within the intrinsic row height), rather than anchored to the top-left header.
- The vertical connecting spine passes continuously behind the hollow node dot from top to bottom of each row, ensuring a seamless vertical line across consecutive encounters.

### Decision 4: Cross-Surface Harmonization
- These geometry and color refinements apply identically across:
  1. The app-wide Living Timeline (`TimelineScreen.kt` - `PastTimelineNodeRow`).
  2. The Contact Profile Screen's living timeline (`LivingTimeline.kt` - `TimelineNodeItem`).

---

## 3. Consequences

### Positive
- **Visual Balance:** Centering the node dot to the card midpoint creates an anchored, balanced cadence regardless of card height.
- **Ink & Paper Aesthetic:** Crisp hollow nodes with bold vertical spines evoke a physical notebook timeline.
- **Theme Resilience:** Using semantic `onSurface` and `surface` tokens guarantees readability in both light and dark modes.
