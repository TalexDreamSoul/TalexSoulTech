# Changelog

本文件记录 TalexSoulTech 的生产发布，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，最新的在最前。

两次发布的 Maven 版本号都是 `3.0.0-SNAPSHOT`，靠源修订与制品哈希区分。版本号将从下一个发布起采用正式语义化版本，不再复用同一个 SNAPSHOT 号。

## [未发布]

站点侧已部署；插件侧构件 `b107467d8a29343d6032d17925ddf31af69b866cadcb73e67a479ef310a91210` 已构建但**尚未经过真机 Paper 冒烟测试**，因此下载页仍指向上一版生产制品 `c1b7a1ca...`。

### 新增

- 玩法遥测闭环。插件在主线程累计有界聚合计数（产出、机器操作、工具使用、充电、在线秒数、当日独立玩家、学科解锁），随既有 CloudSync 快照上报；Worker 校验后写入 D1 `telemetry_daily`，`/api/admin/telemetry` 与 `/admin` 面板按日与 Top-10 呈现。只上报聚合数量，玩家 UUID 不离开服务端。配置开关 `Settings.telemetry.enabled`（默认开启）。
- GitHub Actions CI：main 分支的 push 与 PR 运行 Java 构建测试与站点 SSR 合约测试。
- `README.md`、`CHANGELOG.md`、`docs/index.md`。

### 变更

- 站点导航按受众分为玩家、服主、开发三组，URL 全部保持不变；`/guide` 301 重定向到 `/docs`。
- `/download` 增加发行身份区块：插件 JAR SHA-256、资源包版本与 SHA-1/SHA-256、变更记录链接。
- 前端数据模块改为按页面类型动态加载，首页等页面不再拉取目录与战役数据。
- 迁移 `Ray`、`MagicMysteryHandle`、`MagicNormalHandle` 中的 Paper 弃用 API。

### 移除

- 删除失效的 SSR 之前时代的单页客户端 `site/public/index.html`。

## [3.0.0-SNAPSHOT] - 2026-08-27

全量目录发布：810 条规划目录条目全部落到运行时，运行时记录达到 926 条。

### 新增

- 776 条新注册的运行时物品，覆盖 27 个学科、270 个族、9 个战役波次的 810 条规划条目。
- `server.properties` 公布确切的公开资源包 URL 与匹配的 SHA-1，同时保留 `require-resource-pack=false` 以便回退到原版外观。

### 变更

- 34 条显式的旧 ID 映射；150 条基线运行时记录保持不变。
- 站点目录状态由 49 已实现 / 761 计划中修正为 810 已实现 / 0 计划中。

### 发布身份

| 项 | 值 |
|---|---|
| 源修订 | `67309825d9991cbb3169feeff478365473dbbcda` |
| JAR SHA-256 | `c1b7a1cae5372944219b07df5396496d422d676e09c97a9b41ce547a0e2df8ef` |
| 资源包 SHA-256 | `35e09443836eab46889cb1f485b805c215e9ceaa3cd6f19e46a7f437376b5fff` |
| 资源包 SHA-1 | `757d492fb9f9fd793ec31bc1847500490bd14c53` |
| Worker 修订 | `ff98eb0a-4824-4fe8-94d1-cfed6170fe41` |
| 远端 D1 | 无待应用迁移 |
| 回滚 JAR | `/opt/minecraft/rollback/TalexSoulTech-3.0.0-SNAPSHOT-20260827T025709Z-pre-6730982.jar`（`3aa8dfbe3a487a977de844fb3d286913e6ab0d66c6866e73d5f377904a05c7bb`） |

### 验证

- Java 25 Maven package：76/76 测试通过。
- 站点合约：SSR 39/39，API 1/1；确定性资源重复生成一致。
- 隔离 Paper 重启、W5 状态变更，以及一次带实测损耗的生产者/导线/储能/消费者传输。
- 真实 26.1.2 客户端捕获 93 个批次、覆盖 834 个自定义模型；832 个可发放模型与 2 个动态向导模型均无缺失纹理。
- 生产 `wlcb1` 在零在线玩家下原子部署并重启，容器恢复健康；RCON 报告插件已启用、926 条运行时记录、301 台设施、二 tick 电力周期运行中、重启后云同步成功，PluginManager 隔离区为空。

## [3.0.0-SNAPSHOT] - 2026-08-25

电力装备发布：47 件便携电力装备与 3 台无线充电多方块进入生产。

### 新增

- 50 条电力目录条目（47 便携 + 3 无线），其中 24 个为主动工具；机器总数 38 台。

### 发布身份

| 项 | 值 |
|---|---|
| 源修订 | `e6c4940e9d7b8f3c7189bf66273b029c6aa4cb99` |
| JAR SHA-256 | `3aa8dfbe3a487a977de844fb3d286913e6ab0d66c6866e73d5f377904a05c7bb`（7,220,589 字节） |
| 资源包 SHA-256 | `20f8d355ea8906864cc324f0c93a1be4a9286abefd2ab9e2b982360807691d86`（12,379 字节，8 张 16×16 RGBA 纹理） |
| Worker 修订 | `bb11bf55-60d7-48b1-8b18-121cf7145bb0` |
| 远端 D1 | 无待应用迁移，`0004_admin.sql` 已存在于远端 |
| 回滚 JAR | `/opt/minecraft/rollback/TalexSoulTech-3.0.0-SNAPSHOT-22895eb9-pre-3aa8dfbe.jar` |

### 验证

- Java 25 Maven：56 项测试通过。
- 站点合约：API 1/1，SSR 23/23。
- 该 JAR 原子安装至 `/opt/minecraft/data/plugins/TalexSoulTech-3.0.0-SNAPSHOT.jar`；生产容器健康，RCON 报告 47 便携、3 无线、24 个主动工具、38 台机器，云状态为已配对且重启后同步成功。
- 真实 26.1.2 客户端逐件接收全部 50 条电力条目；8 张资源包模型均渲染为不同的非缺失纹理。
- 新区块基线 TPS 20.0、1 分钟最大 MSPT 7.2 ms；生成 100 个区块时 TPS 保持 20.0、最大 MSPT 20.3 ms，稳态最大 MSPT 2.7 ms。
