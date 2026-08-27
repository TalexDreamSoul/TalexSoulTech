const TELEMETRY_VERSION = 1;
const MAX_TELEMETRY_DAYS = 3;
const MAX_TELEMETRY_KEYS_PER_METRIC = 512;
const MAX_TELEMETRY_STATEMENTS = 2000;
const MAX_TELEMETRY_VALUE = Number.MAX_SAFE_INTEGER;

const TELEMETRY_METRICS = new Set([
  "produce",
  "machine_op",
  "tool_use",
  "charge",
  "session_seconds",
  "unique_players",
  "unlock",
]);
const TELEMETRY_GAUGE_METRICS = new Set(["unique_players"]);
const TELEMETRY_TOP_METRICS = ["produce", "machine_op", "tool_use"];
const TELEMETRY_TOP_PLACEHOLDERS = TELEMETRY_TOP_METRICS.map(() => "?").join(", ");

const TELEMETRY_DAY_PATTERN = /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/;
const TELEMETRY_KEY_PATTERN = /^[a-z0-9_.:-]{1,64}$/;
const TELEMETRY_OVERFLOW_KEY = "__other";
const SERVER_ID_PATTERN = /^srv_[A-Za-z0-9_-]{22}$/;

const DEFAULT_TELEMETRY_QUERY_DAYS = 14;
const MAX_TELEMETRY_QUERY_DAYS = 90;
const MAX_TELEMETRY_TOP_KEYS = 10;
const MAX_TELEMETRY_TOP_SCAN_ROWS = 3000;
const MAX_TELEMETRY_SERVER_ROWS = 100;

const ADDITIVE_UPSERT_SQL = `INSERT INTO telemetry_daily (
  server_id, day, metric, item_key, value, updated_at
) VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT (server_id, day, metric, item_key)
DO UPDATE SET
  value = MIN(telemetry_daily.value + excluded.value, ${MAX_TELEMETRY_VALUE}),
  updated_at = excluded.updated_at`;

const GAUGE_UPSERT_SQL = `INSERT INTO telemetry_daily (
  server_id, day, metric, item_key, value, updated_at
) VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT (server_id, day, metric, item_key)
DO UPDATE SET
  value = MAX(telemetry_daily.value, excluded.value),
  updated_at = excluded.updated_at`;

/**
 * Turns the optional `telemetry` field of a sync payload into D1 statements.
 * Never throws: a malformed telemetry block is skipped so the snapshot still commits.
 */
export function planTelemetry(db, serverId, raw, now) {
  if (raw === undefined) {
    return { present: false, statements: [], applied: false, truncated: false, reason: null };
  }

  const validation = validateTelemetry(raw);
  if (!validation.ok) {
    return { present: true, statements: [], applied: false, truncated: false, reason: validation.reason };
  }

  const built = telemetryStatements(db, serverId, validation.value, now);
  return {
    present: true,
    statements: built.statements,
    applied: true,
    truncated: built.truncated,
    reason: null,
  };
}

/** Sync response fields; an absent telemetry block leaves the legacy response untouched. */
export function telemetryResponseFields(plan) {
  if (!plan.present) {
    return {};
  }
  if (!plan.applied) {
    return { telemetryApplied: false, telemetryReason: plan.reason };
  }
  return { telemetryApplied: true, telemetryTruncated: plan.truncated };
}

export async function handleAdminTelemetry(request, env, url, deps) {
  const { ApiError, jsonResponse, requireAuthenticatedUser } = deps;
  const user = await requireAuthenticatedUser(request, env);
  if (user.role !== "admin") {
    throw new ApiError(403, "admin_required", "需要管理员权限");
  }

  const days = parseQueryDays(url.searchParams.get("days"), ApiError);
  const serverId = parseQueryServerId(url.searchParams.get("serverId"), ApiError);
  const toDay = utcDayOffset(0);
  const fromDay = utcDayOffset(days - 1);
  const scopeClause = serverId ? "AND server_id = ?" : "";
  const scopeArgs = serverId ? [serverId] : [];

  const totalRows = await env.DB.prepare(
    `SELECT day, metric, SUM(value) AS total
     FROM telemetry_daily
     WHERE day BETWEEN ? AND ? ${scopeClause}
     GROUP BY day, metric
     ORDER BY day DESC
     LIMIT ?`,
  )
    .bind(fromDay, toDay, ...scopeArgs, days * TELEMETRY_METRICS.size)
    .all();

  const topRows = await env.DB.prepare(
    `SELECT metric, item_key, SUM(value) AS total
     FROM telemetry_daily
     WHERE day BETWEEN ? AND ?
       AND metric IN (${TELEMETRY_TOP_PLACEHOLDERS})
       ${scopeClause}
     GROUP BY metric, item_key
     ORDER BY metric ASC, total DESC, item_key ASC
     LIMIT ?`,
  )
    .bind(fromDay, toDay, ...TELEMETRY_TOP_METRICS, ...scopeArgs, MAX_TELEMETRY_TOP_SCAN_ROWS)
    .all();

  const serverRows = await env.DB.prepare(
    `SELECT server_id, MAX(day) AS last_day
     FROM telemetry_daily
     WHERE day BETWEEN ? AND ? ${scopeClause}
     GROUP BY server_id
     ORDER BY server_id ASC
     LIMIT ?`,
  )
    .bind(fromDay, toDay, ...scopeArgs, MAX_TELEMETRY_SERVER_ROWS)
    .all();

  return jsonResponse({
    days: groupDailyTotals(totalRows.results ?? []),
    top: groupTopKeys(topRows.results ?? []),
    servers: (serverRows.results ?? []).map((row) => ({
      serverId: String(row.server_id),
      lastDay: String(row.last_day),
    })),
  });
}

