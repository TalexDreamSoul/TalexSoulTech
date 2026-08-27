import { renderSsrRequest } from "./ssr.js";
import { handleAdminTelemetry, planTelemetry, telemetryResponseFields } from "./telemetry.js";

const TEXT_ENCODER = new TextEncoder();
const TEXT_DECODER = new TextDecoder("utf-8", { fatal: true });

const API_PREFIX = "/api/";
const SESSION_COOKIE = "soultech_session";
const SESSION_TTL_SECONDS = 7 * 24 * 60 * 60;
const PAIRING_TTL_MS = 10 * 60 * 1000;
const PBKDF2_ITERATIONS = 100_000;
const AUTH_RATE_LIMIT_WINDOW_MS = 60 * 1000;
const AUTH_RATE_LIMIT_MAX_ATTEMPTS = 10;

const MAX_SERVERS_PER_USER = 16;
const MAX_EXTENSIONS_PER_SERVER = 64;
const MAX_EXTENSION_SOURCE_BYTES_PER_SERVER = 2 * 1024 * 1024;
const MAX_ACTIVE_PAIRING_CODES_PER_USER = 5;
const MAX_SESSIONS_PER_USER = 10;
const DUMMY_PASSWORD_SALT = "AAAAAAAAAAAAAAAAAAAAAA";

const MAX_AUTH_BODY_BYTES = 8 * 1024;
const MAX_SERVER_BODY_BYTES = 8 * 1024;
const MAX_SYNC_BODY_BYTES = 512 * 1024;
const MAX_USERNAME_BYTES = 32;
const MAX_PASSWORD_BYTES = 128;
const MAX_SERVER_NAME_BYTES = 64;
const MAX_SOFTWARE_VERSION_BYTES = 64;
const MAX_SNAPSHOT_DEPTH = 20;
const MAX_SNAPSHOT_COLLECTION_ENTRIES = 10_000;
const MAX_SNAPSHOT_STRING_BYTES = 32 * 1024;
const MAX_EXTENSION_SOURCE_BYTES = 128 * 1024;
const MAX_EXTENSION_BODY_BYTES = MAX_EXTENSION_SOURCE_BYTES + 16 * 1024;
const MAX_EXTENSION_ID_BYTES = 64;
const MAX_EXTENSION_NAME_BYTES = 128;
const MAX_EXTENSION_VERSION_BYTES = 128;
const MAX_EXTENSION_ENTRY_BYTES = 128;
const MAX_EXTENSION_DEPENDENCIES = 64;
const MAX_EXTENSION_PERMISSIONS = 6;

const EXTENSION_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const EXTENSION_ENTRY_PATTERN = /^[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)*$/;
const LUA_EXTENSION_ENTRY_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*$/;
const EXTENSION_ENGINES = new Set(["lua", "javascript"]);
const EXTENSION_PERMISSIONS = new Set([
  "log",
  "schedule",
  "events",
  "commands",
  "kv",
  "catalog",
]);

const PAIRING_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const PAIRING_CODE_PATTERN = /^ST-[A-HJ-NP-Z2-9]{4}(?:-[A-HJ-NP-Z2-9]{4}){3}$/;
const SERVER_ID_PATTERN = /^srv_[A-Za-z0-9_-]{22}$/;
const USER_ID_PATTERN = /^usr_[A-Za-z0-9_-]{22}$/;
const API_KEY_ID_PATTERN = /^key_[A-Za-z0-9_-]{22}$/;
const API_KEY_PATTERN = /^st_live_[A-Za-z0-9_-]{43}$/;
const SHA256_PATTERN = /^[0-9a-f]{64}$/;
const SESSION_TOKEN_PATTERN = /^[A-Za-z0-9_-]{43}$/;

const API_HEADERS = {
  "cache-control": "no-store, max-age=0",
  "content-type": "application/json; charset=UTF-8",
  "referrer-policy": "same-origin",
  "x-content-type-options": "nosniff",
};

class ApiError extends Error {
  constructor(status, code, message, headers = undefined) {
    super(message);
    this.status = status;
    this.code = code;
    this.headers = headers;
  }
}

export class AuthRateLimiter {
  constructor(state) {
    this.state = state;
  }

  async fetch(request) {
    return this.state.blockConcurrencyWhile(async () => {
      try {
        requireMethod(request, "POST");

        const now = Date.now();
        let bucket = await this.state.storage.get("auth-attempts");
        if (
          !bucket ||
          !Number.isSafeInteger(bucket.attempts) ||
          !Number.isSafeInteger(bucket.resetAt) ||
          bucket.resetAt <= now
        ) {
          bucket = { attempts: 0, resetAt: now + AUTH_RATE_LIMIT_WINDOW_MS };
        }

        if (bucket.attempts >= AUTH_RATE_LIMIT_MAX_ATTEMPTS) {
          const retryAfter = Math.max(1, Math.ceil((bucket.resetAt - now) / 1000));
          throw new ApiError(429, "auth_rate_limited", "认证请求过于频繁，请稍后重试", {
            "Retry-After": String(retryAfter),
          });
        }

        bucket.attempts += 1;
        await this.state.storage.put("auth-attempts", bucket);
        await this.state.storage.setAlarm(bucket.resetAt);
        return jsonResponse({ allowed: true, remaining: AUTH_RATE_LIMIT_MAX_ATTEMPTS - bucket.attempts });
      } catch (error) {
        return apiFailure(error);
      }
    });
  }

  async alarm() {
    await this.state.storage.deleteAll();
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname !== "/api" && !url.pathname.startsWith(API_PREFIX)) {
      const ssrResponse = await renderSsrRequest(request, env, url);
      return ssrResponse ?? env.ASSETS.fetch(request);
    }

    try {
      return await handleApiRequest(request, env, url);
    } catch (error) {
      return apiFailure(error);
    }
  },
};

export class SyncCoordinator {
  constructor(state, env) {
    this.state = state;
    this.env = env;
  }

  async fetch(request) {
    return this.state.blockConcurrencyWhile(async () => {
      try {
        requireMethod(request, "POST");
        assertJsonContentType(request);

        const { pathname } = new URL(request.url);
        if (pathname === "/commit") {
          const envelope = await readJsonBody(request, MAX_SYNC_BODY_BYTES + 1024);
          assertKeys(envelope, ["apiKeyId", "keyHash", "payload"]);
          if (!API_KEY_ID_PATTERN.test(envelope.apiKeyId) || !SHA256_PATTERN.test(envelope.keyHash)) {
            throw new ApiError(400, "invalid_internal_request", "内部请求参数不合法");
          }
          const payload = validateSyncPayload(assertObject(envelope.payload));
          return await this.commitSnapshot(payload, {
            id: envelope.apiKeyId,
            keyHash: envelope.keyHash,
          });
        }

        if (pathname === "/extensions/upsert") {
          const envelope = await readJsonBody(request, MAX_EXTENSION_BODY_BYTES + 1024);
          assertKeys(envelope, ["userId", "serverId", "payload"]);
          const identity = validateCoordinatorIdentity(envelope.userId, envelope.serverId);
          const payload = validateExtensionPayload(assertObject(envelope.payload));
          return await commitServerExtensionUpsert(
            this.env,
            identity.userId,
            identity.serverId,
            payload,
          );
        }

        if (pathname === "/extensions/delete") {
          const envelope = await readJsonBody(request, MAX_SERVER_BODY_BYTES);
          assertKeys(envelope, ["userId", "serverId", "extensionId"]);
          const identity = validateCoordinatorIdentity(envelope.userId, envelope.serverId);
          return await commitServerExtensionDelete(
            this.env,
            identity.userId,
            identity.serverId,
            validateExtensionId(envelope.extensionId),
          );
        }

        if (pathname === "/extensions/state") {
          const envelope = await readJsonBody(request, MAX_SERVER_BODY_BYTES);
          assertKeys(envelope, ["userId", "serverId", "extensionId", "enabled"]);
          const identity = validateCoordinatorIdentity(envelope.userId, envelope.serverId);
          if (typeof envelope.enabled !== "boolean") {
            throw new ApiError(400, "invalid_extension_state", "enabled 必须为布尔值");
          }
          return await commitServerExtensionState(
            this.env,
            identity.userId,
            identity.serverId,
            validateExtensionId(envelope.extensionId),
            envelope.enabled,
          );
        }

        throw new ApiError(404, "not_found", "接口不存在");
      } catch (error) {
        return apiFailure(error);
      }
    });
  }

