# Site refine — execution plan

- [ ] Read `site/src/ssr.js` fully (route table, renderers, header/footer, isAssetPath) and the header CSS in `styles.css`/`routes.css`.
- [ ] Grep for `index.html` references; delete `site/public/index.html`.
- [ ] Grouped nav in `renderHeader` + CSS; verify keyboard/mobile/no-JS.
- [ ] `/guide` 301 → `/docs` in `renderSsrRequest`.
- [ ] Admin page: telemetry section + `/admin-telemetry.js` script include + isAssetPath entry (per frozen contract — do not create the JS file).
- [ ] `/download` release identity + CHANGELOG link.
- [ ] `app.js` dynamic per-kind imports; record before/after bytes for `/` and `/catalog` (local `wrangler dev` curl or file-size accounting).
- [ ] Update `site/test/ssr-contract.mjs`; run `cd site && node --test test/ssr-contract.mjs` until green.
- [ ] Self-review against `.trellis/spec/backend/quality-guidelines.md` + TDS visual/content rules.

Validation: `cd site && node --test test/ssr-contract.mjs` (required); optional local render smoke via `node --input-type=module -e` importing ssr.js and rendering key paths.

Rollback: git revert of the single commit.
