# Research: Navbar Easing-Out & Smooth Ease-In-Out Animations in Kotlin/Compose

**Date:** 2026-11-19  
**References:**
- [/animate skill](file:///home/ram/Projects/Hello/.agents/skills/animate/SKILL.md) & [Animation Recipes](file:///home/ram/Projects/Hello/.agents/skills/animate/RECIPES.md)
- [MotionTokens.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/theme/MotionTokens.kt)
- [DockMotionConfig.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/DockMotionConfig.kt)
- [HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt)
- [AppNavigation.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/navigation/AppNavigation.kt)
- [MainAppShell.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/MainAppShell.kt)

---

## 1. Easing Audit Against `/animate`

Per [/animate](file:///home/ram/Projects/Hello/.agents/skills/animate/SKILL.md#L100-L120):
- **Entering or exiting UI** must use **`--ease-out` (`CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)`)** — starts immediately at peak velocity so the interface feels responsive the instant the user taps, then decelerates smoothly.
- **Moving or morphing on screen** must use **`--ease-in-out` (`CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)`)** — symmetric acceleration and deceleration between two resting states.
- **Rule 3:** Extend `MotionTokens.kt` rather than forking private curves.

### Findings in Current Navbar Code
1. **Forked private curve (`DockMotionConfig.kt:41`):** `LiquidMorphEasing = CubicBezierEasing(0.32f, 0.0f, 0.22f, 1.0f)` forked `MotionTokens` and applied an ease-in-out ramp to **entering/exiting fades & text** (`navPillAlpha`, `navShadowAlpha`, `labelAlpha`, `labelSlideOffsetDp`, `labelRevealWeight`), delaying visual response during the initial slow ramp.
2. **Weak built-in Material 2 curve (`MotionTokens.kt:73`):** `NavBarHideEasing = LinearOutSlowInEasing` (`0.0f, 0.0f, 0.2f, 1.0f`) is much weaker than `--ease-out` (`0.23f, 1.0f, 0.32f, 1.0f)` for dock hide/show (`floatingDockVisibilitySpec`).
3. **Default `FastOutSlowInEasing` on Speed Dial scrims (`MainAppShell.kt:1545-1546`, `HomeScreen.kt:135-136`):** `tween(200)` / `tween(150)` omitted an explicit `ease-out` curve.
4. **Reverse-morph polarity:** Running `profileMorphAnimatable` backward (`1f -> 0f`) through forward ease-out curves turns exit fades/blur into ease-in unless direction (`isReversing`) is respected.

---

## 2. Kotlin / Jetpack Compose Smoothness Bottlenecks

Per Android Compose rendering architecture (**Composition → Layout → Draw**), 60–120Hz animations must defer state reads into the **Draw phase** (`Modifier.graphicsLayer { ... }`, `drawBehind { ... }`, `drawWithContent { ... }`) to skip per-frame recomposition and relayout:

1. **Composition-phase frame reads in `NavTabItemContent` ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L835-L837)):** Reading `selectionWeightProvider()` directly in composition (`lerp`, `resolveTabIcon`) invalidates composition for all 3 tabs on every animation frame.
   - **Fix:** Stack inactive (outlined) and active (filled) icons and fade between them via `Modifier.graphicsLayer { alpha = ... }` inside the Draw phase, achieving **0 recompositions/frame** during tab switches.
2. **Composition-phase read of `dockVisibilityProgressState.value` in `MainAppShell` ([MainAppShell.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/MainAppShell.kt#L485-L500)):** Reading `dockVisibilityProgressState.value` to compute `Modifier.padding(bottom = toastBottomPadding)` forces `MainAppShell` / `Scaffold` recomposition and relayout on every dock hide/show frame.
   - **Fix:** Keep static resting bottom padding and apply the delta via `Modifier.graphicsLayer { translationY = ... }` in the Draw phase.
3. **Nested integer `.roundToPx()` layout jitter on `+` icon ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L487-L511)):** Modifying the `+` icon's integer pixel layout box every frame while also applying `graphicsLayer { scaleX; scaleY }` shifts adjacent `Row` siblings in 1px steps.
   - **Fix:** Use a constant `24.dp` icon layout box and animate size purely in the Draw phase via `graphicsLayer { scaleX; scaleY }`.
4. **Main-thread bitmap downscaling on API < 31 (`createSoftBlurredBitmap`):** Running multi-pass `Bitmap.createScaledBitmap` inside `LaunchedEffect` on `Dispatchers.Main` can contend with frame rendering; dispatch it to `Dispatchers.Default`.
5. **Missing `LocalReducedMotion` guards:** `DockTabsLayer` pill tween, FAB rotation, and Speed Dial chips/scrims did not honor `LocalReducedMotion.current` (should snap or swap to gentle opacity-only fades without transform motion per `/animate`).
