# System Runtime and Release Invariants

> Source-backed contracts for TalexSoulTech's Paper runtime, deterministic wilderness data, persistence, Cloudflare control plane, and release boundary.

## 1. Scope / Trigger

Read this specification before changing any of the following:

- electricity, powered machines, or multiblock lifecycle;
- wilderness ore generation, identity, drops, or Chunk PDC;
- player persistence or MySQL startup/shutdown behavior;
- plugin/cloud pairing, snapshots, extension delivery, or Worker/D1 bindings;
- Paper, resource-pack, or Cloudflare release procedures.
- public planning-catalog identity, campaign waves, story anchors, or SSR/client catalog filters.

Runtime baseline: Java 25 and Paper `26.1.2 build 74` (`pom.xml`).

## 2. Signatures

### Electricity domain

```java
long EnergyBuffer.receive(long requested, boolean simulate)
long EnergyBuffer.extract(long requested, boolean simulate)
void PowerGrid.register(PowerEndpoint endpoint)
void PowerGrid.register(PowerCable cable)
boolean PowerGrid.unregister(BlockKey key)
PowerCycleStats PowerGrid.tick()
```

`ElectricityManager` exposes the Bukkit boundary. It runs `PowerGrid.tick()` on the Paper primary thread every two ticks and rejects off-thread mutation.

### Portable electrical equipment

```java
PortableEnergyStorage.Mutation receive(ItemStack stack, long requested, boolean simulate)
PortableEnergyStorage.Mutation extract(ItemStack stack, long requested, boolean simulate)
PortableEnergyStorage.Transfer transfer(ItemStack source, ItemStack target, long requested, boolean simulate)
long PoweredEquipmentService.chargeInventory(Inventory inventory, long budget, boolean simulate)
long PoweredEquipmentService.chargePlayer(Player player, long budget, boolean receiverRequired, boolean simulate)
```

The catalog source is `ElectricalEquipmentCatalog`: exactly 47 portable definitions plus three wireless multiblocks, with exactly 24 active tools.


### Plugin commands and configuration

The single command root is `/talexsoultech` with aliases `/tst`, `/soultech`, `/sc`, `/tech`, and `/soultechnology`. Public `help`, `guide`, `items`, and `item` actions expose the registered catalog without mutating it. `give` requires `talex.soultech.admin`, accepts only an exact online player, a registered SoulTech item ID, and an amount from 1 through 64, and splits output by the prototype's effective maximum stack size. Privileged cloud/extension/debug actions remain separated by `talex.soultech.admin` and `talex.soultech.debug` (`src/main/resources/plugin.yml`).

Runtime keys are declared in `src/main/resources/config.yml`:

- `Settings.mysql.enabled|ip|port|user|pass|db`
- `Settings.cloud.enabled|api-base|server-id|api-key|sync-interval-seconds|sequence`
- `Settings.extensions.enabled|refresh-interval-seconds|max-source-bytes|callback-budget-millis`
- `Features.wilderness.*`

### Cloudflare bindings

`site/wrangler.jsonc` binds static assets as `ASSETS`, D1 as `DB`, and Durable Objects as `SYNC_COORDINATOR` (`SyncCoordinator`) and `AUTH_RATE_LIMITER` (`AuthRateLimiter`). The production custom domain is `soultech.tagzxia.com`.

### Public planning catalog

```javascript
import { CAMPAIGN, CAMPAIGN_ACTS, CAMPAIGN_WAVES } from "./progression.js";
import { DISCIPLINES, CATALOG_STATS } from "./catalog.js";
```

`progression.js` is the single campaign source. `catalog.js` enriches its 27 disciplines and 810 planning items; SSR and client enhancement consume those exports rather than copying wave tables.

## 3. Contracts

### Energy and topology

