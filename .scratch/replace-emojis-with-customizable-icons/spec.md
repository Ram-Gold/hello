# Specification: Replace All Emojis with Customizable Vector Icons

Status: ready-for-agent

## Problem Statement

Across the Hello app, visual metaphors, category indicators, empty states, section headers, property chips, fallback avatars, and snackbar feedback rely on hardcoded Unicode emoji literals (`👋`, `🌐`, `💼`, `☕`, `🏡`, `🎓`, `📅`, `🎂`, `🎉`, `📞`, `✉️`, `✦`, `📍`, `🔍`, `👤`, `🔄`, `✨`, `⏰`, `🤝`, `📝`, `🗑️`, `📦`).

From the user's perspective, hardcoded emojis create three problems:
1. **Visual Inconsistency & Platform Drift:** Emoji glyphs render differently across Android OEM fonts, clash with the app's typography-led Mallow + Vellum design language, and cannot adapt their stroke weight, tint, or container styling to Light, Dark, or Dynamic Material You themes.
2. **Zero Customization:** Users and the theme system cannot customize the icons representing their **Groups** (`Circle`), **Contact** properties, or surface badges—neither changing which icon represents a Group nor adjusting icon tint, size, or container presentation.
3. **Cluttered Copy & Unpredictable Initials:** Fallback avatar initials (`"👋"`), birthday labels (`"Today! 🎂"`), and snackbar messages (`"Snoozed for 3 days ⏰"`) mix decorative emojis directly into domain and presentation strings, making text noisy and impossible to style uniformly.

## Solution

Eliminate every Unicode emoji literal across the entire application and replace them with a unified, theme-aware **Customizable Icon System**:
1. **Centralized Semantic & Customizable Icon Catalog:** Introduce a curated set of customizable vector icons (`HelloIconKey`) backed by crisp vector drawables / `ImageVector` primitives that accept customizable size (`INLINE` `16.dp`, `UI` `20.dp`, `FEATURE` `24.dp`, `HERO` `28.dp`), stroke/tint color (`MaterialTheme.colorScheme` tokens or user-selected accent), and optional tonal badge containers.
2. **User-Customizable Group (`Circle`) Icons:** Allow each **Group** (`Work`, `Friends`, `Family`, `Uni`, plus `All Groups`) to resolve its icon through a customizable mapping so users can customize which icon represents each Group from a curated icon palette, defaulting to clean vector icons (`Globe`, `Briefcase`, `Coffee`, `Home`, `GraduationCap`).
3. **Structured Leading Icons on Chips, Headers, & Cards:** Replace inline emoji prefixes in **Contact Profile Screen** property chips (role/company, birthday, phone, email, custom properties), **Capture Flow** GPS chips, **Timeline Node** birthday milestones, **Upcoming Alert Card** headers, **Quick Capture** choice cards, and empty-state illustrations with composable leading/hero vector icons.
4. **Clean Domain & Feedback Copy:** Strip all trailing and inline emojis from ViewModel snackbars, button labels, birthday countdown strings, and avatar fallback initials (rendering a vector `User` icon inside `DoodleAvatar` when a Contact has no alphanumeric initials).

## User Stories

