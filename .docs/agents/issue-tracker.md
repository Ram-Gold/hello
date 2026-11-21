# Issue tracker: Linear (Opt-In)

Issues and specs for **Hello** live in **Linear** via the `linear_linear` MCP server.

- **Team:** `Ramblings` (key: `RAM`, id: `97012d6b-410c-454d-889b-411954b5a012`)
- **Project:** `Hello` (id: `P-RAM-5`, uuid: `9d3e4ad7-a6ec-40cd-8493-3e058bf1e32d`)
- **Workflow Statuses:** `Backlog`, `Todo`, `In Progress`, `In Review`, `Done`, `Canceled`, `Duplicate`

## Activation Rule (Explicit Opt-In)

- **Only interact with Linear when the user explicitly asks or points to it** — e.g. mentioning an issue ID (`RAM-XX`), asking to list/pick up to-dos from **Hello** / Linear, or invoking a tracker skill (`/to-spec`, `/to-tickets`, `/implement`, `/triage`, `/wayfinder`).
- **Never** touch, search, create, or modify Linear issues during ad-hoc prompts that do not reference Linear.

## Automatic Status Transitions (When Working a Referenced Issue)

1. **Starting work:** As soon as you begin implementing a referenced Linear issue, transition its status to **`In Progress`** using `save_issue`.
2. **Finishing a prompt:** Once implementation and verification (unit tests / `assembleDebug`) succeed, transition the issue to **`In Review`** using `save_issue` and post a brief summary comment via `save_comment` (what changed + verification results).
3. **Never auto-close:** Do **not** set an issue to **`Done`** unless the user explicitly asks you to close/complete it.

## Skill Operations

- **Fetch a ticket:** Use `get_issue` (by identifier like `RAM-12`) or `list_issues` filtered by `team: "Ramblings"` and `project: "Hello"`.
- **Publish a spec (`/to-spec`):** Create or update a Linear Document attached to project `Hello` via `save_document` (or a parent issue in `Hello` if the user prefers).
- **Publish tickets (`/to-tickets`):** Create child/linked issues in team `Ramblings`, project `Hello`, status `Todo` via `save_issue`, using native Linear `blockedBy` / `blocks` relations for dependency edges.
- **Wayfinding (`/wayfinder`):** Store the map as a Linear Document in project `Hello` and decision tickets as issues in `Hello` with native blocking relations.

## Local Scratch Fallback

If working offline or when the user explicitly asks for local markdown instead of Linear, fall back to `.scratch/<feature-slug>/` (`spec.md` and `issues/<NN>-<slug>.md`).
