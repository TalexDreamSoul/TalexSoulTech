# Player telemetry closed loop

## Goal

Give the owner real gameplay data for balance decisions (the journal-recorded blocker): bounded aggregate counters collected in the Paper plugin, shipped through the existing CloudSync snapshot channel, stored in D1, and rendered on the `/admin` dashboard.

## Requirements

1. **Metrics (v1, fixed set — no other groups accepted):**
   - `produce` — items produced by machine commits, keyed by runtime item id.
   - `machine_op` — completed machine operations, keyed by machine id.
   - `tool_use` — powered-equipment actions, keyed by equipment id.
   - `charge` — charge events: keys `total`, `station`, `wireless`, `personal`.
   - `session_seconds` — key `total`, accumulated online seconds (delta per drain).
   - `unique_players` — key `total`, distinct players seen today (gauge, not delta).
   - `unlock` — guide category/wave unlocks, keyed by category id.
2. Day-bucketed by UTC date. Plugin keeps at most 3 day buckets; older buckets are dropped.
3. Bounded memory: ≤512 keys per metric group; overflow increments key `__other`. Keys must match `^[a-z0-9_.:\-]{1,64}$`.
4. All increments happen on the primary thread only; off-thread calls are dropped (never throw, never queue).
5. Delivery piggybacks the existing CloudSync snapshot: counters are drained into the snapshot payload on the primary thread during capture; on outbox persist failure the drained counts are restored (no loss, no double count). Worker-side sequence dedupe already guarantees a retried snapshot is not double-applied.
6. `unique_players` is drained as the current day-set size WITHOUT resetting the set (worker applies MAX); all other metrics reset to zero on successful drain (worker applies ADD).
7. Backward/forward compatible: `telemetry` field in the sync payload is optional; old plugins and old workers interoperate with new ones.
8. Config kill-switch `telemetry.enabled` (default `true`) in the plugin config; disabled means collector is a no-op.
9. Worker: D1 table `telemetry_daily(server_id, day, metric, item_key, value, updated_at)` with PK `(server_id, day, metric, item_key)`; ≤2000 entries applied per sync (deterministic order, response flags truncation); applied atomically with the snapshot insert.
10. Admin API `GET /api/admin/telemetry?days=N&serverId=X` (default 14, max 90) behind the existing admin session auth: per-day totals per metric, top-10 keys for `produce`/`machine_op`/`tool_use`, list of reporting servers.
11. Admin UI: `site/public/admin-telemetry.js` renders into `#telemetry-panel` (contract: section exists on `/admin` with `data-endpoint`) — plain tables per TDS rules, explicit empty state ("等待生产插件上报"), no chart library.
12. No per-player data leaves the server; only aggregate counts. Player UUIDs stay in plugin memory (bounded set ≤2048/day) and are never serialized.

## Acceptance Criteria

- [ ] Java tests: cap enforcement + `__other` overflow, day rollover, drain/restore-on-failure atomicity, unique-players gauge semantics, off-thread drop, disabled config no-op, payload JSON shape.
- [ ] Worker tests (extend `test/api-contract.mjs`): ingest happy path, idempotent replay of same sequence, truncation flag, gauge MAX vs additive ADD, unknown metric group rejected, admin endpoint auth-gated.
- [ ] Existing sync behavior unchanged when `telemetry` is absent (old-plugin compatibility).
- [ ] `./mvnw package` green on JDK 25; `node --test site/test/api-contract.mjs` green.
- [ ] Migration `0005_telemetry.sql` applies cleanly on a fresh local D1.

## Non-Goals

- Per-player funnels or time-to-acquire percentiles (v2 — requires per-player state; deliberately deferred).
- Charts/graphs on the dashboard; tables only.
- A second network channel — CloudSync outbox is the only transport.
