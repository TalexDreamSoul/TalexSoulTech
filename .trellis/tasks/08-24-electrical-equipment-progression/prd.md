# Electrical equipment progression

## Goal

为 TalexSoulTech 增加 **50 件新的电力装备与设备**：47 件可充电便携物品和 3 台电网供能的无线充电多方块。装备必须从 T1 到 T5 形成可理解的升级链；其中至少 20 件是主动工具，最终目录固定为 24 件工具、4 件电池、4 件能量背包、11 件护甲/机动装备、4 件个人辅助设备、3 台无线充电设备。

现有 `industry_energy_cell` 作为第 51 个兼容物品迁移到同一充电契约，但不计入本次 50 件新增目录。

## Confirmed constraints

- 基线为 Java 25、Paper `26.1.2 build 74`。
- 电量统一使用非负 `long` milli-SE，`1 SE = 1000 milli-SE`；不增加电压系统。
- Bukkit 世界、实体、玩家、方块、库存、PDC、粒子和声音只允许主线程访问。
- 复用 `SoulTechItem` 的 `talex_soul_tc=st_items` 与 `soul_tech_item_id` 身份；禁止第二套物品注册表。
- 便携物品随 `ItemStack` PDC 保存，不写入 `PlayerData`/MySQL；插件不得建立玩家背包镜像。
- 只允许一个集中装备周期服务；禁止 50 个独立 Scheduler、全世界扫描、强载区块或每件物品一套 cooldown Map。
- 方块范围效果必须使用正常 Paper 破坏链与保护检查，且有递归保护、方块上限和已加载区块边界。
- 机器充入物品的总量不得超过机器当次实际扣除的能量预算。

## Fixed catalog

容量和耗能均以 SE 表示；实现层转换为 milli-SE。周期耗能指集中装备服务的一次周期，而不是每游戏 tick。

