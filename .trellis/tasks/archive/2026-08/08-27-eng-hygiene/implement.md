# Engineering hygiene — execution plan

Three independent tracks.

## Track 1: Deprecated Paper APIs

- [ ] Read the three files; identify every deprecated symbol (`org.bukkit.util.Consumer`, `EntityEffect` variants).
- [ ] Migrate to current Paper 26 equivalents; keep visual/behavioral parity.
- [ ] Verify: `JH=$(mise where java@temurin-25.0.4+101.0.LTS); [ -d "$JH/Contents/Home" ] && JH="$JH/Contents/Home"; JAVA_HOME="$JH" ./mvnw -q -DskipTests compile` then full `package`; confirm no deprecation warnings from these files.

## Track 2: README / CHANGELOG / docs

- [ ] Extract release facts from `.trellis/workspace/TalexDreamSoul/journal-1.md` (Sessions 3–4) and `docs/TalexSoulTech-体系化发展与电力系统重构说明.md`.
- [ ] Write `README.md`, `CHANGELOG.md`, `docs/index.md`, superseded-note header on the old doc.
- [ ] Verify every command in README actually exists (`./mvnw`, site scripts in `site/package.json`).

## Track 3: CI

- [ ] `.github/workflows/ci.yml` (java + site jobs per prd).
- [ ] Validate YAML locally (`python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml'))"`), and run the site job's exact commands locally to prove they pass without npm install.

Rollback: all additive; revert commit.