  async commitSnapshot(payload, apiKey) {
    const activeKey = await this.env.DB.prepare(
      `SELECT id
       FROM server_api_keys
       WHERE id = ? AND server_id = ? AND key_hash = ? AND revoked_at IS NULL
       LIMIT 1`,
    )
      .bind(apiKey.id, payload.serverId, apiKey.keyHash)
      .first();

    if (!activeKey) {
      throw new ApiError(401, "invalid_api_key", "API Key 无效");
    }

    const server = await this.env.DB.prepare(
      `SELECT id, last_sequence
       FROM servers
       WHERE id = ?
       LIMIT 1`,
    )
      .bind(payload.serverId)
      .first();

    if (!server) {
      throw new ApiError(404, "server_not_found", "服务器不存在");
    }

    const serverJson = JSON.stringify(payload.server);
    const playersJson = JSON.stringify(payload.players);
    const systemsJson = JSON.stringify(payload.systems);
    const catalogJson = JSON.stringify(payload.catalog);
    const payloadHash = await sha256Hex(
      `snapshot:${payload.serverId}:${payload.sequence}:${payload.sentAt}:${serverJson}:${playersJson}:${systemsJson}:${catalogJson}`,
    );
    const now = nowIso();
    const lastSequence = Number(server.last_sequence);

    if (payload.sequence < lastSequence) {
      throw new ApiError(409, "sequence_reverted", "同步序号不能回退");
    }

    if (payload.sequence === lastSequence) {
      const existing = await this.env.DB.prepare(
        `SELECT payload_hash
         FROM server_snapshots
         WHERE server_id = ? AND sequence = ?
         LIMIT 1`,
      )
        .bind(payload.serverId, payload.sequence)
        .first();

      if (!existing || !constantTimeEqual(existing.payload_hash, payloadHash)) {
        throw new ApiError(409, "sequence_conflict", "同步序号已被不同快照使用");
      }

      const touched = await this.env.DB.prepare(
        `UPDATE server_api_keys
         SET last_used_at = ?
         WHERE id = ? AND server_id = ? AND key_hash = ? AND revoked_at IS NULL`,
      )
        .bind(now, apiKey.id, payload.serverId, apiKey.keyHash)
        .run();
      if (Number(touched.meta.changes) !== 1) {
        throw new ApiError(401, "invalid_api_key", "API Key 无效");
      }

      return jsonResponse({
        accepted: true,
        sequence: payload.sequence,
        serverTime: now,
      });
    }

    const snapshotId = randomIdentifier("snp_");
    const eventId = randomIdentifier("evt_");
    const telemetry = planTelemetry(this.env.DB, payload.serverId, payload.telemetry, now, snapshotId);
    const snapshotStatements = [
      this.env.DB.prepare(
        `INSERT INTO server_snapshots (
          id, server_id, sequence, sent_at, received_at, payload_hash,
          server_json, players_json, systems_json, catalog_json
        )
         SELECT ?, s.id, ?, ?, ?, ?, ?, ?, ?, ?
         FROM servers s
         INNER JOIN server_api_keys k ON k.server_id = s.id
         WHERE s.id = ?
           AND s.last_sequence = ?
           AND k.id = ?
           AND k.key_hash = ?
           AND k.revoked_at IS NULL`,
      ).bind(
        snapshotId,
        payload.sequence,
        payload.sentAt,
        now,
        payloadHash,
        serverJson,
        playersJson,
        systemsJson,
        catalogJson,
        payload.serverId,
        lastSequence,
        apiKey.id,
        apiKey.keyHash,
      ),
      this.env.DB.prepare(
        `UPDATE servers
         SET last_sequence = ?, last_sync_at = ?, updated_at = ?
         WHERE id = ?
           AND last_sequence = ?
           AND EXISTS (
             SELECT 1
             FROM server_api_keys k
             WHERE k.id = ?
               AND k.server_id = servers.id
               AND k.key_hash = ?
               AND k.revoked_at IS NULL
           )`,
      ).bind(
        payload.sequence,
        now,
        now,
        payload.serverId,
        lastSequence,
        apiKey.id,
        apiKey.keyHash,
      ),
      this.env.DB.prepare(
        `INSERT INTO server_events (
          id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
        )
         SELECT ?, s.id, 'plugin', NULL, 'snapshot_accepted', ?, ?
         FROM servers s
         INNER JOIN server_api_keys k ON k.server_id = s.id
         INNER JOIN server_snapshots ss ON ss.server_id = s.id
         WHERE s.id = ?
           AND s.last_sequence = ?
           AND k.id = ?
           AND k.key_hash = ?
           AND k.revoked_at IS NULL
           AND ss.id = ?
           AND ss.payload_hash = ?`,
      ).bind(
        eventId,
        JSON.stringify({ sequence: payload.sequence }),
        now,
        payload.serverId,
        payload.sequence,
        apiKey.id,
        apiKey.keyHash,
        snapshotId,
        payloadHash,
      ),
      this.env.DB.prepare(
        `UPDATE server_api_keys
         SET last_used_at = ?
         WHERE id = ? AND server_id = ? AND key_hash = ? AND revoked_at IS NULL`,
      ).bind(now, apiKey.id, payload.serverId, apiKey.keyHash),
    ];
    const results = await this.env.DB.batch([...snapshotStatements, ...telemetry.statements]);

    if (results.slice(0, snapshotStatements.length).some((result) => Number(result.meta.changes) !== 1)) {
      const keyStillActive = await this.env.DB.prepare(
        `SELECT id
         FROM server_api_keys
         WHERE id = ? AND server_id = ? AND key_hash = ? AND revoked_at IS NULL
         LIMIT 1`,
      )
        .bind(apiKey.id, payload.serverId, apiKey.keyHash)
        .first();
      if (!keyStillActive) {
        throw new ApiError(401, "invalid_api_key", "API Key 无效");
      }
      throw new ApiError(409, "sequence_conflict", "同步序号发生冲突");
    }

    return jsonResponse({
      accepted: true,
      sequence: payload.sequence,
      serverTime: now,
      ...telemetryResponseFields(telemetry),
    });
  }
}

async function handleApiRequest(request, env, url) {
  const { pathname } = url;

  if (pathname === "/api/health") {
    requireMethod(request, "GET");
    await env.DB.prepare("SELECT 1 AS ok").first();
    return jsonResponse({ ok: true, serverTime: nowIso() });
  }

  if (pathname === "/api/admin/status") {
    requireMethod(request, "GET");
    return getAdminStatus(env);
  }

  if (pathname === "/api/admin/summary") {
    requireMethod(request, "GET");
    return getAdminSummary(request, env);
  }

  if (pathname === "/api/admin/telemetry") {
    requireMethod(request, "GET");
    return handleAdminTelemetry(request, env, url, { ApiError, jsonResponse, requireAuthenticatedUser });
  }

  if (pathname === "/api/auth/register") {
    requireMethod(request, "POST");
    return register(request, env);
  }

  if (pathname === "/api/auth/login") {
    requireMethod(request, "POST");
    return login(request, env);
  }

  if (pathname === "/api/auth/logout") {
    requireMethod(request, "POST");
    return logout(request, env);
  }

  if (pathname === "/api/auth/me") {
    requireMethod(request, "GET");
    const user = await requireAuthenticatedUser(request, env);
    return jsonResponse({ user: publicUser(user) });
  }

  if (pathname === "/api/servers") {
    if (request.method === "GET") {
      return listServers(request, env, url);
    }
    if (request.method === "POST") {
      return createServer(request, env);
    }
    throw methodNotAllowed("GET, POST");
  }

  const ownerExtensionRoute = /^\/api\/servers\/([^/]+)\/extensions(?:\/([^/]+)(?:\/(state))?)?$/.exec(
    pathname,
  );
  if (ownerExtensionRoute) {
    const serverId = decodeServerId(ownerExtensionRoute[1]);
    const extensionSegment = ownerExtensionRoute[2];
    const action = ownerExtensionRoute[3];

    if (!extensionSegment) {
      if (request.method === "GET") {
        return listServerExtensions(request, env, serverId);
      }
      if (request.method === "POST") {
        return upsertServerExtension(request, env, serverId);
      }
      throw methodNotAllowed("GET, POST");
    }

    const extensionId = decodeExtensionId(extensionSegment);
    if (!action && request.method === "DELETE") {
      return deleteServerExtension(request, env, serverId, extensionId);
    }
    if (action === "state" && request.method === "POST") {
      return setServerExtensionState(request, env, serverId, extensionId);
    }

    throw methodNotAllowed(action === "state" ? "POST" : "DELETE");
  }

  if (pathname === "/api/extensions/manifest") {
    requireMethod(request, "GET");
    return getExtensionManifest(request, env);
  }

  const extensionSourceRoute = /^\/api\/extensions\/([^/]+)\/source$/.exec(pathname);
  if (extensionSourceRoute) {
    requireMethod(request, "GET");
    return getExtensionSource(request, env, decodeExtensionId(extensionSourceRoute[1]));
  }

  if (pathname === "/api/pair/claim") {
    requireMethod(request, "POST");
    return claimPairingCode(request, env, url);
  }

  const serverRoute = /^\/api\/servers\/([^/]+)(?:\/(pairing|snapshot))?$/.exec(pathname);
  if (serverRoute) {
    const serverId = decodeServerId(serverRoute[1]);
    const action = serverRoute[2];

    if (!action && request.method === "GET") {
      return getServer(request, env, serverId);
    }
    if (action === "pairing" && request.method === "POST") {
      return createPairingCode(request, env, serverId);
    }
    if (action === "snapshot" && request.method === "GET") {
      return getLatestSnapshot(request, env, serverId);
    }

    throw methodNotAllowed(
      action === "pairing" ? "POST" : action === "snapshot" ? "GET" : "GET",
    );
  }

  if (pathname === "/api/sync") {
    requireMethod(request, "POST");
    return syncSnapshot(request, env);
  }

  throw new ApiError(404, "not_found", "接口不存在");
}

