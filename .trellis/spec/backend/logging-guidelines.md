# Logging Guidelines

> Logs are operational evidence, not a data export channel.

## Logging surfaces

- Paper uses `JavaPlugin#getLogger()` for runtime state and the existing colored `plugin.log(...)` helper for legacy lifecycle messages.
- Cloudflare uses `console.error` only for unexpected failures; expected API errors flow through `ApiError` without noisy stack traces.
- Durable business actions are recorded as tenant-scoped D1 audit events rather than reconstructed from console logs.

## Levels

- **INFO**: lifecycle transitions, catalog counts, machine load/save completion, successful cloud sync summary, measured durations.
- **WARNING**: explicitly degraded but safe operation, such as MySQL-disabled non-durable mode, read-only player fallback, invalid optional config using a documented safe default.
- **SEVERE / error**: a service cannot uphold its contract. Prefer failing plugin startup or the request instead of repeatedly logging from a tick loop.
- Debug detail belongs behind a deliberate debug command/config; do not add unconditional per-tick/per-event logs.

## Required context

A useful operational line names the bounded operation and safe identity:

```java
plugin.getLogger().info(
    "Machine catalog ready: " + total
        + " total (legacy=" + legacy
        + ", powered-multiblock=" + powered + ")"
);
```

Include counts, revisions, sequence numbers, durations, and stable non-secret IDs when they change an operator decision.

## Never log

- Passwords, cookies, session tokens, API keys, pairing codes, secret config, full authorization headers.
- Full snapshot/request bodies or extension source.
- Complete process command lines when they may contain credentials.
- Player IP addresses copied into project reports.
- Repeated expected failures every tick; aggregate or rate-limit them.

## Failure reporting

- Paper asynchronous workers publish bounded warning summaries back to the main-thread owner.
- Worker unexpected failures log only `SoulTech Worker request failed`; the client receives `internal_error`.
- Release evidence records hashes, commit IDs, Worker version IDs, counts, and timestamps, never credentials.

## Review checklist

- Is the line actionable and bounded?
- Can it fire once per tick/entity/block? If so, remove or aggregate it.
- Does it expose user or secret data?
- Does warning/error correspond to a safe degraded state or a failed contract?
