# 0012. Modular Capture Flow Engine, Dynamic Component Sequencing, and Hybrid Contact Schema

Date: 2026-11-01

## Status
Accepted

## Context
ADR-0004 established Flow A (Add Contact) as a hardcoded 4-step sequence (`Name` → `Place` → `Tags & Circles` → `Note & FollowUp`) anchored above the IME with a sub-50ms launch target. However, capturing relationships across varying social scenarios (e.g. professional tech conferences, casual social gatherings, university meetups) requires flexible, contextual fields—such as job titles, organizations, birthdays, and custom key-value details.

Hardcoding forms or bloating the capture card with unused inputs degrades capture speed and increases cognitive burden. Hello requires a modular, Obsidian-properties-inspired Capture Flow system where fields can be reordered, omitted, or augmented via custom presets while maintaining local-first storage invariants, sub-50ms keyboard readiness, and zero vendor lock-in.

## Decision

1. **Domain Entities & Preset Storage**:
   - Introduce `Capture Flow` (the active step-by-step sequence), `Flow Preset` (a named preset template), and `Capture Component` (individual input units: Name, Role, Company, Place, Circles, Tags, Email, Phone, Birthday, FollowUp, Note, Custom) into the domain model.
   - Store presets and component ordering in canonical Room DB tables (`capture_flow_presets` and `capture_flow_components`). Seed with 4 built-in system presets:
     - **Default**: Name → Role → Company → Place → Note
     - **Conference**: Name → Company → Role → Email → Tags → Note
     - **Casual Party**: Name → Place → Phone → Tags → Note
     - **Campus**: Name → Company/University → Circles → Phone → Note
   - System presets can be reordered or reset to defaults, but cannot be deleted. Custom user presets can be created, edited, and deleted.

2. **Hybrid Contact Schema & Multi-Format Portability**:
   - Extend `Contact` domain model and `ContactEntity` in Room with first-class typed columns for common structured properties: `role: String?`, `company: String?`, and `birthday: LocalDate?`.
   - Store arbitrary user-defined properties in `customProperties: Map<String, String>`, backed by a Room JSON `TypeConverter`.
   - Update `ExportBundleGenerator` to output all extended fields 1:1 into lossless `contacts.json` and human-readable YAML frontmatter in `contacts/{name}.md` for native Obsidian Properties interoperability.

3. **Name Invariant & Escape Hatch Guard**:
   - The `Name` component is mandatory in every preset, but its sequential position is flexible (users may order Place or Birthday before Name if desired).
   - Tapping the Quick Save escape hatch (`✔`) when Name is empty immediately aborts premature persistence and smoothly jumps focus directly to the Name step, ensuring no orphan or nameless contacts are created.

4. **IME Discipline & Settings-Only Flow Configuration**:
   - Uphold the extreme capture speed invariant (<50ms cold start, persistent keyboard).
   - Changing the active flow and configuring presets (reordering steps, adding/removing fields, creating custom presets) is managed exclusively in Settings (`SettingsFlowsScreen`).
   - The Quick Add card executes the user's active preset directly with zero modal interruptions, distractions, or flow-switching dropdowns, ensuring extreme capture speed and low cognitive load.
   - Non-text components (e.g., Birthday selector, Circles/Tags multi-chips, Place suggestions) render compact inline controls directly above the soft keyboard without dismissing or cycling the IME.

5. **Clean Settings Separation**:
   - Preset management (reordering, toggling components, creating new presets) lives exclusively in `SettingsFlowsScreen` under Settings (ADR-0007).
   - Quick Add remains strictly focused on capture speed without inline configuration sheets.
   - Captured properties render seamlessly inside `PropertiesPanel` on `PersonDetailScreen` and as editable fields in `PersonEditScreen`.

## Consequences
- **Positive**:
  - Full modularity matching Obsidian properties mental model without adding capture friction.
  - Zero loss of cold-start capture velocity: keyboard remains pinned and responsive.
  - Preserves data sovereignty: custom properties cleanly export to plain Markdown frontmatter.
  - Prevents anonymous records while granting users complete control over question sequencing.
- **Trade-offs**:
  - Requires Room database migration (version bump) for `contact_table` and new preset tables.
  - QuickAction state machine transitions from fixed enum steps (`FlowAStep`) to a dynamic list of active `CaptureComponent` instances.