async function getAdminStatus(env) {
  const status = await env.DB.prepare(
    `SELECT COUNT(*) AS admin_count
     FROM users
     WHERE role = ?`,
  )
    .bind("admin")
    .first();

  return jsonResponse({ initialized: Number(status?.admin_count ?? 0) > 0 });
}

async function getAdminSummary(request, env) {
  const user = await requireAuthenticatedUser(request, env);
  if (user.role !== "admin") {
    throw new ApiError(403, "admin_required", "需要管理员权限");
  }

  const counts = await env.DB.prepare(
    `SELECT
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM servers) AS servers,
       (SELECT COUNT(*) FROM servers WHERE paired_at IS NOT NULL) AS paired_servers,
       (SELECT COUNT(*) FROM server_snapshots) AS snapshots,
       (SELECT COUNT(*) FROM server_extensions) AS extensions,
       (SELECT COUNT(*) FROM server_extensions WHERE enabled = 1) AS enabled_extensions`,
  ).first();

  return jsonResponse({
    summary: {
      users: Number(counts?.users ?? 0),
      servers: Number(counts?.servers ?? 0),
      pairedServers: Number(counts?.paired_servers ?? 0),
      snapshots: Number(counts?.snapshots ?? 0),
      extensions: Number(counts?.extensions ?? 0),
      enabledExtensions: Number(counts?.enabled_extensions ?? 0),
    },
  });
}

async function register(request, env) {
  const payload = await readMutationJson(request, MAX_AUTH_BODY_BYTES);
  assertKeys(payload, ["username", "password"]);

  const username = validateUsername(payload.username);
  const password = validatePassword(payload.password);
  await enforceAuthRateLimit(request, env);
  const credentials = await hashPassword(password);
  const userId = randomIdentifier("usr_");
  const now = nowIso();
  const session = await newSession(now);

  try {
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO users (
          id, username, password_hash, password_salt, password_iterations, role, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, 'owner', ?, ?)`,
      ).bind(
        userId,
        username,
        credentials.hash,
        credentials.salt,
        PBKDF2_ITERATIONS,
        now,
        now,
      ),
      env.DB.prepare(
        `INSERT INTO sessions (token_hash, user_id, created_at, expires_at)
         VALUES (?, ?, ?, ?)`,
      ).bind(session.tokenHash, userId, now, session.expiresAt),
    ]);
  } catch (error) {
    if (isUsernameConstraint(error)) {
      throw new ApiError(409, "username_taken", "用户名已被使用");
    }
    throwQuotaApiError(error);
  }

  return jsonResponse(
    {
      user: {
        id: userId,
        username,
        role: "owner",
        createdAt: now,
      },
    },
    201,
    { "set-cookie": sessionCookie(session.token, session.expiresAt) },
  );
}

async function login(request, env) {
  const payload = await readMutationJson(request, MAX_AUTH_BODY_BYTES);
  assertKeys(payload, ["username", "password"]);

  const username = validateUsername(payload.username);
  const password = validatePassword(payload.password);
  await enforceAuthRateLimit(request, env);
  const user = await env.DB.prepare(
    `SELECT id, username, password_hash, password_salt, password_iterations, role, created_at
     FROM users
     WHERE username = ?
     LIMIT 1`,
  )
    .bind(username)
    .first();
  const passwordMatches = await verifyPassword(password, user);

  if (!user || !passwordMatches) {
    throw new ApiError(401, "invalid_credentials", "用户名或密码错误");
  }

  const now = nowIso();
  const session = await newSession(now);
  try {
    await env.DB.batch([
      env.DB.prepare(
        `DELETE FROM sessions
         WHERE user_id = ? AND expires_at <= ?`,
      ).bind(user.id, now),
      env.DB.prepare(
        `DELETE FROM sessions
         WHERE token_hash IN (
           SELECT token_hash
           FROM sessions
           WHERE user_id = ? AND expires_at > ?
           ORDER BY created_at DESC, token_hash DESC
           LIMIT -1 OFFSET ?
         )`,
      ).bind(user.id, now, MAX_SESSIONS_PER_USER - 1),
      env.DB.prepare(
        `INSERT INTO sessions (token_hash, user_id, created_at, expires_at)
         VALUES (?, ?, ?, ?)`,
      ).bind(session.tokenHash, user.id, now, session.expiresAt),
    ]);
  } catch (error) {
    throwQuotaApiError(error);
  }

  return jsonResponse(
    { user: publicUser(user) },
    200,
    { "set-cookie": sessionCookie(session.token, session.expiresAt) },
  );
}

async function logout(request, env) {
  assertMutationRequest(request);
  assertRequestLength(request, MAX_AUTH_BODY_BYTES);

  const token = readCookie(request.headers.get("Cookie"), SESSION_COOKIE);
  if (token && SESSION_TOKEN_PATTERN.test(token)) {
    const tokenHash = await sha256Hex(`session:${token}`);
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(tokenHash).run();
  }

  return jsonResponse(
    { ok: true },
    200,
    {
      "set-cookie": `${SESSION_COOKIE}=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT`,
    },
  );
}

async function listServers(request, env, url) {
  const user = await requireAuthenticatedUser(request, env);
  const limit = parseLimit(url.searchParams.get("limit"));
  const results = await env.DB.prepare(
    `SELECT id, name, software_version, paired_at, last_sequence, last_sync_at, created_at, updated_at
     FROM servers
     WHERE owner_user_id = ?
     ORDER BY created_at DESC, id DESC
     LIMIT ?`,
  )
    .bind(user.id, limit)
    .all();

  return jsonResponse({ servers: results.results.map(publicServer) });
}

async function createServer(request, env) {
  assertMutationRequest(request);
  assertRequestLength(request, MAX_SERVER_BODY_BYTES);
  const user = await requireAuthenticatedUser(request, env);
  await assertServerQuota(env, user.id);
  const payload = await readJsonBody(request, MAX_SERVER_BODY_BYTES);
  assertKeys(payload, ["name"], ["softwareVersion"]);

  const name = validateServerName(payload.name);
  const softwareVersion = validateOptionalSoftwareVersion(payload.softwareVersion);
  const serverId = randomIdentifier("srv_");
  const eventId = randomIdentifier("evt_");
  const now = nowIso();

  try {
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO servers (
          id, owner_user_id, name, software_version, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)`,
      ).bind(serverId, user.id, name, softwareVersion, now, now),
      env.DB.prepare(
        `INSERT INTO server_events (
          id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
        ) VALUES (?, ?, 'user', ?, 'server_created', ?, ?)`,
      ).bind(eventId, serverId, user.id, JSON.stringify({ source: "console" }), now),
    ]);
  } catch (error) {
    throwQuotaApiError(error);
  }

  return jsonResponse({
    server: publicServer({
      id: serverId,
      name,
      software_version: softwareVersion,
      paired_at: null,
      last_sequence: -1,
      last_sync_at: null,
      created_at: now,
      updated_at: now,
    }),
  }, 201);
}

async function getServer(request, env, serverId) {
  const user = await requireAuthenticatedUser(request, env);
  const server = await getOwnedServer(env, user.id, serverId);
  return jsonResponse({ server: publicServer(server) });
}

async function createPairingCode(request, env, serverId) {
  assertMutationRequest(request);
  assertRequestLength(request, MAX_SERVER_BODY_BYTES);
  const user = await requireAuthenticatedUser(request, env);
  await getOwnedServer(env, user.id, serverId);

  const code = makePairingCode();
  const codeHash = await sha256Hex(`pairing-code:${code}`);
  const now = nowIso();
  await assertPairingCodeQuota(env, user.id, serverId, now);
  const expiresAt = new Date(Date.now() + PAIRING_TTL_MS).toISOString();
  const eventId = randomIdentifier("evt_");

  try {
    await env.DB.batch([
      env.DB.prepare(
        `UPDATE pairing_codes
         SET revoked_at = ?
         WHERE server_id = ? AND used_at IS NULL AND revoked_at IS NULL`,
      ).bind(now, serverId),
      env.DB.prepare(
        `INSERT INTO pairing_codes (
          code_hash, server_id, created_by_user_id, created_at, expires_at
        ) VALUES (?, ?, ?, ?, ?)`,
      ).bind(codeHash, serverId, user.id, now, expiresAt),
      env.DB.prepare(
        `INSERT INTO server_events (
          id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
        ) VALUES (?, ?, 'user', ?, 'pairing_code_created', ?, ?)`,
      ).bind(eventId, serverId, user.id, JSON.stringify({ expiresAt }), now),
    ]);
  } catch (error) {
    throwQuotaApiError(error);
  }

  return jsonResponse({ serverId, code, expiresAt }, 201);
}

