# ADR-0017: Ambient Depth Gradients on Scroll Headers and Atmospheric Living Timeline Spine Fades

- **Status:** Accepted
- **Date:** 2026-11-05
- **Context:** Introducing atmospheric depth and polish to Hello's list presentation layers on the People Screen and Living Timeline.

---

## 1. Problem & Motivation

As list items scroll beneath headers and along timeline spines:
1. **Abrupt Clipping under Headers:** Sticky headers on the People Screen (`ContactGroupHeader`) and the fixed top filter container on the Timeline Screen (`TimelineHeader`) produce hard visual seams where cards abruptly cut off.
2. **Infinite Spine Cutoff:** The chronological spine in the Living Timeline terminates abruptly at the top of future events and at the bottom/top of the past log, rather than receding gently into the atmospheric canvas.

---

## 2. Decisions

### Decision 1: Ambient Header Bottom Gradient Scrim (12dp)
- A 12.dp vertical gradient scrim (`Brush.verticalGradient(listOf(backgroundColor, Color.Transparent))`) is placed directly underneath:
  1. **People Screen Headers (`ContactGroupHeader`):** Placed at the bottom edge of sticky group headers ("Recently Met", "Earlier", "A", etc.) so scrolling contact cards tuck smoothly underneath without a hard clipping edge.
  2. **Timeline Screen Header (`TimelineHeader`):** Placed directly beneath the temporal filter pills container (`All` | `Future` | `Past`), allowing timeline cards to scroll cleanly behind the fixed controls.

### Decision 2: Atmospheric Spine Fades via Vertical Alpha Gradients
- Instead of abrupt line starts and terminations, the Living Timeline spine employs theme-aware alpha gradient brushes:
  1. **Top of Future Horizon:** The top spine segment above the first future node fades in from `Color.Transparent` down to `dashColor` (2.dp dashed stroke), evoking an open future horizon.
  2. **Top of Past Log:** When transitioning from the Today anchor or section header, the top spine segment fades in from `Color.Transparent` down to full `spineColor`.
  3. **Bottom of Past Log:** On the terminal past encounter node, the bottom spine segment extends 24.dp and fades out smoothly from `spineColor` to `Color.Transparent`, signaling the depth of past history.

---

## 3. Consequences

### Positive
- **Tactile Depth:** Creates an elevated M3 editorial feel where lists tuck naturally into their sections.
- **Theme-Adaptive:** Uses Compose brushes referencing dynamic `MaterialTheme.colorScheme.background` and `onSurface`, ensuring flawless presentation across light and dark modes.
- **Zero Performance Penalty:** Gradients are drawn via single-layer Compose shaders without extra layout passes.
