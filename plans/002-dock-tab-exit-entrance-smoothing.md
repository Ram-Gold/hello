# 002 — Dock Tab Entrance & Exit Smoothing and Clip Alignment

- **Status**: DONE
- **Commit**: bb90a39
- **Severity**: HIGH
- **Category**: Physicality & Origin, Performance & Layout Thrashing, Geometry Cohesion
- **Estimated scope**: 3 files (`DockMotionConfig.kt`, `HomeFloatingBottomDock.kt`, `SmoothTabsTest.kt`)

## Problem

When switching between tabs in the bottom navigation dock (e.g. Home $\to$ People / Friends / Connections / Contacts), the relationship tab label "gets out" and is visibly clipped by the moving pill boundary. Furthermore, the entering and exiting icons suffer from layout thrashing and jarring horizontal displacement:

1. **Discontinuous Slot Weight Jump**:
   In `app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt:268-277`, slot weights snap abruptly on frame 1 using a boolean condition:
   ```kotlin
   // app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt:268-277 — current
   val homeWeight = if (selectedTab == HomeNavTab.HOME) {
       dimensions.homePillWidthDp.toFloat()
   } else {
       dimensions.homeInactiveSlotWidthDp.toFloat()
   }
   val relWeight = if (selectedTab == HomeNavTab.HOME) {
       dimensions.relationshipInactiveSlotWidthDp.toFloat()
   } else {
       dimensions.relationshipPillWidthDp.toFloat()
   }
   ```
   When the user taps the relationship tab, `selectedTab` toggles immediately. The slot centers instantly jump horizontally by ~15.4dp to the left on Frame 1, while `pillProgressState` is still at `0.0f` on the far left.

2. **Main-Thread Layout Width Thrashing**:
   In `app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt:460-472`, the icon wrapper animates its measured layout width on every single frame:
   ```kotlin
   // app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt:460-472 — current
   Row(
       verticalAlignment = Alignment.CenterVertically,
       modifier = Modifier.layout { measurable, constraints ->
           val placeable = measurable.measure(constraints)
           val fraction = iconFractionProvider().coerceIn(0f, 1f)
           val currentWidth = (placeable.width * fraction).roundToInt()
           layout(currentWidth, placeable.height) {
               if (fraction > 0.001f) {
                   val offsetX = ((1f - fraction) * placeable.width * offsetFraction).roundToInt()
                   placeable.placeRelativeWithLayer(offsetX, 0) {
                       alpha = if (isSelected) 1f else fraction
                   }
               }
           }
       }
   )
   ```
   Because `currentWidth` expands from `0` to `32dp`, it forces Compose to re-measure and re-layout the Row 60–120 times over 380ms. This continuously shifts the adjacent `Text` sibling to the right. On entering, this shift pushes the text beyond the right edge of `CenteredPillClipShape` (`graphicsLayer { clip = true }`), slicing off the end of words like "People" and "Connections".

3. **Icon Overlapping Text**:
   Because `offsetX` uses `placeable.width * offsetFraction` while `currentWidth` is collapsed to near-zero, the entering icon is drawn with a positive horizontal offset directly over the first letters of the text.

## Target

A 100% GPU-accelerated, zero-layout-thrashing motion architecture adhering to Emil Kowalski's rules:
- **Stationary Slot Centers**: Slot 0 is permanently centered at `homeCenter` (`slotPadding + homePillWidthDp / 2`), and Slot 1 is permanently centered at `relCenter` (`containerWidth - slotPadding - relPillWidthDp / 2`). Zero weight jumping.
- **GPU-Only Motion**: Icon enters and exits via `graphicsLayer` (transform translation, `scale(0.85f .. 1.0f)`, and `alpha`), with zero per-frame `Modifier.layout` re-measurement.
- **Coordinated Text Micro-Translation**: Text glides smoothly by $\pm 12\text{dp}$ on the GPU via `graphicsLayer { translationX = ... }` as the icon appears/disappears, staying centered and never exceeding the pill's $\pm 11\text{dp}$ breathing room.
- **Directional Parallax**:
  - Home $\to$ People: People icon enters from right to left (`+12dp` $\to$ `0`), Home icon exits from left to right (`0` $\to$ `+12dp`).
  - People $\to$ Home: Home icon enters from left to right (`-12dp` $\to$ `0`), People icon exits from right to left (`0` $\to$ `-12dp`).
- **Zero Pill Cut-out**: The pill bounds in `CenteredPillClipShape` cleanly hug the content at all stages of motion across all relationship terminologies ("People", "Friends", "Contacts", "Connections").

## Repo conventions to follow

- Easing: `MotionTokens.M3EmphasizedEasing` / `CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)`.
- Pill transition tween: `MotionTokens.tabTransitionTween()` (380ms).
- Zero-allocation static configs: calculate slot coordinates inside `DockMotionConfig`.
- Exemplar: `CenteredPillClipShape` in `app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt:555-580`.

## Steps

1. In `app/src/main/java/com/example/hello/ui/home/components/DockMotionConfig.kt`:
   - Add helper functions to calculate stable slot center offsets and GPU text micro-translation ranges.
   - Refactor `resolveIconOffsetFraction` into a deterministic pixel translation helper returning bounded offsets ($\pm 12\text{dp}$).

2. In `app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt`:
   - Replace dynamic weights in `TabsRow` with fixed slot layouts positioned at `homeCenter` and `relCenter`.
   - Remove `Modifier.layout` dynamic width resizing in `NavTabItemContent`.
   - Apply `graphicsLayer { ... }` on the icon Box:
     - `alpha`: smooth fade (`fraction.coerceIn(0f, 1f)`).
     - `scaleX` & `scaleY`: `0.85f + 0.15f * fraction`.
     - `translationX`: `(1f - fraction) * directionalSlidePx`.
   - Apply `graphicsLayer { ... }` on the label Text:
     - `translationX`: smooth micro-shift compensating for icon presence so the active grouping remains perfectly centered within the pill.

3. In `app/src/test/java/com/example/hello/ui/components/SmoothTabsTest.kt` and `DockPerformanceTest.kt`:
   - Update unit tests to verify stable slot center geometry, zero weight jumping, and bounded text offsets across all terminology variants.

## Boundaries

- Do NOT touch `MainAppShell.kt` or `HomeScreen.kt` drawer gating.
- Do NOT alter navigation backstack or route structures.
- Do NOT add new dependencies or third-party animation libraries.
- Maintain Material 3 dynamic color and warm beige palette tokens.

## Verification

- **Mechanical**:
  - Run `./gradlew testDebugUnitTest` to verify all dock math and component tests pass.
  - Run `./gradlew compileDebugKotlin` to verify zero compiler warnings.
- **Feel check**:
  - Install on physical device (`HYVOXWFQSOOFVW6L` / `25057RN09G`).
  - Switch repeatedly between Home $\leftrightarrow$ People at 1x and 0.5x animation scale.
  - Confirm:
    - The word "People" / "Friends" / "Connections" NEVER pokes out or gets cut off by the pill's edge.
    - The active pill glides smoothly without jumping or stuttering.
    - The icon smoothly scales and slides into place while the outgoing icon dissolves and slides away in the opposite direction.
    - Changing terminology in Settings to "Connections" preserves identical smooth margins with zero cut-off.
- **Done when**:
  - Switching tabs displays fluid, continuous motion with zero text clipping and zero dropped frames on entry-level hardware (`mt6765`).