async function claimPairingCode(request, env, url) {
  const payload = await readMutationJson(request, MAX_SERVER_BODY_BYTES);
  assertKeys(payload, ["code", "name", "softwareVersion"]);

  const code = validatePairingCode(payload.code);
  const name = validateServerName(payload.name);
  const softwareVersion = validateSoftwareVersion(payload.softwareVersion);
  const codeHash = await sha256Hex(`pairing-code:${code}`);
  const apiKey = `st_live_${randomBase64Url(32)}`;
  const keyHash = await sha256Hex(`api-key:${apiKey}`);
  const claimToken = randomIdentifier("clm_");
  const keyId = randomIdentifier("key_");
  const eventId = randomIdentifier("evt_");
  const now = nowIso();

  const results = await env.DB.batch([
    env.DB.prepare(
      `UPDATE pairing_codes
       SET used_at = ?, claim_token = ?
       WHERE code_hash = ?
         AND used_at IS NULL
         AND revoked_at IS NULL
         AND expires_at > ?`,
    ).bind(now, claimToken, codeHash, now),
    env.DB.prepare(
      `UPDATE server_api_keys
       SET revoked_at = ?
       WHERE server_id = (
         SELECT server_id FROM pairing_codes WHERE code_hash = ? AND claim_token = ?
       ) AND revoked_at IS NULL`,
    ).bind(now, codeHash, claimToken),
    env.DB.prepare(
      `INSERT INTO server_api_keys (id, server_id, key_hash, created_at)
       SELECT ?, server_id, ?, ?
       FROM pairing_codes
       WHERE code_hash = ? AND claim_token = ?`,
    ).bind(keyId, keyHash, now, codeHash, claimToken),
    env.DB.prepare(
      `UPDATE servers
       SET name = ?, software_version = ?, paired_at = ?, updated_at = ?
       WHERE id = (
         SELECT server_id FROM pairing_codes WHERE code_hash = ? AND claim_token = ?
       )`,
    ).bind(name, softwareVersion, now, now, codeHash, claimToken),
    env.DB.prepare(
      `INSERT INTO server_events (
        id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
      )
       SELECT ?, server_id, 'plugin', NULL, 'server_paired', ?, ?
       FROM pairing_codes
       WHERE code_hash = ? AND claim_token = ?`,
    ).bind(eventId, JSON.stringify({ softwareVersion }), now, codeHash, claimToken),
  ]);

  if (Number(results[0].meta.changes) !== 1) {
    throw new ApiError(400, "pairing_code_invalid", "配对码无效、已使用或已过期");
  }

  const pairing = await env.DB.prepare(
    `SELECT p.server_id, s.last_sequence
     FROM pairing_codes p
     INNER JOIN servers s ON s.id = p.server_id
     WHERE code_hash = ? AND claim_token = ?
     LIMIT 1`,
  )
    .bind(codeHash, claimToken)
    .first();

  if (!pairing) {
    throw new ApiError(500, "internal_error", "服务器暂时无法处理请求");
  }

  const lastSequence = Number(pairing.last_sequence);
  if (!Number.isSafeInteger(lastSequence)) {
    throw new ApiError(500, "internal_error", "服务器暂时无法处理请求");
  }

  return jsonResponse({
    serverId: pairing.server_id,
    apiKey,
    lastSequence: Math.max(0, lastSequence),
    apiBase: ["localhost", "127.0.0.1", "::1"].includes(url.hostname) ? url.origin : `https://${url.host}`,
  });
}

async function getLatestSnapshot(request, env, serverId) {
  const user = await requireAuthenticatedUser(request, env);
  const snapshot = await env.DB.prepare(
    `SELECT ss.server_id, ss.sequence, ss.sent_at, ss.received_at,
            ss.server_json, ss.players_json, ss.systems_json, ss.catalog_json
     FROM server_snapshots ss
     INNER JOIN servers s ON s.id = ss.server_id
     WHERE ss.server_id = ? AND s.owner_user_id = ?
     ORDER BY ss.sequence DESC
     LIMIT 1`,
  )
    .bind(serverId, user.id)
    .first();

  if (!snapshot) {
    await getOwnedServer(env, user.id, serverId);
    throw new ApiError(404, "snapshot_not_found", "服务器尚无同步快照");
  }

  return jsonResponse({ snapshot: publicSnapshot(snapshot) });
}

async function syncSnapshot(request, env) {
  assertMutationRequest(request);
  const apiKey = readBearerToken(request);
  const keyHash = await sha256Hex(`api-key:${apiKey}`);
  const apiKeyRow = await env.DB.prepare(
    `SELECT k.id, k.server_id
     FROM server_api_keys k
     INNER JOIN servers s ON s.id = k.server_id
     WHERE k.key_hash = ? AND k.revoked_at IS NULL
     LIMIT 1`,
  )
    .bind(keyHash)
    .first();

  if (!apiKeyRow) {
    throw new ApiError(401, "invalid_api_key", "API Key 无效");
  }

  const payload = validateSyncPayload(await readJsonBody(request, MAX_SYNC_BODY_BYTES));
  if (payload.serverId !== apiKeyRow.server_id) {
    throw new ApiError(403, "server_key_mismatch", "API Key 不属于该服务器");
  }

  const stub = env.SYNC_COORDINATOR.get(
    env.SYNC_COORDINATOR.idFromName(payload.serverId),
  );
  const response = await stub.fetch(
    new Request("https://sync-coordinator.internal/commit", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        apiKeyId: apiKeyRow.id,
        keyHash,
        payload,
      }),
    }),
  );

  return response;
}

async function enforceAuthRateLimit(request, env) {
  const clientAddress = request.headers.get("CF-Connecting-IP")?.trim() || "unknown";
  const fingerprint = await sha256Hex(`auth-rate:${clientAddress}`);
  const stub = env.AUTH_RATE_LIMITER.get(env.AUTH_RATE_LIMITER.idFromName(fingerprint));

  let response;
  try {
    response = await stub.fetch(
      new Request("https://auth-rate-limiter.internal/check", { method: "POST" }),
    );
  } catch {
    throw new ApiError(503, "auth_rate_limiter_unavailable", "认证服务暂时不可用");
  }

  if (response.ok) {
    return;
  }

  let failure;
  try {
    failure = await response.json();
  } catch {
    throw new ApiError(503, "auth_rate_limiter_unavailable", "认证服务暂时不可用");
  }
  const retryAfter = response.headers.get("Retry-After");
  const headers = retryAfter ? { "Retry-After": retryAfter } : undefined;
  if (
    response.status >= 400 &&
    response.status <= 599 &&
    typeof failure?.error?.code === "string" &&
    typeof failure?.error?.message === "string"
  ) {
    throw new ApiError(response.status, failure.error.code, failure.error.message, headers);
  }
  throw new ApiError(503, "auth_rate_limiter_unavailable", "认证服务暂时不可用");
}

async function requireAuthenticatedUser(request, env) {
  const token = readCookie(request.headers.get("Cookie"), SESSION_COOKIE);
  if (!token || !SESSION_TOKEN_PATTERN.test(token)) {
    throw new ApiError(401, "authentication_required", "请先登录");
  }

  const tokenHash = await sha256Hex(`session:${token}`);
  const user = await env.DB.prepare(
    `SELECT u.id, u.username, u.role, u.created_at
     FROM sessions s
     INNER JOIN users u ON u.id = s.user_id
     WHERE s.token_hash = ? AND s.expires_at > ?
     LIMIT 1`,
  )
    .bind(tokenHash, nowIso())
    .first();

  if (!user) {
    throw new ApiError(401, "authentication_required", "请先登录");
  }

  return user;
}

async function assertServerQuota(env, userId) {
  const usage = await env.DB.prepare(
    `SELECT COUNT(*) AS server_count
     FROM servers
     WHERE owner_user_id = ?`,
  )
    .bind(userId)
    .first();
  if (Number(usage?.server_count ?? 0) >= MAX_SERVERS_PER_USER) {
    throw new ApiError(409, "server_quota_exceeded", "每个账号最多创建 16 台服务器");
  }
}

async function assertPairingCodeQuota(env, userId, serverId, now) {
  const usage = await env.DB.prepare(
    `SELECT COUNT(*) AS active_code_count
     FROM pairing_codes
     WHERE created_by_user_id = ?
       AND server_id != ?
       AND used_at IS NULL
       AND revoked_at IS NULL
       AND expires_at > ?`,
  )
    .bind(userId, serverId, now)
    .first();
  if (Number(usage?.active_code_count ?? 0) >= MAX_ACTIVE_PAIRING_CODES_PER_USER) {
    throw new ApiError(409, "pairing_code_quota_exceeded", "每个账号最多保留 5 个有效配对码");
  }
}

function validateCoordinatorIdentity(userId, serverId) {
  if (
    typeof userId !== "string" ||
    !USER_ID_PATTERN.test(userId) ||
    typeof serverId !== "string" ||
    !SERVER_ID_PATTERN.test(serverId)
  ) {
    throw new ApiError(400, "invalid_internal_request", "内部请求参数不合法");
  }
  return { userId, serverId };
}

async function getOwnedServer(env, userId, serverId) {
  const server = await env.DB.prepare(
    `SELECT id, name, software_version, paired_at, last_sequence, last_sync_at, created_at, updated_at
     FROM servers
     WHERE id = ? AND owner_user_id = ?
     LIMIT 1`,
  )
    .bind(serverId, userId)
    .first();

  if (!server) {
    throw new ApiError(404, "server_not_found", "服务器不存在");
  }

  return server;
}

function publicUser(user) {
  return {
    id: user.id,
    username: user.username,
    role: user.role,
    createdAt: user.created_at,
  };
}

function publicServer(server) {
  return {
    id: server.id,
    name: server.name,
    softwareVersion: server.software_version ?? null,
    pairedAt: server.paired_at ?? null,
    lastSequence: Number(server.last_sequence),
    lastSyncAt: server.last_sync_at ?? null,
    createdAt: server.created_at,
    updatedAt: server.updated_at,
  };
}

