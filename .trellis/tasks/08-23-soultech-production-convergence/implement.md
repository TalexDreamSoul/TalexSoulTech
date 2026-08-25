# Production convergence implementation plan

Do not start this plan until the owner reviews `prd.md`, resolves its open decisions, and runs `task.py start`.

## 1. Reconcile concurrent work

- [ ] Re-run `get_context.py --mode record`.
- [ ] Attribute every dirty path; leave unrelated work untouched.
- [ ] Define the exact source revision and include/exclude set.
- [ ] Review the candidate as one cross-layer contract; do not merge generated artifacts from another revision.

**Gate:** no ambiguous dirty file remains in release scope.

## 2. Prepare immutable artifacts

- [ ] Run Java 25 `mvn -B -ntp test`.
- [ ] Run Java 25 `mvn -B -ntp package`.
- [ ] Generate the resource pack and download manifest with `site/scripts/prepare-assets.mjs`.
- [ ] Run the existing site/API contract checks that cover the candidate.
- [ ] Record commit, JAR SHA-256, resource-pack SHA-256, manifest hash, migrations, and candidate Worker revision.

**Gate:** all artifacts derive from one source revision and hashes are recorded.

## 3. Release the cloud surface

- [ ] Apply only unapplied ordered migrations under `site/migrations/` to remote D1.
- [ ] Deploy the exact Worker candidate from `site/wrangler.jsonc`.
- [ ] Exercise live authentication, tenant isolation, pairing, ordered snapshots, extension CRUD, downloads, and cache behavior.
- [ ] Stop on any cross-tenant response, stale schema, sequence regression, or credential exposure.

**Rollback:** restore the previous Worker revision. D1 changes must be additive or have an explicitly reviewed forward repair.

## 4. Release the Paper surface

- [ ] Back up the currently running JAR and resource-pack identity.
- [ ] Atomically replace the plugin with the recorded JAR.
- [ ] Restart and verify container health, Paper version, `TalexSoulTech [ENABLED]`, RCON command responses, configured persistence mode, cloud link, extension status, and absence of cycle errors.
- [ ] Exercise a graceful stop/restart before declaring the runtime stable.

**Rollback:** atomically restore the previous JAR/config pair and repeat health/RCON smoke.

## 5. Real-player acceptance

- [ ] Guide delivery/opening and clickable help.
- [ ] Resource-pack download, hash, custom models, and fallback behavior.
- [ ] Wilderness v2 mining/reward and carrier-block exclusion.
- [ ] Representative 3x3x3 and 5x5x5 construction, energy flow, inventory transaction, reload, break, and rebuild.
- [ ] Multi-tenant server visibility and monotonic cloud snapshots.
- [ ] Valid extension lifecycle and invalid-update last-known-good recovery.
- [ ] Record early-game timing and energy/recipe bottlenecks.

**Gate:** observable player behavior, not source inspection or unit tests alone, satisfies every PRD acceptance criterion.

## 6. Close the task

- [ ] Apply only evidence-backed balance changes, then rerun affected gates.
- [ ] Update `.trellis/spec/` and the developer journal with final evidence and remaining risks.
- [ ] Commit application work only when explicitly requested and following the normal Phase 3.4 flow.
- [ ] Archive the task only after the work commits exist; do not use archive as a substitute for completion.
