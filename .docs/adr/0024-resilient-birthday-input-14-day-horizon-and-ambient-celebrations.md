# 0024. Resilient Birthday Input, 14-Day Horizon, and Ambient Celebrations

Date: 2026-11-08
Status: Accepted

## Context

Birthday tracking in Hello suffered from input brittleness and inconsistent horizon visibility:
1. **Input Brittle & Parsing Reset:** In `PersonEditScreen` and `FlowAAddContactCard`, parsing `LocalDate.parse(input)` directly within `onValueChange` caused any partial keystroke (such as typing the first digit of month or year) to throw `DateTimeParseException` and evaluate to `null`, instantly resetting the text field and preventing typing.
2. **Format Ambiguity:** Birthdays were labeled `YYYY-MM-DD`, whereas users expect the standard, human-friendly `Month-Day-Year` (`MM-DD-YYYY` or `MM-DD`) format with optional birth year.
3. **Horizon Confusion:** Home Upcoming alerts displayed birthdays up to 30 days out, diluting near-term urgency.
4. **Day-of-Birthday Celebration Gap:** On the exact day of a contact's birthday, there was no prominent ambient celebration presence in the Home Recent Encounters list or Living Timeline with a natural bridge to record a shared encounter.

## Decision

1. **Dedicated Clickable Calendar Field (No IME / Keyboard):**
   - Eliminate text-typing brittleness entirely by making the birthday input a dedicated clickable field.
   - Tapping anywhere on the field (in `PersonEditScreen`) or on the prominent calendar card (in `FlowAAddContactCard`) opens the Material 3 `DatePickerDialog` directly without triggering the soft keyboard.
   - Display the chosen birthday formatted in clean, human-readable format (e.g. `Nov 6, 1996` or `November 6, 1996`), accompanied by a cake/calendar leading icon and a trailing clear button.
   - Restrict calendar date selection to past and today dates only via `SelectableDates`, preventing accidental future birthdates.
   - Guard leap-year calculations against non-leap year crashes (e.g. Feb 29 mapped to Feb 28 in non-leap years).
2. **14-Day Upcoming Alert Horizon:**
   - Restrict Home Screen `UpcomingAlert.BirthdayAlert` generation to within `0..14` days (`daysUntil in 0..14`) to keep the "Keep in Touch" carousel focused on actionable near-term milestones.
   - Retain the full 365-day year-round horizon on the Living Timeline (`TimelineViewModel`) as defined in ADR-0019.
3. **Ambient Birthday Celebration Cards:**
   - On the contact's actual birthday (`daysUntil == 0L`):
     - Surface a prominent `Birthday Celebration Card` in Home Recent Encounters and at the `Today!` anchor on the Living Timeline.
     - Include a one-tap action `"Log Encounter"` that launches the Quick Add encounter sheet pre-filled with `"Celebrated [Name]'s Birthday 🎉"`, avoiding artificial synthetic records while making relationship logging frictionless.

## Consequences

- **Positive:** Users can type or select birthdays effortlessly without text erasure, observe imminent birthdays 14 days in advance, and record celebrations on the day of the event with 1 tap.
- **Negative:** None. Fully backwards-compatible with existing ISO `LocalDate` storage in Room (`ContactEntity`).

