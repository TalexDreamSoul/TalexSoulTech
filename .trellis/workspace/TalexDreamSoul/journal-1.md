# Journal - TalexDreamSoul (Part 1)

> AI development session journal
> Started: 2026-08-23

---



## Session 1: 野外玩法、箱子、资源包与持久化收口

**Date**: 2026-08-23
**Task**: 野外玩法、箱子、资源包与持久化收口
**Branch**: `main`

### Summary

Paper 26 玩法与性能改造、控制站点及资源包代码已完成并推送；部署、真实客户端和新区块运行态烟测仍待执行。

### Main Changes

#### 上下文

- 目标：把野外矿物/怪物、交互箱子、现代材质与数据库链路收敛到 Paper 26.1.2 / Java 25 可运行基线，同时避免区块扫描、同步数据库 I/O 和复制漏洞。
- 工作提交：`fa95e52`（运行时与玩法现代化）和 `f946cf2`（控制站点与资源包）；本轮总结不把当前其他窗口的未提交改动算入这两个提交。
- 当前工作树另有 17 项未提交变更，集中在 `site/`、扩展运行时和 `.agents/.claude/.codex`；来源晚于上述提交，本次未修改、未暂存。

#### 已完成

1. **Wilderness v2**：新生成区块使用确定性、每区块最多 8 个候选的 `BlockPopulator`；Chunk PDC 记录真实位置和消费位图，覆盖普通挖掘、精准采集、时运、创造、放置重放、活塞与爆炸；`RAW_GOLD_BLOCK` 永不作为奖励掉落。
2. **自然精英怪**：只增强 `NATURAL` 原因生成的白名单敌对生物，Entity PDC 防重复，不增加全局实体扫描。
3. **自定义箱子**：铜箱 27 格公共、铁箱 27 格所有者、虚空箱 9 格所有者；原生 Container 是唯一库存源，已拦截双箱、爆炸、活塞、漏斗、合成、Crafter 和熔炉燃料复制/吞箱路径。
4. **现代物品与资源包**：采用 Paper 26 字符串 CustomModelData selector，PDC 保持服务端身份；8 张 16×16 像素纹理及原版 fallback 可确定性生成。资源包 SHA-256：`20f8d355ea8906864cc324f0c93a1be4a9286abefd2ab9e2b982360807691d86`。
5. **玩家持久化**：继续使用 MySQL；有界单线程后台队列按 UUID load/save，失败加载只读、5 秒有界关闭、NIO 数据目录锁防 reload 旧线程覆盖新会话；MySQL 关闭时使用明确的非持久内存模式。
6. **性能收敛**：物品交互改为 PDC ID 的 O(1) 路由；高频事件不再遍历全部自定义物品或扫描区块/世界。
7. **验证**：Java 25 Maven 完整构建通过，45 项测试通过；站点 API 合约通过；资源包重复生成一致且 ZIP 完整性通过；最终 JAR 内 `plugin.yml` 已核验。
8. **版本控制**：上述两个工作提交已推送到 `origin/main`；完成时本地与远端均指向 `f946cf2057291f1073d37f86f8634446a5e0ff50`。

#### 后续待办

- [ ] 按 Trellis `08-23-release-client-validation` 执行 Paper 26 原子发布：将同一最终 JAR 部署到 wlcb1；重启前先通过 RCON 确认无在线玩家，部署后核验容器健康、插件 ENABLED 和 RCON 插件列表。
- [ ] 配置资源包自动下发，使用已发布归档和匹配摘要；真实客户端确认模型、fallback、向导书和三类箱子视觉。
- [ ] 保持 Wilderness 默认关闭，部署后在测试世界启用并仅加载新区块；记录 tick/TPS、区块生成耗时、矿物消费状态和自然怪增强数量。
- [ ] 真实客户端完成向导书领取/右键、箱子所有者权限、Shift/双击/拖拽、破坏掉落、漏斗/活塞/爆炸防复制回归。
- [ ] 完成 Trellis `00-bootstrap-guidelines`：以仓库真实代码补齐 `.trellis/spec/backend/` 五类规范和代码例子，随后 finish/archive。
- [ ] 将旧粒子库和 `EntityEffect` 的 Paper deprecated 警告作为 P2 单独收敛，不与部署验收混做。

#### 当前状态

代码与自动化验证已完成；生产插件部署、服务器重启、真实客户端与新区块运行态烟测尚未执行。

