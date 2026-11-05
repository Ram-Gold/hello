# 01: Ambient Depth Gradients under People and Timeline Headers

**What to build:** Add a 12.dp vertical gradient scrim beneath sticky section headers on the People screen (`ContactGroupHeader`) and beneath the fixed filter container on the Timeline screen (`TimelineHeader`). This creates an ambient depth tuck effect where scrolling contact and timeline cards seamlessly slide underneath without harsh visual cutoff lines.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] `ContactGroupHeader` on `ContactsScreen.kt` has a 12.dp vertical gradient scrim (`Brush.verticalGradient(listOf(backgroundColor, Color.Transparent))`) attached to its bottom edge.
- [x] `TimelineHeader` on `TimelineScreen.kt` has a 12.dp vertical gradient scrim placed directly beneath the filter strip container.
- [x] Scrolling lists on both screens tuck smoothly beneath headers in both light and dark themes.
- [x] Unit tests pass cleanly (`./gradlew testDebugUnitTest`).
