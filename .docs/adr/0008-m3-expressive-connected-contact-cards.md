# 8. Material 3 Expressive Connected Contact Cards

## Context
Hello previously rendered contacts in both `ContactsScreen` (Relationship Tab) and `RecentEncountersSection` (Home Screen) using isolated, outlined `Card` components with 1dp borders, 1dp elevation, and squircle corner clipping. In contrast, the app's settings redesign introduced the Google Material 3 Expressive connected container paradigm (`surfaceContainer`, `shapeFor(index, totalItems)`, 56dp circular avatar containers, and 2dp item separation gaps) inspired by PixelPlayer. Users required contact cards to reflect this same warm, modern M3 Expressive layout while surfacing the contact's name, encounter date/recency, notes preview, and relationship tags with zero visual clutter.

## Decision
1. **Unified M3 Expressive Connected Container (`ContactCard`)**:
   Replace the legacy outlined cards in `ContactsScreen` and `RecentEncountersSection` with `ContactCard`:
   - Surface color: `MaterialTheme.colorScheme.surfaceContainer` with 0dp elevation and zero borders.
   - Connected grouping: Items within the same section or alphabetical letter group are grouped using `shapeFor(index, totalItems)` (24dp outer corners, 4dp inner corners, 2dp separation gap).
2. **Three-Tier Information Architecture**:
   - **Header Row**: 56dp circular avatar badge (`CircleShape`) with tactile doodle/photo/initials and tonal container background, contact's bold name (`titleMedium`), and an aligned tonal date pill badge (`surfaceContainerHigh`, displaying recency e.g. "Met 2d ago" or encounter date).
   - **Body Row**: Contextual notes preview (1-2 lines in `bodyMedium` with `onSurfaceVariant`).
   - **Footer Row**: Horizontal relationship tags (colored `#tag` pills) alongside Circle category badges (Work, Friends, etc.).
3. **Adaptive Row Collapse**:
   If a contact has no notes, the body row collapses cleanly without empty space. If no tags or circles exist, the footer row collapses cleanly. Minimalist contacts remain slim and touch-efficient; rich contacts surface all context.
4. **Data Contract Enrichment**:
   Extend `ContactSummary` with `notes: String = ""` and `tags: List<Tag> = emptyList()`, populated from Room DB entities (`ContactEntity` and `TagEntity`) with sub-2ms query performance.
