# Database Guidelines

> TalexSoulTech uses direct MySQL and Cloudflare D1 access; there is no ORM.

## Storage boundaries

- Paper player persistence uses `MysqlManager`. It is optional and controlled by `Settings.mysql.enabled`.
- Cloud SaaS state uses D1 through `env.DB.prepare(...).bind(...)` and ordered SQL migrations.
- ItemStack energy, machine runtime buffers, wilderness Chunk PDC, and extension local KV are not mirrored into MySQL or D1.

## Paper / MySQL

- Startup is fail-closed when MySQL is enabled: acquire the data-folder lock and connect before publishing writable player data.
- Disabled MySQL means explicit in-memory, non-durable defaults. Never silently turn a failed enabled connection into writable defaults.
- Parameterize player operations by UUID. The bounded persistence worker performs JDBC I/O; the Paper thread only publishes complete `PlayerData` instances.
- Fence asynchronous loads with the per-login session token. A stale completion must not replace a newer login.
- A failed or malformed load yields read-only defaults, so shutdown cannot overwrite a valid persisted row.

```java
mysqlManager.enqueuePlayerLoad(
    new MysqlManager.PlayerLoadRequest(uuid, session.name(), session.token())
);

if (current == null || current.token() != result.session()) {
    continue; // stale completion
}
```

## Cloudflare / D1 queries

- Always use prepared statements and `.bind(...)`; never interpolate request data into SQL.
- Scope every server, snapshot, pairing code, API key, and extension query through owner/server identity.
- Use `env.DB.batch([...])` when one logical mutation writes several rows or audit events. Validate affected-row counts after the batch.
- Return only public projections. Password hashes, token hashes, pairing-code hashes, and API-key hashes never cross the API boundary.

```js
const server = await env.DB.prepare(
  `SELECT id, name, last_sequence
   FROM servers
   WHERE id = ? AND owner_user_id = ?
   LIMIT 1`,
).bind(serverId, user.id).first();
```

## Migrations

- Files under `site/migrations/` are ordered, additive, and production-reviewed.
- Release order is migration -> Worker deploy -> live API checks -> Paper deploy.
- Never edit an applied migration. Add a forward migration or an explicitly reviewed forward repair.
- Destructive DDL/data deletion requires a separate migration plan, backup, and explicit approval.

## Naming

- Tables and columns use `snake_case`; primary IDs retain domain prefixes such as `srv_`, `snp_`, and `evt_`.
- Constraint/index names describe the invariant, especially quota and uniqueness constraints.
- MySQL legacy tables keep their established `soul_tech_*` and `st_*` names until a separately approved migration.

## Common failures to avoid

- JDBC work on the Paper thread.
- Publishing partial/default player data before load completion.
- Missing tenant predicates in D1 reads.
- Retrying a snapshot with a newly serialized body or a lower sequence.
- Treating D1 batch success as proof that every expected row changed; inspect `meta.changes`.
