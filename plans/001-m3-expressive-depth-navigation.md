# Plan 001: Material 3 Expressive Depth Navigation Pipeline

> **Commit Baseline:** Current HEAD  
> **Category:** Easing & Duration, Physicality, Depth Illusion, High Refresh Rate Support  
> **Inspiration:** PixelPlayer Material 3 Expressive Navigation Pipeline  

## 1. Problem Statement
The previous Home ↔ Connection (People) transition utilized a pure ease-out curve (`CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)`) inside a standard `HorizontalPager`. While rapid, it produced a flat, robotic cardboard-cutout slide across the screen without organic acceleration, depth, fade, scale, or background dimming.

## 2. Target Design & Physics
1. **Ease-in-Out Motion Curve**:
   Adopt `M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)` with duration `380ms`. Smooth initial acceleration followed by a luxurious, fluid deceleration tail.
2. **Smooth Fade & Scale**:
   Receding page scales down to `0.94f` and fades cleanly to `0.50f` alpha. Entering page scales up from `0.94f` and fades in cleanly.
3. **ScreenWrapper Depth Effects**:
   - `animatedCornerRadius`: 0.dp to 28.dp during scroll.
   - `animatedAlpha`: 1.0f down to 0.50f clean translucent fade (replacing Gaussian blur for crisp, artifact-free visuals).
   - `animatedDimAlpha`: 0f to 0.18f black overlay on the receding page for physical card depth contrast.
   - Zero Gaussian blur (`Modifier.blur` eliminated for crisp text and zero shader pipeline overhead).
   - `CompositingStrategy.Offscreen` during active transitions to eliminate layout flashes and ensure sharp squircle clipping.
4. **Coordinated Dock & NavHost Transitions**:
   Bottom dock indicator pill and Navigation Compose shared-axis transitions synchronized to `M3EmphasizedEasing`.
