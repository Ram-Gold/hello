# ADR-0013: Multi-Circle Membership, Pinned Hybrid Chip Strip, and Cadence Arbitration

- **Status:** Accepted
- **Date:** 2026-11-05
- **Context:** Deciding relationship taxonomy structure (Circles vs Tags) during the step-by-step Capture Flow.

---

## 1. Problem & Decision Drivers

In Hello, users capture contacts quickly using a sequential **Capture Flow** (`Name` → `Role` → `Phone` → `Email` → `Place` → `Note`).
We needed to resolve how **Circles** (life spheres) and **Tags** (topics/contexts) interact:
1. Can a contact belong to more than one Circle (e.g., someone who is both a close friend and a colleague)?
2. How should Circles and Tags be picked during the capture flow without interrupting text input speed?
3. If a contact belongs to multiple Circles with differing cadences (e.g. Work @ 14d vs Friends @ 30d), which cadence governs reconnect alerts?

---

## 2. Decisions

### Decision 1: Multi-Circle and Multi-Tag Membership
- **Circles:** Non-exclusive. A Contact can belong to zero, one, or multiple Circles (`Set<Circle>`). Circles start **unselected by default** (`emptySet()`) when opening the capture flow, allowing users to tap only what applies.
- **Tags:** Non-exclusive. A Contact can possess multiple freeform `#tags` (`Set<Tag>`).

### Decision 2: Pinned Hybrid Chip Strip
- Circles and top Tags are **not** forced into an isolated step in the linear funnel.
- Instead, a horizontal **Hybrid Chip Strip** is pinned at the bottom of the capture card:
  - Life Sphere Circles come first (`#work`, `#friends`, `#family`, `#uni`).
  - Top/recently-used topic Tags follow (`#design`, `#coffee`, `#tech`, etc.).
  - A `+ Tag` chip allows instant on-the-fly custom tag entry.
- Users can toggle spheres and tags at any point while typing Name, Role, Phone, Email, Place, or Note.

### Decision 3: Cadence Arbitration (Shortest / Most Frequent Interval Wins)
- When a contact belongs to multiple Circles with distinct cadences:
  - If a specific `cadenceDays` is explicitly set on the contact, it takes precedence.
  - Otherwise, the **shortest (most frequent)** active Circle cadence is applied.
  - *Example:* Belonging to Work (14d) and Friends (30d) results in an effective 14-day cadence, ensuring professional or closer touchpoints are not neglected.

---

## 3. Consequences

### Positive
- **Zero Friction:** Users don't have to pause their typing flow to reach a separate "Categorization" screen.
- **Realistic Relationships:** Modern contacts frequently cross boundaries (coworkers who are friends, campus alumni in family circles).
- **Predictable Cadence:** Prioritizing the tighter cadence prevents overdue follow-ups from slipping through.

### Negative / Tradeoffs
- UI in list views must render multiple sphere badges or unified `#tag` pills cleanly (addressed via Tier 3 single-row tag overflow in `ContactCard`).
