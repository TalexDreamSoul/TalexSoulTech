# Electrical equipment implementation plan

## Child work map

1. `powered-item-core`: rechargeable contract, typed PDC storage, legacy cell adapter, spec/catalog validation, shared service lifecycle.
2. `powered-tools`: the 24 active tool actions and bounded successful-action charging.
3. `powered-wearables`: four batteries, four backpacks, eleven armor/mobility items, and four personal auxiliaries.
4. `wireless-charging-machines`: generic barrel charging plus three conserved-budget wireless multiblocks.
5. Parent integration: CategoryManager/plugin lifecycle, recipe/tier visibility, cross-slice review, and Paper smoke.

Children 2–4 depend on the public contracts from child 1. They may execute in parallel only after that contract compiles.

## Integration sequence

### A. Establish the core

- Add `RechargeableItem`, `PortableEnergyStorage`, `PoweredAbility`, `PoweredItemSpec`, `PoweredItem`, `EquipmentCatalogContent`, and `PoweredEquipmentService`.
- Preserve existing SoulTech PDC identity and migrate legacy `chargeMilliSe` only on successful mutation.
- Enforce max stack one and dynamic energy/mode lore.
- Implement UUID cooldown and owned-flight cleanup; one synchronous bounded task only.

**Gate:** the catalog can construct all portable specs once and pure energy/catalog validation compiles.

### B. Implement tools

- Implement T1 direct tools, T2 bounded area/chain/repair tools, and T3 laser/combat/analyzer/universal tools.
- Route extra block breaks through `Player.breakBlock` with recursion guard.
- Require loaded chunks, non-player offensive targets, normal protection, and success-before-charge.

**Gate:** every tool ability in catalog has an exhaustive service branch; no default no-op branch exists.

### C. Implement portable support and mobility

- Add batteries/backpacks with deterministic two-sided transfers.
- Add periodic armor effects and proportional damage reduction.
- Add impulse jetpacks and owned sustained gravitic flight with fail-closed transition cleanup.
- Add wireless receiver, force field, and loaded-destination recall.

**Gate:** equip/empty/quit/death/teleport/world-change/disable transitions leave no owned flight or effect state behind.

### D. Implement charging machines

- Replace hard-coded energy-cell charging with the shared portable storage API.
- Add pad, beacon, and pylon multiblocks with finite operation budgets, deterministic player/slot ordering, target caps, and receiver rules.
- Register all three through the existing powered-machine catalog path so save/load, UUID ownership, structure claims, and electric buffers remain inherited.

**Gate:** one operation can never insert more energy than the machine cycle debited.

### E. Integrate and exercise

- Construct industrial prerequisites and the equipment catalog once in `CategoryManager`; register T1–T5 slices and all three machines.
- Create/register/start/stop the equipment service in `TalexSoulTech` without touching concurrent `site/` or extension work.
- Run the Java 25 build and existing suite, then start an isolated Paper 26.1.2 server.
- Exercise representative T1/T2/T3 tool actions, legacy/new item charging, backpack transfer, armor drain, jetpack impulse, sustained-flight revocation, all three station ranges, machine save/load, and graceful stop.

**Rollback point:** before integration, new source files are isolated under the powered-equipment package; after integration, reverting CategoryManager, IndustrialMachines, and TalexSoulTech disconnects the feature cleanly.