- Domain energy is a non-negative `long` measured in milli-SE; `1 SE = 1000 milli-SE` (`EnergyUnits`).
- `receive`/`extract` return the actual amount. `simulate=true` must not mutate state. Negative, overflowing, or out-of-capacity states are invalid.
- There is no voltage tier. Do not reintroduce legacy `Capacity`, mutable transfer tokens, or per-generator DFS.
- Endpoint/cable registration is unique by `BlockKey`. Registration/removal marks topology dirty; normal cycles reuse the topology snapshot.
- Settlement stays on the primary thread. Producer output serves consumers first; storage supplements shortages and receives only surplus. A storage endpoint never charges and discharges in the same cycle.
- Cable throughput is shared per cycle. Loss is applied per segment. Oversized networks are reported and skipped rather than blocking the server thread.

### Multiblocks

- Supported templates are 3x3x3 and 5x5x5. Detection reads already loaded chunks and must not force chunk loads.
- `MultiblockStructureRegistry` is the atomic occupancy authority. Controller ownership is a UUID, not a player name.
- Inventory changes use simulate-then-commit semantics. Failed structure, energy, inventory, ownership, unload, or reload checks must not duplicate or consume items partially.
- Piston, explosion, hopper, and unauthorized player interactions must respect claimed structure boundaries.

### Portable equipment and wireless charging

- Every portable charge is a non-negative typed `LONG` on the ItemStack PDC. `PlayerData`/MySQL never mirrors inventory charge.
- Rechargeable stacks have maximum stack size one. Storage rejects an amount other than one so a stack cannot share one charge value.
- Actual receive/extract/transfer returns replacement clones; the owning inventory slot is written only after both sides simulate successfully.
- Legacy `industry_energy_cell` string `chargeMilliSe` remains readable and migrates on the first successful mutation without losing bounded charge.
- `ElectricalEquipmentCatalog` must fail fast unless it has 47 portable entries, three wireless machines, 24 active tools, unique IDs, tiers 1..5, and backward-only upgrade dependencies.
- One main-thread `PoweredEquipmentService` handles all periodic equipment. Periodic work is bounded to online players and six fixed equipment/hand slots; no item owns a scheduler.
- Area mining uses accepted `BlockBreakEvent` plus guarded `Player.breakBlock` for secondary targets. It never sets blocks to air directly, targets unloaded chunks, or expands recursively.
- Offensive electrical abilities never target `Player`. Entity radius and target count are catalog bounds.
- Jetpacks provide owned impulses; only the gravitic harness provides sustained flight. The service revokes only `allowFlight` it granted and clears ownership on unequip, empty charge, quit, death, respawn, teleport, world change, mode change, and disable.
- Barrel and wireless charging distribute a finite budget. Total ItemStack insertion for one completed operation must be less than or equal to the machine energy debited for that operation.


### Wilderness v2

- New ore placement is deterministic per world seed/chunk and bounded to at most eight candidates/blocks per chunk (`WildernessOrePlan.MAX_CANDIDATES_PER_CHUNK`).
- Chunk PDC index version is `2`; it stores count, consumed mask, and bounded packed positions. The carrier `RAW_GOLD_BLOCK` is never the reward.
- Disabling generation stops new placement only. Existing valid v2 indexes remain authoritative for interaction and consumption.
- Runtime identity must not depend on the current configured world list, generation toggle, or a rescan of chunk blocks.

### Player persistence

- MySQL is optional. `Settings.mysql.enabled=false` means explicit non-durable in-memory defaults.
- Enabled persistence must acquire the data-folder lock and establish MySQL before startup continues. Connection failure is fail-closed.
- Loads/saves are UUID-parameterized through the bounded single persistence worker. The Paper thread only publishes/removes complete `PlayerData` objects.
- A failed load produces a read-only default; it must never overwrite a previously persisted row. Shutdown stops new loads and has a five-second bounded drain.

### Cloud, extensions, and release

- `Settings.cloud.api-base` must be HTTPS; HTTP is allowed only for `localhost`, `127.0.0.1`, or loopback IPv6 development.
- Tenant/server ownership scopes every snapshot, sequence, credential, and extension mutation. A multi-server administrator does not imply cross-server data access.
- Retried sync uses the exact serialized body and monotonically increasing per-server sequence.
- Extension code runs through bounded LuaJ/Rhino host APIs with private bounded KV, dependency ordering, LIFO disposers, and last-known-good recovery. It never receives raw Bukkit, filesystem, database, network, or secret access.
- Release order is fixed: freeze/build JAR and resource pack -> apply remote D1 migrations -> deploy Worker -> pass live SaaS E2E -> atomically deploy the exact JAR to `wlcb1` -> pass Paper/RCON and real-client smoke.

