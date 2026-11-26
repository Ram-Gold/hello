# Hello Domain Model

Local-first personal CRM and relationship keeper for Android, designed around ease of use and seamlessness to help users cultivate meaningful personal and professional relationships through effortless capture and organic serendipity.

## Relationships

**Contact**:
An individual with whom the user maintains a personal or professional relationship.
_Avoid_: Person, Connection, Client, Account

**Nickname**:
An informal, alternative, or familiar name attached to a Contact to aid natural search and recognition.
_Avoid_: Alias, Handle, Username

**Favorite Contact**:
A starred Contact surfaced in dedicated quick-access Favorites sections on both the Home Tab (as a vertical connected list of compact `<Contact Avatar> | <Name>` cards positioned between Upcoming reminders and Recent Encounters) and the Relationship Tab (as a horizontal scrolling strip of `<Contact Avatar>` stacked above `<Name>` positioned beneath the tag filter bar and above alphabetical group headers), ordered by most recently met, edited, or updated.
_Avoid_: Pinned Contact, VIP, Bookmarked Person, Starred Account

**Relationship Terminology**:
The user's chosen display nomenclature (People, Friends, Connections, or Contacts) applied across the presentation layer to describe Contacts according to personal mental models.
_Avoid_: User Role, Category Type, Custom Entity

**Tag**:
A clean, lightweight freeform label (strictly without `#` prefix) attached to Contacts to support flexible categorization and ad-hoc filtering. Tags are automatically pre-colored using deterministic palette mapping and displayed across all surfaces as Tonal Tag Pills. Serves as the single categorization primitive across the entire application.
_Avoid_: Circle, Group, Sphere, Category, Label, Folder, Hashtag

**Tonal Tag Pill**:
A borderless Material 3 Expressive visual component displaying a Tag's name using a soft tinted background fill and matching saturated text with uniform proportional padding. Renders as a full stadium pill when unselected or displayed as a badge, and smoothly morphs into a saturated squircle when selected.
_Avoid_: Hashtag chip, Outlined tag, Bordered badge, Category box

**Encounter**:
A recorded interaction or meeting between the user and one or more Contacts, preserving context, notes, and recency.
_Avoid_: Interaction, Touchpoint, Meeting, Event

**Place**:
A named physical venue or location where Encounters take place.
_Avoid_: Venue, Location, Spot

**FollowUp**:
An actionable stay-in-touch reminder tied to one or more Contacts, with a scheduled due time and completion state; presented to users in the interface as a "Reminder".
_Avoid_: Task, To-do, Ping

**Serendipity Prompt**:
A quiet, in-app-only weighted-random reconnect suggestion surfaced in the Home carousel (`"Say hi"` badge, capped at 2 cards per day, zero OS push notifications) drawn deterministically each day from active Contacts not met in 14+ days (higher weight for longer gaps, clamped at 365 days). Tapping `"Skip"` rolls the next eligible Contact for today, and tapping `"View"` opens the Contact Profile Screen without auto-logging synthetic encounters.
_Avoid_: Cadence, Manual Frequency, Overdue SLA, Ping Schedule, Subconscious Check-In

**Check-in Digest**:
An opt-in, periodic local notification (Weekly or Biweekly, default Off) that batches 2–3 quiet Contacts selected via Serendipity Prompt weighting into a single low-pressure nudge at the user's preferred delivery hour.
_Avoid_: Cadence Alert, Overdue Digest, Push Nag, Ping Schedule

**Export Bundle**:
A user-generated, portable archive containing complete representations of relationship data in JSON, individual Markdown notes, and CSV formats.
_Avoid_: Backup dump, Database export, System dump

## Capture & Flows

**Capture Flow**:
The single-surface floating Quick Capture composer presented above the keyboard for adding a Contact or logging an Encounter ("Met Again") with minimal cognitive load, combining an auto-focused primary input, expandable Notes line, floating detail popovers hovering above the composer card (never expanding inside the composer container), a `+` overflow popover, and a pinned right-thumb Save button (ADR-0040).
_Avoid_: Wizard, Form, Onboarding, Input Funnel

**Flow Preset**:
A named, user-customizable Quick Add button preset (such as Default, Conference, Casual Party, or Campus) defining which Capture Components are pinned as detail buttons in the Quick Capture action bar and in what order, editable via `+ → Customize Quick Add` or Settings (ADR-0040).
_Avoid_: Template, Profile, Form Schema

**Capture Component**:
A modular, self-contained detail unit (such as Location, Tags, Reminder, Time, Occupation, Phone & Email, Birthday, or Custom Property) surfaced as a detail button or inside the `+` overflow popover within the Capture Flow.
_Avoid_: Field, Form Item, Input Widget

## Presentation & Navigation

