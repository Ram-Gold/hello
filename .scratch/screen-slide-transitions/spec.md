Status: ready-for-agent

# Specification: Directional Slide Transitions Between Home and People

## Problem Statement

When users navigate between the Home screen and the People (Contacts) screen, the current screen change uses a basic crossfade. Users experience a visual disconnect between the physical, spatial position of the bottom dock tabs (where "Home" is positioned on the left and "People" is positioned on the right) and the screen content change. Without directional motion, the app feels less tactile, grounded, and cohesive.

## Solution

Implement smooth, organic directional slide transitions between the Home and People surfaces:
- Moving from **Home to People** (left to right in mental model): The entering People screen slides in from the right to the center, while the departing Home screen slides out from the center to the left.
- Moving from **People to Home** (right to left in mental model): The entering Home screen slides in from the left to the center, while the departing People screen slides out from the center to the right.
- The motion is paired with a gentle fade to prevent harsh visual edges and synchronize seamlessly with the spring-animated sliding indicator pill in the floating dock.

## User Stories

1. As a user on the Home screen, I want the screen to slide smoothly to the left when navigating to People, so that I perceive a natural spatial progression corresponding to the dock layout.
2. As a user on the People screen, I want the screen to slide smoothly to the right when navigating to Home, so that I perceive an intuitive return to my primary relationship dashboard.
3. As a user tapping the bottom navigation dock tabs, I want the screen slide animation to start immediately upon touch, so that there is zero perceptible lag or hesitation.
4. As a user tapping the contacts badge in the Recent Encounters section on Home, I want the screen to slide horizontally into the People screen, so that the shortcut maintains the same spatial continuity as tapping the dock.
5. As a user tapping the "← Home" header button on the People screen, I want the screen to slide to the right returning me Home, so that the header shortcut feels physically connected to the screen hierarchy.
6. As a user triggering a speed-dial action ("New Contact" or "Log Encounter") while on the People screen, I want the app to slide smoothly back to Home and expand the respective accordion, so that I am oriented in the capture workflow without sudden visual cuts.
7. As a user with sensitive motion preferences, I want the slide transition to have a gentle easing curve with an integrated opacity fade, so that rapid tab switching does not cause disorientation or eye strain.
8. As a user repeatedly switching between Home and People, I want scroll positions on both screens to remain intact throughout the slide transitions, so that I never lose my place.
9. As a user interacting with input fields (like the People search bar), I want the keyboard or focus state to be dismissed gracefully during the slide transition, so that no layout stutter occurs.
10. As a user holding the device with one hand, I want the floating dock and companion FAB to remain static and pinned while only the content surface slides, so that my thumb targets never shift or wobble during navigation.
11. As a user switching themes (Light vs Dark mode), I want the slide transition backgrounds to use the theme surface and background tokens, so that there are no bright flashes or dark artifacts during the transition.
12. As a user on a low-end Android device, I want the transition animation to maintain 60–120 FPS by utilizing hardware layer clipping and render offsets, so that navigation always feels fluid and ultra-responsive.

## Implementation Decisions

- **Motion Architecture**: Define a dedicated directional transition resolver module that inspects the initial and target navigation tab states, computing directional vectors (`Left` vs `Right`) and applying spring/tween specs.
- **Directional Contract**:
  - `Home -> People`: Target slides in from `Right`, initial slides out to `Left`.
  - `People -> Home`: Target slides in from `Left`, initial slides out to `Right`.
  - Same tab re-selection: No transition (no-op).
- **Animation Specs**: Utilize an organic Compose tween or spring with a duration aligned with the dock configuration (180ms–240ms) with an overlapping opacity fade (alpha 0.85 -> 1.0) for a warm, polished feel.
- **Persistent Floating Dock**: The floating bottom navigation dock and quick-add FAB remain outside the animated content container, pinned above system navigation bars, ensuring anchor stability while screens slide beneath.
- **State Preservation**: Both screens retain their respective view model flows and scroll states across slide transitions.

```kotlin
// Prototype Directional Resolver Shape
enum class NavigationSlideDirection {
    FORWARD_TO_RIGHT, // Home -> People
    BACKWARD_TO_LEFT, // People -> Home
    NONE
}
```

## Testing Decisions

- **What makes a good test**: Tests must verify the public behavioral contract (correct slide direction mapping, transition parameters, and tab delta calculations) without testing internal Compose framework layout passes.
- **Modules under test**: The directional transition spec resolver and navigation motion configurations.
- **Prior art**: Follows the existing unit test patterns established in `DockPerformanceTest` and `HomeViewModelTest`.

## Out of Scope

- Shared element transitions for individual contact avatars or cards (deferred to contact detail screen work).
- Vertical gesture swipe-to-dismiss transitions.
- Predictive back gesture customization on Android 14+ (to be addressed in a dedicated platform polish ticket).

## Further Notes

- Maintains strict compliance with the prime directive: cold start and capture performance remain unaffected, as transition configurations are pre-computed statically without runtime allocations during startup.