1. As a user opening the **Home Tab**, I want the greeting header to display clean typography without a hardcoded waving-hand emoji, so that the top of the screen feels calm and editorial.
2. As a user opening the **Groups** navigation drawer, I want each **Group** (`All Groups`, `Work`, `Friends`, `Family`, `Uni`) to render a crisp, theme-tinted vector icon instead of a system emoji, so that the drawer aligns with the minimal presentation.
3. As a user organizing my relationships, I want to be able to customize the icon associated with any **Group** (`Circle`) from a curated icon picker, so that my Groups reflect my personal mental model.
4. As a user viewing the **Keep in Touch** section header on the **Home Tab**, I want the section header to use a customizable calendar/bell vector icon alongside `"KEEP IN TOUCH"` instead of `"📅 KEEP IN TOUCH"`, so that the header scales and tints cleanly across themes.
5. As a user viewing an **Upcoming Alert Card** for a birthday, I want the card description (`"Wish them well on their birthday"`) and date badge (`"Today!"`) to be free of inline cake or party-popper emojis while relying on the card's 40dp tonal Cake icon badge, so that typography and iconography never duplicate each other.
6. As a user viewing the empty state of the **Keep in Touch** carousel or **Recent Encounters** list on the **Home Tab**, I want the empty-state badge to display a customizable vector icon (such as a coffee cup or check-circle icon) inside its tonal circle instead of a raw `"☕"` text literal, so that the illustration respects dark mode and dynamic color tokens.
7. As a user searching or browsing the **Relationship Tab** when no **Contacts** match, I want the empty-state illustration to render a customizable `Search` or `UserPlus` vector icon instead of `"🔍"` or `"👋"` emojis, so that empty states look intentional and cohesive.
8. As a user opening the Quick Capture choice sheet from the **Floating Bottom Dock**, I want the `"Add Contact"` and `"Met Again / Log Encounter"` option cards to display their customizable `PersonAdd` and `EditCalendar` / `Refresh` vector icons inside the 52dp accent container instead of `"👤"` and `"🔄"` emojis, so that the sheet uses a single cohesive icon language.
9. As a user capturing a new **Contact** or logging an **Encounter** in **Capture Flow**, I want the current-location chip to render a leading `MapPin` vector icon beside `"Current GPS"` instead of `"📍 Current GPS"`, so that location chips match tag and group chips.
10. As a user completing the 2-second escape hatch in **Capture Flow**, I want the primary save button to read `"Save <Name> Now"` with an optional leading/trailing vector icon instead of `"Save <Name> Now ✨"`, so that CTAs remain crisp and free of emoji clutter.
11. As a user viewing a **Contact Profile Screen**, I want the extended **Properties** chips (Role/Company, Birthday, Phone, Email, and Custom Properties) to render structured leading vector icons (`Briefcase`, `Cake`, `Phone`, `Mail`, `Sparkles`/`Tag`) instead of prepending `"💼 "`, `"🎂 "`, `"📞 "`, `"✉️ "`, or `"✦ "` to the chip text, so that each icon can be independently tinted and sized.
12. As a user viewing **Groups** (`CirclePillChip`) on the **Contact Profile Screen** or selecting **Groups** on the **Contact Edit** screen, I want each chip to render the Group's customizable leading vector icon instead of `"💼"`, `"☕"`, `"🏡"`, or `"🎓"`, so that Group identity is consistent across every screen.
13. As a user viewing a **Contact's** **Living Timeline** on the **Contact Profile Screen**, I want the pinned Birthday Milestone node to display a customizable `Cake` vector icon alongside `"Birthday — <Month Day>"` and `"Today!"` instead of `"🎂 Birthday"` and `"Today! 🎂"`, so that timeline milestones match the **Timeline Tab**.
14. As a user browsing the **Timeline Tab**, I want `BirthdayNode` items occurring today to display `"Today!"` alongside their `Birthday` vector badge instead of `"Today! 🎂"`, so that future horizon nodes have uniform badge + label hierarchy.
15. As a user viewing a **Contact** whose name has no extractable initials (or during **Onboarding** preview), I want `DoodleAvatar` to render a customizable `User` vector icon when initials are blank instead of falling back to the `"👋"` emoji string, so that avatars never render system emoji glyphs.
16. As a user completing **Onboarding**, I want the final CTA button to read `"Enter Hello"` with a vector arrow/check icon instead of `"Enter Hello ✨"`, so that the onboarding flow matches the app's editorial typography.
17. As a user performing actions across the app (saving a Contact, logging an Encounter, marking a FollowUp done, snoozing a Subconscious Check-In, deleting Contacts, or exporting an **Export Bundle**), I want snackbar confirmation messages to use clean, human-readable prose without trailing emojis (`✨`, `⏰`, `🤝`, `📝`, `🗑️`, `🔄`, `📦`), so that system feedback is calm and distraction-free.

## Implementation Decisions

1. **Unified Customizable Icon Catalog (`HelloIconKey` & `CustomizableIcon`):**
   - Define an enum `HelloIconKey` representing all semantic and user-selectable icons in the domain and UI layers (e.g., `GLOBE`, `BRIEFCASE`, `COFFEE`, `HOME`, `GRADUATION_CAP`, `HEART`, `STAR`, `USERS`, ` COMPASS`, `CODE`, `MUSIC`, `BOOK`, `CAKE`, `CALENDAR`, `CLOCK`, `MAP_PIN`, `PHONE`, `MAIL`, `SPARKLES`, `SEARCH`, `USER`, `USER_PLUS`, `REFRESH`, `CHECK`, `TRASH`, `PACKAGE`, `TAG`).
   - Provide a single composable `CustomizableIcon(iconKey: HelloIconKey, ...)` (and corresponding vector resolver) that supports customizing:
     - `size: FeatherIconSize` (`INLINE` `16.dp`, `UI` `20.dp`, `FEATURE` `24.dp`, `HERO` `28.dp`)
     - `tint: Color` (defaulting to theme-aware `MaterialTheme.colorScheme` tokens)
     - `containerColor: Color?` and `shape: Shape` when rendered inside a badge or avatar container.
   - Add any missing Feather-compatible Android Vector Drawables (`ic_feather_*.xml`) or Compose vector definitions so every `HelloIconKey` renders with a consistent `2.dp` stroke, rounded caps, and rounded joins.

