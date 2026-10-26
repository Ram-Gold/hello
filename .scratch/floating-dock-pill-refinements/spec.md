Status: ready-for-agent

# Specification: Floating Bottom Navigation Dock Visual Polish

## Problem Statement

Users navigating the Hello app observe that the bottom navigation dock's floating dropdown shadow is too faint and subtle, blending into warm backgrounds and failing to convey distinct tactile elevation above scrolling content. Additionally, the sliding active pill indicator is still wider than desired, taking up excessive horizontal real estate and diminishing the compact, organic feel of the dock. Finally, the container displays a rigid 1.dp outline border around the pill dock, which clashes with the soft, borderless aesthetic and distracts from the clean drop shadow elevation.

## Solution

Provide visual polish and tactile refinement to the floating bottom navigation dock:

1. **Pronounced Dropdown Shadow**: Increase shadow elevation (from 8.dp to 12.dp) and enrich shadow ambient and spot opacity using warm, earthy undertones, ensuring the dock casts an unmistakable, soft dropdown shadow over content beneath.
2. **Narrower Pill Width**: Reduce pill horizontal content padding (from 16.dp to 8.dp) and tighten internal layout padding, ensuring the active pill wraps the icon and text tightly as a compact, elegant chip.
3. **Borderless Floating Dock Container**: Remove the 1.dp outline border stroke from the dock container surface, allowing the prominent drop shadow and warm surface elevation to delineate the organic pill silhouette naturally without artificial line strokes.

---

## User Stories

1. As a user viewing my relationship feed on the Home screen, I want the bottom navigation dock to cast a deep, noticeable dropdown shadow, so that it clearly floats as a distinct tactile layer above cards and encounters.
2. As a user navigating in bright light, I want the dropdown shadow to be rich and distinct enough to be instantly visible, so that the navigation dock never looks flat against light backgrounds.
3. As a user looking at the active navigation tab, I want the indicator pill to have a tighter, narrower width, so that it wraps the active icon and label cleanly without excessive empty space on the sides.
4. As a user with sensitive visual preferences, I want the active pill to feel like a compact, nimble chip rather than an oversized rounded rectangle, so that the bar feels lightweight and modern.
5. As a user who appreciates clean minimalism, I want the outer floating dock to have no border stroke, so that the boundary is defined entirely by depth and elevation rather than a distracting outline.
6. As a user switching between Home and People, I want the narrower pill to glide with the same smooth spring physics across the dock, maintaining snappy translation without jitter.
7. As a user with customized Relationship Terminology (such as "Friends" or "Connections"), I want the narrower pill calculation to hug variable text lengths with consistent, snug padding, so that longer or shorter labels remain balanced.
8. As a user tapping on the navigation bar, I want the interactive touch targets to remain expansive and easy to tap (>= 48dp), even though the visual pill indicator itself is narrower.
9. As a user switching between light and dark themes, I want the more visible shadow to adjust its ambient and spot intensity gracefully, so that it creates tactile depth in light mode without glowing or clipping in dark mode.
10. As a user operating the app with one hand, I want the companion Quick-Add Floating Action Button to share the same borderless, elevated depth as the dock container, ensuring visual harmony across the bottom row.
11. As a user rapidly toggling tabs, I want the narrower pill's draw-phase clip to execute with zero heap allocation, maintaining 60-120 FPS render performance.
12. As a user scrolling through long contact lists on the People screen, I want the dock's dropdown shadow to create a clear visual separation between the fixed navigation elements and scrolling list items.
13. As a user observing the text slide animation, I want the narrower pill boundaries to contain the left-to-right enter/exit transitions cleanly without text peeking outside the pill edges.
14. As an accessibility user, I want the visual narrowing of the pill and removal of the border to preserve all accessibility roles (`Role.Tab`) and selection announcements unchanged.

---

## Implementation Decisions

### Elevation and Shadow Intensification
- Increase the floating dock shadow elevation token from 8.dp to 12.dp.
- Deepen the warm ambient and spot shadow color definitions to boost visibility against `#F9F6F0` (warm beige) surfaces without turning into harsh black.
- Align the companion Quick-Add Floating Action Button's default elevation to 12.dp with matching warm shadow styling.

### Pill Width Compaction
- Reduce the pill content horizontal padding token from 16.dp to 8.dp.
- Adjust tab item horizontal content padding from 14.dp to 10.dp.
- Retain the zero-allocation, draw-phase outline calculation, dynamically scaling the active pill to tightly envelope the measured icon and text bounds.

### Border Removal & Borderless Styling
- Remove the 1.dp `outlineVariant` border modifier from the floating dock container.
- Declare a formal `HAS_DOCK_BORDER = false` configuration token in the dock motion config.
- Remove the outer border stroke on the companion Quick-Add Floating Action Button to maintain visual consistency across the bottom bar.

---

## Testing Decisions

### What Makes a Good Test
Tests verify external behavior, layout token contracts, and mathematical bounds without asserting internal Compose layout passes:
- Verify that `DOCK_SHADOW_ELEVATION_DP` is updated to 12.dp.
- Verify that `PILL_CONTENT_HORIZONTAL_PADDING_DP` is reduced to 8.dp.
- Verify that `HAS_DOCK_BORDER` is `false`.
- Verify that `calculatePillBounds` produces narrower, tightly bound coordinates that wrap content with the reduced horizontal padding.

### Modules Under Test
- `DockPerformanceTest`: Extend to validate updated shadow elevation token (12.dp), reduced horizontal padding token (8.dp), and `HAS_DOCK_BORDER == false`.
- `SmoothTabsTest`: Extend to validate pill bounds calculation with the reduced 8.dp horizontal padding across dynamic terminology variants.

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
