# Telemetry loop — execution plan

Two independent tracks (may run in parallel; the payload contract in design.md is the interface).

## Track A: Java plugin

- [ ] Read `.trellis/spec/backend/system-invariants.md`, `directory-structure.md`, `quality-guidelines.md`.
- [ ] Implement `telemetry/TelemetryCollector` (bounded, primary-thread-only, day-bucketed, drain/restore).
- [ ] Wire config kill-switch.
- [ ] Hook machine commit, equipment use, charging, unlock, join/quit session tracking (single-line call sites).
- [ ] Integrate drain into `CloudSyncService` snapshot capture + restore on persist failure; embed JSON.
- [ ] Domain tests in `src/test/java/pubsher/talexsoultech/domain/` following existing test style (no Bukkit server needed for collector logic).
- [ ] Validate: `JAVA_HOME=$(mise where java@temurin-25.0.4+101.0.LTS 2>/dev/null) ./mvnw -q package` (if JDK 25 unavailable in-session, compile-check what is possible and flag for orchestrator verification).

## Track B: Worker + admin UI

- [ ] Read `site/src/worker.js` sync + admin auth paths; mirror patterns exactly.
- [ ] `site/migrations/0005_telemetry.sql`.
- [ ] `site/src/telemetry.js` (validate/statements/admin handler).
- [ ] Three minimal `worker.js` wiring edits (import, sync batch, admin dispatch) — nothing else in that file.
- [ ] `site/public/admin-telemetry.js` panel module.
- [ ] Extend `site/test/api-contract.mjs` per prd acceptance list, following the existing test harness style.
- [ ] Validate: `cd site && node --test test/api-contract.mjs`.

## Rollback points

- All changes additive; revert = drop the wiring edits + do not apply migration (or leave table unused).