### Planning catalog and campaign

- Planning identity and runtime identity are separate: dotted catalog IDs describe 810 planning entries; `/runtime` exposes the 150 registered command/PDC IDs. An implemented planning entry still directs operators to `/runtime` for the actual runtime ID.
- The campaign has four acts and exactly nine waves. Every one of 27 disciplines belongs to one wave; `stage`, `wave`, and `status` are independent axes.
- Each discipline has ten stable `discipline.family` keys and thirty stable `discipline.family.item` IDs. Exactly two families per discipline are narrative anchors: 54 families and 162 story items, exactly 20% of 810.
- Story order is explicit `1..3` and independent of Roman tier. Tier comes only from the catalog item; E-family declaration order is never treated as tier order.
- Cross-family campaign links are unique, non-self `supports` relations. They guide reading but never become recipes, unlocks, or hard runtime dependencies without a separately implemented transaction contract.
- Planned water, energy, automation, transport, quantum, and time mechanics remain finite and recoverable. Water uses a finite source ledger; energy stays in the single milli-SE domain; legacy unlimited-magic markers cannot fund grids or cross-domain costs.
- SSR owns the no-JS contract. `/catalog` normalizes `q/wave/discipline/narrative/status/page`; client enhancement must preserve the same semantics and leave SSR rows usable if JavaScript fails.

## 4. Validation & Error Matrix

| Condition | Required result |
|---|---|
| Negative energy, invalid buffer state, non-positive cable throughput, or loss outside `0..999` permille | Reject with `IllegalArgumentException`; do not clamp silently. |
| Two power nodes at one `BlockKey` | Reject with `IllegalStateException`; never overwrite registration. |
| Electricity mutation off the Paper primary thread | Reject with `IllegalStateException`. |
| Network exceeds the configured node limit | Report it in `PowerCycleStats` and skip settlement for that network. |
| Invalid/stale wilderness PDC | Treat as non-custom; do not infer identity from carrier material. |
| MySQL disabled | Use explicit in-memory mode and warn that no data is loaded/saved. |
| MySQL enabled but lock/connection unavailable | Fail plugin startup; never fall back to writable defaults. |
| Cloud base is insecure or malformed | Disable/reject cloud operation before sending credentials. |
| Stale or invalid extension update | Keep/restore last-known-good and dispose every staged resource. |
| D1 migration or live SaaS E2E fails | Do not deploy the dependent Paper artifact. |
| Paper smoke or client acceptance fails | Roll back to the previous recorded artifact; do not patch production ad hoc. |
| Rechargeable stack amount is not one or SoulTech identity is missing | Reject ItemStack energy mutation and leave the stack unchanged. |
| Portable request is negative or exceeds capacity/transfer bounds | Reject negatives; return only the actual bounded amount for valid requests. |
| Equipment catalog count, ID, tier, tool count, or upgrade order drifts | Fail startup before constructing partial prototypes. |
| Protected/cancelled block, failed secondary break, invalid target, or unloaded chunk | Perform no effect and consume no portable energy. |
| Wireless output would exceed the debited operation budget | Cap deterministic distribution at the remaining budget; never create energy. |
| Jetpack/harness is removed, empty, or lifecycle cleanup fires | Revoke only service-owned flight and clear transient UUID state. |
| Campaign wave/discipline, anchor/story, family key, or soft relation count/identity drifts | Fail module import before rendering a partial catalog. |
| A family link is unknown, duplicated, or self-referencing | Reject the catalog; never infer direction from prose. |
| A planning item is presented as a `/tst`/PDC runtime ID | Label it as a planning ID and direct runtime operations to `/runtime`. |
| Client query enhancement fails | Keep the SSR form, table, pagination, and links; report the enhancement error without blanking the page. |

## 5. Good / Base / Bad Cases

