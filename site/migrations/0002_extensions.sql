CREATE TABLE server_extensions (
  server_id TEXT NOT NULL,
  extension_id TEXT NOT NULL,
  manifest_json TEXT NOT NULL CHECK (json_valid(manifest_json)),
  source TEXT NOT NULL CHECK (length(CAST(source AS BLOB)) BETWEEN 1 AND 131072),
  sha256 TEXT NOT NULL CHECK (length(sha256) = 64),
  enabled INTEGER NOT NULL DEFAULT 0 CHECK (enabled IN (0, 1)),
  revision INTEGER NOT NULL DEFAULT 1 CHECK (revision >= 1),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  PRIMARY KEY (server_id, extension_id),
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_server_extensions_server_enabled
  ON server_extensions(server_id, enabled, extension_id);
