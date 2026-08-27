# Roadmap wave 1 umbrella

## Goal

Close the player-data loop (R1), refine the public site IA (R2), and clear engineering hygiene debt (R3/R4) — in parallel, with strict file-ownership boundaries so sub-agents can work concurrently without conflicts.

## Child task map

| Child | Scope | Priority |
|---|---|---|
| `08-27-telemetry-loop` | Bounded gameplay counters in the Paper plugin -> CloudSync -> Worker/D1 -> admin dashboard panel | P0 |
| `08-27-site-refine` | Remove dead legacy client, audience-grouped nav, `/guide` redirect, download-page release identity, lazy data loading, SSR contract updates | P1 |
| `08-27-eng-hygiene` | Paper deprecated-API migration (3 files), README/CHANGELOG/docs refresh, GitHub Actions CI | P2 |

## Cross-child contracts (frozen)

1. **Admin telemetry mount** — site-refine adds to the `/admin` SSR page: `<section id="telemetry-panel" data-endpoint="/api/admin/telemetry"></section>` and includes script `/admin-telemetry.js`. telemetry-loop provides `site/public/admin-telemetry.js` and the API. Neither side touches the other's files.
2. **worker.js edit boundary** — telemetry-loop may only: add one import of `./telemetry.js`, extend the accepted-snapshot path of `/api/sync`, and add one `/api/admin/telemetry` dispatch in the admin route block. site-refine does not edit `site/src/worker.js` at all. All other telemetry Worker logic lives in new file `site/src/telemetry.js`.
3. **Java boundary** — telemetry-loop owns `src/main/java/pubsher/talexsoultech/telemetry/**`, edits to `cloud/CloudSyncService.java`, and single-line hooks in machine/equipment commit paths. eng-hygiene owns only `particlelib/pobject/Ray.java`, `talex/magic/MagicMysteryHandle.java`, `talex/magic/MagicNormalHandle.java`.
4. **Docs boundary** — eng-hygiene owns `README.md`, `CHANGELOG.md`, `docs/`. site-refine may link to `CHANGELOG.md` on GitHub but does not create it.

## Integration acceptance (parent-owned)

- [ ] Full Java build passes on JDK 25 (`./mvnw package`), all tests green.
- [ ] Site contract tests pass (`node --test test/ssr-contract.mjs test/api-contract.mjs`).
- [ ] Site deployed to soultech.tagzxia.com with D1 migration 0005 applied; live smoke on `/`, `/docs`, `/admin`, `/api/health`.
- [ ] New plugin JAR built and staged; **production wlcb1 restart requires a separate owner-approved window** (out of scope for autonomous execution).
- [ ] Journal + spec updates recorded; commits pushed.

## Non-goals

- Restarting the production Paper server without an explicit owner window.
- Rebuilding the SSR site architecture (it is healthy; this wave only refines).
- Per-player telemetry or any personally identifiable data leaving the server beyond aggregate counts.