### Git Commits

| Hash | Message |
|------|---------|
| `fa95e52` | (see git log) |
| `f946cf2` | (see git log) |

### Testing

- [OK] Java 25 Maven 完整构建：45 项测试通过，0 失败
- [OK] 站点 API 合约测试通过
- [OK] 资源包重复生成摘要一致，ZIP 完整性通过
- [OK] 最终 JAR 内 `plugin.yml`、入口和命令声明已核验

### Status

[!] **代码与自动化验证已完成并推送；生产部署及真实客户端验收待执行**

### Next Steps

- P0：推进 Trellis `08-23-release-client-validation`，原子部署同一最终 JAR 并完成服务器与客户端验收
- P1：完成资源包、向导书、三类箱子与 Wilderness 新区块真实客户端回归
- P1：完成并归档 Trellis `00-bootstrap-guidelines`
- P2：单独收敛旧 Paper deprecated 警告


## Session 2: TalexSoulTech architecture and production handoff

**Date**: 2026-08-23
**Task**: TalexSoulTech architecture and production handoff
**Branch**: `main`

### Summary

Recorded the Paper, wilderness, power/multiblock, Cloudflare SaaS, resource-pack, and extension baseline; created the production-convergence planning task and source-backed system invariants without touching concurrent uncommitted work.

### Main Changes

## Context recorded

TalexSoulTech now spans a Paper plugin, deterministic resource pack, Cloudflare Worker/D1/Durable Objects control plane, and a bounded LuaJ/Rhino extension host. The project baseline is Java 25 and Paper `26.1.2 build 74`.

## Completed and evidenced

- Paper 26 modernization and optional fail-closed MySQL player persistence were previously deployed and smoke-tested on `wlcb1`.
- Wilderness v2 deterministic bounded ore indexing was committed and pushed. Deployment of that exact revision is not established in this journal entry.
- The no-voltage milli-SE power grid, multiblock lifecycle, and 30 machines passed 38 tests, Java 25 packaging, and isolated Paper load/cycle/graceful-stop smoke. The historical isolated JAR SHA-256 was `1c33deae86ac10b1102fac6cecf668ca287632521be3f55d25cedd5d01072739`; that exact artifact was not deployed in the finishing session.
- `https://soultech.tagzxia.com` is live with the 27-discipline/810-item catalog, multi-tenant server control plane, pairing, ordered snapshots, and cloud extension CRUD.
- The deterministic Paper 26 resource-pack baseline recorded SHA-256 `20f8d355ea8906864cc324f0c93a1be4a9286abefd2ab9e2b982360807691d86`.
- Relevant work commits are `fa95e52` and `f946cf2`.

## Current state and risk

At capture time, Trellis reported 17 uncommitted paths touching `site/`, `extensions/`, and agent configuration. They are treated as concurrent user/other-session work. No reset, overwrite, staging, archive, or commit was performed. The existing `00-bootstrap-guidelines` task is still incomplete, so generated backend template specs remain non-authoritative except for the new source-backed `system-invariants.md`.

## Next planned work

Created planning task `.trellis/tasks/08-23-soultech-production-convergence/` with converged `prd.md`, `design.md`, `implement.md`, and curated implement/check context. It requires:

1. ownership and release-scope attribution for every dirty path;
2. one immutable JAR/resource-pack/Worker/migration receipt;
3. D1 migrations and Worker live E2E before Paper cutover;
4. atomic `wlcb1` release and rollback proof;
5. real-player guide/help, resource-pack, wilderness, multiblock, cloud-sync, and extension-LKG acceptance;
6. observed early-game balance data before any new content.

The task remains `planning`. Production deployment must not begin until the owner resolves its three open decisions and approves implementation/release.

## Durable pointers

- `.trellis/spec/backend/system-invariants.md`
- `.trellis/tasks/08-23-soultech-production-convergence/prd.md`
- `.trellis/tasks/08-23-soultech-production-convergence/design.md`
- `.trellis/tasks/08-23-soultech-production-convergence/implement.md`
- `docs/TalexSoulTech-体系化发展与电力系统重构说明.md`


### Git Commits

| Hash | Message |
|------|---------|
| `fa95e52` | (see git log) |
| `f946cf2` | (see git log) |

### Testing

