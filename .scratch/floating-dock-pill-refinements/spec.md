Status: ready-for-agent

# Specification: Floating Bottom Navigation Dock Visual Polish

## Problem Statement

Users navigating the Hello app observe that the bottom navigation dock's floating dropdown shadow was too faint and subtle, blending into warm backgrounds and failing to convey distinct tactile elevation above scrolling content. Additionally, stretching the entire dock container across the full screen width made the bottom navigation bar excessively wide, whereas the internal sliding selection indicator pill was already deemed perfect. Finally, the container displayed a rigid 1.dp outline border around the dock, which clashed with the soft, borderless aesthetic and distracted from the clean drop shadow elevation.

## Solution

Provide visual polish and tactile refinement to the floating bottom navigation dock:

1. **Single Unified Hardware Elevation Shadow**: Removed all artificial multi-tier `drawBehind` layers and eliminated artificial downward vertical offsets. Applied a single, clean hardware `Modifier.shadow` (12.dp elevation) using soft warm ambient (`0x592B1F14`, ~35% alpha) and spot (`0x73140D07`, ~45% alpha) tones, casting a snug, single diffuse shadow directly beneath the dock and companion FAB with zero stepped banding.
2. **Consistent Pill Padding Across All Tabs**: Symmetrically calibrated selection pill bounds so that the distance from the pill to the dock container edges is completely consistent on all sides:
   - When Home is selected: Left padding == Top padding == Bottom padding == `4.dp` (`vPaddingPx`).
   - When Friends is selected: Right padding == Top padding == Bottom padding == `4.dp` (`vPaddingPx`).
   - The Home and Friends selection pills maintain identical, proportional widths (`halfWidth - 2 * vPaddingPx`), gliding smoothly with zero stretching or lopsided edge gaps.
3. **Compact Entire Dock Width & Grouped Right Placement**: Constrained the entire outer dock container to a compact width of 220.dp (`DOCK_CONTAINER_WIDTH_DP = 220`) grouped on the right (`Arrangement.End`) alongside the Quick-Add FAB.
4. **Borderless Floating Dock Container**: Maintained a borderless container surface (`HAS_DOCK_BORDER = false`), allowing the single elevation shadow to delineate the organic pill silhouette naturally.

---

## User Stories

1. As a user viewing my relationship feed on the Home screen, I want the bottom navigation dock to cast a deep, noticeable dropdown shadow, so that it clearly floats as a distinct tactile layer above cards and encounters.
2. As a user navigating in bright light, I want the dropdown shadow to be rich and distinct enough to be instantly visible, so that the navigation dock never looks flat against light backgrounds.
3. As a user looking at the active navigation tab, I want the indicator pill to maintain its earlier perfect width (16dp content padding), snugly enveloping the active icon and label with balanced proportions.
4. As a user preferring an ergonomic single-hand reach, I want the entire dock container to be compact (220dp) and grouped to the right alongside the Quick-Add FAB, rather than stretched across the full width of the screen.
5. As a user who appreciates clean minimalism, I want the outer floating dock to have no border stroke, so that the boundary is defined entirely by depth and elevation rather than a distracting outline.
6. As a user switching between Home and People, I want the selection pill to glide with the same smooth spring physics across the dock, maintaining snappy translation without jitter.
7. As a user with customized Relationship Terminology (such as "Friends" or "Connections"), I want the pill calculation to hug variable text lengths with consistent padding, so that longer or shorter labels remain balanced.
8. As a user tapping on the navigation bar, I want the interactive touch targets to remain expansive and easy to tap (>= 48dp), even though the overall dock container is compact.
9. As a user switching between light and dark themes, I want the more visible shadow to adjust its ambient and spot intensity gracefully, so that it creates tactile depth in light mode without glowing or clipping in dark mode.
10. As a user operating the app with one hand, I want the companion Quick-Add Floating Action Button to share the same borderless, elevated depth as the dock container, ensuring visual harmony across the bottom row.
11. As a user rapidly toggling tabs, I want the pill's draw-phase clip to execute with zero heap allocation, maintaining 60-120 FPS render performance.
12. As a user scrolling through long contact lists on the People screen, I want the dock's dropdown shadow to create a clear visual separation between the fixed navigation elements and scrolling list items.
13. As a user observing the text slide animation, I want the pill boundaries to contain the left-to-right enter/exit transitions cleanly without text peeking outside the pill edges.
14. As an accessibility user, I want the visual adjustments and removal of the border to preserve all accessibility roles (`Role.Tab`) and selection announcements unchanged.

