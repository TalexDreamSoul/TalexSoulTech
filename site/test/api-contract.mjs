import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import test from "node:test";

const DEFAULT_BASE_URL = "http://127.0.0.1:8788";
const REQUEST_TIMEOUT_MS = 15_000;
const SESSION_COOKIE = "soultech_session";
const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "::1", "[::1]"]);
const API_KEY_PATTERN = /^st_live_[A-Za-z0-9_-]{43}$/;
const PAIRING_CODE_PATTERN = /^ST-[A-HJ-NP-Z2-9]{4}(?:-[A-HJ-NP-Z2-9]{4}){3}$/;
const SHA256_PATTERN = /^[a-f0-9]{64}$/;
const ETAG_PATTERN = /^"[a-f0-9]{64}"$/;

const BASE_URL = resolveBaseUrl();

class CookieJar {
  #cookies = new Map();

  capture(response) {
    const setCookies = typeof response.headers.getSetCookie === "function"
      ? response.headers.getSetCookie()
      : [response.headers.get("set-cookie")].filter(Boolean);

    for (const setCookie of setCookies) {
      const [nameValue, ...attributes] = setCookie.split(";");
      const separator = nameValue.indexOf("=");
      if (separator < 1) {
        continue;
      }

      const name = nameValue.slice(0, separator).trim();
      const value = nameValue.slice(separator + 1).trim();
      const expiresImmediately = attributes.some((attribute) => /^max-age\s*=\s*0$/i.test(attribute.trim()));
      if (expiresImmediately || value.length === 0) {
        this.#cookies.delete(name);
      } else {
        this.#cookies.set(name, value);
      }
    }
  }

