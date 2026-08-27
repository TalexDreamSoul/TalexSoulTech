# Site IA refinement and dead code removal

## Goal

The live site is already a healthy SSR multi-page app (verified 2026-08-27: `/` is a slim task-routed landing; `/docs /disciplines /download /architecture /console /catalog /runtime /extensions /admin` all render server-side with sitemap/robots/404). This task removes the dead pre-SSR client, groups navigation by audience, fixes small IA gaps, and cuts client data weight — **no rebuild**.

## Requirements

1. **Dead code removal**: `site/public/index.html` (the old 8-section single-page client) is unreachable — SSR handles `/` and unknown paths get the SSR 404. Verify nothing references it (tests, scripts, docs), then delete it. `app.js`, `shell.js`, `admin.js`, `styles.css`, `routes.css` are ALIVE (referenced by SSR pages) — do not delete.
2. **Audience-grouped navigation**: regroup the flat 9-item header nav into three visible groups without changing any URL:
   - 玩家: 教程 `/docs`, 学科 `/disciplines`, 资料库 `/catalog`, 实装目录 `/runtime`
   - 服主: 下载 `/download`, 控制台 `/console`, 扩展 `/extensions`
   - 开发: 架构 `/architecture`
   - 首页 `/` stays as the brand/home link. `/admin` stays out of the main nav (footer only, current behavior preserved).
   - Pure HTML/CSS (no JS menus); keyboard accessible; mobile keeps single-column with the existing pattern; respects reduced-motion and color-scheme prefs.
3. **`/guide` → 301 redirect to `/docs`** inside the SSR resolver (currently 404).
4. **Download page release identity**: `/download` shows plugin version + JAR SHA-256, resource-pack version + SHA-1 + URL, sourced from the existing `RUNTIME_RELEASE` / manifest data modules (no hard-coded copies beyond what already exists); add a "变更记录" link to `https://github.com/TalexDreamSoul/TalexSoulTech/blob/main/CHANGELOG.md`.
5. **Admin telemetry mount (frozen cross-task contract)**: on the `/admin` SSR page add `<section id="telemetry-panel" data-endpoint="/api/admin/telemetry"></section>` and include script `/admin-telemetry.js` alongside `/admin.js`. Do NOT create `admin-telemetry.js` (another task owns it).
6. **Client data weight**: `app.js` statically imports `catalog.js` + `progression.js` + `content.js` (~330KB) on every page that loads it. Convert to per-page-kind dynamic `import()` so each page only fetches the modules it actually enhances; pages must remain fully readable without JS (SSR already renders full content). Measure and record before/after transferred bytes for `/` and `/catalog`.
7. **Contract tests**: update `site/test/ssr-contract.mjs` for every change (nav groups, redirect, admin mount, download identity); keep semantic coverage of all existing assertions; all tests green.
8. **Forbidden**: no edits to `site/src/worker.js`, `site/public/data/*` generation, `site/scripts/*`, `site/public/assets/**`, migrations. No new frameworks, no build step, no new dependencies.

## Acceptance Criteria

- [ ] `index.html` deleted; `git grep` shows no dangling references.
- [ ] All existing URLs return the same status as before; `/guide` returns 301 → `/docs`.
- [ ] Nav renders three labeled groups on desktop and mobile, no-JS usable.
- [ ] `/admin` contains the telemetry section + script include exactly per contract.
- [ ] `/download` shows plugin + resource pack identity and the CHANGELOG link.
- [ ] Slim pages (e.g. `/`) no longer fetch the data modules; enhanced pages still work.
- [ ] `cd site && node --test test/ssr-contract.mjs` green.

## Non-Goals

- Visual redesign, new pages beyond the redirect, changelog page (link only), data-module splitting per discipline (defer), touching the resource-pack asset tree.
