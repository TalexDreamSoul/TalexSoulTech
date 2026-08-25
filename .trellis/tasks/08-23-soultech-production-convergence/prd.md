# SoulTech production convergence

## Goal

Converge the already implemented Paper runtime, gameplay systems, resource pack, Cloudflare SaaS, and extension host into one traceable production baseline. The next milestone is not more content; it is a release with rollback proof and a real-player acceptance loop.

## Confirmed baseline

| Area | Confirmed state | Production status |
|---|---|---|
| Paper runtime | Migrated to Paper `26.1.2 build 74` and Java 25; legacy NMS/material/NBT paths were replaced with supported APIs. | An earlier build was enabled and healthy on `wlcb1`. |
| Player persistence | Optional MySQL persistence uses a bounded single-thread queue, UUID-parameterized load/save, read-only fallback after failed loads, a 5-second shutdown bound, and a data-folder file lock. Disabled MySQL is explicitly in-memory only. | Earlier server smoke confirmed table creation and an enabled plugin. |
| Wilderness v2 | New chunks use deterministic bounded ore candidates; Chunk PDC stores a v2 position/consumption index with at most eight entries. Runtime identity does not depend on the current world list or generation settings. | Committed and pushed; deployment is not established by the current Trellis record. |
| Electricity and multiblocks | No-voltage milli-SE grid, bounded buffers, main-thread settlement, cached residual routes, 3x3x3/5x5x5 structures, UUID ownership, atomic inventory transactions, and 30 machines across four disciplines. | 38 tests, Java 25 package, and isolated Paper startup/cycle/shutdown smoke passed. This final artifact was not deployed in that session. |
| Web and SaaS | `https://soultech.tagzxia.com` serves the 27-discipline/810-item catalog and a multi-tenant Cloudflare control plane backed by D1 and `SyncCoordinator`/`AuthRateLimiter` Durable Objects. | Live at the custom domain. |
| Resource pack | Paper 26 string-selector CustomModelData pack is generated deterministically by `site/scripts/prepare-assets.mjs`; the recorded pack SHA-256 is `20f8d355ea8906864cc324f0c93a1be4a9286abefd2ab9e2b982360807691d86`. | Included in the site/resource-pack work; the production server/client handshake must be rechecked during convergence. |
| Cloud extensions | `/tst cloud ...` and `/tst ext ...`, exact-body outbox, bounded LuaJ/Rhino host, dependency ordering, LIFO disposal, and last-known-good recovery exist. | Current extension and site files contain uncommitted concurrent work and are not a releasable baseline yet. |

## Repository state captured on 2026-08-24

- Branch: `main`.
- Recent work commits: `fa95e52` (`modernize TalexSoulTech runtime and gameplay`) and `f946cf2` (`add SoulTech control site and resource pack`).
- Trellis reported 17 uncommitted changes, including `site/`, `extensions/`, and agent configuration paths. Treat every one as concurrent user/other-session work until ownership and release scope are established; never reset or overwrite them.
- `00-bootstrap-guidelines` remains in progress. The generated backend spec templates are not yet an authoritative description of the whole repository.

## Requirements

### R1 — Establish one owned release scope

- Attribute every dirty path to its owning task/session before editing or staging it.
- Decide whether the current `site/` and `extensions/` changes belong to this release.
- Preserve unrelated changes exactly; no reset, checkout-overwrite, or broad formatting.

### R2 — Produce immutable, mutually compatible artifacts

- Build the plugin with Java 25 and generate the resource pack/manifest from the same source revision.
- Record commit, plugin JAR SHA-256, resource-pack SHA-256, migration set, and Worker revision in one release receipt.
- Do not publish a manifest that points at a different JAR or pack than the server release.

### R3 — Release cloud before the dependent plugin

- If API/schema behavior changed, apply remote D1 migrations first, deploy Worker second, and pass live SaaS E2E before touching the Paper server.
- Preserve tenant isolation: one administrator may own multiple servers, but server data, API keys, sequences, extensions, and snapshots remain server-scoped.
- Keep dynamic credentials out of commits, logs, task metadata, and browser-visible responses.

### R4 — Cut over Paper atomically and retain rollback

- Deploy the exact recorded JAR and resource pack to `wlcb1` only after cloud verification.
- Retain the previous working artifacts and configuration for a one-step rollback.
- Verify Paper/RCON enablement, scheduled electricity cycles, persistence mode, cloud sync, extension activation, and graceful shutdown/restart behavior.

### R5 — Run a real-player acceptance loop before adding content

- A player must receive/open the guide, understand and use clickable command help, accept the resource pack, and see custom models.
- A player must mine one indexed wilderness ore and receive only the configured custom reward, not the carrier block.
- A player must build, power, stop, break, reload, and rebuild representative 3x3x3 and 5x5x5 machines without duplicate energy/items or orphaned claims.
- A linked server must appear in the SaaS console with correctly isolated, monotonically ordered snapshots.
- One extension must install, run a command/event/schedule, survive a valid reload, and remain on last-known-good after an invalid update.

### R6 — Balance from observed play, not machine count

- Freeze new disciplines and machines for this task.
- Record time-to-first-power, time-to-first-processed-ingot, energy surplus/deficit, recipe bottlenecks, and failure points from at least one complete player run.
- Change rates only after the observed report identifies a concrete bottleneck.

### R7 — Leave Trellis authoritative

- Update applicable code-specs with final runtime/release contracts and validation evidence.
- Record the release or blocked outcome in the developer journal.
- Archive this task only after acceptance criteria are met and work commits already exist; Trellis closeout must not create application changes.

## Acceptance criteria

- [ ] Every pre-existing dirty path has an owner and an explicit include/exclude decision.
- [ ] Java 25 `mvn -B -ntp test` and `mvn -B -ntp package` pass from the release source.
- [ ] The release receipt records source commit, JAR hash, resource-pack hash, D1 migrations, and Worker revision.
- [ ] Remote migrations and Worker deployment pass live tenant-isolation and sequence-order E2E checks.
- [ ] The exact recorded JAR is atomically deployed to `wlcb1`; Paper/RCON smoke and rollback proof pass.
- [ ] Guide/help, resource pack, wilderness mining, representative multiblocks, cloud sync, and extension LKG are exercised by a real player/client.
- [ ] A balance report contains observed timings and identifies either a justified tuning change or a no-change decision.
- [ ] Trellis specs and journal reflect the final outcome; no completion claim relies only on unit tests.

## Out of scope

- Adding more disciplines, machines, materials, mobs, or storage tiers.
- Replacing MySQL with Redis or PostgreSQL without a separately approved data-migration task.
- Redesigning the public site or extension API beyond defects required for the convergence acceptance loop.
- Deploying production changes before the user approves the release window and scope.

## Open decisions before `task.py start`

1. Which current uncommitted `site/` and `extensions/` changes are included in the candidate release?
2. What production release window and acceptable rollback interruption should be used?
3. What player-time targets define acceptable early-game balance for the first powered production chain?
