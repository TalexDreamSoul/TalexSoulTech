CREATE INDEX IF NOT EXISTS idx_pairing_codes_creator_active
  ON pairing_codes(created_by_user_id, used_at, revoked_at, expires_at);

CREATE TRIGGER IF NOT EXISTS enforce_servers_per_user_quota
BEFORE INSERT ON servers
FOR EACH ROW
WHEN (
  SELECT COUNT(*)
  FROM servers
  WHERE owner_user_id = NEW.owner_user_id
) >= 16
BEGIN
  SELECT RAISE(ABORT, 'quota_servers_per_user');
END;

CREATE TRIGGER IF NOT EXISTS enforce_sessions_per_user_quota
BEFORE INSERT ON sessions
FOR EACH ROW
WHEN (
  SELECT COUNT(*)
  FROM sessions
  WHERE user_id = NEW.user_id
    AND expires_at > NEW.created_at
) >= 10
BEGIN
  SELECT RAISE(ABORT, 'quota_sessions_per_user');
END;

CREATE TRIGGER IF NOT EXISTS enforce_pairing_codes_per_user_quota
BEFORE INSERT ON pairing_codes
FOR EACH ROW
WHEN NEW.used_at IS NULL
  AND NEW.revoked_at IS NULL
  AND (
    SELECT COUNT(*)
    FROM pairing_codes
    WHERE created_by_user_id = NEW.created_by_user_id
      AND used_at IS NULL
      AND revoked_at IS NULL
      AND expires_at > NEW.created_at
  ) >= 5
BEGIN
  SELECT RAISE(ABORT, 'quota_pairing_codes_per_user');
END;

CREATE TRIGGER IF NOT EXISTS enforce_extensions_per_server_quota_insert
BEFORE INSERT ON server_extensions
FOR EACH ROW
WHEN NOT EXISTS (
  SELECT 1
  FROM server_extensions
  WHERE server_id = NEW.server_id
    AND extension_id = NEW.extension_id
)
  AND (
    SELECT COUNT(*)
    FROM server_extensions
    WHERE server_id = NEW.server_id
  ) >= 64
BEGIN
  SELECT RAISE(ABORT, 'quota_extensions_per_server');
END;

CREATE TRIGGER IF NOT EXISTS enforce_extensions_per_server_quota_move
BEFORE UPDATE OF server_id ON server_extensions
FOR EACH ROW
WHEN NEW.server_id != OLD.server_id
  AND (
    SELECT COUNT(*)
    FROM server_extensions
    WHERE server_id = NEW.server_id
  ) >= 64
BEGIN
  SELECT RAISE(ABORT, 'quota_extensions_per_server');
END;

CREATE TRIGGER IF NOT EXISTS enforce_extension_source_quota_insert
BEFORE INSERT ON server_extensions
FOR EACH ROW
WHEN (
  SELECT COALESCE(SUM(length(CAST(source AS BLOB))), 0)
  FROM server_extensions
  WHERE server_id = NEW.server_id
) - COALESCE((
  SELECT length(CAST(source AS BLOB))
  FROM server_extensions
  WHERE server_id = NEW.server_id
    AND extension_id = NEW.extension_id
), 0) + length(CAST(NEW.source AS BLOB)) > 2097152
BEGIN
  SELECT RAISE(ABORT, 'quota_extension_source_per_server');
END;

CREATE TRIGGER IF NOT EXISTS enforce_extension_source_quota_update
BEFORE UPDATE OF server_id, extension_id, source ON server_extensions
FOR EACH ROW
WHEN (
  SELECT COALESCE(SUM(length(CAST(source AS BLOB))), 0)
  FROM server_extensions
  WHERE server_id = NEW.server_id
    AND NOT (
      server_id = OLD.server_id
      AND extension_id = OLD.extension_id
    )
) + length(CAST(NEW.source AS BLOB)) > 2097152
BEGIN
  SELECT RAISE(ABORT, 'quota_extension_source_per_server');
END;

CREATE TRIGGER IF NOT EXISTS enforce_strict_extension_id_insert
BEFORE INSERT ON server_extensions
FOR EACH ROW
WHEN length(NEW.extension_id) = 0
  OR length(CAST(NEW.extension_id AS BLOB)) > 64
  OR NEW.extension_id GLOB '*[^a-z0-9-]*'
  OR substr(NEW.extension_id, 1, 1) = '-'
  OR substr(NEW.extension_id, -1, 1) = '-'
  OR instr(NEW.extension_id, '--') > 0
  OR COALESCE(json_type(NEW.manifest_json, '$.id'), '') != 'text'
  OR json_extract(NEW.manifest_json, '$.id') != NEW.extension_id
BEGIN
  SELECT RAISE(ABORT, 'invalid_extension_id');
END;

CREATE TRIGGER IF NOT EXISTS enforce_strict_extension_id_update
BEFORE UPDATE OF extension_id, manifest_json ON server_extensions
FOR EACH ROW
WHEN length(NEW.extension_id) = 0
  OR length(CAST(NEW.extension_id AS BLOB)) > 64
  OR NEW.extension_id GLOB '*[^a-z0-9-]*'
  OR substr(NEW.extension_id, 1, 1) = '-'
  OR substr(NEW.extension_id, -1, 1) = '-'
  OR instr(NEW.extension_id, '--') > 0
  OR COALESCE(json_type(NEW.manifest_json, '$.id'), '') != 'text'
  OR json_extract(NEW.manifest_json, '$.id') != NEW.extension_id
BEGIN
  SELECT RAISE(ABORT, 'invalid_extension_id');
END;

DELETE FROM server_events
WHERE id IN (
  SELECT id
  FROM (
    SELECT
      id,
      ROW_NUMBER() OVER (
        PARTITION BY server_id
        ORDER BY created_at DESC, id DESC
      ) AS retained_position
    FROM server_events
  )
  WHERE retained_position > 1000
);

CREATE TRIGGER IF NOT EXISTS retain_latest_server_events
AFTER INSERT ON server_events
FOR EACH ROW
BEGIN
  DELETE FROM server_events
  WHERE id IN (
    SELECT id
    FROM server_events
    WHERE server_id = NEW.server_id
    ORDER BY created_at DESC, id DESC
    LIMIT -1 OFFSET 1000
  );
END;
