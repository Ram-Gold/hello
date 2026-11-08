# Consolidation on Singular Minimalist Theme and Toast Pill Geometry

**Status**: Accepted

The previous dual-theme architecture supporting both "Warm Organic (Terracotta & Sage)" and "Minimalist (Mallow & Vellum)" created visual fragmentation, duplicate palette definitions, and inconsistent surface corner radii across the app. We decided to completely retire the Terracotta / Warm Organic theme in favor of a single canonical design system—Minimalist Monochrome—while retaining system, light, and dark modes. Concurrently, the Toast Pill geometry was redesigned from a 24dp capsule to a crisp 8dp rounded rectangle with 1dp border, inverted high-contrast surface coloring, and right-aligned Undo action button to align with the core Minimalist design system.

## Consequences

- The `ThemeStyle` preference selector is removed from Settings > Appearance; only Theme Mode (System / Light / Dark) and Reduce Animations remain.
- All legacy Terracotta (`#E06D3B`) color references (in QuickAdd widget, tag fallbacks, and theme palettes) are standardized to Minimalist monochrome tokens (`#121212`, `#FFFFFF`, `#E2E8F0`).
- The Toast Pill uses `RoundedCornerShape(8.dp)` with 1dp border, high-contrast inverted surface (`#121212` in light mode, `#FFFFFF` in dark mode), and right-anchored action button via `Modifier.weight(1f)`.

