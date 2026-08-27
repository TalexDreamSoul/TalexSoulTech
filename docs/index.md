# 文档索引

当前事实以仓库根目录的 [README.md](../README.md) 与 [CHANGELOG.md](../CHANGELOG.md) 为准。本目录下的长文是设计说明，数字会随发布推进而过时。

| 文档 | 内容 | 状态 |
|---|---|---|
| [TalexSoulTech-体系化发展与电力系统重构说明.md](TalexSoulTech-体系化发展与电力系统重构说明.md) | 电力域模型、拓扑与公平分配、多方块检测与生命周期、机器实现步骤、便携电力装备与无线充电、2026-08-25 验收记录 | 历史存档 / 部分过时：机器数量与制品哈希停留在全量目录发布之前 |
| [../README.md](../README.md) | 项目定位、一份清单生成三目标的体系结构、CloudSync 回路、构建与测试命令、仓库结构 | 当前 |
| [../CHANGELOG.md](../CHANGELOG.md) | 两次生产发布的完整发布身份与验证记录 | 当前 |

## 规范在别处

工程约束不放在 `docs/`，放在 `.trellis/spec/`：

- [system-invariants.md](../.trellis/spec/backend/system-invariants.md) — 运行时与发布不变量；改动电力、多方块、持久化、云同步或发布流程前必读
- [directory-structure.md](../.trellis/spec/backend/directory-structure.md) — 包边界与放置规则
- [backend/index.md](../.trellis/spec/backend/index.md) — 后端规范索引
