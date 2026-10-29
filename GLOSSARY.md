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
A relationship sphere or community grouping (e.g., Work, Friends, Family, Uni) used to categorize contacts and manage context.
_Avoid_: Group, Category, Folder

**Tag**:
A lightweight freeform label attached to Contacts to support cross-cutting categorization and ad-hoc filtering.
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

**My Card**:
The user's own shareable digital identity card profile (supporting Work or Social contexts), presenting quick contact details and offline exchange capabilities.
_Avoid_: User Profile, ID Card, Business Card

**Export Bundle**:
A user-generated, portable archive containing complete representations of relationship data in JSON, individual Markdown notes, and CSV formats.
_Avoid_: Backup dump, Database export, System dump

## Presentation & Navigation

**Floating Bottom Dock**:
The floating bottom navigation bar that houses the primary navigation tabs (Home Tab and Relationship Tab) alongside the companion Quick-Add Floating Action Button.
_Avoid_: Navbar, Bottom bar, Tab bar

**Relationship Tab**:
The primary navigation tab within the Floating Bottom Dock that opens the contacts surface, dynamically labeled and styled according to the active Relationship Terminology (People, Friends, Connections, or Contacts).
_Avoid_: People Tab, Contacts Tab, Navbar item
