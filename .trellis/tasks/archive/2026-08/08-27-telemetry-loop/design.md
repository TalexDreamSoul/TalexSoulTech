# Telemetry loop — technical design

## Payload contract (frozen, v1)

Optional field `telemetry` added to the existing `/api/sync` snapshot payload (`serverId`, `sequence`, `sentAt`, `server`, `players`, `systems`, `catalog` stay unchanged):

```json
{
  "telemetry": {
    "v": 1,
    "days": [
      {
        "day": "2026-08-27",
        "counters": {
          "produce":         { "industry_refined_ingot": 12 },
          "machine_op":      { "industry_crusher": 4 },
          "tool_use":        { "electric_drill": 33 },
          "charge":          { "total": 7, "station": 5, "wireless": 1, "personal": 1 },
          "session_seconds": { "total": 5400 },
          "unique_players":  { "total": 3 },
          "unlock":          { "industry": 1 }
        }
      }
    ]
  }
}
```

Validation: ≤3 day entries; day is `YYYY-MM-DD`; only the 7 known groups; ≤512 keys/group; key regex `^[a-z0-9_.:\-]{1,64}$` plus literal `__other`; values integer `0..Number.MAX_SAFE_INTEGER`.

## Plugin side (Java)

- New package `pubsher/talexsoultech/telemetry/`:
  - `TelemetryCollector` — singleton owned by the plugin lifecycle. In-memory `Map<day, Map<group, Map<key, long>>>` + per-day bounded UUID set (≤2048). Public API: `produce(itemId, amount)`, `machineOp(machineId)`, `toolUse(equipmentId)`, `charge(kind)`, `sessionSeconds(seconds)`, `playerSeen(uuid)`, `unlock(categoryId)`, `drainForSnapshot()`, `restore(drained)`, `setEnabled(boolean)`.
  - Increment methods: `if (!Bukkit.isPrimaryThread()) return;` — silent drop. Never throw.
- Integration points (single-line hooks; agent verifies exact sites by reading code):
  - Machine commit: the `simulate=false` success path of the shared machine framework (`PoweredMultiblockMachineItem` / `MachineInventoryOps.transform` commit) → `machineOp` + `produce` per output stack.
  - Equipment: `PoweredEquipmentService` action success path → `toolUse`.
  - Charging: charging-station commit, wireless charge delivery, personal charger transfer → `charge`.
  - Guide unlocks: `CategoryManager` (or equivalent guider unlock site) → `unlock`.
  - Join/quit listener + periodic tick already available in the plugin → `playerSeen` and `sessionSeconds` (accumulate on drain: for each online player add `now - lastMark`, update mark; add remainder on quit).
- CloudSync integration: `CloudSyncService.captureSnapshot(...)` already runs on the primary thread (enforced at line ~579). Drain there, embed into the snapshot JSON, and if `outbox.persist(...)` throws, call `restore(drained)`. Sequence-based dedupe on the Worker makes outbox retries safe.
- Config: `telemetry.enabled: true` read wherever the plugin reads its existing config; when disabled the collector no-ops.

## Worker side

- New file `site/src/telemetry.js` (all logic lives here):
  - `validateTelemetry(raw)` → `{ok, value}` or `{ok:false, error}`.
  - `telemetryStatements(env, serverId, telemetry, nowIso)` → array of D1 prepared statements: additive groups use `INSERT ... ON CONFLICT DO UPDATE SET value = value + excluded.value`; `unique_players` uses `MAX(value, excluded.value)`. Deterministic order (day, metric, key), cap 2000, return `{statements, truncated}`.
  - `handleAdminTelemetry(request, env, url)` — reuse the same admin session guard pattern as `/api/admin/status` (agent mirrors the existing auth helper).
- `site/src/worker.js` minimal wiring (only these three edits):
  1. `import { ... } from "./telemetry.js"` next to the existing ssr import.
  2. In the `/api/sync` accepted-new-snapshot path (near the `D1 batch` that inserts `server_snapshots`, lines ~239–272): validate optional `payload.telemetry`; append telemetry statements to the same batch; include `telemetryApplied`/`telemetryTruncated` in the response.
  3. In the admin route block (near `/api/admin/status`, lines ~403–411): dispatch `/api/admin/telemetry`.
- Migration `site/migrations/0005_telemetry.sql`:

```sql
CREATE TABLE IF NOT EXISTS telemetry_daily (
  server_id TEXT NOT NULL,
  day TEXT NOT NULL,
  metric TEXT NOT NULL,
  item_key TEXT NOT NULL,
  value INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL,
  PRIMARY KEY (server_id, day, metric, item_key)
);
CREATE INDEX IF NOT EXISTS idx_telemetry_daily_day ON telemetry_daily(day);
```

## Admin panel

`site/public/admin-telemetry.js` (ES module, no dependencies): on DOMContentLoaded find `#telemetry-panel`, fetch `data-endpoint` with credentials, render: (1) 14-day totals table (rows = days, columns = metrics), (2) top-10 tables for produce/machine_op/tool_use, (3) reporting servers list. Empty state text: `等待生产插件上报`. Errors render inline, never throw.

## Compatibility & rollout

- Additive only: old plugin + new worker → no telemetry rows; new plugin + old worker → field ignored server-side (unknown fields are not in the dedupe hash? — check: the hash at worker line ~244 concatenates the four JSON sections; keep telemetry OUT of that hash so identical snapshots ± telemetry stay deduped by sequence anyway).
- Rollback: revert worker deploy; table is additive; plugin flag `telemetry.enabled: false`.
