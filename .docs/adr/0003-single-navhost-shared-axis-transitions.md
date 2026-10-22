# 3. Single NavHost Architecture with Route-Aware Shared-Axis Transitions

## Context
When navigating between primary tabs (`Home` and `People`), users experienced noticeable delays and frame drops on entry-level Android devices. Previously, `MainAppShell` relied on bespoke `AnimatedContent` switching that either executed heavy unclipped dual-screen layout container measurements (`slideIntoContainer`) or sacrificed directional motion for an instantaneous crossfade. Furthermore, navigation state was redundantly duplicated inside `HomeViewModel`.

## Decision
1. **Single `NavHost` with Type-Safe Routes**:
   Adopt `androidx.navigation.compose` as the single source of navigation truth. Routes are declared as type-safe Kotlin objects (`HomeRoute`, `PeopleRoute`).
2. **Route-Aware Shared-Axis Transitions**:
   Restrict horizontal directional slide animations (`slideInHorizontally` / `slideOutHorizontally`) exclusively between root tabs (`Home` $\leftrightarrow$ `People`). Deeper screens or non-root destinations do not animate the root tabs.
3. **GPU Hardware Layer Clipping**:
   Apply `Modifier.graphicsLayer { clip = true }` across animated screen containers to ensure pixel clipping occurs on the GPU without triggering expensive layout measurement passes across full composable subtrees.
4. **`navigateSafely` Backstack Policy**:
   Provide a centralized `navigateSafely` extension that enforces `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true`, coupled with a debounced tap guard to prevent duplicate pushes or backstack churn.
5. **Decoupled ViewModel Ownership**:
   Purge `selectedNavTab` and tab switching events from `HomeViewModel`. The `NavController` owns navigation hierarchy and backstack state, while the bottom dock directly observes the current destination.
