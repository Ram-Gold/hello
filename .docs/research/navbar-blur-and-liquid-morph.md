# Research: Subtle Navbar Blur & Eliminating Sharp Edges During Liquid Morph

**Date:** 2026-11-19  
**Baseline:** `minSdk = 24`, `compileSdk = 37`, `targetSdk = 37` ([app/build.gradle.kts](file:///home/ram/Projects/Hello/app/build.gradle.kts#L11-L18)), Compose BOM `2026.02.01` ([libs.versions.toml](file:///home/ram/Projects/Hello/gradle/libs.versions.toml#L10)), test device `vivo 1901` (Android 11 / API 30).

---

## 1. Why Sharp Edges Still Appear During the Morph

### 1.1 API < 31 (Android 11 / `vivo 1901`): `BlurMaskFilter` Only Draws a Veil Over Crisp Glyphs
In [HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L324-L357):
- `drawContent()` (`L325`) renders the 3-tab icons, label text, and `secondaryContainer` pill **100% sharp**.
- The `< Build.VERSION_CODES.S` branch (`L327-L356`) then draws a solid `resolvedNavSurfaceColor` rounded rect with `BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)` **on top** of `drawContent()`.
- `BlurMaskFilter` only softens the perimeter alpha of that overlay rect—it does **not** convolve or blur the icons, labels, or selection pill underneath. Near the capsule rim, the inward edge feather makes the veil even more transparent, leaving perimeter edges sharp until `navContentAlpha` hits `0`.

### 1.2 API 31+ (`BlurEffect`): Clipping & Blur/Alpha Timing
- **Hard Capsule Clip:** `Child 1` has `shape = RoundedCornerShape(30.dp)` and `clip = true` ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L299-L300)), which slices off the outward blur halo around the `secondaryContainer` pill and icons when `BlurEffect` runs inside it (`L318`).
- **Alpha Outraces Blur:** In [DockMotionConfig.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/DockMotionConfig.kt#L505-L508), `navPillBlurDp` starts at `p = 0.06f` and peaks (`16.dp`) at `0.54f`, while `navContentAlpha` fades out from `0.08f..0.50f`. By `p = 0.25f`, blur is still low while icons are already fading, and by `0.50f` icons are gone before peak blur is reached.

### 1.3 Sharp Wavefront & Hard Text Clipping
1. **Unfeathered Wavefront (`Child 2`):** `Child 2` is a `clip = true` solid `primaryPillColor` box ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L389-L392)). As `pillLeftDp` sweeps across `resolvedNavSurfaceColor`, its left semicircular edge forms a high-contrast 0px cliff, and `pillWidthPx = ...roundToPx()` (`L504`) steps in 1px increments.
2. **Premature `Child 0` Color Reveal:** `Child 0` ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L263-L282)) has a full-width `primaryPillColor` background fading in at `0.34..0.46`, while `Child 1`'s `navContainerAlpha` fades out at `0.74..0.88` ([DockMotionConfig.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/DockMotionConfig.kt#L493-L508))—causing the unswallowed left end of the dock to uniformly crossfade instead of staying soft under the wavefront.
3. **Hard `clipToBounds()` on `+ Catching up?`:** `MorphActionPillContent` wraps `"Catching up?"` in `Modifier.clipToBounds()` with `roundToInt()` width slicing ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L595-L605)), slicing incoming letters with a sharp vertical edge.
4. **Liquid Bridge Cutoff (`xB > xA`):** Once the sweeping pill passes ~51dp into the dock, `xB` drops below `xA` and `if (xB > xA + 1f)` ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L440)) stops drawing the bridge abruptly.

---

## 2. Primary-Source Techniques Available Without New Dependencies

Based on Android Graphics / Compose `1.10.4` (`minSdk = 24`):

1. **True Optical Blur on API 24–30 (for Subtle Dissolve on `vivo 1901`):**
   - Because `DockTabsLayer` is visually static while morphing `0f -> 1f`, record it into a Compose `GraphicsLayer` (`rememberGraphicsLayer()` → `toImageBitmap()`), then generate a softly blurred `Bitmap` using either:
     - **Progressive bilinear downsample/upsample pyramid** (`Bitmap.createScaledBitmap(..., filter = true)` down to `1/4` or `1/8` and back up, zero deprecated APIs, <0.2ms for a `226dp × 60dp` dock), or
     - `android.renderscript.ScriptIntrinsicBlur` on a `0.5x`-downscaled bitmap (native on API 17–30).
   - Crossfade smoothly from crisp `drawContent()` into the blurred bitmap starting at `p = 0.00f` with a gentler peak blur (`8–12.dp` instead of `16.dp`) and overlapping alpha (`0.15f..0.68f`), so icons softly melt rather than turning muddy or disappearing while sharp.
2. **Soft Capsule Edge Masking (`CompositingStrategy.Offscreen` + `BlendMode.DstIn`):**
   - Supported in hardware acceleration across **all API levels (24–37)** via `Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }`.
   - Replaces hard `clip = true` on `Child 1` during blur and replaces `Modifier.clipToBounds()` on `"Catching up?"` ([HomeFloatingBottomDock.kt](file:///home/ram/Projects/Hello/app/src/main/java/com/example/hello/ui/home/components/HomeFloatingBottomDock.kt#L595)) with a horizontal gradient `BlendMode.DstIn` mask (~16–20dp soft fade feather at the reveal boundary).
3. **Subtle Feathered Wavefront & Unified Offscreen Liquid Surface:**
   - Render the `resolvedNavSurfaceColor` base + concave Bézier bridge + sweeping `primaryPillColor` wave inside one `CompositingStrategy.Offscreen` layer masked to the continuous capsule silhouette, with a soft horizontal gradient feather (`18–24.dp`) on the sweeping left edge while in motion (`0.08f..0.90f`) that resolves to crisp `0dp` at `p = 0` and `p = 1`.
   - Keep `Child 0` shadow-only (transparent fill or sized to the union bounds) so `primaryPillColor` never leaks behind `Child 1` before the wave reaches `x = 0`.
4. **API 31+ Hardware `BlurEffect` Refinement:**
   - Use `Modifier.blur(radius, BlurredEdgeTreatment(RoundedCornerShape(30.dp)))` or `BlurEffect(..., TileMode.Decal)` without an inner `clip = true` boundary cutting off the blur halo, starting softly at `p = 0.00f`.
