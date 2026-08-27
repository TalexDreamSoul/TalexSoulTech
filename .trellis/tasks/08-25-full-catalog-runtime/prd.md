# Full catalog runtime implementation

## Goal

Turn every currently planned TalexSoulTech catalog entry into real Paper gameplay while preserving the existing production content and the four-act, nine-wave progression. Every planning item must become obtainable, usable, connected to recipes or behavior, visible in the guide and runtime catalog, and covered by bounded failure/recovery rules.

## Background

- The public catalog contains 27 disciplines × 10 families × 3 items = 810 planning entries.
- 49 catalog entries are marked implemented; the current Paper runtime independently exposes 150 registered items, including legacy and electrical content outside the 810 planning shape.
- The campaign source is `site/public/data/progression.js`; catalog identity is dotted, while runtime command/PDC identity remains stable snake_case.
- The user explicitly requires every planned entry and planned gameplay loop to be implemented, and authorizes real testing on Mac Work.

## Requirements

1. Preserve every existing registered item, player item, machine, save, PDC key, category unlock and production rollback path.
2. Give all 810 catalog entries a real SoulTech runtime record. Evidence proves only 34 entries can preserve an existing one-to-one runtime ID; the remaining 776 receive new stable snake_case IDs. Preserve all 150 existing runtime records, producing an expected final total of 926 unique runtime records.
3. Generated runtime IDs use stable snake_case; the 34 evidenced existing entries keep an explicit immutable legacy mapping. Vanilla outputs, recipe concepts, BaseMachine records, missing registrations and duplicate identities do not count as item mappings and receive independent SoulTech records.
4. Make every entry materially real:
   - materials/components are obtainable and consumed by at least one recipe or process;
   - tools/equipment perform a bounded observable action;
   - facilities/machines expose an inventory or world operation with finite input, output, cost, stop and recovery states;
   - research/records provide real observation or unlock evidence and are not lore-only collectibles.
5. Implement nine playable wave loops using the fixed campaign mapping. W2–W9 must satisfy their behavior and verification gates from `progression.js`; W1 existing behavior remains compatible.
6. Register all 27 disciplines in the in-game guide with prerequisites, learning/unlock order, recipe pages and production-vs-planning identity removed once implementation is complete.
7. Use one data-driven content manifest and one runtime registry. Do not create 761 bespoke scheduler-owning classes or duplicate item registries.
8. Keep Bukkit/world/inventory access on the primary thread. Pure manifest validation, recipe planning and state-machine logic stay Bukkit-free and bounded.
9. Preserve the no-voltage non-negative `long` milli-SE domain, simulate-then-commit inventory/energy behavior, unloaded-chunk safety, unique ownership, idempotent recovery and no-duplication rules.
10. Add deterministic unique item models/textures and safe vanilla fallbacks for every new runtime item. The resource pack must remain generated from the same frozen content manifest.
11. Generate website `/runtime`, catalog status and release metadata from actual runtime/manifests so 810 catalog IDs, 926 runtime records, 776 new registrations and 34 explicit legacy mappings cannot drift.
12. Provide full automated tests for schema shape, ID mapping, recipe closure, reachability, finite resources, idempotent recovery, main-thread boundaries and each wave gate.
13. Build and smoke-test on Java 25/Paper 26.1.2 locally and on Mac Work. Exercise the real 26.1.2 Fabric client for guide, textures, representative items and all nine wave loops before any production release.
14. Do not deploy Production until exact JAR/resource-pack hashes, rollback material, no-online-player/restart authorization and all release gates are satisfied.

## Acceptance Criteria

- [ ] Exactly 810 catalog entries have a validated runtime mapping; all 761 previously planned entries are now marked implemented.
- [ ] Existing 150 runtime IDs remain readable/usable; final runtime registry has the expected unique count and no silent aliases or overwrites.
- [ ] Every new item is obtainable and has a recipe/process consumer or bounded active behavior.
- [ ] All 27 guide disciplines and 270 families are reachable through valid prerequisites with no cycle or dead end.
- [ ] W1–W9 each pass an observable end-to-end behavior gate, including injected failure and same-operation recovery without duplication.
- [ ] All resource inputs are finite/accounted; water, magic, automation, transport, quantum and time paths cannot create free resources or skip costs.
- [ ] All generated item models/textures load in a real client; no missing model, purple/black texture or unsafe vanilla fallback remains.
- [ ] Java 25 Maven package, all Java tests, SSR tests and API contracts pass.
- [ ] Isolated Paper start/cycle/graceful-stop/restart succeeds and the runtime command catalog matches generated documentation.
- [ ] Mac Work source/artifact hashes match the authoritative worktree and real-client acceptance evidence is recorded.
- [ ] Production remains untouched until the separate release gate is explicitly reached.

## Non-Goals

- Replacing Paper, Java 25, the existing electricity domain, MySQL model or Cloudflare control plane.
- Hiding incomplete mechanics behind lore, admin-only give commands, mocks, placeholders or no-op interactions.
- Treating story coverage as an unlock requirement.
