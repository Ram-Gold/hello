# 02: Centered Hollow Node Geometry and High-Contrast Timeline Spine

**What to build:** Transform the past encounter timeline nodes into centered hollow circular waypoints connected by a continuous high-contrast spine. The hollow node dot (12dp size, 2dp border, surface background fill) is centered vertically relative to the entire height of the adjacent encounter card. The vertical connecting spine uses 2dp high-contrast black/onSurface stroke running continuously from top to bottom through the row. This behavior is applied harmoniously across both `TimelineScreen` (`PastTimelineNodeRow`) and the Contact Profile's `LivingTimeline` (`TimelineNodeItem`).

**Blocked by:** 01-minimal-pixel-style-timeline-header

**Status:** done

- [x] Past timeline spine color is updated from faint 50% opacity gray to high-contrast `MaterialTheme.colorScheme.onSurface` (crisp black in light theme, off-white in dark theme).
- [x] Timeline node dot is rendered as a 12dp circular hollow shape with a 2dp border (`onSurface`) and filled with `MaterialTheme.colorScheme.surface` (pure white in light theme).
- [x] The hollow node dot is vertically centered to the card's midpoint (`Alignment.CenterVertically` within intrinsic card row height) instead of anchored at the top-left.
- [x] Continuous vertical spine runs through the column from top to bottom (unless it is the final node where the bottom segment terminates).
- [x] Changes are implemented across both `TimelineScreen.kt` and `LivingTimeline.kt`.
- [x] Visual verification on Android device confirms vertical centering and high-contrast styling.
- [x] All unit tests pass (`./gradlew testDebugUnitTest`).
