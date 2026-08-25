# Backend Development Guidelines

> Source-backed guidance for the TalexSoulTech Paper runtime and Cloudflare control plane.

## Required reading

Before backend work, read the files matching the change:

| Guide | Use when changing | Status |
|---|---|---|
| [Directory Structure](./directory-structure.md) | Package/file ownership or new modules | Current |
| [Database Guidelines](./database-guidelines.md) | MySQL, D1, migrations, persistence | Current |
| [Error Handling](./error-handling.md) | Exceptions, API errors, degraded modes | Current |
| [Quality Guidelines](./quality-guidelines.md) | Implementation, review, tests, release | Current |
| [Logging Guidelines](./logging-guidelines.md) | Runtime logs, audit evidence, redaction | Current |
| [System Runtime and Release Invariants](./system-invariants.md) | Electricity, equipment, multiblocks, wilderness, cloud, release | Authoritative |

Also read the shared [thinking guides](../guides/index.md) for cross-layer and reuse checks.

## Pre-development checklist

- Identify whether the code is pure domain, Bukkit main-thread boundary, persistence worker, Worker API, or SSR/static UI.
- Read `system-invariants.md` for any runtime/release change.
- Search for the existing catalog, registry, decoder, migration, or transaction owner before adding one.
- Map lifecycle: enable/load -> active operation -> save/close/rollback.
- Define the observable verification before editing.

## Project baseline

- Java 25, Paper 26.1.2 build 74, Maven.
- Cloudflare Worker + D1 + Durable Objects under `site/`.
- Direct MySQL/JDBC compatibility for optional Paper player persistence.
- One no-voltage `long` milli-SE energy domain.
- Main-thread Bukkit access, bounded generation/event work, fail-closed tenant/security boundaries.
