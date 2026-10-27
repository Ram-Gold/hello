# Feature Specification: Supporting Screens, Onboarding & Navigation Wiring

Status: ready-for-agent

## Problem Statement

When using Hello as a personal relationship keeper, users currently encounter incomplete navigation paths, missing management controls, and unguided initial experiences:
1. **Lack of Personalization**: Users mentally categorize their network using distinct mental models—some consider people "Friends", others "Connections", "People", or "Contacts". Without customizable display terminology, the interface feels rigid.
2. **Missing Taxonomy Management**: Users accumulate tags across contacts and encounters during rapid capture, but have no surface to view, color-code, rename, or merge duplicate or related tags, causing taxonomy bloat over time.
3. **No First-Launch Guidance**: A new user opening the app for the first time is dropped directly into the main shell without an explanation of Hello's local-first philosophy, extreme capture speed (<50ms), or why location access is requested only when recording an encounter.
4. **Cold Empty States**: When a user has not yet recorded any contacts or encounters, or when filtering/searching yields zero matches, the screens feel hollow or inert instead of warm, tactile, and guiding.
5. **Dead-End Navigation**: Tapping the settings gear in the top bar produces no action, leaving the app feeling unfinished.

## Solution

Complete the navigation graph and deliver supporting screens and touchpoints:
1. **Dedicated Full-Screen Settings Destination**: A clean, warm settings screen accessible from the home top bar offering relationship terminology selection, tag taxonomy management, local-first data export triggers, and privacy disclosures.
2. **Tag Taxonomy Manager**: An interactive manager allowing users to view tag usage frequency, adjust tag colors from a warm curated palette, rename tags, and merge redundant tags across their contacts via atomic cascade updates, with clean unlinking on deletion.
3. **First-Launch 3-Step Onboarding Carousel**: A smooth, linear introductory experience gated by Jetpack Preferences DataStore that introduces Hello's core values, lets users select their preferred relationship terminology, explains foreground-only location discipline, and transitions into the main shell with zero cold-start flicker.
4. **Contextual Warm Empty States**: Intentional, friendly empty states on Home and People screens featuring tactile doodles, informative copy, and smart routing action buttons ("Add your first contact", "Log encounter", "Clear filters").
5. **Decoupled Presentation Terminology**: A reactive preferences layer that dynamically adapts UI nomenclature across tabs, headers, counters, empty states, and speed-dial actions while strictly preserving domain and persistence models.

---

## User Stories