function publicSnapshot(snapshot) {
  return {
    serverId: snapshot.server_id,
    sequence: Number(snapshot.sequence),
    sentAt: snapshot.sent_at,
    receivedAt: snapshot.received_at,
    server: JSON.parse(snapshot.server_json),
    players: JSON.parse(snapshot.players_json),
    systems: JSON.parse(snapshot.systems_json),
    catalog: JSON.parse(snapshot.catalog_json),
  };
}

async function listServerExtensions(request, env, serverId) {
  const user = await requireAuthenticatedUser(request, env);
  await getOwnedServer(env, user.id, serverId);

  const extensions = await env.DB.prepare(
    `SELECT e.extension_id, e.manifest_json, e.source, e.sha256, e.enabled,
            e.revision, e.created_at, e.updated_at
     FROM server_extensions e
     INNER JOIN servers s ON s.id = e.server_id
     WHERE e.server_id = ? AND s.owner_user_id = ?
     ORDER BY e.extension_id ASC`,
  )
    .bind(serverId, user.id)
    .all();

  return jsonResponse({
    extensions: extensions.results.map(publicOwnerExtension),
  });
}

async function upsertServerExtension(request, env, serverId) {
  const user = await requireAuthenticatedUser(request, env);
  const payload = validateExtensionPayload(
    await readMutationJson(request, MAX_EXTENSION_BODY_BYTES),
  );
  return forwardServerMutation(env, serverId, "/extensions/upsert", {
    userId: user.id,
    serverId,
    payload,
  });
}

async function commitServerExtensionUpsert(env, userId, serverId, payload) {
  await getOwnedServer(env, userId, serverId);
  const existing = await findOwnedExtension(env, userId, serverId, payload.manifest.id);
  await assertOwnedExtensionDependencies(env, userId, serverId, payload.manifest);
  await assertExtensionQuota(env, serverId, existing, payload.source);

  const enabled = payload.enabled ?? (existing ? Number(existing.enabled) === 1 : false);
  const sha256 = await sha256Hex(payload.source);
  const now = nowIso();
  const nextRevision = existing ? Number(existing.revision) + 1 : 1;
  const eventType = existing ? "extension_updated" : "extension_created";
  let results;
  try {
    results = await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO server_extensions (
          server_id, extension_id, manifest_json, source, sha256,
          enabled, revision, created_at, updated_at
        )
         SELECT s.id, ?, ?, ?, ?, ?, 1, ?, ?
         FROM servers s
         WHERE s.id = ? AND s.owner_user_id = ?
         ON CONFLICT (server_id, extension_id) DO UPDATE SET
           manifest_json = excluded.manifest_json,
           source = excluded.source,
           sha256 = excluded.sha256,
           enabled = excluded.enabled,
           revision = server_extensions.revision + 1,
           updated_at = excluded.updated_at
         WHERE server_extensions.revision = ?`,
      ).bind(
        payload.manifest.id,
        JSON.stringify(payload.manifest),
        payload.source,
        sha256,
        enabled ? 1 : 0,
        now,
        now,
        serverId,
        userId,
        existing ? Number(existing.revision) : -1,
      ),
      extensionAuditStatement(env, {
        serverId,
        userId,
        eventType,
        metadata: {
          extensionId: payload.manifest.id,
          revision: nextRevision,
          sha256,
          enabled,
        },
        createdAt: now,
        revision: nextRevision,
        sha256,
        enabled,
        updatedAt: now,
      }),
    ]);
  } catch (error) {
    throwQuotaApiError(error);
  }

  if (
    Number(results[0].meta.changes) !== 1 ||
    Number(results[1].meta.changes) !== 1
  ) {
    throw new ApiError(409, "extension_update_conflict", "扩展已被并发修改，请刷新后重试");
  }

  const extension = await getOwnedExtension(env, userId, serverId, payload.manifest.id);
  return jsonResponse(
    { extension: publicOwnerExtension(extension) },
    existing ? 200 : 201,
  );
}

async function deleteServerExtension(request, env, serverId, extensionId) {
  const user = await requireAuthenticatedUser(request, env);
  return forwardServerMutation(env, serverId, "/extensions/delete", {
    userId: user.id,
    serverId,
    extensionId,
  });
}

async function commitServerExtensionDelete(env, userId, serverId, extensionId) {
  await getOwnedServer(env, userId, serverId);
  const existing = await getOwnedExtension(env, userId, serverId, extensionId);
  await assertExtensionNotRequired(env, userId, serverId, extensionId);

  const now = nowIso();
  const results = await env.DB.batch([
    env.DB.prepare(
      `DELETE FROM server_extensions
       WHERE server_id = ?
         AND extension_id = ?
         AND revision = ?
         AND EXISTS (
           SELECT 1
           FROM servers s
           WHERE s.id = server_extensions.server_id
             AND s.id = ?
             AND s.owner_user_id = ?
         )`,
    ).bind(serverId, extensionId, Number(existing.revision), serverId, userId),
    extensionDeleteAuditStatement(env, {
      serverId,
      userId,
      eventType: "extension_deleted",
      metadata: {
        extensionId,
        revision: Number(existing.revision),
        sha256: existing.sha256,
      },
      createdAt: now,
    }),
  ]);

  if (
    Number(results[0].meta.changes) !== 1 ||
    Number(results[1].meta.changes) !== 1
  ) {
    throw new ApiError(409, "extension_update_conflict", "扩展已被并发修改，请刷新后重试");
  }

  return jsonResponse({ deleted: true, extensionId });
}

async function setServerExtensionState(request, env, serverId, extensionId) {
  const user = await requireAuthenticatedUser(request, env);
  const payload = await readMutationJson(request, MAX_SERVER_BODY_BYTES);
  assertKeys(payload, ["enabled"]);
  if (typeof payload.enabled !== "boolean") {
    throw new ApiError(400, "invalid_extension_state", "enabled 必须为布尔值");
  }

  return forwardServerMutation(env, serverId, "/extensions/state", {
    userId: user.id,
    serverId,
    extensionId,
    enabled: payload.enabled,
  });
}

async function commitServerExtensionState(env, userId, serverId, extensionId, enabled) {
  await getOwnedServer(env, userId, serverId);
  const existing = await getOwnedExtension(env, userId, serverId, extensionId);
  if ((Number(existing.enabled) === 1) === enabled) {
    return jsonResponse({ extension: publicOwnerExtension(existing) });
  }

  const now = nowIso();
  const nextRevision = Number(existing.revision) + 1;
  const results = await env.DB.batch([
    env.DB.prepare(
      `UPDATE server_extensions
       SET enabled = ?, revision = ?, updated_at = ?
       WHERE server_id = ?
         AND extension_id = ?
         AND revision = ?
         AND enabled != ?
         AND EXISTS (
           SELECT 1
           FROM servers s
           WHERE s.id = server_extensions.server_id
             AND s.id = ?
             AND s.owner_user_id = ?
         )`,
    ).bind(
      enabled ? 1 : 0,
      nextRevision,
      now,
      serverId,
      extensionId,
      Number(existing.revision),
      enabled ? 1 : 0,
      serverId,
      userId,
    ),
    extensionAuditStatement(env, {
      serverId,
      userId,
      eventType: enabled ? "extension_enabled" : "extension_disabled",
      metadata: {
        extensionId,
        revision: nextRevision,
        sha256: existing.sha256,
        enabled,
      },
      createdAt: now,
      revision: nextRevision,
      sha256: existing.sha256,
      enabled,
      updatedAt: now,
    }),
  ]);

  if (
    Number(results[0].meta.changes) !== 1 ||
    Number(results[1].meta.changes) !== 1
  ) {
    throw new ApiError(409, "extension_update_conflict", "扩展已被并发修改，请刷新后重试");
  }

  const extension = await getOwnedExtension(env, userId, serverId, extensionId);
  return jsonResponse({ extension: publicOwnerExtension(extension) });
}

async function forwardServerMutation(env, serverId, pathname, payload) {
  const stub = env.SYNC_COORDINATOR.get(env.SYNC_COORDINATOR.idFromName(serverId));
  return stub.fetch(
    new Request(`https://sync-coordinator.internal${pathname}`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
    }),
  );
}

async function getExtensionManifest(request, env) {
  const apiKey = await requireExtensionServerApiKey(request, env);
  const extensions = await env.DB.prepare(
    `SELECT extension_id, manifest_json, sha256, enabled, revision
     FROM server_extensions
     WHERE server_id = ?
     ORDER BY extension_id ASC`,
  )
    .bind(apiKey.server_id)
    .all();
  const etag = await extensionManifestEtag(apiKey.server_id, extensions.results);

  await touchExtensionApiKey(env, apiKey);
  if (ifNoneMatchMatches(request.headers.get("If-None-Match"), etag)) {
    return notModifiedResponse(etag);
  }

  return jsonResponse(
    { extensions: extensions.results.map(publicPluginExtension) },
    200,
    { ETag: etag },
  );
}

