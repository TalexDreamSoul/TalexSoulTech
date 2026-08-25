# Directory Structure

> Source-backed package boundaries for the Paper runtime and Cloudflare control plane.

## Repository layout

```text
src/main/java/pubsher/talexsoultech/
├── TalexSoulTech.java          # Paper lifecycle and service composition root
├── Commands.java               # Single /tst command dispatcher
├── cloud/                      # Paper-to-Cloud snapshot client and outbox
├── extensions/                 # Sandboxed extension runtime, storage, LKG
├── entity/                     # Published player session/domain data
├── inventory/                  # Inventory UI screens
├── listener/                   # Cross-feature Bukkit event entry points
├── platform/                   # Paper-version platform adapters
├── talex/
│   ├── electricity/            # Pure energy/grid domain plus Bukkit manager
│   ├── multiblock/             # Pure templates, detection, occupancy
│   ├── machine/                # Bukkit machine runtimes and transactions
│   ├── items/                  # Item prototypes and data-driven catalogs
│   ├── storage/                # Copper/iron/void storage-box boundary
│   └── world/                  # Wilderness generation and event lifecycle
└── utils/                      # Existing shared serialization/item/block helpers
src/test/java/pubsher/talexsoultech/
├── domain/                     # Pure electricity/multiblock/equipment contracts
├── extensions/                 # Sandbox, storage, KV, and LKG contracts
├── cloud/                      # Exact-body outbox contracts
└── talex/world/                # Deterministic wilderness plan contracts
site/
├── src/worker.js               # Cloudflare API router and domain services
├── src/ssr.js                  # SSR document rendering
├── migrations/                 # Ordered additive D1 migrations
├── public/                     # Static shell, assets, downloads
├── scripts/                    # Deterministic release/admin tooling
└── test/                       # API and SSR contract tests
```

## Placement rules

- Put pure arithmetic/state machines under their owning `talex` package. They must not import Bukkit world/entity APIs.
- Put Bukkit access at event/service boundaries. `ElectricityManager`, `PoweredEquipmentService`, and `WildernessManager` own the main-thread boundary for their domains.
- Define stable item/machine IDs in one catalog, then let listeners/services consume that catalog. Do not create a second registry next to `SoulTechItem` or a domain catalog.
- Put Cloudflare route dispatch in `site/src/worker.js`; reusable SSR rendering stays in `site/src/ssr.js`. D1 schema changes belong only in numbered migration files.
- Mirror production package ownership in tests. Do not create a generic `test/utils` bucket for feature contracts.

## Naming

- Java packages: lowercase; classes/records/enums: `PascalCase`; methods/fields: `camelCase`.
- Persisted IDs, item IDs, machine IDs, D1 tables/columns, and config keys: stable `snake_case` unless an existing public key already uses kebab case.
- Java test classes end in `Test`; one test method names one observable invariant.
- Cloudflare files use lowercase kebab-case paths; API routes remain under `/api`.

## Reference modules

- Pure domain boundary: `talex/electricity/PowerGrid.java`.
- Bukkit lifecycle boundary: `talex/machine/multiblock/PoweredMultiblockMachineItem.java`.
- Data-driven item family: `talex/items/equipment/ElectricalEquipmentCatalog.java`.
- Safe world lifecycle: `talex/world/WildernessManager.java` and `WildernessOrePlan.java`.
- Tenant-scoped API: `site/src/worker.js`.