2. **Group (`Circle`) Icon Customization:**
   - Extend `CircleGroupConfig` with `val iconKey: HelloIconKey = Circle.defaultIconKey(circle)` so each **Group** (`Work`, `Friends`, `Family`, `Uni`) has a default vector icon (`BRIEFCASE`, `COFFEE`, `HOME`, `GRADUATION_CAP`) that can be customized at runtime via `HomeUiEvent.OnUpdateCircleConfig` (and persisted in preferences/state).
   - Provide a lightweight customization affordance (e.g., long-press or edit action on a Group in the **Groups** drawer or settings) allowing the user to pick any `HelloIconKey` from the curated palette for that Group.
   - Remove `iconEmoji` from `CardType` and replace `emoji: String` parameters in `GroupDrawerContent`, `QuickActionChoiceSheet`, `PropertiesPanel`, and `PersonEditScreen` with `HelloIconKey`.

3. **Property & Chip Leading Icons:**
   - Update `PropertyChip` in the **Contact Profile Screen** to accept `iconKey: HelloIconKey` alongside `label: String`, rendering a `14.dp`–`16.dp` tinted leading icon for Role/Company (`BRIEFCASE`), Birthday (`CAKE`), Phone (`PHONE`), Email (`MAIL`), and Custom Properties (`SPARKLES`).
   - Update `LocationChip` in Flow A and Flow B capture sheets to accept an optional `leadingIconKey: HelloIconKey? = null` so the `"Current GPS"` chip renders `HelloIconKey.MAP_PIN` instead of `"📍 Current GPS"`.

4. **Avatar Fallback & Empty-State Illustrations:**
   - Change `extractInitials` / `.ifEmpty { "👋" }` across `ContactRepository`, `HomeViewModel`, `TimelineViewModel`, `MainAppShell`, `DetailCollapsingHeader`, `PersonEditScreen`, and `OnboardingScreen` to return `""` (empty string) when no alphanumeric initials exist.
   - Update `DoodleAvatar` so that when `initials.isBlank()`, it renders `CustomizableIcon(iconKey = HelloIconKey.USER)` scaled proportionally (`size * 0.48f`) instead of rendering a text emoji.
   - Update empty states in `UpcomingFeedSection`, `RecentEncountersSection`, and `ContactsScreen` to render `CustomizableIcon` inside their tonal circular containers.

5. **Emoji-Free Copy Across Domain, ViewModels, & Timelines:**
   - Normalize `"Today! 🎂"` to `"Today!"` in `ContactRepository`, `TimelineViewModel`, and `LivingTimeline`.
   - Normalize `"🎂 Birthday — $monthDayLabel"` in `LivingTimeline` to render a leading `HelloIconKey.CAKE` icon beside `"Birthday — $monthDayLabel"`.
   - Remove trailing emojis from all `userMessage` snackbar strings in `HomeViewModel`, `TimelineViewModel`, `ContactsViewModel`, `QuickActionViewModel`, `SettingsViewModel`, and `MainAppShell`, and from button labels in `FlowAAddContactCard` and `OnboardingScreen`.

## Testing Decisions

- **What makes a good test:** Tests should verify observable behavior and structural contracts at the module boundary rather than internal Composable tree details.
- **Single High-Level Seam — Icon Catalog & Zero-Emoji Contract Test:**
  1. **Icon Resolution & Group Customization Contract:** Verify that every `Circle` resolves its default `HelloIconKey`, that updating `CircleGroupConfig.iconKey` via `HomeViewModel` updates the resolved icon for that Group, and that every `HelloIconKey` maps to a valid non-null vector graphic / drawable resource.
  2. **Avatar & Timeline Label Contract:** Verify that blank contact names produce `""` initials (triggering the vector `USER` icon fallback in `DoodleAvatar`) and that same-day birthdays in `ContactRepository` and `TimelineViewModel` produce `"Today!"` without emoji characters.
  3. **Codebase-Wide Zero-Emoji Source & State Invariant:** A unit test that scans initial UI states (`HomeViewModel`, `TimelineViewModel`, `ContactsViewModel`, `SettingsViewModel`, `QuickActionViewModel`) and Kotlin source files under `app/src/main/java` to assert that zero Unicode emoji codepoints (`U+1F300..U+1FAFF`, `U+2600..U+27BF`, `U+2728`, `U+FE0F`) exist anywhere in the production code.
- **Prior Art:** Follows the pattern of existing architectural invariant tests in the repository such as `DesignThemeTest`, `TypographyTest`, and `HomeViewModelTest`.

## Out of Scope

- Filtering or stripping emojis that a user manually types into freeform text inputs (such as a Contact's custom notes or encounter notes at runtime). The zero-emoji guarantee applies to all app-authored UI chrome, icons, badges, defaults, fallbacks, and system messages.
- Introducing third-party network icon libraries (all icons must remain 100% local Android Vector Drawables / Compose vectors per the Local-First prime directive).

## Further Notes

- Complies with **ADR-0018** (minimal Groups drawer presentation), **ADR-0019** (subconscious check-ins and set-in-stone timeline birthdays), and the `.agents/design.md` Feather icon specification (`16.dp` inline, `20.dp` UI, `24.dp` feature sizes).