async function getExtensionSource(request, env, extensionId) {
  const apiKey = await requireExtensionServerApiKey(request, env);
  const extension = await env.DB.prepare(
    `SELECT extension_id, manifest_json, source, sha256, revision
     FROM server_extensions
     WHERE server_id = ? AND extension_id = ? AND enabled = 1
     LIMIT 1`,
  )
    .bind(apiKey.server_id, extensionId)
    .first();

  if (!extension) {
    throw new ApiError(404, "extension_not_found", "扩展不存在或未启用");
  }

  await touchExtensionApiKey(env, apiKey);
  return jsonResponse({
    manifest: parseStoredExtensionManifest(extension.manifest_json),
    source: extension.source,
    revision: Number(extension.revision),
    sha256: extension.sha256,
  });
}

async function requireExtensionServerApiKey(request, env) {
  const apiKey = readBearerToken(request);
  const keyHash = await sha256Hex(`api-key:${apiKey}`);
  const key = await env.DB.prepare(
    `SELECT k.id, k.server_id
     FROM server_api_keys k
     INNER JOIN servers s ON s.id = k.server_id
     WHERE k.key_hash = ? AND k.revoked_at IS NULL AND s.id = k.server_id
     LIMIT 1`,
  )
    .bind(keyHash)
    .first();

  if (!key) {
    throw new ApiError(401, "invalid_api_key", "API Key 无效");
  }

  return key;
}

async function touchExtensionApiKey(env, apiKey) {
  await env.DB.prepare(
    `UPDATE server_api_keys
     SET last_used_at = ?
     WHERE id = ? AND server_id = ? AND revoked_at IS NULL`,
  )
    .bind(nowIso(), apiKey.id, apiKey.server_id)
    .run();
}

async function findOwnedExtension(env, userId, serverId, extensionId) {
  return env.DB.prepare(
    `SELECT e.extension_id, e.manifest_json, e.source, e.sha256, e.enabled,
            e.revision, e.created_at, e.updated_at
     FROM server_extensions e
     INNER JOIN servers s ON s.id = e.server_id
     WHERE e.server_id = ? AND e.extension_id = ? AND s.owner_user_id = ?
     LIMIT 1`,
  )
    .bind(serverId, extensionId, userId)
    .first();
}

async function getOwnedExtension(env, userId, serverId, extensionId) {
  const extension = await findOwnedExtension(env, userId, serverId, extensionId);
  if (!extension) {
    throw new ApiError(404, "extension_not_found", "扩展不存在");
  }
  return extension;
}

async function assertExtensionQuota(env, serverId, existing, source) {
  const usage = await env.DB.prepare(
    `SELECT COUNT(*) AS extension_count,
            COALESCE(SUM(length(CAST(source AS BLOB))), 0) AS source_bytes
     FROM server_extensions
     WHERE server_id = ?`,
  )
    .bind(serverId)
    .first();
  const extensionCount = Number(usage?.extension_count ?? 0);
  const sourceBytes = Number(usage?.source_bytes ?? 0);
  const replacedBytes = existing ? byteLength(existing.source) : 0;

  if (!existing && extensionCount >= MAX_EXTENSIONS_PER_SERVER) {
    throw new ApiError(409, "extension_quota_exceeded", "每台服务器最多保存 64 个扩展");
  }
  if (
    !Number.isSafeInteger(sourceBytes) ||
    sourceBytes - replacedBytes + byteLength(source) > MAX_EXTENSION_SOURCE_BYTES_PER_SERVER
  ) {
    throw new ApiError(409, "extension_source_quota_exceeded", "每台服务器的扩展源码总量不能超过 2 MiB");
  }
}

async function assertExtensionNotRequired(env, userId, serverId, extensionId) {
  const extensions = await env.DB.prepare(
    `SELECT e.extension_id, e.manifest_json
     FROM server_extensions e
     INNER JOIN servers s ON s.id = e.server_id
     WHERE e.server_id = ? AND e.extension_id != ? AND s.owner_user_id = ?`,
  )
    .bind(serverId, extensionId, userId)
    .all();

  for (const extension of extensions.results) {
    const manifest = parseStoredExtensionManifest(extension.manifest_json);
    if (Array.isArray(manifest.dependencies) && manifest.dependencies.includes(extensionId)) {
      throw new ApiError(409, "extension_dependency_in_use", "扩展仍被其他扩展依赖，不能删除");
    }
  }
}

async function assertOwnedExtensionDependencies(env, userId, serverId, manifest) {
  if (manifest.dependencies.length === 0) {
    return;
  }

  const extensions = await env.DB.prepare(
    `SELECT e.extension_id, e.manifest_json
     FROM server_extensions e
     INNER JOIN servers s ON s.id = e.server_id
     WHERE e.server_id = ? AND s.owner_user_id = ?`,
  )
    .bind(serverId, userId)
    .all();
  const graph = new Map();
  for (const extension of extensions.results) {
    graph.set(
      extension.extension_id,
      parseStoredExtensionManifest(extension.manifest_json).dependencies,
    );
  }
  graph.set(manifest.id, manifest.dependencies);

  for (const dependency of manifest.dependencies) {
    if (!graph.has(dependency)) {
      throw new ApiError(400, "extension_dependency_not_found", "扩展依赖必须属于当前服务器");
    }
  }

  if (hasExtensionDependencyCycle(manifest.id, graph)) {
    throw new ApiError(400, "extension_dependency_cycle", "扩展依赖不能形成循环");
  }
}

function hasExtensionDependencyCycle(rootId, graph) {
  const visiting = new Set();
  const visited = new Set();

  function visit(extensionId) {
    if (visiting.has(extensionId)) {
      return true;
    }
    if (visited.has(extensionId)) {
      return false;
    }

    visiting.add(extensionId);
    for (const dependency of graph.get(extensionId) || []) {
      if (visit(dependency)) {
        return true;
      }
    }
    visiting.delete(extensionId);
    visited.add(extensionId);
    return false;
  }

  return visit(rootId);
}

function extensionAuditStatement(
  env,
  { serverId, userId, eventType, metadata, createdAt, revision, sha256, enabled, updatedAt },
) {
  return env.DB.prepare(
    `INSERT INTO server_events (
      id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
    )
     SELECT ?, e.server_id, 'user', ?, ?, ?, ?
     FROM server_extensions e
     INNER JOIN servers s ON s.id = e.server_id
     WHERE e.server_id = ?
       AND s.owner_user_id = ?
       AND e.extension_id = ?
       AND e.revision = ?
       AND e.sha256 = ?
       AND e.enabled = ?
       AND e.updated_at = ?`,
  ).bind(
    randomIdentifier("evt_"),
    userId,
    eventType,
    JSON.stringify(metadata),
    createdAt,
    serverId,
    userId,
    metadata.extensionId,
    revision,
    sha256,
    enabled ? 1 : 0,
    updatedAt,
  );
}

function extensionDeleteAuditStatement(
  env,
  { serverId, userId, eventType, metadata, createdAt },
) {
  return env.DB.prepare(
    `INSERT INTO server_events (
      id, server_id, actor_type, actor_user_id, event_type, metadata_json, created_at
    )
     SELECT ?, s.id, 'user', ?, ?, ?, ?
     FROM servers s
     WHERE s.id = ?
       AND s.owner_user_id = ?
       AND NOT EXISTS (
         SELECT 1
         FROM server_extensions e
         WHERE e.server_id = s.id AND e.extension_id = ?
       )`,
  ).bind(
    randomIdentifier("evt_"),
    userId,
    eventType,
    JSON.stringify(metadata),
    createdAt,
    serverId,
    userId,
    metadata.extensionId,
  );
}

function publicOwnerExtension(extension) {
  return {
    manifest: parseStoredExtensionManifest(extension.manifest_json),
    source: extension.source,
    sha256: extension.sha256,
    enabled: Number(extension.enabled) === 1,
    revision: Number(extension.revision),
    createdAt: extension.created_at,
    updatedAt: extension.updated_at,
  };
}

function publicPluginExtension(extension) {
  return {
    manifest: parseStoredExtensionManifest(extension.manifest_json),
    sha256: extension.sha256,
    enabled: Number(extension.enabled) === 1,
    revision: Number(extension.revision),
  };
}

function parseStoredExtensionManifest(manifestJson) {
  try {
    return JSON.parse(manifestJson);
  } catch {
    throw new ApiError(500, "internal_error", "服务器暂时无法处理请求");
  }
}

function validateExtensionPayload(payload) {
  assertKeys(payload, ["manifest", "source"], ["enabled"]);
  if (typeof payload.source !== "string") {
    throw new ApiError(400, "invalid_extension_source", "扩展源码必须为字符串");
  }
  if (byteLength(payload.source) === 0 || payload.source.includes("\0")) {
    throw new ApiError(400, "invalid_extension_source", "扩展源码不能为空且不能包含 NUL 字符");
  }
  if (byteLength(payload.source) > MAX_EXTENSION_SOURCE_BYTES) {
    throw new ApiError(413, "payload_too_large", "扩展源码不能超过 128 KiB");
  }
  if (Object.hasOwn(payload, "enabled") && typeof payload.enabled !== "boolean") {
    throw new ApiError(400, "invalid_extension_state", "enabled 必须为布尔值");
  }

  return {
    manifest: validateExtensionManifest(payload.manifest),
    source: payload.source,
    enabled: payload.enabled,
  };
}

