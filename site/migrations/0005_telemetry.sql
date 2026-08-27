CREATE TABLE IF NOT EXISTS telemetry_daily (
  server_id TEXT NOT NULL,
  day TEXT NOT NULL CHECK (length(day) = 10),
  metric TEXT NOT NULL CHECK (length(metric) BETWEEN 1 AND 32),
  item_key TEXT NOT NULL CHECK (length(item_key) BETWEEN 1 AND 64),
  value INTEGER NOT NULL DEFAULT 0 CHECK (value BETWEEN 0 AND 9007199254740991),
  updated_at TEXT NOT NULL,
  PRIMARY KEY (server_id, day, metric, item_key),
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX IF NOT EXISTS idx_telemetry_daily_day_metric ON telemetry_daily(day, metric);
