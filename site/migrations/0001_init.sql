
CREATE TABLE users (
  id TEXT PRIMARY KEY CHECK (length(id) = 26),
  username TEXT NOT NULL COLLATE NOCASE UNIQUE CHECK (length(username) BETWEEN 3 AND 32),
  password_hash TEXT NOT NULL CHECK (length(password_hash) BETWEEN 43 AND 128),
  password_salt TEXT NOT NULL CHECK (length(password_salt) BETWEEN 22 AND 128),
  password_iterations INTEGER NOT NULL CHECK (password_iterations BETWEEN 100000 AND 1000000),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
) STRICT;

CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY CHECK (length(token_hash) = 64),
  user_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_sessions_user_expires_at ON sessions(user_id, expires_at);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);

CREATE TABLE servers (
  id TEXT PRIMARY KEY CHECK (length(id) = 26),
  owner_user_id TEXT NOT NULL,
  name TEXT NOT NULL CHECK (length(name) BETWEEN 1 AND 64),
  software_version TEXT CHECK (software_version IS NULL OR length(software_version) BETWEEN 1 AND 64),
  paired_at TEXT,
  last_sequence INTEGER NOT NULL DEFAULT -1 CHECK (last_sequence >= -1),
  last_sync_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_servers_owner_created_at ON servers(owner_user_id, created_at DESC, id DESC);

CREATE TABLE server_api_keys (
  id TEXT PRIMARY KEY CHECK (length(id) = 26),
  server_id TEXT NOT NULL,
  key_hash TEXT NOT NULL UNIQUE CHECK (length(key_hash) = 64),
  created_at TEXT NOT NULL,
  revoked_at TEXT,
  last_used_at TEXT,
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_server_api_keys_server_active ON server_api_keys(server_id, revoked_at);

CREATE TABLE pairing_codes (
  code_hash TEXT PRIMARY KEY CHECK (length(code_hash) = 64),
  server_id TEXT NOT NULL,
  created_by_user_id TEXT NOT NULL,
  claim_token TEXT UNIQUE,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  used_at TEXT,
  revoked_at TEXT,
  CHECK (
    (used_at IS NULL AND claim_token IS NULL)
    OR (used_at IS NOT NULL AND claim_token IS NOT NULL)
  ),
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_pairing_codes_server_active ON pairing_codes(server_id, used_at, revoked_at, expires_at);
CREATE INDEX idx_pairing_codes_expires_at ON pairing_codes(expires_at);

CREATE TABLE server_snapshots (
  id TEXT PRIMARY KEY CHECK (length(id) = 26),
  server_id TEXT NOT NULL,
  sequence INTEGER NOT NULL CHECK (sequence >= 0),
  sent_at TEXT NOT NULL,
  received_at TEXT NOT NULL,
  payload_hash TEXT NOT NULL CHECK (length(payload_hash) = 64),
  server_json TEXT NOT NULL CHECK (json_valid(server_json)),
  players_json TEXT NOT NULL CHECK (json_valid(players_json)),
  systems_json TEXT NOT NULL CHECK (json_valid(systems_json)),
  catalog_json TEXT NOT NULL CHECK (json_valid(catalog_json)),
  UNIQUE (server_id, sequence),
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
) STRICT;

CREATE INDEX idx_server_snapshots_server_received_at ON server_snapshots(server_id, received_at DESC, sequence DESC);

CREATE TABLE server_events (
  id TEXT PRIMARY KEY CHECK (length(id) = 26),
  server_id TEXT NOT NULL,
  actor_type TEXT NOT NULL CHECK (actor_type IN ('user', 'plugin', 'system')),
  actor_user_id TEXT,
  event_type TEXT NOT NULL CHECK (length(event_type) BETWEEN 1 AND 64),
  metadata_json TEXT NOT NULL CHECK (json_valid(metadata_json)),
  created_at TEXT NOT NULL,
  FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
  FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
) STRICT;

CREATE INDEX idx_server_events_server_created_at ON server_events(server_id, created_at DESC, id DESC);
CREATE INDEX idx_server_events_actor_created_at ON server_events(actor_user_id, created_at DESC);