function validateExtensionManifest(value) {
  assertObject(value);
  assertKeys(value, [
    "id",
    "name",
    "version",
    "engine",
    "entry",
    "dependencies",
    "permissions",
  ]);

  const id = validateExtensionId(value.id);
  if (!EXTENSION_ENGINES.has(value.engine)) {
    throw new ApiError(400, "invalid_extension_engine", "扩展引擎必须为 lua 或 javascript");
  }

  const dependencies = validateExtensionDependencies(value.dependencies);
  if (dependencies.includes(id)) {
    throw new ApiError(400, "extension_dependency_cycle", "扩展不能依赖自身");
  }

  return {
    id,
    name: validateExtensionText(value.name, MAX_EXTENSION_NAME_BYTES, "invalid_extension_name", "扩展名称"),
    version: validateExtensionText(
      value.version,
      MAX_EXTENSION_VERSION_BYTES,
      "invalid_extension_version",
      "扩展版本",
    ),
    engine: value.engine,
    entry: validateExtensionEntry(value.entry, value.engine),
    dependencies,
    permissions: validateExtensionPermissions(value.permissions),
  };
}

function validateExtensionId(value) {
  if (
    typeof value !== "string" ||
    byteLength(value) === 0 ||
    byteLength(value) > MAX_EXTENSION_ID_BYTES ||
    !EXTENSION_ID_PATTERN.test(value)
  ) {
    throw new ApiError(400, "invalid_extension_id", "扩展标识必须由小写字母或数字开头结尾，并仅以单个连字符分隔");
  }
  return value;
}

function decodeExtensionId(segment) {
  let extensionId;
  try {
    extensionId = decodeURIComponent(segment);
  } catch {
    throw new ApiError(400, "invalid_extension_id", "扩展标识格式不正确");
  }
  return validateExtensionId(extensionId);
}

function validateExtensionText(value, maxBytes, code, label) {
  if (
    typeof value !== "string" ||
    byteLength(value.trim()) === 0 ||
    byteLength(value.trim()) > maxBytes ||
    containsControlCharacter(value)
  ) {
    throw new ApiError(400, code, `${label}不能为空、不能含控制字符且长度受限`);
  }
  return value.trim();
}

function validateExtensionEntry(value, engine) {
  const pattern = engine === "lua" ? LUA_EXTENSION_ENTRY_PATTERN : EXTENSION_ENTRY_PATTERN;
  if (
    typeof value !== "string" ||
    byteLength(value) === 0 ||
    byteLength(value) > MAX_EXTENSION_ENTRY_BYTES ||
    !pattern.test(value)
  ) {
    throw new ApiError(400, "invalid_extension_entry", "扩展入口必须是安全的脚本标识符");
  }
  return value;
}

function validateExtensionDependencies(value) {
  if (!Array.isArray(value) || value.length > MAX_EXTENSION_DEPENDENCIES) {
    throw new ApiError(400, "invalid_extension_dependencies", "扩展依赖列表不合法");
  }
  const dependencies = value.map(validateExtensionId);
  if (new Set(dependencies).size !== dependencies.length) {
    throw new ApiError(400, "invalid_extension_dependencies", "扩展依赖不能重复");
  }
  return dependencies;
}

function validateExtensionPermissions(value) {
  if (!Array.isArray(value) || value.length > MAX_EXTENSION_PERMISSIONS) {
    throw new ApiError(400, "invalid_extension_permissions", "扩展权限列表不合法");
  }
  if (value.some((permission) => typeof permission !== "string" || !EXTENSION_PERMISSIONS.has(permission))) {
    throw new ApiError(400, "invalid_extension_permissions", "扩展请求了未授权的能力");
  }
  if (new Set(value).size !== value.length) {
    throw new ApiError(400, "invalid_extension_permissions", "扩展权限不能重复");
  }
  return value;
}

async function extensionManifestEtag(serverId, extensions) {
  const fingerprint = extensions
    .map(
      (extension) =>
        `${extension.extension_id}\u0000${extension.revision}\u0000${extension.sha256}\u0000${extension.enabled}`,
    )
    .join("\u0001");
  return `"${await sha256Hex(`extension-manifest:${serverId}:${fingerprint}`)}"`;
}

function ifNoneMatchMatches(value, etag) {
  if (!value) {
    return false;
  }
  return value.split(",").some((candidate) => {
    const tag = candidate.trim();
    return tag === "*" || tag === etag || tag === `W/${etag}`;
  });
}

function notModifiedResponse(etag) {
  const headers = new Headers(API_HEADERS);
  headers.set("etag", etag);
  return new Response(null, { status: 304, headers });
}

function requireMethod(request, method) {
  if (request.method !== method) {
    throw methodNotAllowed(method);
  }
}

function methodNotAllowed(allow) {
  return new ApiError(405, "method_not_allowed", "请求方法不被支持", { Allow: allow });
}

async function readMutationJson(request, maxBytes) {
  assertMutationRequest(request);
  return readJsonBody(request, maxBytes);
}

function assertMutationRequest(request) {
  assertJsonContentType(request);
  const origin = request.headers.get("Origin");
  const requestOrigin = new URL(request.url).origin;
  if (origin && origin !== requestOrigin) {
    throw new ApiError(403, "origin_forbidden", "请求来源不受信任");
  }

  if (!origin && request.headers.get("Sec-Fetch-Site") === "cross-site") {
    throw new ApiError(403, "origin_forbidden", "请求来源不受信任");
  }
}

function assertJsonContentType(request) {
  const contentType = request.headers.get("Content-Type") || "";
  if (!/^application\/json(?:\s*;|$)/i.test(contentType)) {
    throw new ApiError(415, "json_content_type_required", "请求必须使用 application/json");
  }
}

function assertRequestLength(request, maxBytes) {
  const contentLength = request.headers.get("Content-Length");
  if (!contentLength) {
    return;
  }

  if (!/^\d+$/.test(contentLength) || Number(contentLength) > maxBytes) {
    throw new ApiError(413, "payload_too_large", "请求体超过允许大小");
  }
}

async function readJsonBody(request, maxBytes) {
  assertRequestLength(request, maxBytes);
  const body = await readBodyBytes(request, maxBytes);
  if (body.byteLength === 0) {
    throw new ApiError(400, "invalid_json", "请求体必须是 JSON 对象");
  }

  try {
    const parsed = JSON.parse(TEXT_DECODER.decode(body));
    return assertObject(parsed);
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(400, "invalid_json", "请求体必须是有效 JSON");
  }
}