  header() {
    return [...this.#cookies.entries()]
      .map(([name, value]) => `${name}=${value}`)
      .join("; ");
  }
}

function resolveBaseUrl() {
  const configured = process.env.BASE_URL ?? DEFAULT_BASE_URL;
  let url;

  try {
    url = new URL(configured);
  } catch {
    throw new Error("BASE_URL rejected: expected a localhost HTTP(S) origin.");
  }

  if (
    !["http:", "https:"].includes(url.protocol) ||
    !LOCAL_HOSTS.has(url.hostname.toLowerCase()) ||
    url.username ||
    url.password ||
    url.pathname !== "/" ||
    url.search ||
    url.hash
  ) {
    throw new Error("BASE_URL rejected: API contract traffic is limited to a bare localhost HTTP(S) origin.");
  }

  return new URL(url.origin);
}

function assertClaimApiBase(value, contract) {
  if (typeof value !== "string") {
    assert.fail(`${contract}: apiBase must be a valid root HTTP(S) URL.`);
  }

  let apiBase;
  try {
    apiBase = new URL(value);
  } catch {
    assert.fail(`${contract}: apiBase must be a valid root HTTP(S) URL.`);
    return;
  }

  assert.ok(
    ["http:", "https:"].includes(apiBase.protocol) &&
      !apiBase.username &&
      !apiBase.password &&
      apiBase.pathname === "/" &&
      !apiBase.search &&
      !apiBase.hash,
    `${contract}: apiBase must be a root HTTP(S) URL without credentials.`,
  );

  const apiBaseIsLocal = LOCAL_HOSTS.has(apiBase.hostname.toLowerCase());
  if (apiBaseIsLocal) {
    assert.ok(
      LOCAL_HOSTS.has(BASE_URL.hostname.toLowerCase()),
      `${contract}: a localhost apiBase is only valid for a localhost BASE_URL.`,
    );
  } else {
    assert.equal(apiBase.protocol, "https:", `${contract}: non-local apiBase must use HTTPS.`);
  }
}

function endpointUrl(path) {
  const url = new URL(path, BASE_URL);
  if (url.origin !== BASE_URL.origin) {
    throw new Error(`${path}: request target escaped the local API origin.`);
  }
  return url;
}

async function request(contract, path, { method = "GET", jar, bearer, json, headers } = {}) {
  const requestHeaders = new Headers(headers);
  requestHeaders.set("accept", "application/json");

  if (json !== undefined) {
    requestHeaders.set("content-type", "application/json");
  }

  const cookie = jar?.header();
  if (cookie) {
    requestHeaders.set("cookie", cookie);
  }
  if (bearer) {
    requestHeaders.set("authorization", `Bearer ${bearer}`);
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  let response;

  try {
    response = await fetch(endpointUrl(path), {
      method,
      headers: requestHeaders,
      body: json === undefined ? undefined : JSON.stringify(json),
      signal: controller.signal,
    });
  } catch {
    const failure = controller.signal.aborted ? "request timed out" : "network request failed";
    throw new Error(`${contract}: ${failure}.`);
  } finally {
    clearTimeout(timeout);
  }

  jar?.capture(response);
  return response;
}

async function expectJson(response, expectedStatus, contract) {
  assert.equal(response.status, expectedStatus, `${contract}: expected HTTP ${expectedStatus}.`);
  assert.match(
    response.headers.get("content-type") ?? "",
    /^application\/json(?:\s*;|$)/i,
    `${contract}: response must declare JSON content.`,
  );

  try {
    return await response.json();
  } catch {
    assert.fail(`${contract}: response body must be valid JSON.`);
  }
}

function assertRecord(value, contract) {
  assert.ok(
    value !== null && typeof value === "object" && !Array.isArray(value),
    `${contract}: response value must be a JSON object.`,
  );
}

function assertKeys(value, expectedKeys, contract) {
  assertRecord(value, contract);
  assert.deepEqual(
    Object.keys(value).sort(),
    [...expectedKeys].sort(),
    `${contract}: response JSON keys changed.`,
  );
}

function assertTimestamp(value, contract) {
  assert.ok(
    typeof value === "string" && Number.isFinite(Date.parse(value)),
    `${contract}: expected an ISO-compatible timestamp.`,
  );
}

function assertError(body, expectedCode, contract) {
  assertKeys(body, ["error"], contract);
  assertKeys(body.error, ["code", "message"], contract);
  assert.equal(body.error.code, expectedCode, `${contract}: error code changed.`);
  assert.ok(
    typeof body.error.message === "string" && body.error.message.length > 0,
    `${contract}: error message must be a non-empty string.`,
  );
}

function assertUser(user, expected, contract) {
  assertKeys(user, ["id", "username", "createdAt"], contract);
  assert.equal(user.id, expected.id, `${contract}: user id changed.`);
  assert.equal(user.username, expected.username, `${contract}: username changed.`);
  assertTimestamp(user.createdAt, contract);
}

function assertServer(server, expected, contract) {
  assertKeys(
    server,
    ["id", "name", "softwareVersion", "pairedAt", "lastSequence", "lastSyncAt", "createdAt", "updatedAt"],
    contract,
  );
  assert.equal(server.id, expected.id, `${contract}: server id changed.`);
  assert.equal(server.name, expected.name, `${contract}: server name changed.`);
  assert.equal(server.softwareVersion, expected.softwareVersion, `${contract}: software version changed.`);
  assert.ok(Number.isSafeInteger(server.lastSequence), `${contract}: lastSequence must be an integer.`);
  assert.ok(server.pairedAt === null || typeof server.pairedAt === "string", `${contract}: pairedAt shape changed.`);
  assert.ok(server.lastSyncAt === null || typeof server.lastSyncAt === "string", `${contract}: lastSyncAt shape changed.`);
  assertTimestamp(server.createdAt, contract);
  assertTimestamp(server.updatedAt, contract);
}

function assertOwnerExtension(extension, expected, contract) {
  assertKeys(
    extension,
    ["manifest", "source", "sha256", "enabled", "revision", "createdAt", "updatedAt"],
    contract,
  );
  assert.deepEqual(extension.manifest, expected.manifest, `${contract}: manifest changed.`);
  assert.equal(extension.source, expected.source, `${contract}: extension source changed.`);
  assert.ok(SHA256_PATTERN.test(extension.sha256), `${contract}: SHA-256 format changed.`);
  assert.equal(extension.enabled, expected.enabled, `${contract}: enabled state changed.`);
  assert.ok(
    Number.isSafeInteger(extension.revision) && extension.revision > 0,
    `${contract}: revision must be a positive integer.`,
  );
  assertTimestamp(extension.createdAt, contract);
  assertTimestamp(extension.updatedAt, contract);
}

function assertPluginExtension(extension, expected, contract) {
  assertKeys(extension, ["manifest", "sha256", "enabled", "revision"], contract);
  assert.deepEqual(extension.manifest, expected.manifest, `${contract}: plugin manifest changed.`);
  assert.ok(SHA256_PATTERN.test(extension.sha256), `${contract}: SHA-256 format changed.`);
  assert.equal(extension.enabled, expected.enabled, `${contract}: plugin enabled state changed.`);
  assert.ok(
    Number.isSafeInteger(extension.revision) && extension.revision > 0,
    `${contract}: plugin revision must be a positive integer.`,
  );
}

function assertAcceptedSync(body, sequence, contract) {
  assertKeys(body, ["accepted", "sequence", "serverTime"], contract);
  assert.equal(body.accepted, true, `${contract}: snapshot was not accepted.`);
  assert.equal(body.sequence, sequence, `${contract}: accepted sequence changed.`);
  assertTimestamp(body.serverTime, contract);
}

function findById(items, id, contract) {
  assert.ok(Array.isArray(items), `${contract}: expected an array.`);
  const item = items.find((candidate) => candidate?.id === id || candidate?.manifest?.id === id);
  assert.ok(item, `${contract}: expected fixture item was not returned.`);
  return item;
}

function createFixture() {
  const suffix = randomUUID().replaceAll("-", "").slice(0, 18);
  return {
    passwordA: `contract-a-${randomUUID()}`,
    passwordB: `contract-b-${randomUUID()}`,
    usernameA: `api_ct_a_${suffix}`,
    usernameB: `api_ct_b_${suffix}`,
    serverName: `API contract ${suffix}`,
    pluginServerName: `Plugin contract ${suffix}`,
    softwareVersion: "3.0.0-contract",
    extensionId: `contract-${suffix}`,
    extensionName: `Contract extension ${suffix}`,
    marker: `marker-${suffix}`,
  };
}

function makeSnapshot(serverId, sequence, marker) {
  return {
    serverId,
    sequence,
    sentAt: "2026-01-02T03:04:05.000Z",
    server: { contract: marker },
    players: [{ id: `player-${marker}`, online: true }],
    systems: { contract: { marker, sequence } },
    catalog: [{ id: `catalog-${marker}`, enabled: true }],
  };
}

test("SoulTech local Worker API contract", { concurrency: false }, async () => {
  const fixture = createFixture();
  const tenantA = new CookieJar();
  const tenantB = new CookieJar();

  const health = await expectJson(
    await request("GET /api/health health contract", "/api/health"),
    200,
    "GET /api/health health contract",
  );
  assertKeys(health, ["ok", "serverTime"], "GET /api/health health contract");
  assert.equal(health.ok, true, "GET /api/health health contract: health check changed.");
  assertTimestamp(health.serverTime, "GET /api/health health contract");

  const registrationA = await expectJson(
    await request("POST /api/auth/register tenant A registration", "/api/auth/register", {
      method: "POST",
      jar: tenantA,
      json: { username: fixture.usernameA, password: fixture.passwordA },
    }),
    201,
    "POST /api/auth/register tenant A registration",
  );
  assertKeys(registrationA, ["user"], "POST /api/auth/register tenant A registration");
  const userA = registrationA.user;
  assertKeys(userA, ["id", "username", "createdAt"], "POST /api/auth/register tenant A registration");
  assert.equal(userA.username, fixture.usernameA, "POST /api/auth/register tenant A registration: username changed.");
  assertTimestamp(userA.createdAt, "POST /api/auth/register tenant A registration");

  const initialMe = await expectJson(
    await request("GET /api/auth/me registered session", "/api/auth/me", { jar: tenantA }),
    200,
    "GET /api/auth/me registered session",
  );
  assertKeys(initialMe, ["user"], "GET /api/auth/me registered session");
  assertUser(initialMe.user, userA, "GET /api/auth/me registered session");

  const logout = await expectJson(
    await request("POST /api/auth/logout session revocation", "/api/auth/logout", {
      method: "POST",
      jar: tenantA,
      json: {},
    }),
    200,
    "POST /api/auth/logout session revocation",
  );
  assertKeys(logout, ["ok"], "POST /api/auth/logout session revocation");
  assert.equal(logout.ok, true, "POST /api/auth/logout session revocation: logout acknowledgement changed.");

  const loggedOutMe = await expectJson(
    await request("GET /api/auth/me logged-out rejection", "/api/auth/me", { jar: tenantA }),
    401,
    "GET /api/auth/me logged-out rejection",
  );
  assertError(loggedOutMe, "authentication_required", "GET /api/auth/me logged-out rejection");

  const loginA = await expectJson(
    await request("POST /api/auth/login tenant A login", "/api/auth/login", {
      method: "POST",
      jar: tenantA,
      json: { username: fixture.usernameA, password: fixture.passwordA },
    }),
    200,
    "POST /api/auth/login tenant A login",
  );
  assertKeys(loginA, ["user"], "POST /api/auth/login tenant A login");
  assertUser(loginA.user, userA, "POST /api/auth/login tenant A login");

  const loggedInMe = await expectJson(
    await request("GET /api/auth/me logged-in session", "/api/auth/me", { jar: tenantA }),
    200,
    "GET /api/auth/me logged-in session",
  );
  assertKeys(loggedInMe, ["user"], "GET /api/auth/me logged-in session");
  assertUser(loggedInMe.user, userA, "GET /api/auth/me logged-in session");

  const createdServer = await expectJson(
    await request("POST /api/servers server creation", "/api/servers", {
      method: "POST",
      jar: tenantA,
      json: { name: fixture.serverName, softwareVersion: fixture.softwareVersion },
    }),
    201,
    "POST /api/servers server creation",
  );
  assertKeys(createdServer, ["server"], "POST /api/servers server creation");
  assertKeys(
    createdServer.server,
    ["id", "name", "softwareVersion", "pairedAt", "lastSequence", "lastSyncAt", "createdAt", "updatedAt"],
    "POST /api/servers server creation",
  );
  assert.ok(
    typeof createdServer.server.id === "string" && createdServer.server.id.length > 0,
    "POST /api/servers server creation: server id must be a non-empty string.",
  );
  const serverId = createdServer.server.id;
  assertServer(
    createdServer.server,
    { id: serverId, name: fixture.serverName, softwareVersion: fixture.softwareVersion },
    "POST /api/servers server creation",
  );

  const ownerList = await expectJson(
    await request("GET /api/servers owner list", "/api/servers?limit=100", { jar: tenantA }),
    200,
    "GET /api/servers owner list",
  );
  assertKeys(ownerList, ["servers"], "GET /api/servers owner list");
  assertServer(
    findById(ownerList.servers, serverId, "GET /api/servers owner list"),
    { id: serverId, name: fixture.serverName, softwareVersion: fixture.softwareVersion },
    "GET /api/servers owner list",
  );

  const ownerDetail = await expectJson(
    await request("GET /api/servers/:id owner detail", `/api/servers/${encodeURIComponent(serverId)}`, { jar: tenantA }),
    200,
    "GET /api/servers/:id owner detail",
  );
  assertKeys(ownerDetail, ["server"], "GET /api/servers/:id owner detail");
  assertServer(
    ownerDetail.server,
    { id: serverId, name: fixture.serverName, softwareVersion: fixture.softwareVersion },
    "GET /api/servers/:id owner detail",
  );

  const registrationB = await expectJson(
    await request("POST /api/auth/register tenant B registration", "/api/auth/register", {
      method: "POST",
      jar: tenantB,
      json: { username: fixture.usernameB, password: fixture.passwordB },
    }),
    201,
    "POST /api/auth/register tenant B registration",
  );
  assertKeys(registrationB, ["user"], "POST /api/auth/register tenant B registration");

  const foreignDetail = await expectJson(
    await request("GET /api/servers/:id tenant isolation", `/api/servers/${encodeURIComponent(serverId)}`, { jar: tenantB }),
    404,
    "GET /api/servers/:id tenant isolation",
  );
  assertError(foreignDetail, "server_not_found", "GET /api/servers/:id tenant isolation");

  const foreignList = await expectJson(
    await request("GET /api/servers tenant isolation", "/api/servers?limit=100", { jar: tenantB }),
    200,
    "GET /api/servers tenant isolation",
  );
  assertKeys(foreignList, ["servers"], "GET /api/servers tenant isolation");
  assert.ok(Array.isArray(foreignList.servers), "GET /api/servers tenant isolation: servers must be an array.");
  assert.ok(
    !foreignList.servers.some((server) => server?.id === serverId),
    "GET /api/servers tenant isolation: another tenant's server leaked into the list.",
  );
  const rateLimitAddress = `contract-rate-${fixture.marker}`;
  const invalidRateLimitUsername = `rate_${fixture.marker}`;
  for (let attempt = 1; attempt <= 10; attempt += 1) {
    const invalidLogin = await expectJson(
      await request(`POST /api/auth/login isolated rate-limit attempt ${attempt}`, "/api/auth/login", {
        method: "POST",
        json: { username: invalidRateLimitUsername, password: fixture.passwordA },
        headers: { "cf-connecting-ip": rateLimitAddress },
      }),
      401,
      `POST /api/auth/login isolated rate-limit attempt ${attempt}`,
    );
    assertError(invalidLogin, "invalid_credentials", `POST /api/auth/login isolated rate-limit attempt ${attempt}`);
  }

  const rateLimitedResponse = await request("POST /api/auth/login isolated rate-limit rejection", "/api/auth/login", {
    method: "POST",
    json: { username: invalidRateLimitUsername, password: fixture.passwordA },
    headers: { "cf-connecting-ip": rateLimitAddress },
  });
  const rateLimited = await expectJson(
    rateLimitedResponse,
    429,
    "POST /api/auth/login isolated rate-limit rejection",
  );
  assertError(rateLimited, "auth_rate_limited", "POST /api/auth/login isolated rate-limit rejection");
  assert.match(
    rateLimitedResponse.headers.get("retry-after") ?? "",
    /^[1-9][0-9]*$/,
    "POST /api/auth/login isolated rate-limit rejection: Retry-After must be a positive integer.",
  );


  const firstPairing = await expectJson(
    await request("POST /api/servers/:id/pairing first pairing code", `/api/servers/${encodeURIComponent(serverId)}/pairing`, {
      method: "POST",
      jar: tenantA,
      json: {},
    }),
    201,
    "POST /api/servers/:id/pairing first pairing code",
  );
  assertKeys(firstPairing, ["serverId", "code", "expiresAt"], "POST /api/servers/:id/pairing first pairing code");
  assert.equal(firstPairing.serverId, serverId, "POST /api/servers/:id/pairing first pairing code: server id changed.");
  assert.ok(PAIRING_CODE_PATTERN.test(firstPairing.code), "POST /api/servers/:id/pairing first pairing code: code format changed.");
  assertTimestamp(firstPairing.expiresAt, "POST /api/servers/:id/pairing first pairing code");

  const firstClaim = await expectJson(
    await request("POST /api/pair/claim initial claim", "/api/pair/claim", {
      method: "POST",
      json: {
        code: firstPairing.code,
        name: fixture.pluginServerName,
        softwareVersion: fixture.softwareVersion,
      },
    }),
    200,
    "POST /api/pair/claim initial claim",
  );
  assertKeys(firstClaim, ["serverId", "apiKey", "lastSequence", "apiBase"], "POST /api/pair/claim initial claim");
  assert.equal(firstClaim.serverId, serverId, "POST /api/pair/claim initial claim: server id changed.");
  assert.ok(API_KEY_PATTERN.test(firstClaim.apiKey), "POST /api/pair/claim initial claim: API key format changed.");
  assert.equal(firstClaim.lastSequence, 0, "POST /api/pair/claim initial claim: initial sequence changed.");
  assertClaimApiBase(firstClaim.apiBase, "POST /api/pair/claim initial claim");
  const initialPluginApiKey = firstClaim.apiKey;


  const repeatedClaim = await expectJson(
    await request("POST /api/pair/claim one-time claim", "/api/pair/claim", {
      method: "POST",
      json: {
        code: firstPairing.code,
        name: fixture.pluginServerName,
        softwareVersion: fixture.softwareVersion,
      },
    }),
    400,
    "POST /api/pair/claim one-time claim",
  );
  assertError(repeatedClaim, "pairing_code_invalid", "POST /api/pair/claim one-time claim");

  const rotationPairing = await expectJson(
    await request("POST /api/servers/:id/pairing key rotation", `/api/servers/${encodeURIComponent(serverId)}/pairing`, {
      method: "POST",
      jar: tenantA,
      json: {},
    }),
    201,
    "POST /api/servers/:id/pairing key rotation",
  );
  assertKeys(rotationPairing, ["serverId", "code", "expiresAt"], "POST /api/servers/:id/pairing key rotation");
  assert.ok(PAIRING_CODE_PATTERN.test(rotationPairing.code), "POST /api/servers/:id/pairing key rotation: code format changed.");


  const firstSequence = firstClaim.lastSequence + 1;
  const firstSnapshot = makeSnapshot(serverId, firstSequence, `${fixture.marker}-first`);
  const firstSync = await expectJson(
    await request("POST /api/sync first sequence", "/api/sync", {
      method: "POST",
      bearer: initialPluginApiKey,
      json: firstSnapshot,
    }),
    200,
    "POST /api/sync first sequence",
  );
  assertAcceptedSync(firstSync, firstSequence, "POST /api/sync first sequence");

  const idempotentSync = await expectJson(
    await request("POST /api/sync idempotent sequence", "/api/sync", {
      method: "POST",
      bearer: initialPluginApiKey,
      json: firstSnapshot,
    }),
    200,
    "POST /api/sync idempotent sequence",
  );
  assertAcceptedSync(idempotentSync, firstSequence, "POST /api/sync idempotent sequence");

  const conflictingSnapshot = makeSnapshot(serverId, firstSequence, `${fixture.marker}-conflict`);
  const sequenceConflict = await expectJson(
    await request("POST /api/sync sequence conflict", "/api/sync", {
      method: "POST",
      bearer: initialPluginApiKey,
      json: conflictingSnapshot,
    }),
    409,
    "POST /api/sync sequence conflict",
  );
  assertError(sequenceConflict, "sequence_conflict", "POST /api/sync sequence conflict");

  const rotatedClaim = await expectJson(
    await request("POST /api/pair/claim key rotation", "/api/pair/claim", {
      method: "POST",
      json: {
        code: rotationPairing.code,
        name: fixture.pluginServerName,
        softwareVersion: fixture.softwareVersion,
      },
    }),
    200,
    "POST /api/pair/claim key rotation",
  );
  assertKeys(rotatedClaim, ["serverId", "apiKey", "lastSequence", "apiBase"], "POST /api/pair/claim key rotation");
  assert.equal(rotatedClaim.serverId, serverId, "POST /api/pair/claim key rotation: server id changed.");
  assert.ok(API_KEY_PATTERN.test(rotatedClaim.apiKey), "POST /api/pair/claim key rotation: API key format changed.");
  assert.equal(
    rotatedClaim.lastSequence,
    firstSequence,
    "POST /api/pair/claim key rotation: lastSequence did not preserve the prior accepted sequence.",
  );
  assert.ok(rotatedClaim.apiKey !== initialPluginApiKey, "POST /api/pair/claim key rotation: API key did not rotate.");
  assertClaimApiBase(rotatedClaim.apiBase, "POST /api/pair/claim key rotation");
  const pluginApiKey = rotatedClaim.apiKey;

  const revokedKey = await expectJson(
    await request("GET /api/extensions/manifest revoked key rejection", "/api/extensions/manifest", {
      bearer: initialPluginApiKey,
    }),
    401,
    "GET /api/extensions/manifest revoked key rejection",
  );
  assertError(revokedKey, "invalid_api_key", "GET /api/extensions/manifest revoked key rejection");

  const nextSequence = rotatedClaim.lastSequence + 1;
  const nextSnapshot = makeSnapshot(serverId, nextSequence, `${fixture.marker}-next`);
  const nextSync = await expectJson(
    await request("POST /api/sync next sequence", "/api/sync", {
      method: "POST",
      bearer: pluginApiKey,
      json: nextSnapshot,
    }),
    200,
    "POST /api/sync next sequence",
  );
  assertAcceptedSync(nextSync, nextSequence, "POST /api/sync next sequence");

  const sequenceReverted = await expectJson(
    await request("POST /api/sync sequence rollback", "/api/sync", {
      method: "POST",
      bearer: pluginApiKey,
      json: firstSnapshot,
    }),
    409,
    "POST /api/sync sequence rollback",
  );
  assertError(sequenceReverted, "sequence_reverted", "POST /api/sync sequence rollback");

  const ownerSnapshot = await expectJson(
    await request("GET /api/servers/:id/snapshot owner read", `/api/servers/${encodeURIComponent(serverId)}/snapshot`, {
      jar: tenantA,
    }),
    200,
    "GET /api/servers/:id/snapshot owner read",
  );
  assertKeys(ownerSnapshot, ["snapshot"], "GET /api/servers/:id/snapshot owner read");
  assertKeys(
    ownerSnapshot.snapshot,
    ["serverId", "sequence", "sentAt", "receivedAt", "server", "players", "systems", "catalog"],
    "GET /api/servers/:id/snapshot owner read",
  );
  assert.equal(ownerSnapshot.snapshot.serverId, serverId, "GET /api/servers/:id/snapshot owner read: server id changed.");
  assert.equal(ownerSnapshot.snapshot.sequence, nextSequence, "GET /api/servers/:id/snapshot owner read: latest sequence changed.");
  assert.equal(
    ownerSnapshot.snapshot.systems?.contract?.marker,
    `${fixture.marker}-next`,
    "GET /api/servers/:id/snapshot owner read: latest snapshot payload changed.",
  );
  assertTimestamp(ownerSnapshot.snapshot.sentAt, "GET /api/servers/:id/snapshot owner read");
  assertTimestamp(ownerSnapshot.snapshot.receivedAt, "GET /api/servers/:id/snapshot owner read");

  const extensionManifest = {
    id: fixture.extensionId,
    name: fixture.extensionName,
    version: "1.0.0-contract",
    engine: "javascript",
    entry: "main",
    dependencies: [],
    permissions: ["log"],
  };
  const extensionSource = `export function main() { return ${JSON.stringify(fixture.marker)}; }\n`;
  const extensionPayload = { manifest: extensionManifest, source: extensionSource, enabled: false };

  for (const invalidExtension of [
    { name: "leading hyphen", id: `-${fixture.extensionId}` },
    { name: "trailing hyphen", id: `${fixture.extensionId}-` },
    { name: "double hyphen", id: `contract--${fixture.marker}` },
  ]) {
    const contract = `POST /api/servers/:id/extensions ${invalidExtension.name} extension ID rejection`;
    const invalidExtensionResponse = await expectJson(
      await request(contract, `/api/servers/${encodeURIComponent(serverId)}/extensions`, {
        method: "POST",
        jar: tenantA,
        json: {
          ...extensionPayload,
          manifest: { ...extensionManifest, id: invalidExtension.id },
        },
      }),
      400,
      contract,
    );
    assertError(invalidExtensionResponse, "invalid_extension_id", contract);
  }

  const createdExtension = await expectJson(
    await request("POST /api/servers/:id/extensions extension creation", `/api/servers/${encodeURIComponent(serverId)}/extensions`, {
      method: "POST",
      jar: tenantA,
      json: extensionPayload,
    }),
    201,
    "POST /api/servers/:id/extensions extension creation",
  );
  assertKeys(createdExtension, ["extension"], "POST /api/servers/:id/extensions extension creation");
  assertOwnerExtension(
    createdExtension.extension,
    { manifest: extensionManifest, source: extensionSource, enabled: false },
    "POST /api/servers/:id/extensions extension creation",
  );

  const ownerExtensions = await expectJson(
    await request("GET /api/servers/:id/extensions owner list", `/api/servers/${encodeURIComponent(serverId)}/extensions`, {
      jar: tenantA,
    }),
    200,
    "GET /api/servers/:id/extensions owner list",
  );
  assertKeys(ownerExtensions, ["extensions"], "GET /api/servers/:id/extensions owner list");
  assertOwnerExtension(
    findById(ownerExtensions.extensions, fixture.extensionId, "GET /api/servers/:id/extensions owner list"),
    { manifest: extensionManifest, source: extensionSource, enabled: false },
    "GET /api/servers/:id/extensions owner list",
  );

  const disabledSource = await expectJson(
    await request("GET /api/extensions/:id/source disabled denial", `/api/extensions/${encodeURIComponent(fixture.extensionId)}/source`, {
      bearer: pluginApiKey,
    }),
    404,
    "GET /api/extensions/:id/source disabled denial",
  );
  assertError(disabledSource, "extension_not_found", "GET /api/extensions/:id/source disabled denial");

  const firstPluginManifestResponse = await request(
    "GET /api/extensions/manifest initial plugin manifest",
    "/api/extensions/manifest",
    { bearer: pluginApiKey },
  );
  const firstPluginManifest = await expectJson(
    firstPluginManifestResponse,
    200,
    "GET /api/extensions/manifest initial plugin manifest",
  );
  assertKeys(firstPluginManifest, ["extensions"], "GET /api/extensions/manifest initial plugin manifest");
  assertPluginExtension(
    findById(firstPluginManifest.extensions, fixture.extensionId, "GET /api/extensions/manifest initial plugin manifest"),
    { manifest: extensionManifest, enabled: false },
    "GET /api/extensions/manifest initial plugin manifest",
  );
  const initialEtag = firstPluginManifestResponse.headers.get("etag");
  assert.ok(ETAG_PATTERN.test(initialEtag ?? ""), "GET /api/extensions/manifest initial plugin manifest: ETag format changed.");

  const notModifiedResponse = await request(
    "GET /api/extensions/manifest conditional request",
    "/api/extensions/manifest",
    { bearer: pluginApiKey, headers: { "if-none-match": initialEtag } },
  );
  assert.equal(notModifiedResponse.status, 304, "GET /api/extensions/manifest conditional request: expected HTTP 304.");
  assert.ok(
    notModifiedResponse.headers.get("etag") === initialEtag,
    "GET /api/extensions/manifest conditional request: ETag changed on 304.",
  );
  assert.ok(
    (await notModifiedResponse.text()).length === 0,
    "GET /api/extensions/manifest conditional request: 304 must not include a response body.",
  );

  const enabledExtension = await expectJson(
    await request(
      "POST /api/servers/:id/extensions/:extensionId/state enable extension",
      `/api/servers/${encodeURIComponent(serverId)}/extensions/${encodeURIComponent(fixture.extensionId)}/state`,
      { method: "POST", jar: tenantA, json: { enabled: true } },
    ),
    200,
    "POST /api/servers/:id/extensions/:extensionId/state enable extension",
  );
  assertKeys(enabledExtension, ["extension"], "POST /api/servers/:id/extensions/:extensionId/state enable extension");
  assertOwnerExtension(
    enabledExtension.extension,
    { manifest: extensionManifest, source: extensionSource, enabled: true },
    "POST /api/servers/:id/extensions/:extensionId/state enable extension",
  );

  const enabledPluginManifestResponse = await request(
    "GET /api/extensions/manifest enabled plugin manifest",
    "/api/extensions/manifest",
    { bearer: pluginApiKey },
  );
  const enabledPluginManifest = await expectJson(
    enabledPluginManifestResponse,
    200,
    "GET /api/extensions/manifest enabled plugin manifest",
  );
  assertKeys(enabledPluginManifest, ["extensions"], "GET /api/extensions/manifest enabled plugin manifest");
  assertPluginExtension(
    findById(enabledPluginManifest.extensions, fixture.extensionId, "GET /api/extensions/manifest enabled plugin manifest"),
    { manifest: extensionManifest, enabled: true },
    "GET /api/extensions/manifest enabled plugin manifest",
  );
  const enabledEtag = enabledPluginManifestResponse.headers.get("etag");
  assert.ok(ETAG_PATTERN.test(enabledEtag ?? ""), "GET /api/extensions/manifest enabled plugin manifest: ETag format changed.");
  assert.ok(enabledEtag !== initialEtag, "GET /api/extensions/manifest enabled plugin manifest: ETag did not change after enable.");

  const enabledSource = await expectJson(
    await request("GET /api/extensions/:id/source enabled source", `/api/extensions/${encodeURIComponent(fixture.extensionId)}/source`, {
      bearer: pluginApiKey,
    }),
    200,
    "GET /api/extensions/:id/source enabled source",
  );
  assertKeys(enabledSource, ["manifest", "source", "revision", "sha256"], "GET /api/extensions/:id/source enabled source");
  assert.deepEqual(enabledSource.manifest, extensionManifest, "GET /api/extensions/:id/source enabled source: manifest changed.");
  assert.equal(enabledSource.source, extensionSource, "GET /api/extensions/:id/source enabled source: source changed.");
  assert.ok(SHA256_PATTERN.test(enabledSource.sha256), "GET /api/extensions/:id/source enabled source: SHA-256 format changed.");
  assert.ok(
    Number.isSafeInteger(enabledSource.revision) && enabledSource.revision > 0,
    "GET /api/extensions/:id/source enabled source: revision shape changed.",
  );

  const disabledExtension = await expectJson(
    await request(
      "POST /api/servers/:id/extensions/:extensionId/state disable extension",
      `/api/servers/${encodeURIComponent(serverId)}/extensions/${encodeURIComponent(fixture.extensionId)}/state`,
      { method: "POST", jar: tenantA, json: { enabled: false } },
    ),
    200,
    "POST /api/servers/:id/extensions/:extensionId/state disable extension",
  );
  assertKeys(disabledExtension, ["extension"], "POST /api/servers/:id/extensions/:extensionId/state disable extension");
  assertOwnerExtension(
    disabledExtension.extension,
    { manifest: extensionManifest, source: extensionSource, enabled: false },
    "POST /api/servers/:id/extensions/:extensionId/state disable extension",
  );

  const sourceAfterDisable = await expectJson(
    await request("GET /api/extensions/:id/source post-disable denial", `/api/extensions/${encodeURIComponent(fixture.extensionId)}/source`, {
      bearer: pluginApiKey,
    }),
    404,
    "GET /api/extensions/:id/source post-disable denial",
  );
  assertError(sourceAfterDisable, "extension_not_found", "GET /api/extensions/:id/source post-disable denial");

  const deletedExtension = await expectJson(
    await request(
      "DELETE /api/servers/:id/extensions/:extensionId extension deletion",
      `/api/servers/${encodeURIComponent(serverId)}/extensions/${encodeURIComponent(fixture.extensionId)}`,
      { method: "DELETE", jar: tenantA },
    ),
    200,
    "DELETE /api/servers/:id/extensions/:extensionId extension deletion",
  );
  assertKeys(deletedExtension, ["deleted", "extensionId"], "DELETE /api/servers/:id/extensions/:extensionId extension deletion");
  assert.equal(deletedExtension.deleted, true, "DELETE /api/servers/:id/extensions/:extensionId extension deletion: deletion acknowledgement changed.");
  assert.equal(deletedExtension.extensionId, fixture.extensionId, "DELETE /api/servers/:id/extensions/:extensionId extension deletion: extension id changed.");

  const extensionsAfterDelete = await expectJson(
    await request("GET /api/servers/:id/extensions deleted owner list", `/api/servers/${encodeURIComponent(serverId)}/extensions`, {
      jar: tenantA,
    }),
    200,
    "GET /api/servers/:id/extensions deleted owner list",
  );
  assertKeys(extensionsAfterDelete, ["extensions"], "GET /api/servers/:id/extensions deleted owner list");
  assert.ok(Array.isArray(extensionsAfterDelete.extensions), "GET /api/servers/:id/extensions deleted owner list: extensions must be an array.");
  assert.ok(
    !extensionsAfterDelete.extensions.some((extension) => extension?.manifest?.id === fixture.extensionId),
    "GET /api/servers/:id/extensions deleted owner list: deleted extension remained visible.",
  );

  const pluginManifestAfterDelete = await expectJson(
    await request("GET /api/extensions/manifest deleted plugin manifest", "/api/extensions/manifest", {
      bearer: pluginApiKey,
    }),
    200,
    "GET /api/extensions/manifest deleted plugin manifest",
  );
  assertKeys(pluginManifestAfterDelete, ["extensions"], "GET /api/extensions/manifest deleted plugin manifest");
  assert.ok(Array.isArray(pluginManifestAfterDelete.extensions), "GET /api/extensions/manifest deleted plugin manifest: extensions must be an array.");
  assert.ok(
    !pluginManifestAfterDelete.extensions.some((extension) => extension?.manifest?.id === fixture.extensionId),
    "GET /api/extensions/manifest deleted plugin manifest: deleted extension remained visible to plugins.",
  );
});
