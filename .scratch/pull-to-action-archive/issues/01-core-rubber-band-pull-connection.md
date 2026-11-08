# 01: Core Rubber-Band NestedScroll Motion Modifier

**What to build:**
A reusable, robust Compose `NestedScrollConnection` that intercepts pull-down gestures when the contacts list is at its top boundary. Implements tactile rubber-band resistance physics (diminishing return formula preventing hard stops), tracks normalized pull progress (0.0 to 1.0) toward an activation threshold, and drives snap-back spring animations on release.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] Create `PullToActionNestedScrollConnection` intercepting downward pull gestures when list scroll position is at the top (`firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0`).
- [x] Apply diminishing returns rubber-band resistance formula so overscroll feels tactile and controlled rather than 1:1 linear.
- [x] Maintain an `Animatable` pull offset with snappy spring snap-back when the user releases finger.
- [x] Fire threshold trigger callback when user releases past activation threshold (~72dp).

