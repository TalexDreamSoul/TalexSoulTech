# Design — Full catalog runtime implementation

## 1. Source and generated manifest

`site/public/data/catalog.js` and `progression.js` remain the authoring source for the 810 planning entries and nine-wave campaign. A deterministic Node exporter writes one committed Java resource:

`src/main/resources/talexsoultech/content/catalog-runtime.json`

The generated resource is never edited by hand. It contains the authoring SHA-256 and every catalog entry. Java startup and Node tests reject a stale, missing, duplicate or incomplete resource.

Each manifest entry contains:

```text
catalogId            dotted stable planning ID
runtimeKind          SOULTECH_ITEM | VANILLA_ITEM | LEGACY_MACHINE | LEGACY_PROCESS
runtimeId            snake_case command/PDC ID for SoulTech entries
legacyRuntimeId       existing immutable ID when mapped to a live item/process/machine
waveId/disciplineId/familyId/slug/tier/type/name
familyKind/form       one of the shared behavior forms
baseMaterial/modelKey/stackLimit
recipe                workstation + explicit typed ingredient rows + output amount
behavior              kind/action/bounds/cost/state policy
facility              optional footprint/ports/operation fields
recovery              stop/rollback/retry policy
```

New IDs use `discipline_family_slug`, replacing dots and hyphens with a single underscore. Existing PDC IDs never change. A bidirectional validator rejects catalog/runtime/legacy collisions before any object is constructed.

## 2. Runtime ownership

New pure/data classes live under `talex/content`:

- `ContentManifest`, `ContentEntry`, `RecipeSpec`, `BehaviorSpec`, `FacilitySpec`
- `ContentManifestLoader` and `ContentManifestValidator`
- `RuntimeId` normalization and legacy mapping
- pure `OperationPlan`, receipt/state records and conservation checks

Bukkit adapters live under the existing item/machine boundaries:

- `ManifestSoulTechItem` for portable/material/component items
- `ManifestFacilityItem` for placeable facilities
- `ContentRegistry` as the single construction/lookup owner
- `ContentBehaviorService` for held-item and world actions
- `FacilityScheduler` for bounded placed-facility plans

`SoulTechItem` remains the authoritative live PDC registry. `ContentRegistry` validates the whole manifest and existing baseline before constructing any new item, so the legacy map can no longer be silently overwritten.

## 3. Recipes and reachability

Every new SoulTech item gets a real recipe. Recipes are generated from an explicit kind palette plus the previous item in its family:

- the first item uses finite vanilla/legacy ingredients and, from W2 onward, the previous-wave catalyst;
- the second item consumes the first plus bounded family ingredients;
- the third item consumes the second plus structural/electrical components and becomes a facility where applicable.

The exporter emits typed ingredient references, never natural-language parsing. A pure graph validator proves:

- every recipe input resolves to vanilla or a registered runtime ID;
- every new output is reachable from vanilla/W1 roots;
- no recipe cycle exists;
- every material/component has at least one downstream consumer;
- recipe IDs and outputs are globally unique.

Legacy recipe/process entries are explicit mappings; they do not impersonate SoulTech items. Missing historical identities such as normal mesh, fire rod, wrench and recipe-chain receive new generated SoulTech IDs rather than sharing or inventing a legacy ID.

## 4. Behavior model

The shared dispatcher supports fifteen behavior kinds:

- research: bounded observation -> signed report -> powered station
- resource: finite extraction/processing -> alloy/component -> storage block
- processing: reagent -> control core -> atomic vat
- plant: seed -> bounded culture -> finite-water greenhouse
- defense: component -> worn/held protection -> bounded bastion
- machine: part -> drive -> workstation
- energy: coil -> bounded cell -> producer/consumer/storage unit in the single milli-SE domain
- magic: finite typed charge/intent -> owner-bound tool -> array
- space: record/anchor -> owner-bound route -> escrow gate
- gravity: mass/readout -> bounded tool -> capacity-limited field
- logistics: tag -> one-batch sorter -> configured relay/return
- construction: material -> frame -> loaded-chunk-only workshop/world patch
- fluid: filter -> finite pump -> source-ledger network
- commerce: token -> owner-bound contract -> escrow exchange/public works
- quantum: bounded state bit -> finite core -> commit/rollback gate

Materials/components are valid behavior through recipe consumption; they are never collectible-only. Tools/facilities expose observable actions. All Bukkit mutations run on the primary thread and honour protection/cancellation/ownership.

## 5. Facilities and atomic operations

Facilities use 1-block, 3x3 or 5x5 footprints according to form. One bounded scheduler is integrated with the ElectricityManager cycle:

1. `prepareBounded` selects loaded/formed machines with a round-robin cursor and creates pure plans.
2. `PowerGrid.tick` settles only selected requests.
3. `commitGranted` validates unchanged inventory/world digests, commits inventory/energy/world patches once, and records the operation receipt.

`OperationPlan` owns a multi-resource transaction: inventory snapshots, finite energy reservation, world preconditions, outputs/byproducts and rollback. Existing powered machines are adapted without changing IDs; the old consume-energy-before-second-process race is removed.

Persisted state includes state version, operation/batch/source ID, phase, remaining work, escrow, inventory/world digest, reserved/spent/released energy, attempts, owner UUID, structure digest and failure code. Unloaded chunks become `PENDING_VALIDATION`, never forced-loaded or treated as broken.

## 6. Guide and player progression

The in-game guide becomes Root -> W1..W9 -> 27 disciplines -> 270 families -> 810 items. Wave order is the only hard campaign edge; same-wave disciplines are parallel. The 96 campaign family links stay soft guidance.

PlayerData gains versioned JSON objects for canonical unlocks, evidence and wave completion. Migration preserves every legacy regular/paid unlock and unknown field. Rendering is read-only; a single `tryUnlock` transaction validates evidence and writes state. Evidence is receipt-based, never inventory-count-based.

All machine/workbench/place/click/list gates resolve the discipline ancestor through CategoryManager rather than assuming an item is a direct discipline child.

## 7. Assets and website

The resource-pack generator reads the same generated manifest and creates a unique deterministic model/texture selector for every new SoulTech runtime item plus safe vanilla fallback. Java selector, pack model, runtime ID and website runtime table form one validated closure.

`runtime-catalog.js` and catalog status are generated from the manifest/build receipt instead of hand-maintained counts. Catalog entries become implemented only when runtime ID, recipe and behavior/facility validation all pass.

## 8. Compatibility and rollout

- Existing 150 runtime IDs and saved PDC items remain valid.
- Vanilla/process/machine mappings are typed; no command alias is fabricated.
- Legacy duplicate `griddle_mesh` is preserved for the currently active advanced mesh; normal mesh receives a new generated runtime ID.
- Legacy infinite-magic lore is never accepted as finite charge.
- Production stays untouched during implementation. Build and real-client gates run locally and on Mac Work first.
- Final release uses the normal exact-artifact, rollback, no-online-player and Paper/RCON process.