1. As a new user opening Hello for the first time, I want to see a welcoming onboarding screen, so that I understand Hello is a local-first, privacy-focused relationship keeper.
2. As a new user on the first onboarding step, I want to learn about the app's promise of instant (<50ms) capture and zero cloud sync, so that I feel confident my personal data remains solely on my device.
3. As a new user on the second onboarding step, I want to choose my preferred relationship terminology (`People`, `Friends`, `Connections`, or `Contacts`), so that the app immediately speaks my language.
4. As a new user on the third onboarding step, I want to read an honest explanation of foreground-only location access, so that I understand my location is never tracked in the background and only recorded when I choose to log an encounter.
5. As a new user completing onboarding, I want to tap "Get Started" and transition seamlessly to the home screen, so that I can begin using the app without seeing onboarding again on subsequent launches.
6. As a returning user opening the app, I want to see a seamless cold start without layout flicker or flashing onboarding screens, so that start time remains <50ms.
7. As a user on the home screen, I want to tap the settings gear icon in the top bar, so that I can configure application preferences.
8. As a user entering the settings screen, I want to see a full-screen view that smoothly slides in while hiding the bottom navigation dock, so that I have a focused space for settings without dock distractions.
9. As a user in settings, I want to tap a back button in the header, so that I return smoothly to where I left off on the home screen.
10. As a user in settings, I want to see a "Display & Terminology" section showing the active terminology choice, so that I know what nomenclature is currently applied.
11. As a user in settings, I want to change my relationship terminology from "People" to "Friends", so that all main tabs, headers, counters, and capture buttons update dynamically to say "Friends".
12. As a user in settings, I want to change my relationship terminology to "Connections", so that the app feels appropriately professional for my networking needs.
13. As a user in settings, I want to preview how my terminology selection affects copy before confirming, so that I understand the visual impact of my choice.
14. As a user in settings, I want my terminology preference to persist across app restarts via DataStore, so that my customized language is always preserved.
15. As a user in settings, I want to see a "Tag Taxonomy" section listing all tags currently in use, along with their color dots and contact usage counts, so that I can review how my tags are organized.
16. As a user in the tag taxonomy manager, I want to tap "+ Add Tag", enter a name, and select a color, so that I can define new labels before attaching them to contacts.
17. As a user in the tag taxonomy manager, I want to select a tag color from a curated palette of 8 warm earth tones, so that my tags always look visually harmonious with the app theme.
18. As a user in the tag taxonomy manager, I want to edit an existing tag's name and color, so that I can correct typos or refine my visual hierarchy.
19. As a user in the tag taxonomy manager, I want to merge one tag into another (e.g. merge `#coffee` into `#cafes`), so that I can clean up redundant tags without losing categorization on my contacts.
20. As a user merging tags, I want to see a confirmation dialog detailing how many contacts will be affected and verifying the removal of the source tag, so that I do not accidentally lose tag associations.
21. As a user who confirms a tag merge, I want all contacts associated with the source tag to automatically inherit the target tag without duplicate associations, so that my contact records stay clean and accurate.
22. As a user in the tag taxonomy manager, I want to delete an unused or obsolete tag, so that it is cleanly removed from the repository and unlinked from any contacts without causing errors.
23. As a user in settings, I want to see a "Data & Storage" section explaining the "Markdown is Truth, Room is Disposable" architecture, so that I understand how my data is stored locally.
24. As a user in settings, I want to tap an "Export Contacts as Markdown" trigger stub, so that I see an instant spring loading feedback state and trigger a mock export to the system share sheet.
25. As a user in settings, I want to tap an "Export Backup (JSON)" trigger stub, so that I can trigger an export backup stub with system feedback.
26. As a user in settings, I want to tap a "Rebuild Cache" trigger stub, so that I can verify that my Room search database can be cleanly reconstructed from local storage in under two seconds.
27. As a user in settings, I want to see an "About & Privacy" section displaying the app version and a "100% Local-First & Zero Backend" pledge, so that I have complete transparency into app privacy.
28. As a user on the home screen with zero logged encounters, I want to see a warm empty state card with a coffee doodle and encouraging copy, so that I understand the purpose of the recent encounters section.
29. As a user looking at the home screen empty encounters card, I want a smart action button that opens the encounter logger if contacts exist, or routes to adding a contact if my directory is completely empty.
30. As a user on the people/contacts tab on a fresh install with zero contacts, I want to see an inviting empty state card, so that I am guided to add my first contact.
31. As a user on the people/contacts tab empty state, I want to tap an "Add Contact" (or "Add Friend") button, so that the quick-add single-card capture sheet opens immediately with keyboard focus.
32. As a user searching or filtering on the people/contacts tab when no matches exist, I want to see a clear empty state explaining that no contacts matched the query, so that I do not think my data was lost.
33. As a user seeing a "no matching contacts" search empty state, I want a "Clear filters" button, so that I can reset search and circle filters in a single tap.
34. As a user navigating backwards during onboarding, I want system Back to retreat step-by-step (Step 3 to Step 2 to Step 1) or exit the app on Step 1, so that navigation is predictable and linear.

---

## Implementation Decisions

### Modules & Architecture
- **Preferences Module**:
  - Implement a dedicated user preferences repository backed by Jetpack Preferences DataStore.
  - Store key preferences: `has_completed_onboarding` (Boolean) and `preferred_terminology` (Enum/String: `PEOPLE`, `FRIENDS`, `CONNECTIONS`, `CONTACTS`).
  - Expose a reactive `Flow` of user preferences consumed by `MainAppShell`, `SettingsViewModel`, and `OnboardingViewModel`.
  - Handle cold start via a nullable initial state to prevent flash-of-onboarding for returning users.
- **Navigation Module**:
  - Introduce `SettingsRoute` as a top-level route in the navigation graph.
  - Add `OnboardingRoute` as an initial gating route when `has_completed_onboarding` is false.
  - Implement navigation transitions: horizontal shared-axis slide for settings entry/exit, suppressing the floating bottom dock while settings is active.
  - Ensure system Back button on Settings cleanly returns to the root destination.
- **Settings Screen & ViewModel**:
  - `SettingsViewModel` managing state: active terminology, tag list, active tag edit/merge modal state, export action feedback.
  - Composable UI broken into modular components: TerminologySelectorCard, TagTaxonomySection, TagEditDialog, TagMergeDialog, DataStorageSection, AboutPrivacySection.
- **Tag Taxonomy & Cascade Merge in Contact Repository**:
  - Extend repository with `updateTag(tag: Tag)`, `mergeTags(sourceTagId: String, targetTagId: String)`, and `deleteTag(tagId: String)`.
  - When merging:
    - Identify all `Contact` records containing `sourceTagId`.
    - Replace `sourceTagId` with `targetTagId` in their `tagIds` list, ensuring no duplicate IDs.
    - Sum `sourceTag.useCount` into `targetTag.useCount`.
    - Atomically remove `sourceTag` from the repository's `tags` stream.
    - Update both `fullContacts`, `contacts`, and `recentContacts` state flows.
  - When deleting:
    - Unlink `tagId` from all contacts' `tagIds` list and remove from `tags`.
- **Onboarding Flow**:
  - Linear 3-step carousel powered by a Compose `HorizontalPager`.
  - Step 1 (Welcome): Brand doodle, value proposition, "Next" button.
  - Step 2 (Terminology): 4 choice cards with live preview, selecting the initial preference.
  - Step 3 (Location Explainer): Explains offline, foreground-only GPS capture. Tapping "Enter Hello" updates DataStore and pops onboarding from the backstack.
  - Step-by-step BackHandler navigation.
