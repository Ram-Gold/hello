# Spec: Seamless, Zero-Delay Navigation & Low-End Device Optimization

**Status:** ready-for-agent

---

## Problem Statement

When navigating between screens (e.g., switching between the Home and People tabs via the floating bottom dock), users experience a noticeable delay of up to ~1 second, particularly on low-end and entry-level Android devices. Instead of delivering Hello's core architectural promise of instantaneous, tactile responsiveness (<50ms cold start and capture speed), the UI hitches, stutters, or stalls while transitioning. 

This latency occurs because the navigation layer executes concurrent multi-screen layout animations across two complete screen hierarchies simultaneously (measuring and animating both screens during full-width slide-in and slide-out transitions), overwhelming low-spec CPU/GPU threads and causing severe frame drops.

---

## Solution

Eliminate main-thread jank and navigation lag by optimizing the screen transition pipeline for low-end hardware:
1. Replace heavy, concurrent dual-screen sliding container animations with an ultra-lightweight, instantaneous crossfade (<=50ms duration) or direct snappy tab switching.
2. Maintain the tactile spring-animated sliding pill in the bottom dock as the primary visual motion cue, ensuring the navigation feedback is immediate and fluid without incurring heavy full-screen rendering penalties.
3. Optimize screen composition hierarchies and state caching so that switching between Home and People preserves scroll states and avoids expensive recompositions during tab changes.
4. Guarantee responsive, hitch-free navigation on low-end devices with zero perceived latency.

---

## User Stories

1. As a mobile CRM user on a budget Android device, I want tapping a navigation dock tab to switch screens immediately (<50ms), so that I never experience sluggishness or hesitation when checking my contacts or notes.
2. As a user recording quick encounters, I want the bottom navigation dock pill to slide across smoothly without freezing the screen, so that the interaction feels organic and responsive.
3. As a user navigating between Home and People, I want zero dropped frames during screen transitions, so that the visual experience remains buttery-smooth even on resource-constrained hardware.
4. As a user who switches tabs frequently, I want repeated taps on the navigation dock to handle instantly without queuing up lagged animations or freezing the app.
5. As a user browsing the People screen, I want my search filters and scroll position to be preserved when I briefly switch to Home and return, so that I don't lose my place or wait for the list to re-sort.
6. As a user navigating back to Home, I want the personalized greeting, active cards, and cadence alerts to display instantly without layout shifts or re-rendering delay.
7. As a mobile user in battery saver mode, I want navigation transitions to consume minimal CPU and GPU cycles, so that the app stays fast and doesn't drain my battery.
8. As a user opening the Quick Capture action from the dock, I want the speed dial options to appear immediately without lag, so that I can log an encounter in seconds.
9. As an accessibility user, I want tab changes to announce cleanly and transition deterministically without flickering or animation stalls.
10. As a user switching circles/groups from the side drawer, I want the filtered home feed to reflect updates without any perceptible re-layout delay.

---

## Implementation Decisions

### 1. Lightweight Screen Transition Strategy
- Eliminate heavy dual-screen layout container transforms (`slideIntoContainer` and `slideOutOfContainer`) that force continuous concurrent measurement of both full-screen component trees across screen-width translation frames.
- Adopt a fast, hardware-accelerated alpha crossfade transition with a maximum duration of 50ms, or direct instant composition switching when running on constrained profiles.
- Keep the visual motion delight centered on the organic spring pill inside the floating bottom dock, which operates on a lightweight isolated canvas.

### 2. Screen Composition & Layout Stabilization
- Decouple screen content rendering from transition animators so that only the incoming screen consumes active layout and draw passes once selected.
- Retain screen UI states (scroll positions in Home feed and People lazy list) using Compose `rememberSaveable` or ViewModel state preservation, preventing re-instantiation of complex list trees.
- Ensure drawer state synchronization hooks in the Home screen do not emit redundant event dispatches or trigger unnecessary recomposition loops during screen transitions.

### 3. Motion & Physics Budget
- Dock sliding pill spring stiffness: `>= 1400f` for instantaneous tactile response.
- Screen transition duration: `<= 50ms` (or 0ms instant switch under low-power / low-end conditions).
- Zero click ripple background overhead on tab items (maintaining tactile clean look with only the moving pill).

---

## Testing Decisions

### What Makes a Good Test
- Tests verify external behavior and performance contracts: measuring transition duration budgets, state dispatch immediacy, and absence of redundant recomposition events.
- Tests do not couple to internal private variables or arbitrary layout measurements.

### Modules Tested
- **Navigation Transition & Motion Module**: Validates that transition specs adhere to the strict `<= 50ms` duration threshold and spring stiffness requirements.
- **Navigation Reducer & State Module**: Validates that tab selection events immediately update the active navigation state without intermediate delays or blocking operations.

### Prior Art
- Existing test suite in `DockPerformanceTest.kt` validating pill spring stiffness and transition duration constraints.

---

## Out of Scope

- Modifying Room database schemas or local markdown file serialization format.
- Adding cloud sync or remote telemetry (prohibited by Hello architectural invariants).
- Altering visual branding, color palettes, or core Material 3 design tokens.

---

## Further Notes

- Aligns strictly with `AGENTS.md` Prime Directive 4: Extreme Capture Speed (<50ms cold start & snappy interactions).
- Complements the existing floating pill and quick-add dock architecture established in `.docs/adr/0001-floating-pill-and-quick-add-dock.md`.
