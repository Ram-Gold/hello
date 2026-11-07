# ADR-0020: Retirement of Groups/Circles, Removal of Shell Drawer, and Consolidation on Tags

## Status
Accepted

## Context
Hello originally maintained two distinct categorization taxonomies: hardcoded life spheres called **Circles** (Work, Friends, Family, University) and ad-hoc freeform **Tags** (`#design`, `#coffee`, etc.). In the UI, Circles were surfaced as **Groups** in a shell-level navigation drawer accessed via a hamburger icon on the Home Top Bar (ADR-0018).

This two-tier hierarchy introduced redundant cognitive load and visual clutter:
1. **Taxonomy Duplication**: Users had to decide whether a categorization belonged to a formal "Group" (Circle) or a "Tag".
2. **Navigation Friction**: The shell-level drawer and hamburger button added an entire modal layer merely to filter the Home feed by 4 preset categories.
3. **Arbitration Complexity**: Multi-circle membership required complex cadence arbitration logic across differing sphere schedules (ADR-0013).

## Decision
1. **Consolidate on Tags as the Single Categorization Primitive**:
   - Retire the `Circle` enum, `CircleGroupConfig`, and `GroupIconRegistry` entirely from the domain model.
   - Remove the `circles` property from `Contact` and `ContactSummary`. Tags (`Set<Tag>`) now serve as the sole categorization system.
2. **Remove Shell Drawer and Hamburger Navigation**:
   - Remove `ModalNavigationDrawer` and `GroupDrawerContent` from `MainAppShell`.
   - Remove the hamburger menu button and active sphere filter badge from `HomeTopBar`, leaving a clean, bold headline on the left and the Settings button on the right.
3. **Simplify Quick Capture Flow**:
   - Adapt the pinned chip strip in Quick Capture (`FlowAAddContactCard`) to be purely Tag-based (`+ Tag` and popular tags), eliminating `CaptureComponentType.CIRCLES`.
4. **Cadence and Reminders**:
   - Rely exclusively on explicit per-contact `cadenceDays` and adaptive Subconscious Check-Ins (ADR-0019) rather than group-level interval defaults.

## Consequences
- The home screen and main app shell become lean, fast, and free of modal drawer overlays.
- Contact models, cards, profile screens, and edit screens have a single, unified `#tag` presentation.
- Existing tests and schemas cleanly shed circle logic without breaking relationship history or notes.
