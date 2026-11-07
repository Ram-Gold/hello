# Specification: People Filter Bar Multi-Tag Selection, Equal-Size "All" Pill, and Smooth Squircle Morph Animation

**Status:** done

## Problem Statement

On the Relationship Tab ("People" screen), selecting filter pills had three UX and visual shortcomings:
1. **Unnoticeable & Unanimated Shape Change:** On a compact `26.dp`-tall pill, an `8.dp` corner radius (`~31%`) was visually too close to a `13.dp` (`50%`) stadium circle, and switching shapes instantaneously with no transition made the squircle transformation feel static and imperceptible.
2. **Inconsistent "All" Filter Pill Size:** The leading `"All"` chip used Material 3's `FilterChip` (`32.dp` min height), clashing with the height and padding of `TonalTagPill`.
3. **Checkmark Clutter & Single-Tag Restriction:** Selecting a tag inserted a checkmark icon that caused horizontal layout shifts, and only one tag could be selected at a time.

## Solution

1. **Distinct Squircle Geometry (`6.dp` Corner Radius) with Smooth Draw-Phase Morph Animation:**
   - Unselected filter pills render as full stadium pills (`50%` corner radius = `height / 2f`) with `FontWeight.Medium` text.
   - Selected filter pills smoothly morph into a crisp **squircle** (`6.dp` corner radius, `~23%` of height) with `FontWeight.Bold` text and solid container fill, with zero checkmark icon.
   - Drive the transition via a single `selectionProgress` float (`animateFloatAsState` with a snappy non-bouncy spring: `dampingRatio = Spring.DampingRatioNoBouncy`, `stiffness = Spring.StiffnessMedium`) and a tactile press scale spring (`0.95f` when pressed).
   - Perform corner-radius interpolation (`lerp(size.height / 2f, 6.dp.toPx(), progress)`) and background color interpolation (`lerp(unselectedContainerColor, selectedContainerColor, progress)`) inside `Modifier.drawBehind` so that **animation frames execute purely in the draw phase with zero layout or measurement churn**.
   - Bypass animation state allocations completely when `onClick == null` (static badges on Contact Cards), preserving 100% scroll performance.
2. **Identical `"All"` Pill Geometry & Animation:**
   - Render the `"All"` pill in `ContactsFilterBar` using the exact same `horizontal = 10.dp, vertical = 5.dp` padding, `labelSmall` typography, and draw-phase stadium-to-squircle morph animation as `TonalTagPill`.
3. **Multi-Tag Union (OR) Filtering:**
   - Support selecting multiple Tags simultaneously (`selectedTags: Set<String>`) using Union (OR) filtering, with individual tag toggle and single-tap reset via `"All"`.

## Implementation Decisions

- **Zero-Relayout Draw-Phase Squircle Morph (`TonalTagPill` & `"All"` Filter Pill):**
  - Fast path for static badges (`onClick == null` and `!isSelected`): render with zero `animate*AsState` allocations and a lightweight `drawBehind` rounded rectangle (`CornerRadius(size.height / 2f)`).
  - Interactive path (`onClick != null` or `isSelected`):
    - Track `MutableInteractionSource` and `collectIsPressedAsState()` to drive a subtle tactile press scale (`0.95f` when pressed, `1f` at rest) in `Modifier.graphicsLayer`.
    - Animate `selectionProgress` (`0f` unselected → `1f` selected) using `animateFloatAsState` with `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)`.
    - In `Modifier.drawBehind`, compute `cornerRadiusPx = lerp(size.height / 2f, 6.dp.toPx(), selectionProgress)` and `bgColor = lerp(unselectedBgColor, selectedBgColor, selectionProgress)`, drawing the pill via `drawRoundRect(color = bgColor, cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx))`.
    - Interpolate text color via `lerp(unselectedContentColor, selectedContentColor, selectionProgress)` and set `fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium`.
    - Use `indication = null` on `clickable` so the custom draw-phase background color morph and `graphicsLayer` press scale provide clean, unclipped tactile feedback without requiring expensive path clipping on every frame.
