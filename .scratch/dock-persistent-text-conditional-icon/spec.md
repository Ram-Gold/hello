# Feature Specification: Persistent Navigation Labels with Conditional Icon Reveal

Status: ready-for-agent

## Problem Statement

The user is experiencing aesthetic and layout friction with the floating navigation dock. In the current design, navigation tabs display a bare icon when unselected, while the selected tab expands horizontally to reveal its text label. Because text labels vary dramatically in length across destinations and user-selected relationship terminologies (e.g., "Home" is 4 characters, while "Connections" is 11 characters), the expansion delta and visual weight change dynamically and unpredictably across tabs. 

Furthermore, unselected tabs lack textual clarity, forcing the user to rely solely on icons to understand the unselected destination. Conversely, navigation icons are strictly uniform and 1:1 in aspect ratio (24dp × 24dp) regardless of terminology. By expanding variable text while leaving the icon fixed, the dock feels visually unbalanced, and text length disparities directly impact the pill shape and transition optics.

## Solution

Invert the transition mechanics between text and icons across the floating dock tabs:
1. **Always-Visible Text Labels**: Both the Home tab and the Relationship tab ("People", "Friends", "Contacts", or "Connections") permanently display their text labels at all times, in both selected and unselected states. This provides immediate, unmistakable destination recognition.
2. **Conditional 1:1 Icon Reveal**: The 1:1 navigation icon is hidden on the unselected tab and animates into view only on the currently active/selected tab. 
   - When the user is on Home: the Home icon appears beside "Home", while the Relationship tab displays only its text label without an icon.
   - When the user is on the Relationship screen: the Relationship icon appears beside its label, while Home displays only "Home" without an icon.
3. **Deterministic Geometric Expansion**: Because every icon is uniformly 24dp in width with an 8dp spacer, the expansion footprint on tab selection is an exact, invariant 32dp across every screen and every terminology. The selection pill seamlessly glides and expands to hug `[Icon + Gap + Label]` on the active tab while the inactive tab rests cleanly as `[Label]`.

## User Stories

1. As a mobile app user, I want both navigation destinations on the dock to always display their text labels, so that I can immediately identify where each tab leads without deciphering abstract icons.
2. As a mobile app user, I want the active tab to display a clear 1:1 icon alongside its label, so that the currently selected destination has strong visual hierarchy and tactile focus.
3. As a mobile app user, I want the inactive tab to hide its icon and present only its clean text label, so that the unselected destination looks understated and does not compete with the active selection.
4. As a mobile app user, I want the icon to slide and expand smoothly into the active tab when switching destinations, so that the motion feels organic and responsive.
5. As a mobile app user, I want the outgoing tab's icon to collapse and fade out smoothly when leaving a destination, so that the transition feels continuous without sudden visual popping.
6. As a user who configured the relationship terminology to "People", I want the People tab to display "People" at all times, gaining the People icon only when selected.
7. As a user who configured the relationship terminology to "Friends", I want the Friends tab to display "Friends" at all times, gaining the Friends icon only when selected.
8. As a user who configured the relationship terminology to "Contacts", I want the Contacts tab to display "Contacts" at all times, gaining the Contacts icon only when selected.
9. As a user who configured the relationship terminology to "Connections", I want the Connections tab to display "Connections" at all times, gaining the Connections icon only when selected.
10. As a user switching tabs rapidly, I want the icon enter/exit animation to be velocity-preserving and interruptible, so that fast tapping never causes animation glitches or layout clipping.
11. As a mobile app user, I want both tabs to preserve comfortable inline padding inside the selection pill, so that text and icons never collide with the rounded pill boundaries.
12. As a mobile app user, I want the dock container width to remain static during navigation, so that the dock and the adjacent Quick-Add action button never jitter or shift.
13. As a mobile app user, I want the touch targets for both tabs to remain large and easily tappable (>48dp), so that navigating between tabs is effortless and error-free.
14. As a visually impaired user navigating with a screen reader, I want both tabs to announce their full destination names and selected state regardless of whether the visual icon is shown or collapsed.

## Implementation Decisions

- **Inverted Visibility Contract in Tab Items**: The tab item composable contract will invert the visibility bindings: text rendering will be unconditional (`Text` always visible), while the leading icon slot will be wrapped in the directional expansion and fade transition (`AnimatedVisibility` driven by `isSelected`).
- **Standardized 1:1 Icon Footprint**: The leading icon container will retain a strict 24dp × 24dp size with a 22dp vector icon and an 8dp spacer. Because this footprint is constant (32dp total), active pill expansion is identical regardless of which tab is active.
- **Dock and Pill Sizing Model**:
  - Unselected tab resting width is determined solely by the text label width plus standard resting padding.
  - Selected tab resting width equals the text label width plus the invariant 32dp icon footprint plus inline selection padding (~15dp on each side).
  - Pre-configured terminology dimensions in the motion configuration will account for the persistent text on both slots and the single expanding icon on the active slot.
- **Slot Centering & Static Dock Geometry**: The dock container width will remain static for any chosen terminology, satisfying the boundary invariants:
  - Exact 4dp outer margins at both extremes.
  - Symmetrical resting centers ensuring unselected text and active `[Icon + Text]` remain centered in their respective slots without drifting.
- **Draw-Phase Reveal Integrity**: The dual-layer clip-path reveal architecture will be preserved: both the base inactive layer and the active pill layer will share the exact same persistent text layout, ensuring subpixel-perfect edge clipping during pill translation.
- **Accessibility Semantics**: Tab semantics will continue to expose destination names and selection state directly on the root interactive boxes, ensuring that collapsing the visual icon does not alter screen reader accessibility.

## Testing Decisions

- **Test Seam**: High-level JVM Component & Motion Contract Seam (`DockPerformanceTest` and `SmoothTabsTest`).
- **Behavioral Focus**: Test purely observable geometry, sizing invariants, visibility contracts, and deterministic bounds calculations. Avoid testing private layout passes or internal Compose placeables directly.
- **Test Invariants**:
  1. *Icon Visibility Invariant*: Ensure the active tab resolves a visible icon footprint (32dp), while the inactive tab resolves zero icon footprint.
  2. *Label Invariance*: Ensure that text label widths are accounted for continuously in both tabs.
  3. *Zero-Jitter Invariant*: Verify that the dock container width remains identical whether Home or the relationship tab is active.
  4. *Margin & Padding Invariants*: Verify that resting selection pills maintain exact 4dp outer margins and ~15dp inline padding across People, Friends, Contacts, and Connections terminologies.
- **Prior Art**: Builds on the existing test patterns in `DockPerformanceTest.kt` and `SmoothTabsTest.kt`.

## Out of Scope

- Changing navigation destinations or adding a third tab to the dock.
- Modifying the Quick-Add floating action button or speed dial popover mechanics.
- Altering the Material 3 color tokens (`surface`, `secondaryContainer`, `onSurfaceVariant`, `onSecondaryContainer`).
- Modifying settings persistence or relationship terminology definitions in the domain layer.

## Further Notes

- Swapping the mechanic such that the icon animates while text remains static solves the core visual asymmetry problem of variable-length words: an icon is an invariant 24dp square on all platforms, whereas words vary by up to 300% in length. This makes animations significantly easier to balance and visually cleaner.
