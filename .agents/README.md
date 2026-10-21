# Workspace Agent Customizations

This folder contains project-specific customizations for Antigravity agents working in this repository.

## Directory Structure

```text
.agents/
├── skills/                     # Project-specific agent skills
│   └── android-workflows/     # Runbooks for Gradle, unit tests, building, and linting
│       └── SKILL.md
└── rules/                      # Contextual coding rules & guidelines
    └── android-conventions.md  # Kotlin & Jetpack Compose best practices
```

## How It Works in Antigravity

- **Skills** (`.agents/skills/<name>/SKILL.md`): Discovered automatically by Antigravity and loaded progressively when relevant workflows (testing, building, debugging) are triggered.
- **Rules** (`.agents/rules/*.md`): Loaded automatically to guide the agent on codebase conventions, architectural patterns, and quality requirements.
- **Tracking & Architecture**: Global engineering flows (`/grill-with-docs`, `/wayfinder`, `/tdd`, etc.) use the local markdown issue tracker defined in `AGENTS.md` and `../.docs/agents/`.
