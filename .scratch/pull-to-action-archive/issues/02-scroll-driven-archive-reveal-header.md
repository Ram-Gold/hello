# 02: Scroll-Driven Reveal Header with Animated Archive Icon & Text

**What to build:**
A zero-idle-footprint reveal header positioned above the contacts list whose height, icon rotation/scale, and text label are driven directly by pull offset progress. Transitions from "Pull for archive" to "Release for archive" when crossing the threshold, providing tactile haptic feedback. Removes the previous bottom footer bar from `ContactsScreen`.

**Blocked by:** 01 (Core Rubber-Band NestedScroll Motion Modifier)

**Status:** closed

- [x] Create `PullToActionArchiveHeader` rendered dynamically within the overscroll clearance height above the contacts list.
- [x] Connect `Icons.Rounded.Archive` rotation and scale to the scroll-driven progress (scaling from 0.7f to 1.1f with continuous angular rotation).
- [x] Transition label text dynamically between "Pull for archive" (below threshold) and "Release for archive" (at or above threshold).
- [x] Remove the previous `ArchivedPullUpFooter` from the bottom of `ContactsScreen` to ensure zero idle clutter on the Relationship Tab.

