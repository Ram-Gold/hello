# 01: Timeline Header Sticky Overlay Fade

**What to build:** Overlay a 16.dp ambient vertical gradient scrim (`Brush.verticalGradient(listOf(backgroundColor, Color.Transparent))`) directly on top of the scrolling list content at `y = 0` right beneath `TimelineHeader`. By nesting `LazyColumn` and the gradient scrim inside a `Box(modifier = Modifier.weight(1f).fillMaxWidth())` with `Alignment.TopCenter`, encounter cards and spine lines smoothly tuck underneath the sticky header during scroll with zero gap, matching the visual tuck behavior on the People screen.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] `TimelineScreen.kt` wraps the `LazyColumn` and the 16.dp gradient scrim in a `Box(modifier = Modifier.weight(1f).fillMaxWidth())`.
- [x] The vertical gradient scrim (`background` -> `Transparent`) sits pinned at `Alignment.TopCenter` directly over the top of the scrollable content.
- [x] Timeline encounter cards, future nodes, and spine segments slide smoothly beneath the gradient fade as the user scrolls.
- [x] Zero static gap exists between `TimelineHeader` and the first timeline item when at scroll position 0.
- [x] All unit tests pass cleanly (`./gradlew testDebugUnitTest`).
