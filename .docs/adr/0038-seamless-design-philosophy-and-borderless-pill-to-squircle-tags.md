# 0038: Seamless Design Philosophy and Borderless Pill-to-Squircle Tags

**Status**: Accepted  
**Amends**: [ADR-0009](0009-uniform-m3-expressive-card-system.md), [ADR-0021](0021-clean-string-tags-deterministic-tonal-colors.md), [ADR-0031](0031-consolidation-on-singular-minimalist-theme-and-toast-pill-geometry.md)

Hello's design philosophy prioritizes **ease of use and seamlessness** over rigid, outlined card and badge frames. Surfaces, cards, and interactive controls rely on soft tonal background fills (`surfaceContainer`, `surfaceContainerHigh`, and curated tag palette containers), connected Material 3 Expressive container geometry (`24.dp` outer / `4.dp` inner seams), and responsive spring motion rather than 1dp structural borders.

Across every screen and documentation reference, **Tonal Tag Pills** and selectable filter/suggestion chips share one strictly borderless, background-only specification:
1. **Zero Borders**: Tags, filter pills, Quick Capture suggestion chips (`+ tag`, places, custom properties), reminder suggestion chips, and Settings tag color dots/swatches render exclusively with background fills (`0.dp` border in both unselected and selected states).
2. **Pill-to-Squircle Selection Morph**: Unselected and static display tags render as full stadium pills (`CircleShape`, corner radius `height / 2`). When selected, interactive tags, chips, and color swatches smoothly morph in the draw phase to an `8.dp` squircle (`RoundedCornerShape(8.dp)`) with a saturated container fill and high-contrast foreground text/icon.
3. **Uniform Proportional Sizing**: Every tag pill and selectable chip shares a uniform `28.dp` height, proportional 2:1 padding (`horizontal = 12.dp, vertical = 6.dp`), and natural text-relative width (with no artificial 10-character truncation on Contact Cards) so a tag looks identical across Contact Cards, Filter Bars, Contact Profiles, Person Edit, Quick Capture, and Onboarding.
