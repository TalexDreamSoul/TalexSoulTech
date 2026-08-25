# Error Handling

> Fail fast at contract boundaries, fail closed at security/persistence boundaries, and return stable user-facing errors.

## Java error categories

- Use `IllegalArgumentException` for invalid caller/domain input: negative energy, invalid loss, malformed IDs, invalid catalog specifications.
- Use `IllegalStateException` for lifecycle/thread/registration violations: duplicate endpoints, off-thread mutation, missing required plugin configuration, partial catalog construction.
- Let startup-critical failures abort plugin enable. Do not catch and downgrade them to a half-enabled runtime.
- For recoverable async player-load failures, publish read-only defaults and log one bounded warning.
- Bukkit commands and interactions return concise player messages instead of exposing stack traces.

```java
if (!Bukkit.isPrimaryThread()) {
    throw new IllegalStateException(operation + " must run on the primary server thread");
}
```

## Transactional behavior

- Validate and simulate before mutation. If energy, structure, ownership, inventory, or protection checks fail, perform no partial side effect.
- Extension activation stages source/KV/runtime resources first. Any failure disposes staged resources and preserves/restores last known good.
- Wireless/item transfers commit both replacement ItemStacks only after both sides simulate successfully.

## Worker API errors

`site/src/worker.js` owns one typed error envelope:

```json
{
  "error": {
    "code": "server_not_found",
    "message": "服务器不存在"
  }
}
```

- Throw `ApiError(status, code, message, headers?)` for expected failures.
- Stable machine-readable codes are part of the API contract; human messages may be localized.
- `apiFailure` maps unknown failures to HTTP 500 / `internal_error` and logs only a generic server-side line.
- Mutation bodies have byte limits, strict key validation, and explicit JSON/type validation before database access.
- Method mismatches, auth failures, quota failures, sequence conflicts, and missing tenant-owned resources use distinct status/code pairs.

## Logging and exposure

- Never return JavaScript exception text, SQL details, stack traces, secret values, hashes, cookies, API keys, or pairing codes.
- Do not include full request bodies in errors or logs.
- Log enough identity to diagnose the domain boundary (safe item/server/extension ID, operation, bounded reason), not credentials or PII.

## Common mistakes

- Catching `RuntimeException` and continuing with partially registered machines/items.
- Clamping invalid domain values that should be rejected.
- Consuming energy before a cancellable Paper event succeeds.
- Returning HTTP 200 with an embedded error.
- Treating a transient cloud/network failure as an empty authoritative snapshot.
