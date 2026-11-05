# ADR-0018: Shell-Level Groups Drawer and Minimal Sphere Filtering

## Status
Accepted

## Context
The side panel accessed from the home screen top bar previously carried the title "Life Spheres & Cadences" and was embedded inside `HomeScreen` via a `ModalNavigationDrawer`. This created several friction points:
1. **Z-Ordering & Shell Conflicts**: Because the drawer was defined inside `HomeScreen`, the shell-level `FloatingDockOverlay` (the Floating Bottom Dock and companion Quick-Add FAB in `MainAppShell`) was rendered above or outside the drawer's scrim in the app hierarchy. The drawer needed to sit on top of everything, above the bottom dock.
2. **Cognitive Overload**: The drawer cards exposed cadence frequencies (7d, 14d, 30d chips) and notification toggles alongside circle filtering, accompanied by redundant text ("Tap to activate", "Cadence Frequency", "Cadence Alerts"). Cadence configurations already exist in dedicated settings and per-contact settings.
3. **Domain Terminology**: "Life Spheres & Cadences" and "Cadences" presented internal technical nomenclature rather than natural user vocabulary. Renaming to "Groups" provides an intuitive mental model while maintaining alignment with the underlying `Circle` domain model.

## Decision
1. **Promote Drawer to App Shell (`MainAppShell`)**:
   - Host `ModalNavigationDrawer` at the root of `MainAppShell` so its drawer sheet and modal scrim cover the entire viewport, including the `FloatingDockOverlay` and the navigation tabs.
   - Wire `isDrawerOpen` and circle selection actions between `homeViewModel` and `MainAppShell`.
2. **Streamlined Group Selection UI (`GroupDrawerContent`)**:
   - Rename panel title to **"Groups"** with a clean subtitle "Filter by relationship group".
   - Eliminate redundant cadence interval chips, alert notification switches, and "Tap to activate" labels.
   - Present groups as a clean, tactile list of options:
     - "All Groups" (or "All") with globe icon `🌐`.
     - Circle items (Work `💼`, Friends `☕`, Family `🏡`, University `🎓`) displaying display name and active indicator checkmark when selected.
   - Tapping an item selects the circle and dismisses the drawer smoothly.
3. **Terminology Consistency**:
   - Update `GLOSSARY.md` to document **Group** as the presentation nomenclature for `Circle` context spheres.

## Consequences
- The drawer now slides effortlessly over the entire screen, above the floating bottom dock and Quick-Add FAB.
- Drawer interaction becomes an instant, zero-friction filtering gesture rather than a secondary settings panel.
- Cadence tuning remains cleanly isolated within Settings and Contact Profile flows.
