# Site refine — technical design

## Current architecture (verified)

- `site/src/worker.js` — API + delegates all non-API GETs to `renderSsrRequest` (ssr.js), falling back to `env.ASSETS`.
- `site/src/ssr.js` — route table `NAVIGATION` (9 flat entries), `resolvePage(pathname)`, page renderers per `kind`, shared `htmlResponse` shell (styles.css + routes.css + shell.js on all pages; app.js on 5 page kinds at lines ~504/610/816/838/862; admin.js on admin at ~913), `isAssetPath` allowlist (line ~1028), sitemap/robots/404.
- `site/public/app.js` — progressive enhancement; static top-level imports of `./data/catalog.js`, `./data/progression.js`, `./data/content.js`.
- SSR pages embed full content server-side; JS is enhancement only.

## Changes

1. **Nav**: replace the flat `NAVIGATION` render in `renderHeader` with grouped markup (`<nav>` with three labeled `<ul>` groups or `optgroup`-like sections). Keep every path identical. CSS additions go in `routes.css` (shared shell styles live in `styles.css`; follow whichever file currently styles the header). Mobile: reuse the existing responsive pattern (inspect current header CSS before writing).
2. **Redirect**: in `renderSsrRequest` before `resolvePage`, `if (pathname === "/guide") return 301 Location /docs` using the same Response idioms as sitemap/robots.
3. **Admin mount**: in the admin page renderer add the telemetry section as the LAST section of the page body; add `/admin-telemetry.js` to that page's `scripts` array. Keep `isAssetPath` allowlist updated (`/admin-telemetry.js`).
4. **index.html deletion**: grep first (`git grep -n "index.html" site/`), then delete. `run_worker_first: true` + SSR-handles-everything means ASSETS never serves it; confirm ssr-contract has no assertion on it.
5. **Dynamic imports in app.js**: top of file currently binds module constants. Refactor: read `document.body` class `route-<kind>`; `const needsData = KIND_TO_MODULES[kind]`; `await import()` only required modules inside the init path. Preserve all existing behaviors; no top-level static data imports remain. shell.js stays untouched (it is tiny and generic).
6. **Download identity**: the download page renderer already has access to `RUNTIME_RELEASE` (ssr.js imports runtime-catalog.js) and manifest constants; surface plugin JAR SHA-256, resource pack SHA-1 + URL + version, and the CHANGELOG GitHub link in the existing definition-list style.

## Invariants to preserve

- No-JS: every page fully readable; interactive-only affordances must be additive.
- SEO: canonical, og:*, sitemap entries unchanged (except nothing for /guide — redirects stay out of the sitemap).
- Styles: quiet/neutral, one accent color, no gradients/glassmorphism (TDS visual rules; match existing tokens).
- Contract tests document behavior — update assertions deliberately, never delete coverage.

## Rollback

Single revert of this task's commit; no data or schema involved.
