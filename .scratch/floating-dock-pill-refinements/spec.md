Status: ready-for-agent

# Specification: Floating Bottom Navigation Dock Refinements

## Problem Statement

Users navigating the app currently encounter a bottom navigation dock whose sliding indicator pill spans a rigid 50% half-width of the container, appearing overly bulky and visually heavy rather than sleek and refined. Furthermore, the dock container lacks visible physical depth because previous elevation modifier ordering clipped the drop shadow, making the dock appear flat against underlying content. 

Additionally, when switching tabs, the tab label text exits to the left in reverse of natural reading order, creating a jarring, backward visual sensation. Finally, navigation icons remain outlined in both selected and unselected states, diminishing instant visual glanceability and failing to provide standard Material Design 3 state clarity where the active tab features a filled, grounded icon.

## Solution

Refine the floating bottom navigation dock (`HomeFloatingBottomDock`) with polished visual and tactile ergonomics:

1. **Tight-Hugging Pill Sizing**: Transition from a full 50% half-width block to a sleek, compact pill that hugs the active tab's icon and label with comfortable internal padding, creating an airy and modern aesthetic.
2. **Prominent Unclipped Drop Shadow**: Fix container modifier ordering and establish an unclipped 8.dp drop shadow beneath the floating dock, lifting the dock elegantly off the scrolling content surface.
3. **Consistent Left-to-Right Text Flow**: Reorient tab label exit motion so that text enters from the left (-x to 0) and exits towards the right (0 to +x) alongside smooth opacity crossfades, matching natural reading flow and forward momentum.
4. **State-Driven Icon Fill States**: Instantly swap navigation icons between standard Outlined vectors for unselected tabs and solid Filled vectors for the active tab (e.g., `Icons.Filled.Home` vs `Icons.Outlined.Home`, and `Icons.Filled.People` vs `Icons.Outlined.People`), providing immediate visual reinforcement of current location.

---

## User Stories

1. As an active app user, I want the active tab pill to hug its content rather than occupying half the screen width, so that the navigation dock feels sleek, lightweight, and modern.
2. As a user browsing content, I want the floating dock to cast a soft, prominent drop shadow onto the surface below, so that the navigation bar clearly floats above feed items and cards.
3. As a user switching from the Home tab to People, I want the departing "Home" label to glide smoothly to the right as it fades out, so that the motion feels consistent with forward reading progression.
4. As a user switching from People to Home, I want the newly selected label to slide in smoothly from the left as it fades in, so that text enters naturally without sudden visual jumping.
5. As a user looking at the navigation dock at a glance, I want the active tab's icon to display in a solid filled style, so that I can immediately identify my current screen without reading labels.
6. As a user looking at inactive navigation tabs, I want their icons to appear as clean outlines, so that there is strong visual hierarchy between active and inactive destinations.
7. As a user tapping a tab, I want the icon to switch instantly to its filled state, so that touch feedback is immediate and responsive.
8. As a user who has customized my Relationship Terminology (e.g., "Friends", "Connections", "Contacts"), I want the tighter pill width to adapt gracefully to variable label lengths without clipping or awkward gaps.
9. As a user with smaller hands, I want the touch target of each navigation tab to remain at or above standard accessibility dimensions (at least 48dp width and height), even though the visual pill indicator itself is smaller and tighter.
10. As a user rapidly toggling between tabs, I want the text slide and fade animations to be fully interruptible with zero glitching or overlapping label text.
11. As a user on a warm cream/beige background in light mode, I want the dock drop shadow to cast a warm, soft ambient tone rather than a harsh, dark black drop shadow.
12. As a user navigating in dark mode, I want the drop shadow and container borders to maintain subtle elevation distinction without causing high-contrast glare.
13. As a user interacting with the companion Quick-Add Floating Action Button (`+`), I want the dock container's updated shadow and refined pill to remain visually harmonious with the FAB's elevation and corner radius.
14. As a user on an older or lower-spec Android device, I want the pill resizing and left-to-right text slide animation to execute entirely in the GPU draw and layout phases without triggering unnecessary recompositions.
15. As a user using talkback or accessibility services, I want accessibility semantics and tab selection announcements to reflect the active tab correctly, independent of the visual pill size or icon vector swap.
16. As a user transitioning between screens, I want the dock's refined pill motion to synchronize with the page scroll duration, ensuring complete visual unity across the entire interface.

