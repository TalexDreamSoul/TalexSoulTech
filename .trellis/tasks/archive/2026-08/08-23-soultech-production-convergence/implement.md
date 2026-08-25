# Production convergence implementation plan

Do not start this plan until the owner reviews `prd.md`, resolves its open decisions, and runs `task.py start`.

## 1. Reconcile concurrent work

- [x] Re-run `get_context.py --mode record`.
- [x] Attribute every dirty path; leave unrelated work untouched.
- [x] Define the exact source revision and include/exclude set.
- [x] Review the candidate as one cross-layer contract; do not merge generated artifacts from another revision.

**Gate:** no ambiguous dirty file remains in release scope.

## 2. Prepare immutable artifacts

- [x] Run Java 25 `mvn -B -ntp test`.
- [x] Run Java 25 `mvn -B -ntp package`.
- [x] Generate the resource pack and download manifest with `site/scripts/prepare-assets.mjs`.
- [x] Run the existing site/API contract checks that cover the candidate.
- [x] Record commit, JAR SHA-256, resource-pack SHA-256, manifest hash, migrations, and candidate Worker revision.

**Gate:** all artifacts derive from one source revision and hashes are recorded.

## 3. Release the cloud surface

- [x] Apply only unapplied ordered migrations under `site/migrations/` to remote D1.
- [x] Deploy the exact Worker candidate from `site/wrangler.jsonc`.
- [x] Exercise live authentication, tenant isolation, pairing, ordered snapshots, extension CRUD, downloads, and cache behavior.
- [x] Stop on any cross-tenant response, stale schema, sequence regression, or credential exposure.

**Rollback:** restore the previous Worker revision. D1 changes must be additive or have an explicitly reviewed forward repair.

## 4. Release the Paper surface

- [x] Back up the currently running JAR and resource-pack identity.
- [x] Atomically replace the plugin with the recorded JAR.
- [x] Restart and verify container health, Paper version, `TalexSoulTech [ENABLED]`, RCON command responses, configured persistence mode, cloud link, extension status, and absence of cycle errors.
- [x] Exercise a graceful stop/restart before declaring the runtime stable.

**Rollback:** atomically restore the previous JAR/config pair and repeat health/RCON smoke.

## 5. Real-player acceptance

- [x] Guide delivery/opening and clickable help.
- [x] Resource-pack download, hash, custom models, and fallback behavior.
- [x] Wilderness v2 mining/reward and carrier-block exclusion.
- [x] Representative 3x3x3 and 5x5x5 construction, energy flow, inventory transaction, reload, break, and rebuild.
- [x] Multi-tenant server visibility and monotonic cloud snapshots.
- [x] Valid extension lifecycle and invalid-update last-known-good recovery.
- [x] Record early-game timing and energy/recipe bottlenecks.

**Gate:** observable player behavior, not source inspection or unit tests alone, satisfies every PRD acceptance criterion.

## 6. Close the task

- [x] Apply only evidence-backed balance changes, then rerun affected gates.
- [x] Update `.trellis/spec/` and the developer journal with final evidence and remaining risks.
- [x] Commit application work only when explicitly requested and following the normal Phase 3.4 flow.
- [x] Archive the task only after the work commits exist; do not use archive as a substitute for completion.
