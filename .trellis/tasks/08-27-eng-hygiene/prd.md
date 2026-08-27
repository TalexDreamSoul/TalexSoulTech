# Engineering hygiene: deprecated APIs, docs, CI

## Goal

Clear the recorded P2 debt and the undocumented-repo gap: migrate the three files still using deprecated Paper APIs, give the repository a real README/CHANGELOG/docs entry point, and add CI so main is protected by the existing test suites.

## Requirements

### A. Deprecated Paper API migration (3 files, exactly)

1. `src/main/java/pubsher/talexsoultech/particlelib/pobject/Ray.java` — drop deprecated `org.bukkit.util.Consumer` usage for the current Paper 26 API (`java.util.function.Consumer` overloads).
2. `src/main/java/pubsher/talexsoultech/talex/magic/MagicMysteryHandle.java` and `MagicNormalHandle.java` — replace deprecated `EntityEffect` usages with the current non-deprecated equivalent (verify against Paper 26.1.2 javadocs; behavior must stay visually equivalent).
3. No behavior changes beyond the API swap; no other files touched in `src/`.

### B. Documentation

4. `README.md`: what TalexSoulTech is (Paper 26.1.2/Java 25 plugin + Cloudflare control plane + deterministic resource pack), the one-manifest-generates-three-targets architecture (short diagram), build/test commands (`./mvnw package`, site `node --test`), site deploy command, repo layout table, links (soultech.tagzxia.com, docs/, .trellis/spec/backend/system-invariants.md). Facts only from the repo/journals — no invented numbers.
5. `CHANGELOG.md`: seed with the two recorded production releases (2026-08-25 electrical-equipment release, 2026-08-27 full-catalog release) using the release identities recorded in `.trellis/workspace/TalexDreamSoul/journal-1.md` Sessions 3–4 (source revisions, JAR/pack hashes, worker revisions, headline scope). Keep-a-Changelog format, newest first.
6. `docs/`: add a dated "superseded" note at the top of `TalexSoulTech-体系化发展与电力系统重构说明.md` stating it describes the pre-full-catalog state (30 machines / old hashes) and pointing to the runtime catalog + README; add `docs/index.md` listing available documents and their status.

### C. CI

7. `.github/workflows/ci.yml`: on push/PR to main — job `java` (ubuntu, Temurin JDK 25, Maven cache, `./mvnw -B verify`), job `site` (Node 24, `node --test test/ssr-contract.mjs test/api-contract.mjs` in `site/`). No deploy steps, no secrets. Verify site tests need no `npm install` (repo has no dependencies) and no prepared assets; if they do, document and gate accordingly.

## Acceptance Criteria

- [ ] `./mvnw package` on JDK 25 emits no deprecation warnings from the three migrated files; all tests green.
- [ ] README/CHANGELOG/docs contain only repo-backed facts; commands verified runnable.
- [ ] CI workflow YAML is syntactically valid and its commands run green locally.

## Non-Goals

- Migrating other deprecated usages elsewhere; publishing releases; branch protection settings (owner-side).
