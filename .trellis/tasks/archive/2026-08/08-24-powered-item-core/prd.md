# Powered item core

## Goal

建立 50 件电力装备共同依赖的唯一可充电物品契约、typed-PDC 能量存储、目录验证、旧能量单元迁移与集中服务生命周期。此子任务只定义公共边界；具体工具、穿戴与无线机器行为由后续切片完成。

## Requirements

- `RechargeableItem` 只暴露容量与单次收发上限，不成为第二个物品注册表。
- `PortableEnergyStorage` 使用插件 NamespacedKey + `PersistentDataType.LONG`；负数请求抛出，模拟绝不写，实际写使用 clone 后显式回填槽位。
- 旧 `industry_energy_cell` 的字符串 `chargeMilliSe` 可读；首次成功写入迁移为 LONG 并移除旧键。
- `PoweredItemSpec` 对 ID、tier、能力、材质、容量、耗能、范围、目标上限、模式与升级依赖做构造期验证。
- `ElectricalEquipmentCatalog` 一次构造 47 件便携规格和 3 台机器描述，验证恰好 50 个唯一 ID、恰好 24 个主动工具与合法升级拓扑。
- `PoweredItem` 是唯一便携实现，设置 max-stack=1、原版材质 fallback、字符串模型 selector、能量/模式 lore，并把现有 SoulTechItem hook 委托给集中服务。
- `PoweredEquipmentService` 只有一个同步任务，UUID 状态在 quit/death/teleport/world-change/disable 清理；不访问异步 Bukkit API。
- 不修改 `PlayerData`、MySQL、PowerGrid 算法或并行中的 `site/extensions` 文件。

## Acceptance criteria

- [x] 非负 long receive/extract 与 simulate/execute 语义和 `EnergyBuffer` 一致。
- [x] stack amount 不是 1、身份不匹配或非 rechargeable 时拒绝能量写入。
- [x] legacy string charge 读取、限幅与首次成功迁移不丢能量。
- [x] 目录在缺项、重复 ID、少于 24 工具、无效 tier/耗能/依赖时 fail-fast。
- [x] 47 个 `PoweredItem` 原型仅构造一次且全部 max-stack=1、zero-charge、recipe 非空。
- [x] 服务 start/close 幂等，插件 disable 后无任务、flight ownership、cooldown 或递归 guard 残留。

## References

- Parent: `.trellis/tasks/08-24-electrical-equipment-progression/`
- Runtime contract: `.trellis/spec/backend/system-invariants.md`
- Research: `.trellis/tasks/08-24-electrical-equipment-progression/research/power-equipment-patterns.md`
