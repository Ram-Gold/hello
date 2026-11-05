# 02: Atmospheric Spine Fades on Future and Past Timeline Horizons

**What to build:** Refine the Living Timeline continuous spine with atmospheric alpha gradient fades at entry and exit horizons across both `TimelineScreen.kt` and `LivingTimeline.kt`. The first future event's top spine segment fades in from transparent down to dashed stroke, the first past event's top spine segment fades in from transparent to solid black/onSurface, and the terminal past event's bottom spine segment fades out from solid black/onSurface to transparent.

**Blocked by:** 01-ambient-depth-gradients-under-headers

**Status:** done

- [x] Future timeline top segment: First node in the future horizon fades in from `Color.Transparent` down to `dashColor` on its top segment.
- [x] Past timeline top segment: When past log begins or `isFirstPast == true`, top segment fades in from `Color.Transparent` down to `spineColor`.
- [x] Past timeline bottom segment: On the terminal past node (`isLast == true`), the spine extends and smoothly fades out from `spineColor` to `Color.Transparent`.
- [x] Applied harmoniously to `TimelineScreen.kt` and `LivingTimeline.kt`.
- [x] Visual verification on Android device confirms smooth, elegant horizon fades.
- [x] Unit tests pass cleanly (`./gradlew testDebugUnitTest`).
