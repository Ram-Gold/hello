# 3. Single NavHost Architecture with Route-Aware Shared-Axis Transitions

## Context
When navigating between primary tabs (`Home` and the `Relationship Terminology` tab: `People` / `Friends` / `Connections` / `Contacts`), entry-level Android devices suffer frame drops if both full screens are kept permanently composed in a `HorizontalPager(beyondViewportPageCount = 1)` or if offscreen GPU buffers (`CompositingStrategy.Offscreen`), blur shaders, or path clipping are active during root tab switches.

## Decision
1. **Single `NavHost` with Direct Root Routes (`HomeRoute`, `PeopleRoute`)**:
   Adopt `androidx.navigation.compose` as the single source of navigation truth. Root tabs (`HomeRoute`, `PeopleRoute`) are direct top-level composable destinations in the `NavHost` (no nested `"root"` `HorizontalPager`), ensuring only the active tab is composed at rest while ViewModels remain shell-scoped.
2. **MD3 Expressive Root-Tab Transitions (`mainRootEnterTransition` / `mainRootExitTransition`)**:
   Between `HomeRoute` and `PeopleRoute`, apply a `380ms` horizontal slide (`±50%` width offset) paired with a `190ms` (`duration / 2`) fade using the Material 3 Expressive emphasized easing curve `CubicBezierEasing(0.2f, 0f, 0f, 1f)` and pre-allocated static `TweenSpec` instances. The floating bottom dock's sliding pill and tab icon/label expansion are synchronized to the same `380ms` `CubicBezierEasing(0.2f, 0f, 0f, 1f)` curve.
3. **Hierarchical Push/Pop Transitions for Non-Root Destinations**:
   The `NavHost` defaults to `350ms` AOSP shared-axis transitions (`1/3` width slide + fade/`scaleOut(0.92f..0.85f)` on `CubicBezierEasing(0.2f, 0f, 0f, 1f)`), while `HomeRoute` and `PeopleRoute` delegate non-root push/pop transitions to `450ms` MD3 Expressive decelerate/accelerate curves (`25%` parallax exit/pop-enter and `0.95f` pop-enter scale).
4. **Symmetric Zero-Scale Slide Transitions for Contact Detail & Person Edit**:
   For `PersonDetailRoute` and `PersonEditRoute`, eliminate all scaling (`scaleOut` / `scaleIn` fixed at 1.0) on both entrance and exit. Apply symmetrical full-width horizontal slides (`initialOffsetX = { it }` on enter, `targetOffsetX = { it }` on pop-exit) synchronized with a full-duration `350ms` `fadeIn` / `fadeOut` over `CubicBezierEasing(0.2f, 0f, 0f, 1f)`. The underlying root screen retains its subtle background depth parallax (`WarmRootTabsHost` depth fraction) without scaling or disrupting the active detail sheet.
5. **Low-End GPU Compositing Discipline (`CompositingStrategy.Auto`)**:
   Skip offscreen buffer allocation (`CompositingStrategy.Auto`), blur shaders, dim scrims, and corner-radius clipping during root tab switches. `HomeFloatingBottomDock` is hosted in the outer `MainAppShell` above `NavHost` so it remains stationary during tab switches, and hides/shows on non-root routes via pure GPU `graphicsLayer` `translationY` + `alpha` (`220ms` `LinearOutSlowInEasing`).
6. **`navigateToTopLevelSafely` & `navigateSafely` Backstack Policy**:
   Gate navigation on `Lifecycle.State.STARTED` and enforce `popUpTo(HomeRoute.ROUTE_NAME) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true` for top-level tab switches.