**Floating Bottom Dock**:
The floating bottom navigation bar that houses the three primary navigation tabs (Home Tab, Relationship Tab, and Timeline Tab) alongside the companion Quick-Add Floating Action Button on root destinations, and morphs in place into the split Profile Action Bar (Profile Reminder Pill + Profile Encounter Pill) when navigating to the Contact Profile Screen. Icons are permanently visible across all tabs (outlined when inactive, filled when active), while the active tab's text label expands beside its filled icon inside a constant-width selection pill.
_Avoid_: Navbar, Bottom bar, Tab bar

**Profile Encounter Pill**:
The right-anchored bottom-docked primary action button (`+ Catching up?`) on the Contact Profile Screen that morphs in place from the Floating Bottom Dock's Quick-Add Floating Action Button alongside the companion Profile Reminder Pill to log a new Encounter for the viewed Contact.
_Avoid_: Log Meet button, Profile FAB, Inline Log Met button, Catch-up bar

**Profile Reminder Pill**:
The left-anchored bottom-docked companion action button (`Remind`) on the Contact Profile Screen that morphs in place from the Floating Bottom Dock's navigation pill beside the Profile Encounter Pill (`+ Catching up?`) to open the bottom-sheet Reminder flow for the viewed Contact.
_Avoid_: Inline reminder form, Top-bar bell button, Ping button, Navigation button

**Toast Pill**:
A crisp, floating tactile notification card (`RoundedCornerShape(8.dp)` with 1dp border) rendered as an inverted high-contrast surface (`#121212` in light mode, `#FFFFFF` in dark mode) to deliver lightweight, transient user feedback (such as confirmations, destructive alerts, and undo prompts). It pairs semantic leading icons (Archive, Delete, Star, CheckCircle, Info, and the animated Hello Mark Motion icon for Contact, Encounter, and Reminder saves) and a bottom countdown line with concise message copy and a right-anchored action button (such as Undo), positioned with dynamic adaptive clearance above the Floating Bottom Dock on docked root tabs (`dockHeight + 16.dp`) and above system navigation bars on undocked sub-screens (`navigationBars + 16.dp`).
_Avoid_: Toast, Snackbar, Banner, Flash message, Notification bar

**Undo Action**:
A transient 5-second recovery mechanism surfaced on a Toast Pill following a destructive operation (such as archiving or deleting a Contact or Encounter), allowing the user to restore the entity immediately from an in-memory repository cache without navigation interruptions.
_Avoid_: Revert Button, Restore Dialog, Rollback Trigger

**Timeline Tab**:
The primary navigation tab within the Floating Bottom Dock that presents the app-wide Living Timeline—a continuous descending chronological spine anchored at Today and synchronized two-way with the Timeline Calendar Header, uniting future scheduled FollowUps and Contact Birthdays with past recorded Encounters (distinguishing First Met from Met Again).
_Avoid_: Activity Feed, History Screen, Global Log, Audit Trail

**Timeline Calendar Header**:
The interactive temporal navigation header at the top of the Timeline Tab pairing a tappable short abbreviated date heading (`Thu, Nov 25`, never truncating), a Today return action when away from today, and a swipeable 7-day week strip with entry indicators (no `<` / `>` day-step arrow clutter) that stays synchronized two-way with the Living Timeline.
_Avoid_: Agenda bar, Date navbar, Week picker, Filter bar

**Timeline Node**:
A chronological waypoint along the Living Timeline representing an Encounter, Birthday Milestone, or scheduled touchpoint. Past and milestone nodes feature a 12dp hollow circular dot (2dp border, contrasting surface fill) vertically centered alongside the card, connected by a 2dp high-contrast spine with atmospheric alpha gradient fades at entry and exit horizons.
_Avoid_: Event Marker, Dot, Checkpoint

**Ambient Depth Scrim**:
A subtle 12dp vertical gradient (background color fading to transparent) positioned beneath fixed or sticky headers (such as ContactGroupHeader and TimelineHeader) to allow scrolling list content to tuck smoothly underneath without harsh visual cutoff.
_Avoid_: Drop shadow, Box shadow, Divider bar

**Relationship Tab**:
The primary navigation tab within the Floating Bottom Dock that opens the contacts surface, dynamically labeled and styled according to the active Relationship Terminology (People, Friends, Connections, or Contacts).
_Avoid_: People Tab, Contacts Tab, Navbar item

**Archived Contacts Screen**:
A dedicated full-screen destination accessed by pulling down past the activation threshold at the top of the Relationship Tab, displaying archived Contacts in a clean list with instantaneous single-tap Restore capabilities.
_Avoid_: Trash Can, Vault, Cold Storage

**Restore Action**:
An action that reactivates an archived Contact, removing them from the Archived Contacts Screen, moving them back into the active relationship directory, and resuming proactive check-in cadences.
_Avoid_: Unarchive, Recover, Undelete