| # | Tier | ID | 名称 | 类别 | 可观察作用 | 容量 / 耗能 |
|---:|---:|---|---|---|---|---|
| 1 | T1 | `powered_wrench` | 动力扳手 | 工具 | 旋转可定向方块；查看电力端点状态 | 60 / 0.5 次 |
| 2 | T1 | `electric_drill` | 电动钻机 | 工具 | 单块采掘；无电时不提供电动能力 | 80 / 0.5 块 |
| 3 | T1 | `electric_saw` | 电动链锯 | 工具 | 连锁砍伐最多 8 个相连原木 | 80 / 0.75 块 |
| 4 | T1 | `electric_shovel` | 电动铲 | 工具 | 直线处理最多 3 个松软方块 | 60 / 0.5 块 |
| 5 | T1 | `electric_hoe` | 电动锄 | 工具 | 3×3 耕地，不破坏非目标方块 | 60 / 0.5 块 |
| 6 | T1 | `electric_shears` | 电动剪 | 工具 | 3×3 树叶/蛛网处理 | 60 / 0.5 块 |
| 7 | T1 | `ore_scanner` | 矿物扫描仪 | 工具 | 已加载范围内有界扫描并报告最近矿物 | 100 / 4 次 |
| 8 | T1 | `resin_tapper` | 电动树脂采集器 | 工具 | 对原木提取现有树脂物品，带 UUID 冷却 | 80 / 2 次 |
| 9 | T1 | `pocket_battery` | 袖珍电池 | 电池 | 储存并向个人充电器供能 | 400 / 20 转移上限 |
| 10 | T1 | `personal_charger` | 个人充电器 | 辅助 | 主副手之间模拟后提交能量转移 | 500 / 25 转移上限 |
| 11 | T2 | `precision_drill` | 精密钻机 | 工具 | 沿视线打通最多 3 个同类可采方块 | 320 / 1.5 块 |
| 12 | T2 | `excavation_hammer` | 电动开掘锤 | 工具 | 与视面一致的 3×3 有界开掘 | 400 / 1.5 块 |
| 13 | T2 | `lumber_axe` | 伐木动力斧 | 工具 | 连锁砍伐最多 32 个原木，不扫树叶 | 320 / 1.25 块 |
| 14 | T2 | `crop_harvester` | 电动收割器 | 工具 | 3×3 成熟作物收割并原位补种 | 240 / 1 作物 |
| 15 | T2 | `vein_miner` | 矿脉采掘器 | 工具 | 最多 16 个相连同类矿石 | 400 / 2 块 |
| 16 | T2 | `magnetic_collector` | 磁力收集器 | 工具 | 半径 6、最多 16 个物品实体拉近玩家 | 300 / 1+0.25 目标 |
| 17 | T2 | `repair_welder` | 维修焊枪 | 工具 | 修复副手可损坏物品，单次最多 64 耐久 | 300 / 每 8 耐久 1 |
| 18 | T2 | `field_flashlight` | 场域照明器 | 工具 | 潜行右键开关；持有时提供无粒子夜视 | 200 / 0.1 周期 |
| 19 | T2 | `compact_battery` | 压缩电池 | 电池 | 袖珍电池升级，提升容量与传输 | 1600 / 80 转移上限 |
| 20 | T2 | `energy_backpack` | 能量背包 | 背包 | 胸甲槽中按优先级给固定装备槽充电 | 3200 / 40 周期转移 |
| 21 | T3 | `mining_laser` | 采矿激光 | 工具 | 视线方向破坏最多 8 个有效方块 | 1200 / 8 块 |
| 22 | T3 | `plasma_cutter` | 等离子切割器 | 工具 | 最多 4 块精确切割；攻击时追加电伤害 | 1600 / 10 块、20 攻击 |
| 23 | T3 | `arc_welder` | 电弧焊机 | 工具 | 单次按顺序维修主副手与四件护甲 | 1200 / 每 32 耐久 8 |
| 24 | T3 | `terrain_compactor` | 地形压实器 | 工具 | 5×5 表面松软方块处理，上限 25 | 1000 / 3 块 |
| 25 | T3 | `geological_analyzer` | 地质分析仪 | 工具 | 报告目标方块与有界样本中的矿物组成 | 1200 / 16 次 |
| 26 | T3 | `mob_stunner` | 生物电击器 | 工具 | 视线选取一个非玩家实体，施加减速与虚弱 | 1000 / 24 次 |
| 27 | T3 | `shock_baton` | 震荡警棍 | 工具 | 近战命中追加伤害、击退和短冷却 | 800 / 12 命中 |
| 28 | T3 | `universal_matter_tool` | 全能物质工具 | 工具 | 潜行右键切换镐/斧/铲/锄/剪模式，匹配模式 3×3 工作 | 2400 / 6 块 |
| 29 | T3 | `advanced_battery` | 高级电池 | 电池 | 压缩电池升级 | 6400 / 256 转移上限 |
| 30 | T3 | `capacitor_backpack` | 电容背包 | 背包 | 能量背包升级，固定槽快速供能 | 12800 / 160 周期转移 |
| 31 | T1 | `powered_boots` | 动力靴 | 护甲 | 穿戴且有电时提供低级速度/跳跃 | 400 / 0.25 周期 |
| 32 | T2 | `magnetic_boots` | 磁稳靴 | 护甲 | 提供缓降并以能量抵消摔落伤害 | 1200 / 0.5 周期、12 摔落 |
| 33 | T2 | `servo_leggings` | 伺服护腿 | 护甲 | 穿戴移动时提供速度 | 1200 / 0.4 周期 |
| 34 | T3 | `kinetic_leggings` | 动能护腿 | 护甲 | 更高速度与跳跃，冲刺时耗能 | 3200 / 0.8 周期 |
| 35 | T2 | `powered_chestplate` | 动力胸甲 | 护甲 | 有界比例减伤，按实际减伤扣能 | 2000 / 每点减伤 20 |
| 36 | T3 | `shield_chestplate` | 护盾胸甲 | 护甲 | 更高比例减伤并抵抗击退 | 6400 / 每点减伤 40 |
| 37 | T1 | `scout_helmet` | 侦察头盔 | 护甲 | 提供夜视和当前电量提示 | 600 / 0.2 周期 |
| 38 | T2 | `mining_helmet` | 采矿头盔 | 护甲 | 提供夜视与急迫 | 2400 / 0.5 周期 |
| 39 | T3 | `jetpack` | 喷气背包 | 飞行 | 双击飞行键产生一次受控推进，不授予持续飞行 | 8000 / 40 推进 |
| 40 | T4 | `advanced_jetpack` | 高级喷气背包 | 飞行 | 更强推进与悬停缓降，仍按动作扣能 | 32000 / 32 推进/周期 |
| 41 | T5 | `gravitic_harness` | 引力飞行背带 | 飞行 | 服务拥有的持续飞行；飞行期间逐周期扣能 | 128000 / 64 周期 |
| 42 | T4 | `elite_battery` | 精英电池 | 电池 | 高级电池终阶升级 | 25600 / 1024 转移上限 |
| 43 | T4 | `induction_backpack` | 感应能量背包 | 背包 | 电容背包升级；优先充主手/副手/护甲 | 51200 / 640 周期转移 |
| 44 | T5 | `quantum_energy_backpack` | 量子能量背包 | 背包 | 终阶背包；大容量但仍遵守每周期传输上限 | 204800 / 2560 周期转移 |
| 45 | T3 | `wireless_charge_receiver` | 无线充电接收器 | 辅助 | 副手携带时允许远距离站点供能并转发到装备 | 16000 / 400 周期转移 |
| 46 | T4 | `field_generator` | 个人力场发生器 | 辅助 | 潜行右键开关；主/副手持有时按实际减伤扣能 | 64000 / 100 点伤害 |
| 47 | T5 | `phase_recall_device` | 相位召回器 | 辅助 | 仅传送到已加载的床/世界出生点，带 UUID 冷却 | 128000 / 8000 次 |
| 48 | T2 | `wireless_charge_pad` | 感应充电台 | 多方块 | 3×3×3，半径 2，最多 1 名玩家，分配 16 SE 预算 | 4000 缓冲 / 16 操作 |
| 49 | T4 | `area_charge_beacon` | 范围充电信标 | 多方块 | 5×5×5，半径 12，最多 4 名玩家；远距目标需接收器 | 16000 缓冲 / 64 操作 |
| 50 | T5 | `quantum_charge_pylon` | 量子充电塔 | 多方块 | 5×5×5，半径 32，最多 8 名玩家；需接收器 | 64000 缓冲 / 256 操作 |