---

## Implementation Decisions

### Component Architecture
- Update the floating bottom navigation dock container and tab items within the home presentation module.
- Retain the dual-element floating row structure: the primary multi-tab navigation dock on the left, paired with the independent companion Quick-Add FAB on the right.

### Pill Sizing & Layout Geometry
- Replace the rigid 50% (`halfWidth = size.width / 2f`) pill outline calculation with a compact, content-hugging pill indicator.
- Compute the active pill bounds based on the measured tab content (icon width + spacing + expanded label width + horizontal content padding).
- Ensure the interactive touch bounding box spans the full available tab sector (providing a comfortable touch target), while the visual indicator pill cleanly wraps only the active tab content.
- Maintain smooth spring physics for the pill's horizontal translation across tabs (`PILL_SPRING_STIFFNESS` and `PILL_SPRING_DAMPING`).

### Elevation & Drop Shadow Hierarchy
- Reorder the Modifier chain on the floating dock container so that the drop shadow is applied before any surface clipping or border drawing.
- Configure elevation to 8.dp with a soft, warm ambient and spot shadow color to ensure clear separation above scrolling cards and empty states.
- Ensure the companion Quick-Add FAB retains matching elevation and elevation physics when pressed.

### Text Slide and Fade Direction
- Update the enter and exit transition specs in the tab content composable:
  - **Enter transition**: `fadeIn` combined with `slideInHorizontally(initialOffsetX = { -it / 2 })` and horizontal expansion from start.
  - **Exit transition**: `fadeOut` combined with `slideOutHorizontally(targetOffsetX = { it / 2 })` and horizontal shrinkage towards end.
- Both enter and exit motions now proceed in a unified left-to-right trajectory, eliminating the previous right-to-left exit retreat.

### Icon Selection State Contract
- Map tab destinations to paired vector assets:
  - `HomeNavTab.HOME`: `Icons.Filled.Home` when selected; `Icons.Outlined.Home` when unselected.
  - `HomeNavTab.PEOPLE`: `Icons.Filled.People` when selected; `Icons.Outlined.People` when unselected.
- Apply an immediate vector swap conditioned on the tab's active selection state, ensuring instantaneous recognition on pointer down.

---

## Testing Decisions

### What Makes a Good Test
Tests must verify observable behaviors, motion tokens, and layout contracts rather than internal private Compose layout pass details:
- Verify that motion tokens for text slide offset and duration adhere to the left-to-right transition parameters and match dock travel times.
- Verify deterministic mapping of tab selection to corresponding Outlined vs. Filled icon vectors.
- Verify touch-target safety constraints (ensuring touch bounds remain >= 48dp despite compact pill presentation).
- Verify spring and tween specifications for pill translation stability under rapid interruptions.

### Modules Under Test
- `DockPerformanceTest`: Extend to validate updated pill sizing tokens, left-to-right text transition specs, and dock elevation constants.
- `SmoothTabsTest`: Extend to validate icon state resolution (filled vs outlined) across all `HomeNavTab` permutations and dynamic relationship terminology labels.

### Prior Art
- Follows existing tests in `app/src/test/java/com/example/hello/ui/home/DockPerformanceTest.kt` and `app/src/test/java/com/example/hello/ui/components/SmoothTabsTest.kt`.

---

## Out of Scope

- Introducing additional tabs to the bottom navigation dock (Hello maintains a strict two-tab primary model: Home and People).
- Adding custom badge counters or notification dots on the bottom dock tab icons.
- Redesigning the Speed Dial popup menu or the Quick-Add FAB behavior.
- Altering the root pager navigation gestures or screen slide durations.

---

## Further Notes

- **Design System Alignment**: Follows the prime directives in `AGENTS.md` and ADR `0001-floating-pill-and-quick-add-dock.md`, preserving the warm, tactile aesthetic with terracotta accents, rounded pill geometry, and snappy spring physics.
- **Terminology Dynamics**: The People tab label dynamically reflects the user's selected `RelationshipTerminology` (e.g., "People", "Contacts", "Friends", "Connections"), so dynamic pill width calculation must accommodate varying text widths seamlessly.
