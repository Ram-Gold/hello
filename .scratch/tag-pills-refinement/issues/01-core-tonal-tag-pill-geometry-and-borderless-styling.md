# 01: Core Tonal Tag Pill True Stadium Geometry & Borderless Styling

**What to build:** Transform the core `TonalTagPill` component into a true borderless stadium pill (using 50% / circular corner curvature) so tags across the app render with fluid rounded pill ends and zero border stroke clutter, emphasizing tactile tonal color fills.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `TonalTagPill` defaults to `CircleShape` (stadium pill shape with semicircular cap ends) rather than a squircle or rectangular corner radius.
- [ ] Borders are completely removed (`border = null`) across both unselected and selected pill states.
- [ ] `TagColorCache` border stroke allocations and mappings are cleaned up to reduce object overhead.
- [ ] Internal pill padding is tuned to balanced pill proportions (e.g., 10dp horizontal, 4dp vertical) with smooth typography alignment.
- [ ] All unit and component tests continue to compile and pass with zero warnings.