- [OK] Trellis `soultech-production-convergence` implement/check context manifests validated
- [OK] 本次为上下文与规划沉淀；未将既有自动化证据误写为本次重新执行
- [OK] 未暂存、覆盖或提交并行工作区改动

### Status

[!] **架构上下文沉淀完成；生产收敛任务仍为 planning，尚未授权部署**

### Next Steps

- 明确当前 `site/` 与 `extensions/` 未提交改动的归属和发布范围
- 确认生产发布窗口、允许的回滚中断及早期玩法目标
- 审阅并激活 `08-23-soultech-production-convergence` 后，按 cloud → Paper → 真人验收顺序执行

## Session 3: Electrical equipment, real-client acceptance, Mac Work handoff, and production convergence

### Scope and ownership

The 54-path dirty workspace was fully attributed and split into three commits: runtime `36389e6`, Cloudflare SSR/admin `64f8229`, and Trellis workflow `e6c4940`. Mac Work received the complete TalexSoulTech working tree, HMCL 3.13.2, Java 25, Minecraft 26.1.2 client data, the Paper acceptance runtime, and every other project source snapshot; generated dependency/build caches were intentionally excluded.

### Immutable release receipt

- Source revision used for the clean build: `e6c4940`.
- Java 25 Maven: 56 tests passed; shaded JAR `3aa8dfbe3a487a977de844fb3d286913e6ab0d66c6866e73d5f377904a05c7bb`, 7,220,589 bytes.
- Resource pack: `20f8d355ea8906864cc324f0c93a1be4a9286abefd2ab9e2b982360807691d86`, 12,379 bytes, eight distinct 16×16 RGBA textures.
- Site contracts: API 1/1 and SSR 23/23 passed.
- D1: no unapplied migration; `0004_admin.sql` was already present remotely.
- Worker: `bb11bf55-60d7-48b1-8b18-121cf7145bb0` on `soultech.tagzxia.com`.
- Paper production: the exact JAR was atomically installed at `/opt/minecraft/data/plugins/TalexSoulTech-3.0.0-SNAPSHOT.jar`; rollback JAR is `/opt/minecraft/rollback/TalexSoulTech-3.0.0-SNAPSHOT-22895eb9-pre-3aa8dfbe.jar`.

### Runtime and client evidence

- Production container healthy; RCON lists TalexSoulTech and reports 47 portable, 3 wireless, 24 active tools, and 38 total machines. Cloud status was paired/idle with a post-restart successful sync; no TalexSoulTech cycle error occurred in the observation window.
- Real Minecraft 26.1.2 client received every one of the 50 electrical entries. The final 10-entry batch occupied exactly 10 inventory slots.
- Guide command delivered the book and the remapped use key opened `Talexs > 引导`; clickable help rendered run/suggest hover contracts.
- Copper, iron, and void boxes placed with stable PDC identity. Iron/void owner denial, illegal double-box rejection, void 9+18 slot layout, hopper cancellation, piston immobility, and explosion protection were observed.
- All eight resource-pack models rendered as distinct non-missing client textures; the earlier no-pack client view proved vanilla fallback.
- Wilderness v2 produced bounded deterministic carriers in new chunks. A real client broke indexed carriers into exactly one gold nugget and zero raw-gold blocks before and after generation/world-list configuration changes. A remote disabled-generation chunk had no candidates.
- Natural zombies/skeletons held the v1 marker and exact single multipliers; creepers/spiders and a command-spawned zombie did not. Four hostile-type counts changed from 96 to 104 over 104 seconds rather than growing exponentially.
- New-chunk baseline was TPS 20.0 with 1-minute max MSPT 7.2 ms. Generating 100 chunks kept TPS 20.0 and max MSPT 20.3 ms; steady state was TPS 20.0/19.8/19.8 with max MSPT 2.7 ms.
- A 3×3×3 crusher and 5×5×5 electric furnace both persisted `claimed=true`. Injected finite buffers processed real PDC inputs to `industry_crushed_ore ×2` and `industry_refined_ingot ×1`; furnace energy moved from 500,000 to 212,000 milli-SE exactly. Crusher survival recovery dropped both output and controller, removed one endpoint, and same-position rebuild restored it.

### Remaining product decision

No balance number was changed from synthetic acceptance alone. The next tuning decision requires observed survival acquisition time, charge cadence, and tool-use telemetry from normal players; the current evidence only proves correctness and bounded performance.