function validateTelemetry(raw) {
  if (!isPlainObject(raw) || !hasExactKeys(raw, ["v", "days"])) {
    return { ok: false, reason: "invalid_shape" };
  }
  if (raw.v !== TELEMETRY_VERSION) {
    return { ok: false, reason: "unsupported_version" };
  }
  if (!Array.isArray(raw.days) || raw.days.length > MAX_TELEMETRY_DAYS) {
    return { ok: false, reason: "too_many_days" };
  }

  const days = [];
  const seenDays = new Set();

  for (const entry of raw.days) {
    if (!isPlainObject(entry) || !hasExactKeys(entry, ["day", "counters"])) {
      return { ok: false, reason: "invalid_shape" };
    }
    if (!isCalendarDay(entry.day)) {
      return { ok: false, reason: "invalid_day" };
    }
    if (seenDays.has(entry.day)) {
      return { ok: false, reason: "duplicate_day" };
    }
    if (!isPlainObject(entry.counters)) {
      return { ok: false, reason: "invalid_shape" };
    }

    const counters = {};
    for (const [metric, group] of Object.entries(entry.counters)) {
      if (!TELEMETRY_METRICS.has(metric)) {
        return { ok: false, reason: "unknown_metric" };
      }
      if (!isPlainObject(group)) {
        return { ok: false, reason: "invalid_shape" };
      }

      const keys = Object.keys(group);
      if (keys.length > MAX_TELEMETRY_KEYS_PER_METRIC) {
        return { ok: false, reason: "too_many_keys" };
      }

      const values = {};
      for (const key of keys) {
        if (key !== TELEMETRY_OVERFLOW_KEY && !TELEMETRY_KEY_PATTERN.test(key)) {
          return { ok: false, reason: "invalid_key" };
        }
        const value = group[key];
        if (!Number.isSafeInteger(value) || value < 0 || value > MAX_TELEMETRY_VALUE) {
          return { ok: false, reason: "invalid_value" };
        }
        values[key] = value;
      }
      counters[metric] = values;
    }

    seenDays.add(entry.day);
    days.push({ day: entry.day, counters });
  }

  return { ok: true, value: { days } };
}

function telemetryStatements(db, serverId, telemetry, now) {
  const entries = [];
  const orderedDays = [...telemetry.days].sort((left, right) => compareStrings(left.day, right.day));

  for (const entry of orderedDays) {
    for (const metric of Object.keys(entry.counters).sort(compareStrings)) {
      const values = entry.counters[metric];
      for (const key of Object.keys(values).sort(compareStrings)) {
        if (values[key] > 0) {
          entries.push({ day: entry.day, metric, key, value: values[key] });
        }
      }
    }
  }

  const truncated = entries.length > MAX_TELEMETRY_STATEMENTS;
  const statements = entries
    .slice(0, MAX_TELEMETRY_STATEMENTS)
    .map((entry) =>
      db
        .prepare(TELEMETRY_GAUGE_METRICS.has(entry.metric) ? GAUGE_UPSERT_SQL : ADDITIVE_UPSERT_SQL)
        .bind(serverId, entry.day, entry.metric, entry.key, entry.value, now),
    );

  return { statements, truncated };
}

function groupDailyTotals(rows) {
  const byDay = new Map();

  for (const row of rows) {
    const day = String(row.day);
    let totals = byDay.get(day);
    if (!totals) {
      totals = Object.fromEntries([...TELEMETRY_METRICS].map((metric) => [metric, 0]));
      byDay.set(day, totals);
    }
    const metric = String(row.metric);
    if (metric in totals) {
      totals[metric] = Number(row.total ?? 0);
    }
  }

  return [...byDay.entries()].map(([day, totals]) => ({ day, totals }));
}

function groupTopKeys(rows) {
  const top = Object.fromEntries(TELEMETRY_TOP_METRICS.map((metric) => [metric, []]));

  for (const row of rows) {
    const bucket = top[String(row.metric)];
    if (bucket && bucket.length < MAX_TELEMETRY_TOP_KEYS) {
      bucket.push({ key: String(row.item_key), value: Number(row.total ?? 0) });
    }
  }

  return top;
}

function parseQueryDays(raw, ApiError) {
  if (raw === null) {
    return DEFAULT_TELEMETRY_QUERY_DAYS;
  }
  const days = /^[0-9]{1,3}$/.test(raw) ? Number(raw) : Number.NaN;
  if (!Number.isInteger(days) || days < 1 || days > MAX_TELEMETRY_QUERY_DAYS) {
    throw new ApiError(400, "invalid_telemetry_range", "days 需为 1 至 90 的整数");
  }
  return days;
}

function parseQueryServerId(raw, ApiError) {
  if (raw === null) {
    return null;
  }
  if (!SERVER_ID_PATTERN.test(raw)) {
    throw new ApiError(400, "invalid_server_id", "服务器标识格式不正确");
  }
  return raw;
}

function utcDayOffset(offsetDays) {
  const now = new Date();
  const midnight = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return new Date(midnight - offsetDays * 86_400_000).toISOString().slice(0, 10);
}

function isCalendarDay(value) {
  if (typeof value !== "string" || !TELEMETRY_DAY_PATTERN.test(value)) {
    return false;
  }
  const parsed = Date.parse(`${value}T00:00:00.000Z`);
  return Number.isFinite(parsed) && new Date(parsed).toISOString().slice(0, 10) === value;
}

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function hasExactKeys(value, expectedKeys) {
  const keys = Object.keys(value);
  return keys.length === expectedKeys.length && expectedKeys.every((key) => Object.hasOwn(value, key));
}

function compareStrings(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}
