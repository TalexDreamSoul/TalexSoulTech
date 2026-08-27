# Implementation Plan — Full catalog runtime

## Phase A — Freeze and validate content identity

- [x] Add a deterministic exporter from `catalog.js`/`progression.js` to `catalog-runtime.json`.
- [x] Add explicit legacy mapping table for existing SoulTech items, vanilla outputs, recipe/process records and BaseMachine records.
- [x] Generate new runtime IDs with the canonical dotted-to-snake rule.
- [x] Fail fast on missing mappings, duplicate catalog IDs, duplicate runtime/legacy IDs, invalid kinds, unresolved ingredients or stale authoring hash.
- [x] Add manifest-domain tests before runtime construction.

**Rollback:** generated manifest and exporter can be removed without touching existing runtime registration.

## Phase B — Runtime registry and recipes

- [x] Add pure manifest records/loader/validator and a single ContentRegistry.
- [x] Preflight manifest plus existing registry before constructing any new SoulTechItem/RecipeObject/CategoryObject.
- [x] Add ManifestSoulTechItem and ManifestFacilityItem adapters.
- [x] Add explicit typed ingredient resolver and deterministic 3-tier recipes.
- [x] Prove recipe reachability, downstream consumption and no cycles/null ingredients.
- [x] Harden duplicate registration in SoulTechItem, RecipeObject and MachineManager.

**Rollback:** ContentRegistry construction remains behind one lifecycle call; failure leaves legacy registry untouched.

## Phase C — Atomic operations and shared behaviors

- [x] Add pure OperationPlan/receipt/checkpoint/ledger state machines.
- [x] Extend MachineInventoryOps to multi-inventory expected-digest prepare/commit.
- [x] Integrate one bounded FacilityScheduler with the electricity cycle.
- [x] Support SINGLE/3x3/5x5 facilities, owner UUID, loaded-state deferral and persistent checkpoints.
- [x] Implement the fifteen shared behavior kinds with finite inputs, outputs, costs, stop and recovery states.
- [x] Adapt existing powered machine energy-before-commit flow to reservation-aware atomic completion.

**Rollback:** existing powered machines keep their IDs and adapter behavior; new facilities are distinguishable by manifest runtime IDs/state version.

## Phase D — Guide and progression migration

- [x] Build Root -> W1..W9 -> 27 disciplines -> 270 families -> 810 item nodes from the manifest.
- [x] Replace render-time unlock mutation with one atomic tryUnlock service.
- [x] Add versioned canonical unlock/evidence/wave JSON fields and idempotent legacy migration.
- [x] Resolve discipline ancestry for workbench/place/click/list machine gates.
- [x] Preserve all legacy regular/paid/admin unlock access.
- [x] Add graph/reachability/cycle/pagination/evidence replay tests.

## Phase E — Implement every wave

- [x] W1: preserve legacy content, add all missing planned IDs and compatibility fixes (mesh collision, fire rod, wrench, recipe-chain records).
- [x] W2: finite water/soil/compost/plant loops; 81 new IDs plus validated legacy/vanilla mappings.
- [x] W3: bounded defense/construction/no-voltage energy and repair loops.
- [x] W4: owned cargo, routing, checkpoints, contracts, public/private inventory.
- [x] W5: source-bound geology, metallurgy certificates, wear/maintenance/recycling.
- [x] W6: local baseline, finite sites/samples, reactions, pollution capture and retest.
- [x] W7: finite magic, intent/route/load receipts, payload escrow and known-endpoint recovery.
- [x] W8: observation windows, unknown landing, second-anchor creation, finite samples, contamination and return.
- [x] W9: finite snapshots, scheduling without world-time mutation, history, permissions and public services.

Each wave requires all 90 manifest entries, recipes, behavior bindings, state/failure/recovery and pure tests. No wave is marked implemented until its gate passes.

## Phase F — Assets and site convergence

- [x] Generate unique deterministic selectors/models/textures for every new SoulTech runtime item.
- [x] Validate Java/runtime/pack selector closure and vanilla fallbacks.
- [x] Generate runtime-catalog and catalog implementation status from manifest/build evidence.
- [x] Remove stale 150-only/planned-only prose after the corresponding runtime gate passes.
- [x] Keep dotted planning IDs and runtime snake IDs visibly distinct.

## Phase G — Verification

- [x] Java 25 Maven tests/package.
- [x] SSR/API contracts.
- [x] Isolated Paper enable/list/give/guide/recipe/facility/restart smoke.
- [x] Failure injection and recovery for W1–W9.
- [x] Real-client resource-pack batches covering every generated model.
- [x] Mac Work build and real 26.1.2 Fabric/Paper acceptance.
- [x] Compare worktree/source/JAR/pack/runtime-site hashes.

## Phase H — Release candidate

- [x] Freeze one candidate source identity.
- [x] Retain rollback JAR/config/data backup.
- [x] Confirm no online players or obtain explicit restart authorization.
- [x] Only then follow the production-convergence order for Worker, D1, Paper and real-client smoke.

Production deployment is never automatic from implementation completion. This release crossed the separate gate only after explicit user approval, a clean source commit, zero online players, rollback capture, Worker live verification, and exact-artifact checks.

## Release evidence

- Source revision: `67309825d9991cbb3169feeff478365473dbbcda` on `main`/`origin/main`.
- Plugin JAR: `c1b7a1cae5372944219b07df5396496d422d676e09c97a9b41ce547a0e2df8ef`; resource pack: `35e09443836eab46889cb1f485b805c215e9ceaa3cd6f19e46a7f437376b5fff`.
- Cloudflare Worker: `ff98eb0a-4824-4fe8-94d1-cfed6170fe41`; D1 had no unapplied migrations.
- Mac Work: Java 25 package and 76/76 tests, API 1/1, SSR 39/39, two deterministic asset runs, isolated Paper startup/restart, a real W5 state mutation, and a live producer/cable/storage/consumer transfer with measured loss.
- Client assets: 93 in-game batches covered 834 custom models; 832 giveable models plus two dynamic guide models rendered without purple/black missing textures. The guide root rendered 9 waves and the machine entry.
- Production: `wlcb1` had zero online players; the previous JAR/config/server properties were backed up, the JAR was atomically replaced, Paper returned healthy, RCON reported 926 runtime records and 301 facilities, cloud sync succeeded after restart, and quarantine remained empty.
- Rollback JAR: `/opt/minecraft/rollback/TalexSoulTech-3.0.0-SNAPSHOT-20260827T025709Z-pre-6730982.jar` (`3aa8dfbe3a487a977de844fb3d286913e6ab0d66c6866e73d5f377904a05c7bb`).