## Requirements

### R1 — One rechargeable contract

- `RechargeableItem` exposes capacity and receive/extract limits; storage adapter exposes `receive/extract(stack, amount, simulate)` with the same semantics as `EnergyBuffer`.
- New energy PDC is typed `LONG`; mode/enabled state is typed `INTEGER`/`BYTE`. Invalid values are clamped only when reading legacy data and rewritten on the next successful mutation.
- Existing string `chargeMilliSe` energy cells remain readable and migrate without losing charge.
- Every portable item has amount/max-stack size 1 and a visible energy line updated after mutations.

### R2 — Exactly one catalog and one service

- One immutable catalog constructs all 47 portable prototypes and 3 machines exactly once.
- Startup validation rejects duplicate IDs, counts other than 50, fewer than 24 active tools, invalid tiers, non-positive capacities/costs, or missing upgrade prerequisites.
- One main-thread `PoweredEquipmentService` owns PDC mutation, cooldowns, flight ownership, fixed-slot periodic effects, attack hooks, and recursive block-break protection.

### R3 — Bounded successful actions

- Energy is deducted only for successful work. Cancelled events, protected blocks, failed breaks, invalid targets, unloaded chunks, full receivers, failed teleport, or already-complete repairs produce no mutation.
- Area and chain actions use explicit target caps from the catalog, never force-load chunks, never target players for offensive abilities, and preserve ordinary `BlockBreakEvent`/drop/protection behavior.
- Tool recursion cannot trigger another area operation or double-charge energy.

### R4 — Charging and energy conservation

- Barrel charging station accepts every `RechargeableItem`, not only `industry_energy_cell`.
- Personal charger and backpacks simulate both source extraction and target insertion before commit.
- Wireless machines distribute one finite budget. For every operation: machine debit = item charge + explicit unused/lost budget; total item charge can never exceed debit.
- Charge order is deterministic: active receiver/backpack, main hand, off hand, armor, then remaining inventory slots. A stack is visited once.

### R5 — Flight ownership is reversible

- Jetpack and harness never revoke creative/spectator flight or flight granted by another plugin.
- The service records only flight permission it granted; unequip, empty energy, quit, death, respawn, world change, teleport, or plugin disable revokes only owned flight and clears hover/velocity state safely.
- Basic jetpack is impulse-only; only `gravitic_harness` provides sustained flight.

### R6 — Progression and recipes

- Every portable item has a nine-slot advanced-workbench recipe. Higher tiers consume the previous-tier item where an upgrade exists and never duplicate stored energy into outputs.
- Crafting a higher-tier item begins at zero charge; input item charge is intentionally not copied.
- Category tier matches the fixed catalog, and all three machines participate in the existing powered-machine/multiblock registry.

## Acceptance criteria

- [x] Catalog validation reports exactly 50 new entries: 47 portable + 3 machines, 24 active tools, and 50 unique IDs.
- [x] All 47 portable items and legacy `industry_energy_cell` pass bounded simulate/execute receive/extract behavior and max-stack-one enforcement.
- [x] Every numbered catalog action has an observable implementation; no ID is a lore-only or no-op placeholder.
- [x] At least 24 tools consume energy only after successful action and respect cancellation, protection, target, recursion, and loaded-chunk bounds.
- [x] Four batteries and four backpacks form monotonic capacity/transfer upgrade chains without energy duplication.
- [x] Boots, leggings, chestplates, helmets, jetpacks, and harness enable/revoke effects and flight safely across equip, empty, quit, death, teleport, world change, and disable transitions.
- [x] Existing charging station plus three new stations charge all rechargeable items with conserved finite budgets and deterministic priority.
- [x] All 50 entries are visible in the technology categories with non-null recipes and valid prerequisites.
- [x] Java 25 test/package succeeds; pure rules and strict review cover tool/armor/flight behavior; isolated Paper 26 proves real ItemStack/PDC migration, recipes, 47/3 registration, 33 powered machines, cycle stability, and graceful shutdown.

## Non-goals

- No voltage tiers, FE/EU compatibility layer, remote chunk charging, cross-world charging, offline-player charging, or database inventory mirror.
- No custom resource-pack artwork in this task; each item uses a deliberate vanilla fallback material and string model selector ready for a later asset release.
- No balance claim based only on the numeric catalog; final tuning still requires real-player observation.
