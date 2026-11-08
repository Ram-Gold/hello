# Hello Domain Model

Local-first personal CRM and relationship keeper for Android, helping users cultivate meaningful personal and professional relationships through lightweight tracking and proactive cadences.

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
A Material 3 Expressive visual component displaying a Tag's name using a soft tinted background (~14%–18% alpha of the tag's palette color), matching saturated colored text, and subtle tonal border, replacing legacy hashtag text.
_Avoid_: Hashtag chip, Plain badge, Category box

**Encounter**:
A recorded interaction or meeting between the user and a Contact, preserving context, notes, and recency.
_Avoid_: Interaction, Touchpoint, Meeting, Event

**Place**:
A named physical venue or location where Encounters take place.
_Avoid_: Venue, Location, Spot

**FollowUp**:
An actionable stay-in-touch reminder or task tied to a Contact, with a scheduled due date and completion state.
_Avoid_: Task, To-do, Ping

**Subconscious Check-In**:
A zero-configuration, algorithmically inferred prompt surfaced gently in the Home "Keep in Touch" carousel when an active Contact has drifted past their adaptive encounter rhythm (`1.5x` average historical gap clamped to `14..90` days, or `30` days baseline). Unlike concrete dated events, Subconscious Check-Ins never clutter the Living Timeline and are capped to at most 2 ambient suggestions at a time.
_Avoid_: Cadence, Manual Frequency, Overdue SLA, Ping Schedule

**Export Bundle**:
A user-generated, portable archive containing complete representations of relationship data in JSON, individual Markdown notes, and CSV formats.
_Avoid_: Backup dump, Database export, System dump

## Capture & Flows

**Capture Flow**:
The step-by-step sequence of inputs presented during contact creation to record relationship data with minimal cognitive load.
_Avoid_: Wizard, Form, Onboarding, Input Funnel

**Flow Preset**:
A named, user-customizable template defining an ordered sequence of Capture Components tailored to a specific social context (e.g., Conference, Casual Party, Campus).
_Avoid_: Template, Profile, Form Schema

**Capture Component**:
A modular, self-contained input unit (such as Name, Role, Place, Birthday, or Custom Property) that can be added, reordered, or skipped within a Capture Flow.
_Avoid_: Field, Form Item, Input Widget

## Presentation & Navigation

**Floating Bottom Dock**:
The floating bottom navigation bar that houses the three primary navigation tabs (Home Tab, Timeline Tab, and Relationship Tab) alongside the companion Quick-Add Floating Action Button. Icons are permanently visible across all tabs (outlined when inactive, filled when active), while the active tab's text label expands beside its filled icon inside a constant-width selection pill.
_Avoid_: Navbar, Bottom bar, Tab bar

**Toast Pill**:
A compact, floating tactile capsule (`RoundedCornerShape(24.dp)`) surfaced to deliver lightweight, transient user feedback (such as confirmations, destructive alerts, and undo prompts). It pairs context-aware semantic leading icons (Archive, Delete, Star, CheckCircle, Info) with concise message text and an optional trailing action button (such as Undo), positioned with dynamic adaptive clearance above the Floating Bottom Dock on docked root tabs (`dockHeight + 16.dp`) and above system navigation bars on undocked sub-screens (`navigationBars + 16.dp`), preventing any overlap or obstruction of primary navigation controls.
_Avoid_: Toast, Snackbar, Banner, Flash message, Notification bar

**Undo Action**:
A transient 5-second recovery mechanism surfaced on a Toast Pill following a destructive operation (such as archiving or deleting a Contact or Encounter), allowing the user to restore the entity immediately from an in-memory repository cache without navigation interruptions.
_Avoid_: Revert Button, Restore Dialog, Rollback Trigger

**Timeline Tab**:
The primary navigation tab within the Floating Bottom Dock that presents the app-wide Living Timeline—a continuous vertical spine anchored by a Today divider, combining a Future Horizon (set-in-stone upcoming Birthdays across a 365-day year-round horizon and scheduled FollowUps with a dashed spine) and a Past Log (all recorded Encounters across Contacts, distinguishing First Met from Met Again with a bold continuous spine and vertically centered hollow node dots).
_Avoid_: Activity Feed, History Screen, Global Log, Audit Trail

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
The circular visual identity badge for a Contact across all surfaces, displaying their custom cropped portrait photo when set or falling back to a warm pastel monogram of their initials.
_Avoid_: Profile Picture, User Icon, Thumbnail, Headshot

**Upcoming Alert Card**:
The uniform Material 3 Expressive card surface representing time-sensitive relationship prompts (FollowUp, BirthdayAlert, or Subconscious Check-In) in the Upcoming feed, featuring a 40dp tonal icon badge, strict 1-line text truncation, and bottom-aligned action buttons ("Later" / "Update" for Subconscious Check-Ins).
_Avoid_: Reminder Card, Ping Tile, Event Box, Alert Banner

**Birthday Celebration Card**:
An ambient, high-visibility celebration card surfaced in Home Recent Encounters and the Living Timeline when today matches a Contact's birthday. It highlights the milestone with celebratory styling without generating synthetic fake records in SQLite, providing a one-tap shortcut to pre-fill an encounter note ("Celebrated [Name]'s Birthday 🎉").
_Avoid_: Birthday Alert Banner, Auto-Encounter, Birthday Notification

**Contact Selection Mode**:
The contextual interaction phase entered via long-press on a Contact Card within the Relationship Tab, enabling multi-contact batch actions (Share via vCard, Delete with confirmation, Select All). During this mode, selected Contact Cards visually decouple from connected groupings into independent fully-rounded surfaces with theme-aware red tinting and an avatar selection checkmark overlay, while a floating vertical action tray docks on the right edge and the Floating Bottom Dock recedes off-screen.
_Avoid_: Edit Mode, Multi-select checkbox mode, Batch screen

**Timeline Selection Mode**:
The contextual interaction phase entered via long-press on an Encounter or FollowUp card within any Living Timeline surface (the global Timeline Tab and the Contact Profile Screen), enabling multi-item deletion with confirmation and Select All. During this mode, selected cards render with theme-aware error container red tinting and an avatar selection checkmark overlay, a floating vertical action tray docks on the right edge with Delete and Kebab (Select All), and the top header transforms into a selection bar with cancel affordance. Birthday milestone nodes remain non-selectable, and deleting encounters dynamically recalculates affected contacts' meet count and recency states.
_Avoid_: Timeline edit mode, Event delete mode, History multi-select

**Contact Profile Screen**:
The dedicated Material 3 Expressive view displaying a Contact's relationship properties, tags, notes, and the Living Timeline (with a pinned Birthday Milestone node at the top when a birthday is set, followed by past encounters), intentionally stripped of manual cadence status cards and dialer/messaging quick actions to emphasize organic relationship context.
_Avoid_: User Profile, Person Detail, Contact Detail Screen, Address Book Entry
