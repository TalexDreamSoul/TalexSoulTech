# TalexSoulTech

TalexSoulTech 是一个整体：Paper 26.1.2 / Java 25 服务端插件 + Cloudflare 控制面 + 确定性资源包。插件负责运行时玩法、电力与多方块；Cloudflare Worker/D1 负责多租户控制台与快照同步；资源包由与插件同一份清单确定性生成。

生产站点：<https://soultech.tagzxia.com>

## 体系结构

一份冻结的清单生成三个目标，三者的物品身份必须一致：

```text
src/main/resources/talexsoultech/content/catalog-runtime.json   (冻结清单)
        |
        +--> 运行时注册表   打包进 JAR，由 ContentManifestLoader 读取同一文件
        |
        +--> 资源包         site/public/assets/TalexSoulTech-26.1.2-resource-pack.zip
        |
        +--> 站点目录       site/public/data/runtime-catalog.js
```

后两个目标由唯一的生成入口 `site/scripts/prepare-assets.mjs`（`npm run prepare`）产出；同一脚本还把构建产物 JAR 复制到 `site/public/downloads/`，因此它要求 `target/talex-soul-tech-3.0.0-SNAPSHOT.jar` 已经存在。

CloudSync 回路：

```text
Paper 插件 CloudSyncService
        |  POST /api/sync（按服务器单调递增 sequence，重试复用完全相同的请求体）
        v
Cloudflare Worker  site/src/worker.js
        |
        +--> Durable Object SYNC_COORDINATOR  排序并接收快照
        |
        +--> D1 (DB)                          持久化
        |
        +--> 管理台 /admin                     SSR（site/src/ssr.js）+ 静态增强
```

配对走 `/api/pair/claim`，扩展分发走 `/api/extensions/*`。租户与服务器归属限定每一次快照、序号、凭据与扩展变更。

## 数字现状

以 2026-08-27 全量目录发布为准，详见 [CHANGELOG.md](CHANGELOG.md)：

| 项 | 数量 |
|---|---|
| 规划目录条目 | 810（全部已实现，计划中为 0） |
| 运行时记录 | 926 |
| 学科 | 27 |
| 族 | 270 |
| 战役波次 | 9 |
| 便携电力装备 | 47 |
| 无线充电多方块 | 3 |

规划身份与运行时身份是两套 ID：点分的规划 ID 不是命令 ID，实际运行身份从站点 `/runtime` 获取。

## 构建与测试

Java 插件（需要 JDK 25）：

```bash
./mvnw package
```

产物为 `target/talex-soul-tech-3.0.0-SNAPSHOT.jar`。

站点 SSR 合约测试。`site/package.json` 不声明依赖，生成的目录数据也已入库，因此不需要先 `npm install` 或先生成资源：

```bash
cd site
node --test test/ssr-contract.mjs
```

API 合约测试发真实 HTTP 请求，必须先在另一个终端起本地 Worker。目标默认是 `http://127.0.0.1:8788`，可用环境变量 `BASE_URL` 指向其他 localhost 源（只接受 localhost）：

```bash
cd site
wrangler dev --config wrangler.jsonc --port 8788
node --test test/api-contract.mjs
```

站点发布（需要已登录的 wrangler；顺序为生成资源 → 应用远端 D1 迁移 → 部署 Worker）：

```bash
cd site
npm run deploy
```

发布前必须先执行 `./mvnw package`，否则资源生成会因为找不到 JAR 而失败。

## 仓库结构

| 路径 | 内容 |
|---|---|
| `src/` | 插件 Java 源码与测试；`src/main/resources/` 含 `plugin.yml`、`config.yml` 与冻结清单 |
| `site/` | Worker（`src/worker.js`）、SSR（`src/ssr.js`）、D1 迁移、静态资源、生成脚本、合约测试 |
| `docs/` | 面向人的设计说明，索引见 [docs/index.md](docs/index.md) |
| `.trellis/` | 开发规范（`spec/`）、任务与工作日志 |

## 链接

- 生产站点：<https://soultech.tagzxia.com>
- 文档索引：[docs/index.md](docs/index.md)
- 系统不变量，改动电力、多方块、持久化、云同步或发布流程前必读：[.trellis/spec/backend/system-invariants.md](.trellis/spec/backend/system-invariants.md)
- 发布记录：[CHANGELOG.md](CHANGELOG.md)
