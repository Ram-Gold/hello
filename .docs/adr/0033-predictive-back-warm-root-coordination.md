# 0033: Predictive Back & WarmRootTabsHost Coordination

**Status:** Accepted  
**Amends:** [ADR-0003](0003-single-navhost-shared-axis-transitions.md), [ADR-0007](0007-modular-multi-screen-settings-and-motion-reduction.md)

## Context

While [ADR-0003](0003-single-navhost-shared-axis-transitions.md) originally placed root tabs directly inside `NavHost`, `MainAppShell` subsequently introduced `WarmRootTabsHost` (a sibling layer below `MainAppNavHost`) to keep the Home Tab, Relationship Tab, and Timeline Tab permanently composed for 0ms first-frame tab switching, leaving `HomeRoute`, `PeopleRoute`, and `TimelineRoute` empty inside `NavHost`.

With system predictive back enabled (`android:enableOnBackInvokedCallback="true"`), Navigation Compose (`NavHost`) can natively scrub transitions between stacked non-root destinations (`PersonEditRoute` -> `PersonDetailRoute`, Settings sub-screens -> `SettingsRoute`), but cannot visually scrub `WarmRootTabsHost` or the shell-level `FloatingDockOverlay` (`Floating Bottom Dock` and `Toast Pill` clearance) because they sit outside `NavHost`.

## Decision

1. **Keep `WarmRootTabsHost` + Bridge Live Predictive Back Progress**:
   Preserve `WarmRootTabsHost` outside `NavHost` for 0ms root-tab switching latency rather than moving root tabs back into `NavHost`. Introduce a shell-level predictive back progress bridge (`PredictiveBackHandler` coordination) covering:
   - **Deeper screen -> Root tab** (`PersonDetailRoute`, `SettingsRoute`, `ArchivedContactsRoute` when previous entry is a root tab): scrubs the outgoing screen's canonical pop-exit slide/fade (`+X` horizontal travel) in 1:1 lockstep with `WarmRootTabsHost`'s `depthFraction` (`1f -> 0f`), `FloatingDockOverlay` (`translationY` + `alpha`), and `Toast Pill` clearance.
   - **Root tab (`PeopleRoute` / `TimelineRoute`) -> `HomeRoute`**: intercepts back with `PredictiveBackHandler` to scrub `WarmRootTabsHost`'s `slideFraction`, `fadeFraction`, and `Floating Bottom Dock` pill indicator toward `HomeRoute`, then commits `popBackStack()` cleanly without double-animating `NavHost`.
   - **Deeper screen -> Deeper screen** (`PersonEditRoute` -> `PersonDetailRoute`, Settings sub-screen -> `SettingsRoute`): delegates to `NavHost`'s native `SeekableTransitionState` predictive back scrubbing.
2. **Canonical Hierarchical Orientation**:
   All horizontal predictive back scrubs preserve Hello's left-to-right hierarchical pop direction (`+X` slide-out on the outgoing surface, matching `popExitTransition`) regardless of left or right screen edge swipe (`BackEventCompat.swipeEdge`), avoiding directional mismatch on commit.
3. **Interceptor Discipline & Reduced Motion**:
   - Remove unconditional `BackHandler` interceptors on `NavHost` screens (e.g., `ArchivedContactsScreen`) that block predictive back.
   - Retain `BackHandler(enabled = uiState.isModified)` on `PersonEditScreen` when unsaved edits exist so predictive back peek is suppressed and the Discard Changes confirmation dialog triggers safely.
   - Honor [ADR-0007](0007-modular-multi-screen-settings-and-motion-reduction.md) strictly: when `reduceAnimations` is enabled (`LocalReducedMotion`), disable pre-commit visual scrubbing and execute an immediate `0ms` snap on commit.

## Consequences

- `HomeRoute`, `PeopleRoute`, and `TimelineRoute` retain 0ms warm tab-switching performance while supporting full interactive predictive back across root tabs, deeper screens, selection modes, onboarding steps, and quick-action sheets.
- Any future root-level overlay or deeper destination added above `WarmRootTabsHost` must participate in the predictive back progress bridge and avoid unconditional `BackHandler` calls.