---

## Implementation Decisions

### Elevation and Shadow Intensification
- Increased the floating dock shadow elevation token from 8.dp to 12.dp (`DOCK_SHADOW_ELEVATION_DP = 12`).
- Deepened the warm ambient and spot shadow color definitions to boost visibility against `#F9F6F0` (warm beige) surfaces without turning into harsh black.
- Aligned the companion Quick-Add Floating Action Button's default elevation to 12.dp with matching warm shadow styling.

### Compact Dock Container & Right-Aligned Layout
- Constrained the outer dock Surface width to a compact 220.dp (`DOCK_CONTAINER_WIDTH_DP = 220`).
- Changed the bottom navigation bar row arrangement from `Arrangement.SpaceBetween` to `Arrangement.End`, grouping the 220.dp dock and the 60.dp FAB together on the right with a 14.dp gap.

### Selection Pill Width
- Preserved the user's preferred internal selection pill horizontal content padding at 16.dp (`PILL_CONTENT_HORIZONTAL_PADDING_DP = 16`).
- Maintained tab item horizontal padding at 14.dp (`TAB_ITEM_HORIZONTAL_PADDING_DP = 14`).
- Retained the zero-allocation, draw-phase outline calculation, dynamically scaling the active pill to tightly envelope the measured icon and text bounds.

### Border Removal & Borderless Styling
- Removed the 1.dp `outlineVariant` border modifier from the floating dock container.
- Declared a formal `HAS_DOCK_BORDER = false` configuration token in the dock motion config.
- Removed the outer border stroke on the companion Quick-Add Floating Action Button to maintain visual consistency across the bottom bar.

---

## Testing Decisions

### What Makes a Good Test
Tests verify external behavior, layout token contracts, and mathematical bounds without asserting internal Compose layout passes:
- Verify that `DOCK_CONTAINER_WIDTH_DP` is 220.dp.
- Verify that `DOCK_SHADOW_ELEVATION_DP` is 12.dp.
- Verify that `PILL_CONTENT_HORIZONTAL_PADDING_DP` is 16.dp.
- Verify that `TAB_ITEM_HORIZONTAL_PADDING_DP` is 14.dp.
- Verify that `HAS_DOCK_BORDER` is `false`.
- Verify that `calculatePillBounds` produces pill coordinates that wrap content with the restored 16.dp horizontal padding (e.g., 60f + 2 * 16f = 92f).

### Modules Under Test
- `DockPerformanceTest`: Validates container width (220.dp), shadow elevation token (12.dp), selection pill padding tokens (16.dp and 14.dp), and `HAS_DOCK_BORDER == false`.
- `SmoothTabsTest`: Validates pill bounds calculation with the 16.dp horizontal padding across dynamic terminology variants.

### Prior Art
- Follows existing unit tests in `app/src/test/java/com/example/hello/ui/home/DockPerformanceTest.kt` and `app/src/test/java/com/example/hello/ui/components/SmoothTabsTest.kt`.

---

## Out of Scope

- Modifying the dock container's 60.dp overall height or system navigation bar insets.
- Changing icon vector assets or selection resolution logic.
- Altering the left-to-right text transition trajectory or duration.
- Changing the Speed Dial popup menu behavior or trigger mechanics.

---

## Further Notes

- **Tactile Harmony**: Removing the 1.dp border stroke and strengthening the drop shadow brings the floating dock directly in line with Emil Kowalski's tactile UI principles and Material Design 3 Expressive elevation styling.