- **Good:** topology changes once, a two-tick primary-thread cycle shares cable capacity across producers, satisfies consumers fairly, and persists only changed machine state.
- **Base:** MySQL/cloud/wilderness generation are disabled; the plugin remains usable with clearly non-durable players, no remote sync, and no new wilderness placement while existing v2 indexes stay safe.
- **Bad:** an async task scans chunks, treats every `RAW_GOLD_BLOCK` as custom ore, mutates a shared energy map, or saves default player data after a failed load.
- **Good release:** every artifact hash and revision comes from one commit, cloud compatibility is proven first, and Paper is cut over atomically with rollback material ready.
- **Bad release:** upload a new manifest/Worker and later build an unrelated JAR from a dirty workspace.
- **Good portable action:** validate target and simulated charge, let the normal Paper event succeed, commit one bounded effect, then replace the charged stack once.
- **Bad portable action:** consume energy at `LOWEST`, directly set adjacent blocks to air, schedule one task per item, or clear `allowFlight` that the service did not grant.
- **Good planning catalog:** one progression module maps 4 acts -> 9 waves -> 27 disciplines -> 270 families -> 810 items, with 54 anchor families, 162 story items, and unique soft links; SSR and client filters consume the same fields.
- **Base planning catalog:** an ordinary non-anchor item still answers purpose, input, recovery, and next reading step without invented lore; production identity remains independently discoverable in `/runtime`.
- **Bad planning catalog:** duplicate wave constants in content/SSR/client, use a display name as a relation key, count story items as unlock requirements, infer story order from array position, or treat a dotted planning ID as a command ID.

## 6. Tests Required

- Pure Java domain tests: buffer bounds/simulation, energy conservation, cable throughput/loss, multi-source fairness, parallel residual paths, topology rebuilds, oversized networks, multiblock occupancy, and atomic inventory behavior.
- Wilderness tests: deterministic candidate positions, eight-entry bound, PDC validation/consumption, configuration-disable semantics, carrier-block exclusion, and piston/explosion/place/break boundaries.
- Persistence tests: UUID parameterization, bounded queue rejection, read-only failed loads, stale-session fencing, shutdown drain, and file-lock exclusion.
- Site/API tests: auth rate limits, administrator/server isolation, monotonic snapshot sequence, pairing, extension CRUD, exact-body retries, and secret redaction.
- Release verification: Java 25 `mvn -B -ntp test` and `mvn -B -ntp package`, isolated Paper load/cycle/graceful-stop smoke, live SaaS E2E, production Paper/RCON smoke, and a real-client gameplay/resource-pack loop.
- Portable equipment tests: exact 47/3/50 catalog shape, 24 active tools, unique/backward upgrade graph, non-negative bounded arithmetic, simulate immutability, legacy migration, operation-budget divisibility/conservation, and lifecycle ownership transitions.
- Planning/SSR tests: exact 4/9/27/54/162/810 shape, unique non-self soft links, real tier/story binding, W1-W9 discipline coverage, wave/narrative query normalization, item five-question pages, story-only anchor rendering, mobile-accessible table scrolling, and unchanged 150-item runtime catalog.

## 7. Wrong vs Correct

### Wrong

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    scanLoadedAndUnloadedChunks();
    sharedEnergyMap.put(location, mutableCapacity);
});
```

This bypasses Paper thread ownership, makes work proportional to world size, and cannot preserve energy or chunk-lifecycle invariants.

### Correct

```java
// Bukkit-owned registration happens on the primary thread.
electricityManager.registerEndpoint(endpoint);

// The bounded pure-domain grid settles from a scheduled primary-thread cycle.
PowerCycleStats stats = electricityManager.runCycleNow();
```

Topology changes are event-driven, settlement is bounded and deterministic, and observable stats support failure reporting without unsafe world access.

### Public catalog identity

#### Wrong

```javascript
const familyKey = item.family; // translated display name
const runtimeId = item.id;     // dotted planning ID
const wave = SITE_CONTENT.planning.verticalSlices[index];
```

#### Correct

```javascript
const familyKey = item.familyKey; // stable discipline.family
const wave = CAMPAIGN_WAVES.find((entry) => entry.id === item.waveId);
const runtimeId = null; // resolve operational identity from /runtime
```

Planning data can explain progression and story without becoming a second runtime registry.
