# Hello Domain Model

Local-first personal CRM and relationship keeper for Android, helping users cultivate meaningful personal and professional relationships through lightweight tracking and proactive cadences.

## Relationships

**Contact**:
An individual with whom the user maintains a personal or professional relationship.
_Avoid_: Person, Connection, Client, Account

**Relationship Terminology**:
The user's chosen display nomenclature (People, Friends, Connections, or Contacts) applied across the presentation layer to describe Contacts according to personal mental models.
_Avoid_: User Role, Category Type, Custom Entity

**Circle**:
A relationship sphere or community grouping (e.g., Work, Friends, Family, Uni) used to categorize contacts and manage context. Contacts can belong to multiple Circles simultaneously (multi-sphere membership). When a contact belongs to multiple Circles with differing cadences, the most frequent (shortest) cadence governs reminder frequency.
_Avoid_: Group, Category, Folder

**Tag**:
A lightweight freeform label attached to Contacts to support cross-cutting categorization and ad-hoc filtering. Contacts can have multiple tags, which sit alongside Circles in unified filter strips.
_Avoid_: Category, Label, Folder

**Encounter**:
A recorded interaction or meeting between the user and a Contact, preserving context, notes, and recency.
_Avoid_: Interaction, Touchpoint, Meeting, Event

**Place**:
A named physical venue or location where Encounters take place.
_Avoid_: Venue, Location, Spot

**FollowUp**:
An actionable stay-in-touch reminder or task tied to a Contact, with a scheduled due date and completion state.
_Avoid_: Task, To-do, Ping

**Cadence**:
A target stay-in-touch frequency established for a Contact or Circle to trigger proactive reconnection reminders when overdue.
_Avoid_: Interval, Frequency, Ping schedule

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

**Timeline Tab**:
The primary navigation tab within the Floating Bottom Dock that presents the app-wide Living Timeline—a continuous vertical spine anchored by a Today divider, combining a Future Horizon (upcoming Birthdays, scheduled FollowUps, and projected Cadence reconnections with a dashed spine) and a Past Log (all recorded Encounters across Contacts, distinguishing First Met from Met Again with a bold continuous spine and vertically centered hollow node dots).
_Avoid_: Activity Feed, History Screen, Global Log, Audit Trail

**Timeline Node**:
A chronological waypoint along the Living Timeline representing an Encounter or scheduled touchpoint. Past nodes feature a 12dp hollow circular dot (2dp border, contrasting surface fill) vertically centered alongside the encounter card, connected by a 2dp high-contrast spine with atmospheric alpha gradient fades at entry and exit horizons.
_Avoid_: Event Marker, Dot, Checkpoint

**Ambient Depth Scrim**:
A subtle 12dp vertical gradient (background color fading to transparent) positioned beneath fixed or sticky headers (such as ContactGroupHeader and TimelineHeader) to allow scrolling list content to tuck smoothly underneath without harsh visual cutoff.
_Avoid_: Drop shadow, Box shadow, Divider bar

**Relationship Tab**:
The primary navigation tab within the Floating Bottom Dock that opens the contacts surface, dynamically labeled and styled according to the active Relationship Terminology (People, Friends, Connections, or Contacts).
_Avoid_: People Tab, Contacts Tab, Navbar item

**Contact Card**:
The Material 3 Expressive connected surface representing a Contact across the Relationship Tab and Home Screen, featuring a 56dp circular avatar, prominent name, tonal recency badge, contextual notes preview, and relationship tags.
_Avoid_: People Card, Person Card, Contact Row, Contact Tile

**Upcoming Alert Card**:
The uniform Material 3 Expressive card surface representing time-sensitive relationship prompts (FollowUp, BirthdayAlert, or Cadence) in the Upcoming feed, featuring a 40dp tonal icon badge, strict 1-line text truncation, and bottom-aligned action buttons.
_Avoid_: Reminder Card, Ping Tile, Event Box, Alert Banner

**Contact Selection Mode**:
The contextual interaction phase entered via long-press on a Contact Card within the Relationship Tab, enabling multi-contact batch actions (Share via vCard, Delete with confirmation, Select All). During this mode, selected Contact Cards visually decouple from connected groupings into independent fully-rounded surfaces with theme-aware red tinting and an inline name checkmark, while a floating vertical action tray docks on the right edge and the Floating Bottom Dock recedes off-screen.
_Avoid_: Edit Mode, Multi-select checkbox mode, Batch screen

**Contact Profile Screen**:
The dedicated Material 3 Expressive view displaying a Contact's relationship status, Cadence health, circles, tags, notes, and the living timeline of past encounters, intentionally stripped of dialer/messaging quick actions to emphasize relationship context over utility dialers.
_Avoid_: User Profile, Person Detail, Contact Detail Screen, Address Book Entry