**Contact Card**:
The Material 3 Expressive connected surface representing a Contact across the Relationship Tab and Home Screen, featuring a 56dp Contact Avatar, prominent name, tonal recency badge, contextual notes preview, and relationship tags.
_Avoid_: People Card, Person Card, Contact Row, Contact Tile

**Contact Avatar**:
The circular visual identity badge for a Contact across all surfaces, displaying their custom cropped portrait photo or opt-in Notionist Avatar when set, and falling back to a minimalist user icon on a neutral surface otherwise.
_Avoid_: Profile Picture, Thumbnail, Headshot, Initials Monogram

**Notionist Avatar**:
An opt-in, hand-drawn monochrome character illustration customized for a Contact and stored locally on device for offline rendering.
_Avoid_: Doodle Avatar, Cartoon Avatar, Generated Icon, AI Avatar

**Avatar Playground**:
The dedicated full-screen customization workspace launched from Edit Contact where the user crafts or edits a Contact's Notionist Avatar across facial, hairstyle, accessory, and background traits while online.
_Avoid_: Avatar Studio, Character Creator, Notionist Maker, Avatar Editor Modal

**Upcoming Alert Card**:
The uniform Material 3 Expressive card surface representing time-sensitive relationship prompts (FollowUp, BirthdayAlert, or Serendipity Prompt) in the Upcoming feed, featuring a 40dp tonal icon badge, strict 1-line text truncation, and bottom-aligned action buttons ("Skip" / "View" for Serendipity Prompts).
_Avoid_: Reminder Card, Ping Tile, Event Box, Alert Banner

**Birthday Celebration Card**:
An ambient, high-visibility celebration card surfaced in Home Recent Encounters and the Living Timeline when today matches a Contact's birthday. It highlights the milestone with celebratory styling without generating synthetic fake records in SQLite, providing a one-tap shortcut to pre-fill an encounter note ("Celebrated [Name]'s Birthday 🎉").
_Avoid_: Birthday Alert Banner, Auto-Encounter, Birthday Notification

**Contact Selection Mode**:
The contextual interaction phase entered via long-press on a Contact Card within the Relationship Tab, enabling multi-contact batch actions (Share via vCard, Archive with confirmation, Delete with confirmation, Select All). During this mode, selected Contact Cards visually decouple from connected groupings into independent fully-rounded surfaces with theme-aware red tinting and an avatar selection checkmark overlay, while a floating vertical action tray docks on the right edge and the Floating Bottom Dock recedes off-screen.
_Avoid_: Edit Mode, Multi-select checkbox mode, Batch screen

**Timeline Selection Mode**:
The contextual interaction phase entered via long-press on an Encounter or FollowUp card within any Living Timeline surface (the global Timeline Tab and the Contact Profile Screen), enabling multi-item deletion with confirmation and Select All. During this mode, selected cards render with theme-aware error container red tinting and an avatar selection checkmark overlay, a floating vertical action tray docks on the right edge with Delete and Kebab (Select All), and the top header transforms into a selection bar with cancel affordance. Birthday milestone nodes remain non-selectable, and deleting encounters dynamically recalculates affected contacts' meet count and recency states.
_Avoid_: Timeline edit mode, Event delete mode, History multi-select

**Contact Profile Screen**:
The dedicated Material 3 Expressive view displaying a Contact's relationship properties, tags, notes, and the Living Timeline (with a standalone upcoming Birthday card above the timeline when the Contact's birthday falls within 1 calendar month, followed by past encounters), intentionally stripped of manual cadence status cards and dialer/messaging quick actions to emphasize organic relationship context.
_Avoid_: User Profile, Person Detail, Contact Detail Screen, Address Book Entry

**Semantic Tactile Feedback**:
The centralized interaction feedback layer (`HelloHaptics`) providing deliberate physical sensation mapped to interaction gravity: light threshold clicks (`THRESHOLD_ACTIVATION`), subtle mode ticks (`MODE_TOGGLE`), affirmative success pulses (`SUCCESS_CONFIRMATION`), crisp transition taps (`EDIT_ENTRY`), and heavy warning vibrations (`DESTRUCTIVE_ACTION`).
_Avoid_: Raw vibrations, Buzzers, Game haptics, Hardware buzz

**Contact Departure Motion**:
The choreographed two-phase exit animation system driving physical spatial tucks upon archiving, physical dissolution upon deleting, and horizontal glide upon restoring, followed by spring-loaded placement reflow of neighboring Contact Cards and group headers to close the gap without jarring list jump-cuts.
_Avoid_: Sudden vanish, Instant card delete, Jump-cut removal

**Hello Mark Motion**:
The signature handwriting animation system for the Hello name-tag mark and wordmark, evoking jotting someone's name down across three moments: cold app launch, welcome brand presentation (Onboarding and About Hello), and save confirmation inside Toast Pill.
_Avoid_: Splash spinner, Brand loader, Decorative logo bounce, Note-saved badge
