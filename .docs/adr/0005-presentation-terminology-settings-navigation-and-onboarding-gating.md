# 5. Presentation Terminology Decoupling, Settings Navigation, and First-Launch Onboarding Gating

## Context
Users view their personal networks differently: some refer to their contacts as "Friends", others as "Connections", "People", or "Contacts". However, `GLOSSARY.md` and ADR 0002 strictly enforce `Contact` as the invariant domain entity to ensure local-first Markdown and Room cache stability. Furthermore, supporting screens (Settings, Tag taxonomy management, and a 3-step first-launch Onboarding carousel) required a cohesive navigation pattern that does not compromise the prime architectural invariant of extreme launch speed (<50ms).

## Decision
1. **Decoupled Presentation Terminology**:
   Maintain `Contact` as the non-negotiable canonical domain, Room entity, and Markdown frontmatter identifier. Expose a user preference (`Relationship Terminology`: `PEOPLE` [default], `FRIENDS`, `CONNECTIONS`, `CONTACTS`) that dynamically adapts UI presentation copy (tab labels, header titles, counter subtitles, empty states, and speed-dial action prompts) without leaking into domain or persistence structures.
2. **First-Launch Gating via Jetpack Preferences DataStore**:
   Use Jetpack Preferences DataStore (`UserPreferencesRepository`) to persist `hasCompletedOnboarding` and `preferredTerminology`. Gating occurs at the navigation root, presenting the 3-step carousel (`Welcome` → `Terminology Selection` → `Location Explainer`) on first run, and routing directly to the main shell on subsequent launches without disk blocking on the main thread.
3. **Explain-Only Location Explainer**:
   Step 3 of Onboarding strictly explains the foreground-only, local-first rationale for GPS capture, deliberately deferring the system runtime permission dialog until the user explicitly triggers 'Near Current GPS' in the capture sheet, preserving strict permission discipline.
4. **Dedicated Full-Screen Settings Destination**:
   Host Settings as a full-screen route (`SettingsRoute`) navigated from `HomeTopBar` using shared-axis slide transitions. The floating bottom navigation dock is suppressed via GPU `graphicsLayer` translation while in Settings, returning cleanly to `HomeRoute` on back navigation.
5. **Cascade Merge and Clean Unlink for Tag Taxonomy**:
   When merging Tag A into Tag B within the Tag Taxonomy Manager, all contacts referencing Tag A are migrated to Tag B, use counts are combined, and Tag A is deleted atomically from the repository. Deleting an active tag unlinks it from all contacts cleanly without blocking or throwing errors.
6. **Zero-Flicker Cold Start Gating (<50ms)**:
   DataStore emissions default to a nullable sentinel state (`null`), rendering a neutral warm surface during the first 10-20ms of app boot. This completely prevents layout flicker or accidental onboarding flashes for returning users before routing to `HomeRoute` or `OnboardingRoute`.
7. **Linear Onboarding Step Control**:
   The 3-step Onboarding carousel progresses linearly via explicit action buttons. Android BackHandler handles step retreats (Step 3 → Step 2 → Step 1) or exits the task on Step 1, preventing skipped steps before completing onboarding.
