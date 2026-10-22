Status: ready-for-agent

# Specification: Removal of RecentActionsAccordion from Home Screen

## Problem Statement

The Home screen currently embeds an inline `RecentActionsAccordion` positioned between the Upcoming Feed and Recent Encounters sections. This in-content accordion presents inline "Add Contact" and "Update Contact" expansion forms directly within the vertical feed. This introduces visual clutter, creates redundant entry points alongside the floating speed dial action button (`+`), and forces unnecessary accordion state tracking within the Home UI architecture. Users looking for a warm, clean, and distraction-free relationship dashboard find the inline accordion redundant, visually busy, and unintuitive.

## Solution

Remove the `RecentActionsAccordion` component from the Home screen surface and clean up the associated UI state contracts and view model events:
- The Home screen feed will transition smoothly from the `UpcomingFeedSection` directly into the `RecentEncountersSection`.
- Contact creation and encounter logging remain firmly anchored to the persistent bottom floating action button speed dial and quick capture flows as defined in `ADR 0001`.
- `HomeUiState` and `HomeUiEvent` are pruned of accordion expansion state and event toggles, simplifying the state tree and eliminating unnecessary recompositions.

## User Stories

1. As a user viewing my Home screen, I want a clean, unencumbered relationship dashboard without inline form accordions, so that I can focus on my upcoming stay-in-touch reminders and recent encounters.
2. As a user scanning down the Home screen, I want the Upcoming feed carousel to flow directly into the Recent Encounters list, so that related relationship activity is visually continuous.
3. As a user wanting to add a new contact, I want to use the primary floating action button speed dial rather than an inline accordion, so that quick-add is consistently reachable from anywhere on the screen.
4. As a user logging an encounter with an existing contact, I want to trigger the encounter capture flow from the bottom dock or contact details rather than an inline accordion dropdown, so that the capture flow is focused and distraction-free.
5. As a user scrolling vertically through my Home feed, I want fewer layout recalculations and jumpy accordion expansions, so that scrolling remains buttery smooth at 120 FPS.
6. As a user operating the app with one hand, I want bottom thumb reachability prioritized through the floating dock rather than reaching into the center of the screen to expand form fields.
7. As a user switching between Circles via the drawer, I want the filtered feed to display alerts and recent encounters directly, so that context switching is instant and uncluttered.
8. As a user in Dark Mode, I want fewer nested card containers on the home surface, so that the warm Material 3 elevation and contrast tokens remain cohesive.
9. As a developer maintaining the codebase, I want `HomeUiState` to be free of dead `expandedAccordion` state, so that the unidirectional data flow contract remains lean.
10. As a developer modifying `HomeViewModel`, I want obsolete accordion toggle events removed from `HomeUiEvent`, so that event handling is strictly focused on active user interactions.
11. As a developer running unit tests, I want tests to assert only valid Home state transitions without maintaining deprecated accordion test fixtures, so that tests remain maintainable and fast.
12. As a user experiencing low battery or constrained device resources, I want fewer active state holders and recomposition scopes on the primary screen, so that the app starts faster and consumes less power.

## Implementation Decisions

- **Home Surface Simplification**: Remove `RecentActionsAccordion` invocation from `HomeScreen`. The vertical layout order becomes:
  1. Top Bar (Drawer toggle, Title, Settings)
  2. Greeting Section
  3. "My Card" Widget
  4. "Upcoming" Feed Section
  5. Recent Encounters Section
- **UI Contract Pruning**:
  - Remove `expandedAccordion: AccordionType?` from `HomeUiState`.
  - Remove `OnToggleAccordion(val type: AccordionType)` from `HomeUiEvent`.
  - Remove or deprecate `AccordionType` enum from the domain/UI models if no other consumers exist.
- **ViewModel State Clean-up**:
  - Prune `HomeViewModel.onEvent` branch handling `OnToggleAccordion`.
  - Update `OnSpeedDialSelect` logic in `HomeViewModel` to trigger navigation/dialog events directly without setting accordion expansion state.
- **Component File Lifecycle**:
  - Delete `RecentActionsAccordion.kt` from the components package to prevent lingering unused code.
- **Unit Test Modernization**:
  - Update `HomeViewModelTest` to remove accordion expansion tests and assert that state remains clean during navigation and speed dial interactions.

## Testing Decisions

- **What makes a good test**: Tests verify external behavior at the public view model seam (`HomeViewModel.uiState` and `HomeViewModel.onEvent`), asserting that user actions (switching tabs, selecting circles, tapping speed dial) produce correct state without regressions.
- **Modules under test**: `HomeViewModel` and `HomeUiContract`.
- **Prior art**: Follows existing assertions in `HomeViewModelTest.kt` verifying state initialization and event handling.

## Out of Scope

- Modifying the full-screen Quick Capture dialog or activity.
- Modifying the floating dock layout or FAB animation.
- Room persistence or database cache implementations.

## Further Notes

- Removing inline accordion forms directly reinforces Prime Directive 4 (Extreme Capture Speed and Lean Startup), as `HomeScreen` drops complex expandable layout animations and input focus managers.
