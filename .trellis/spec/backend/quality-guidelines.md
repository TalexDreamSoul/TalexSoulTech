# Quality Guidelines

> Correctness and bounded Paper behavior come before abstraction or coverage counts.

## Forbidden patterns

- Async access to Bukkit worlds, chunks, blocks, inventories, entities, sounds, particles, or UI.
- Forced chunk loading from machine/wilderness history paths.
- Per-item/per-machine scheduler creation when one bounded service cycle suffices.
- Direct `Block#setType(AIR)` for area tools; use accepted Paper break events and guarded `Player.breakBlock`.
- `double` for settled energy. Energy is non-negative `long` milli-SE.
- Duplicate registries, silent ID overwrite, forward upgrade dependencies, or lore-only catalog entries.
- New string-NBT charge fields, secrets in source/config defaults, or writable fallback after persistence failure.
- Cloud queries without tenant ownership predicates.

## Required patterns

- Main-thread ownership assertions at Bukkit service boundaries.
- Bounded loops and explicit caps for chunks, blocks, entities, players, queues, payloads, source, and KV.
- Simulate-then-commit inventory/energy transactions.
- UUID ownership for persisted machines, structures, players, and transient state.
- Fail-fast catalog/schema/config validation before partial registration.
- Clean lifecycle teardown: cancel tasks, revoke only owned state, dispose extensions LIFO, save before clearing registries.

## Tests

Use the smallest proof that defends an observable contract:

- Pure Java tests for energy conservation, fairness, topology, catalog shape, multiblock occupancy, wilderness determinism, and portable arithmetic.
- Extension tests for staging, sandbox capabilities, KV atomicity, rollback, corruption recovery, and LKG.
- Site API tests for auth, quotas, tenant isolation, pairing, sequence monotonicity, extension CRUD, and redaction.
- SSR tests for route privacy/cache/indexing and catalog rendering.
- Paper smoke for load, cycle stability, command registration, PDC migration, and graceful stop.
- Real-client checks for visuals, input/UI, placement/break/drop, protected containers, and resource-pack fallback.

Required release commands:

```sh
docker run --rm --volume "$PWD:/workspace" --workdir /workspace \
  maven:3.9-eclipse-temurin-25 mvn -B -ntp package

cd site
npm run test:ssr
npm run test:api   # with the local Worker on 127.0.0.1:8788
```

## Review checklist

- Trace every exported/registered call site and lifecycle owner.
- Verify cancellation/protection/ownership before mutation and charging.
- Check unloaded chunks and reload/disable transitions.
- Check overflow, negative values, full output inventories, duplicate IDs, and stale sequences.
- Separate unrelated pre-existing warnings/failures from the change under review.
- For a release, compare exact JAR/resource-pack/manifest/server hashes and retain rollback material.

## Definition of done

Compilation alone is not proof. The changed path must run end to end at the appropriate boundary, and every acceptance claim must point to observed output, a behavior test, or a real-client/runtime check.
