# 7. Modular Multi-Screen Settings Architecture and Motion Reduction

## Context
As Hello's configuration footprint expanded beyond simple terminology selection to include tag taxonomy management, sovereign data/export bundles, appearance theming, motion reduction, and proactive reminder cadences, hosting all controls in a single monolithic scroll view degraded hierarchy and usability. Users required distinct focused surfaces for each domain while adhering to Material Design 3 guidelines and maintaining extreme cold-start speed (<50ms).

## Decision
1. **Hierarchical NavHost Sub-Routes**:
   Decompose settings into a root hub (`SettingsRoute.ROUTE_NAME`) and dedicated sub-screen destinations in the single `NavHost`:
   - `settings/terminology`: Relationship Terminology selection.
   - `settings/tags`: Tag taxonomy manager (create, edit, merge, delete tags).
   - `settings/data`: Sovereign data storage, multi-format export bundles, JSON backup, and Markdown exports.
   - `settings/appearance`: Theme mode (System / Light / Dark), Dynamic Color (Material You, Android 12+), and Reduce Animations toggle.
   - `settings/preferences`: Proactive reconnection cadence defaults (14/30/60/90 days), reminder notification delivery time, and default encounter location capture.
   - `settings/about`: App version, 100% local-first & zero-backend guarantee, and sovereignty statement.

2. **M3 Hub Card Interaction Pattern**:
   The root Settings screen presents section categories as clickable Material 3 `OutlinedCard` components featuring category iconography, live status subtitles, and prominent action buttons (e.g. "+ Add Tag" quick trigger alongside "Manage All Tags").

3. **Standard AOSP Shared-Axis Sub-Screen Navigation**:
   Sub-screens navigate via `350ms` AOSP shared-axis slide transitions with TopAppBar back navigation returning to the Settings hub.

4. **Zero-Duration Motion Reduction**:
   When the user enables 'Reduce animations' in Appearance settings, the motion system enforces an immediate `0ms` cut (snapping transitions without spring, slide, or fade latency), reducing motion-induced discomfort and saving CPU/battery cycles.