- **Dynamic Terminology Adaptation**:
  - Provide a presentation helper or composable extension that converts the active terminology enum into appropriate singular and plural labels (e.g. `singular`: "Person", "Friend", "Connection", "Contact"; `plural`: "People", "Friends", "Connections", "Contacts").
  - Dynamically supply these terms to bottom dock tab labels, search header titles, counter text, speed-dial action pills, and empty state cards.
- **Empty States**:
  - Refactor `ContactsEmptyState` to distinguish between two distinct modes: `DirectoryEmpty` (zero contacts in the whole app, providing an "Add Contact" CTA) and `FilterEmpty` (search/circle filter yielded zero results, providing a "Clear filters" CTA).
  - Add smart empty state component for `RecentEncountersSection` on Home routing to Flow B (if contacts exist) or Flow A (if 0 contacts exist).

---

## Testing Decisions

### What Makes a Good Test
Tests must verify observable system behavior from the outside (inputs and resulting outputs/state), rather than internal private variables or incidental layout ordering. State reducers, preferences serialization, and repository cascade updates must be tested deterministically with zero flaky timers or sleep calls.

### Modules Under Test
1. **User Preferences & DataStore Integration**:
   - Verify default values on fresh install (`hasCompletedOnboarding == false`, `preferredTerminology == PEOPLE`).
   - Verify updating terminology updates the state flow immediately.
   - Verify marking onboarding complete updates `hasCompletedOnboarding` to true.
2. **SettingsViewModel**:
   - Verify selecting a terminology emits updated state and persists to preferences.
   - Verify initiating a tag merge updates repository contacts and removes the source tag.
   - Verify tag creation, color updates, and clean deletion reflect in the state.
3. **ContactRepository Cascade Merge & Deletion**:
   - Verify merging Tag A into Tag B correctly rewrites contacts containing Tag A to have Tag B.
   - Verify contacts that already had both Tag A and Tag B do not end up with duplicate Tag B IDs.
   - Verify Tag A is removed from `tags` and Tag B's use count reflects the combined usage.
   - Verify deleting a tag unlinks it from all contacts cleanly.
4. **Onboarding & Gating**:
   - Verify step advancement and BackHandler retreats in onboarding state.
   - Verify completion triggers persistence.
5. **Navigation & Empty State Logic**:
   - Verify route mapping and tab label resolution for each terminology option.
   - Verify empty state condition checks (search active vs directory empty vs smart CTA routing).

### Prior Art in Codebase
- `ContactRepositoryTest.kt`: Unit testing repository mutations and contact/encounter creation.
- `ContactsViewModelTest.kt`: Unit testing search queries, circle filtering, and sorting state reductions.
- `HomeViewModelTest.kt`: Testing alert dismissal, speed dial toggle, and drawer state.
- `NavigationTransitionTest.kt`: Testing directional slide resolution and motion specs.

---

## Out of Scope

- Remote cloud synchronization, cloud authentication, or network export triggers (strictly forbidden by Prime Directive #1).
- Physical file writing of ZIP archives to real disk partitions in unit tests (tested via SAF intent creation and repository state triggers).
- Complex custom HSV color wheel canvas picker (a curated 8-color palette is implemented to ensure theme harmony).
- Modifying underlying Room entity definitions or Markdown file frontmatter keys (canonical entity remains `Contact`).

---

## Further Notes

- In alignment with [ADR 0005](file:///home/ram/Projects/Hello/.docs/adr/0005-presentation-terminology-settings-navigation-and-onboarding-gating.md), terminology customization is strictly decoupled from the domain and storage layers.
- In alignment with Prime Directive #4 (<50ms cold start), DataStore initialization must be non-blocking and lightweight, allowing the main shell to render instantly without disk I/O on the UI thread.

## Implementation Status

- **Status**: Implemented & Verified ✅
- **Deliverable 1 (Settings Screen)**: Completed with `SettingsRoute`, 4-option terminology selector, tag taxonomy manager (8 warm palette tones, edit, cascade merge, clean unlink deletion), export trigger stubs, and rebuild cache trigger stub.
- **Deliverable 2 (Onboarding Flow)**: Completed with 3-step carousel (Welcome → Terminology → Foreground-Only Location Explainer), Jetpack Preferences DataStore integration, zero-flicker cold start gating (<50ms), and linear BackHandler navigation.
- **Deliverable 3 (Empty States)**: Completed with contextual tactile empty states on People (`DirectoryEmpty` vs `FilterEmpty` with smart CTA) and Home (`RecentEncountersSection` empty card with smart routing). Dynamic terminology updates tabs, headers, counters, and quick add buttons across the app.
- **Test Coverage**: 100% tests passing across `ContactRepositoryTest`, `SettingsViewModelTest`, `OnboardingViewModelTest`, and `SupportingScreensNavigationTest`.