async function readBodyBytes(request, maxBytes) {
  if (!request.body) {
    return new Uint8Array();
  }

  const reader = request.body.getReader();
  const chunks = [];
  let total = 0;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      total += value.byteLength;
      if (total > maxBytes) {
        await reader.cancel();
        throw new ApiError(413, "payload_too_large", "请求体超过允许大小");
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  const body = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return body;
}

function assertObject(value) {
  if (value === null || Array.isArray(value) || typeof value !== "object") {
    throw new ApiError(400, "invalid_request", "请求参数必须是对象");
  }
  return value;
}

function assertKeys(value, requiredKeys, optionalKeys = []) {
  const allowed = new Set([...requiredKeys, ...optionalKeys]);
  const keys = Object.keys(value);
  if (
    requiredKeys.some((key) => !Object.hasOwn(value, key)) ||
    keys.some((key) => !allowed.has(key))
  ) {
    throw new ApiError(400, "invalid_request", "请求参数不符合接口约定");
  }
}

function validateUsername(value) {
  if (
    typeof value !== "string" ||
    byteLength(value) > MAX_USERNAME_BYTES ||
    !/^[A-Za-z0-9_-]{3,32}$/.test(value)
  ) {
    throw new ApiError(400, "invalid_username", "用户名需为 3 至 32 位字母、数字、下划线或连字符");
  }
  return value;
}

function validatePassword(value) {
  if (
    typeof value !== "string" ||
    byteLength(value) < 8 ||
    byteLength(value) > MAX_PASSWORD_BYTES
  ) {
    throw new ApiError(400, "invalid_password", "密码长度需为 8 至 128 字节");
  }
  return value;
}

function validateServerName(value) {
  if (
    typeof value !== "string" ||
    byteLength(value.trim()) === 0 ||
    byteLength(value.trim()) > MAX_SERVER_NAME_BYTES ||
    containsControlCharacter(value)
  ) {
    throw new ApiError(400, "invalid_server_name", "服务器名称长度需为 1 至 64 字节且不含控制字符");
  }
  return value.trim();
}

function validateOptionalSoftwareVersion(value) {
  if (value === undefined || value === null) {
    return null;
  }
  return validateSoftwareVersion(value);
}

function validateSoftwareVersion(value) {
  if (
    typeof value !== "string" ||
    byteLength(value.trim()) === 0 ||
    byteLength(value.trim()) > MAX_SOFTWARE_VERSION_BYTES ||
    containsControlCharacter(value)
  ) {
    throw new ApiError(400, "invalid_software_version", "软件版本长度需为 1 至 64 字节且不含控制字符");
  }
  return value.trim();
}

function validatePairingCode(value) {
  if (typeof value !== "string") {
    throw new ApiError(400, "invalid_pairing_code", "配对码格式不正确");
  }
  const code = value.trim().toUpperCase();
  if (!PAIRING_CODE_PATTERN.test(code)) {
    throw new ApiError(400, "invalid_pairing_code", "配对码格式不正确");
  }
  return code;
}

function validateSyncPayload(payload) {
  assertKeys(
    payload,
    ["serverId", "sequence", "sentAt", "server", "players", "systems", "catalog"],
    ["telemetry"],
  );

  if (typeof payload.serverId !== "string" || !SERVER_ID_PATTERN.test(payload.serverId)) {
    throw new ApiError(400, "invalid_server_id", "服务器标识格式不正确");
  }
  if (!Number.isSafeInteger(payload.sequence) || payload.sequence < 0) {
    throw new ApiError(400, "invalid_sequence", "同步序号必须是非负安全整数");
  }

  const validated = {
    serverId: payload.serverId,
    sequence: payload.sequence,
    sentAt: validateTimestamp(payload.sentAt),
    server: validateSnapshotValue(payload.server),
    players: validateSnapshotValue(payload.players),
    systems: validateSnapshotValue(payload.systems),
    catalog: validateSnapshotValue(payload.catalog),
  };
  // Telemetry is validated in planTelemetry, where a malformed block is skipped instead of
  // rejecting the snapshot it rides along with.
  if (payload.telemetry !== undefined) {
    validated.telemetry = payload.telemetry;
  }
  return validated;
}

function validateTimestamp(value) {
  if (typeof value !== "string" || value.length > 64) {
    throw new ApiError(400, "invalid_timestamp", "时间格式不正确");
  }
  const timestamp = Date.parse(value);
  if (!Number.isFinite(timestamp)) {
    throw new ApiError(400, "invalid_timestamp", "时间格式不正确");
  }
  return new Date(timestamp).toISOString();
}

function validateSnapshotValue(value, depth = 0) {
  if (depth > MAX_SNAPSHOT_DEPTH) {
    throw new ApiError(400, "snapshot_too_deep", "快照嵌套层级超过限制");
  }

  if (value === null || typeof value === "boolean") {
    return value;
  }
  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      throw new ApiError(400, "invalid_snapshot", "快照包含无效数字");
    }
    return value;
  }
  if (typeof value === "string") {
    if (byteLength(value) > MAX_SNAPSHOT_STRING_BYTES) {
      throw new ApiError(413, "payload_too_large", "快照字段超过允许大小");
    }
    return value;
  }
  if (Array.isArray(value)) {
    if (value.length > MAX_SNAPSHOT_COLLECTION_ENTRIES) {
      throw new ApiError(413, "payload_too_large", "快照集合超过允许大小");
    }
    value.forEach((item) => validateSnapshotValue(item, depth + 1));
    return value;
  }
  if (typeof value === "object") {
    const entries = Object.entries(value);
    if (entries.length > MAX_SNAPSHOT_COLLECTION_ENTRIES) {
      throw new ApiError(413, "payload_too_large", "快照集合超过允许大小");
    }
    for (const [key, item] of entries) {
      if (byteLength(key) > 256 || containsControlCharacter(key)) {
        throw new ApiError(400, "invalid_snapshot", "快照键名不合法");
      }
      validateSnapshotValue(item, depth + 1);
    }
    return value;
  }

  throw new ApiError(400, "invalid_snapshot", "快照包含不支持的数据类型");
}

function decodeServerId(segment) {
  let serverId;
  try {
    serverId = decodeURIComponent(segment);
  } catch {
    throw new ApiError(400, "invalid_server_id", "服务器标识格式不正确");
  }
  if (!SERVER_ID_PATTERN.test(serverId)) {
    throw new ApiError(400, "invalid_server_id", "服务器标识格式不正确");
  }
  return serverId;
}

function parseLimit(value) {
  if (value === null) {
    return 50;
  }
  if (!/^[1-9]\d*$/.test(value)) {
    throw new ApiError(400, "invalid_limit", "limit 必须是正整数");
  }
  const limit = Number(value);
  if (!Number.isSafeInteger(limit) || limit > 100) {
    throw new ApiError(400, "invalid_limit", "limit 不能超过 100");
  }
  return limit;
}

function readBearerToken(request) {
  const authorization = request.headers.get("Authorization") || "";
  const match = /^Bearer (st_live_[A-Za-z0-9_-]{43})$/.exec(authorization);
  if (!match || !API_KEY_PATTERN.test(match[1])) {
    throw new ApiError(401, "invalid_api_key", "API Key 无效");
  }
  return match[1];
}

function readCookie(header, name) {
  if (!header) {
    return null;
  }
  for (const part of header.split(";")) {
    const separator = part.indexOf("=");
    if (separator === -1) {
      continue;
    }
    if (part.slice(0, separator).trim() === name) {
      return part.slice(separator + 1).trim();
    }
  }
  return null;
}

function makePairingCode() {
  const random = crypto.getRandomValues(new Uint8Array(16));
  const characters = Array.from(random, (value) => PAIRING_ALPHABET[value & 31]);
  return `ST-${characters.slice(0, 4).join("")}-${characters.slice(4, 8).join("")}-${characters.slice(8, 12).join("")}-${characters.slice(12, 16).join("")}`;
}

async function hashPassword(password) {
  const salt = randomBase64Url(16);
  return {
    salt,
    hash: await derivePasswordHash(password, salt, PBKDF2_ITERATIONS),
  };
}

async function verifyPassword(password, user) {
  const supportedUser = Boolean(user) && Number(user.password_iterations) === PBKDF2_ITERATIONS;
  const salt = supportedUser ? user.password_salt : DUMMY_PASSWORD_SALT;
  const candidate = await derivePasswordHash(password, salt, PBKDF2_ITERATIONS);
  return supportedUser && constantTimeEqual(candidate, user.password_hash);
}

async function derivePasswordHash(password, salt, iterations) {
  const key = await crypto.subtle.importKey(
    "raw",
    TEXT_ENCODER.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"],
  );
  const bits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      hash: "SHA-256",
      salt: base64UrlToBytes(salt),
      iterations,
    },
    key,
    256,
  );
  return bytesToBase64Url(new Uint8Array(bits));
}

async function newSession() {
  const token = randomBase64Url(32);
  return {
    token,
    tokenHash: await sha256Hex(`session:${token}`),
    expiresAt: new Date(Date.now() + SESSION_TTL_SECONDS * 1000).toISOString(),
  };
}

function sessionCookie(token, expiresAt) {
  return `${SESSION_COOKIE}=${token}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=${SESSION_TTL_SECONDS}; Expires=${new Date(expiresAt).toUTCString()}`;
}

function randomIdentifier(prefix) {
  return `${prefix}${randomBase64Url(16)}`;
}

function randomBase64Url(byteLength) {
  return bytesToBase64Url(crypto.getRandomValues(new Uint8Array(byteLength)));
}

async function sha256Hex(value) {
  const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", TEXT_ENCODER.encode(value)));
  return Array.from(digest, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function bytesToBase64Url(bytes) {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlToBytes(value) {
  if (!/^[A-Za-z0-9_-]+$/.test(value)) {
    throw new Error("Invalid base64url value");
  }
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = `${base64}${"=".repeat((4 - (base64.length % 4)) % 4)}`;
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function constantTimeEqual(left, right) {
  if (typeof left !== "string" || typeof right !== "string" || left.length !== right.length) {
    return false;
  }
  let difference = 0;
  for (let index = 0; index < left.length; index += 1) {
    difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return difference === 0;
}

function throwQuotaApiError(error) {
  const detail = String(error);
  if (detail.includes("quota_servers_per_user")) {
    throw new ApiError(409, "server_quota_exceeded", "每个账号最多创建 16 台服务器");
  }
  if (detail.includes("quota_sessions_per_user")) {
    throw new ApiError(409, "session_quota_exceeded", "每个账号最多保留 10 个有效会话");
  }
  if (detail.includes("quota_pairing_codes_per_user")) {
    throw new ApiError(409, "pairing_code_quota_exceeded", "每个账号最多保留 5 个有效配对码");
  }
  if (detail.includes("quota_extensions_per_server")) {
    throw new ApiError(409, "extension_quota_exceeded", "每台服务器最多保存 64 个扩展");
  }
  if (detail.includes("quota_extension_source_per_server")) {
    throw new ApiError(409, "extension_source_quota_exceeded", "每台服务器的扩展源码总量不能超过 2 MiB");
  }
  throw error;
}

function isUsernameConstraint(error) {
  return String(error).includes("UNIQUE constraint failed: users.username");
}

function containsControlCharacter(value) {
  return /[\u0000-\u001F\u007F]/.test(value);
}

function byteLength(value) {
  return TEXT_ENCODER.encode(value).byteLength;
}

function nowIso() {
  return new Date().toISOString();
}

function jsonResponse(data, status = 200, additionalHeaders = undefined) {
  const headers = new Headers(API_HEADERS);
  if (additionalHeaders) {
    for (const [name, value] of Object.entries(additionalHeaders)) {
      headers.set(name, value);
    }
  }
  return new Response(JSON.stringify(data), { status, headers });
}

function apiFailure(error) {
  if (error instanceof ApiError) {
    return jsonResponse(
      {
        error: {
          code: error.code,
          message: error.message,
        },
      },
      error.status,
      error.headers,
    );
  }

  console.error("SoulTech Worker request failed");
  return jsonResponse(
    {
      error: {
        code: "internal_error",
        message: "服务器暂时无法处理请求",
      },
    },
    500,
  );
}
