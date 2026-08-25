import { CATALOG_STATS, DISCIPLINES } from "../public/data/catalog.js";
import {
  SAAS_ARCHITECTURE,
  SITE_CONTENT,
  TECH_ARCHITECTURE,
  TUTORIALS,
} from "../public/data/content.js";

const DOWNLOAD_URL = "/downloads/TalexSoulTech-3.0.0-SNAPSHOT.jar";
const MANIFEST_URL = "/downloads/manifest.json";
const CATALOG_PAGE_SIZE = 24;

const NAVIGATION = Object.freeze([
  { label: "首页", path: "/" },
  { label: "下载", path: "/download" },
  { label: "教程", path: "/docs" },
  { label: "学科", path: "/disciplines" },
  { label: "资料库", path: "/catalog" },
  { label: "架构", path: "/architecture" },
  { label: "扩展", path: "/extensions" },
  { label: "控制台", path: "/console" },
]);

const STATUS_LABELS = Object.freeze({
  implemented: "已实装",
  planned: "规划中",
});

const FIELD_LABELS = Object.freeze({
  action: "操作",
  actions: "操作",
  api: "API 契约",
  audit: "审计",
  balance: "平衡约束",
  boundaries: "明确边界",
  boundary: "边界",
  capabilities: "能力权限",
  clarification: "口径说明",
  components: "组件",
  composition: "组合关系",
  configuration: "配置项",
  context: "扩展 Context",
  controlPlane: "控制面",
  current: "当前状态",
  currentTables: "当前数据表",
  currentTypes: "已注册类型",
  curriculum: "学科规划",
  dependencyTopology: "依赖拓扑",
  detail: "说明",
  disposal: "资源释放",
  domain: "领域关系",
  economy: "经济循环",
  enabled: "启用状态",
  engine: "运行引擎",
  events: "事件入口",
  flow: "流程",
  guarantees: "保证",
  hotUpdate: "原子热更新",
  implemented: "已实现能力",
  implementedRoots: "已实装学科根目录",
  integrityRule: "完整性规则",
  invariants: "不变量",
  learningGoals: "学习目标",
  lifecycle: "生命周期",
  lifecycleOrder: "生命周期顺序",
  matrix: "兼容矩阵",
  nonClaims: "不承诺事项",
  operatorMeaning: "服主可见语义",
  operatingRules: "运行规则",
  outcome: "结果",
  pairing: "配对",
  phases: "成长阶段",
  plannedItemCapacity: "规划物品容量",
  plannedRoots: "规划学科根目录",
  playerGuidance: "玩家引导",
  playerPromise: "玩家承诺",
  power: "电力系统",
  principles: "策划原则",
  recovery: "故障恢复",
  relationships: "关系",
  requirement: "要求",
  roadmap: "后续路线",
  role: "职责",
  routes: "路由",
  rules: "规则",
  safeguards: "安全措施",
  sandboxes: "沙箱",
  scope: "范围",
  settlement: "结算顺序",
  shutdown: "关闭顺序",
  startup: "启动顺序",
  stages: "阶段",
  state: "状态",
  status: "状态",
  steps: "步骤",
  sync: "快照同步",
  systems: "系统",
  target: "目标",
  tenantIsolation: "租户隔离",
  type: "类型",
  ui: "交互边界",
  useFlow: "使用流程",
  version: "版本",
});

const TITLE_KEYS = Object.freeze([
  "title",
  "name",
  "target",
  "phase",
  "boundary",
  "group",
  "runtime",
  "action",
  "table",
  "key",
  "step",
  "path",
]);

const NARRATIVE_KEYS = new Set([
  "lead",
  "detail",
  "summary",
  "overview",
  "tagline",
  "purpose",
  "clarification",
  "integrityRule",
  "operatorMeaning",
  "playerPromise",
  "designRule",
  "privacy",
  "statement",
]);

const ORDERED_KEYS = new Set([
  "actions",
  "flow",
  "lifecycleOrder",
  "phases",
  "settlement",
  "shutdown",
  "stages",
  "startup",
  "steps",
  "useFlow",
]);

const HTML_ESCAPES = Object.freeze({
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
});

const DISCIPLINE_BY_ID = new Map();
const TUTORIAL_BY_ID = new Map();
const ITEM_BY_ID = new Map();
const CATALOG_RECORDS = [];

for (const tutorial of TUTORIALS) {
  TUTORIAL_BY_ID.set(tutorial.id, tutorial);
}

for (const discipline of DISCIPLINES) {
  DISCIPLINE_BY_ID.set(discipline.id, discipline);
  for (const item of discipline.items) {
    const record = Object.freeze({
      item,
      discipline,
      searchText: [
        item.id,
        item.name,
        item.type,
        item.purpose,
        item.family,
        item.recipeHint,
        discipline.name,
        discipline.stage,
      ].map(normalizeCopy).join(" ").toLocaleLowerCase("zh-CN"),
    });
    CATALOG_RECORDS.push(record);
    ITEM_BY_ID.set(item.id, record);
  }
}
Object.freeze(CATALOG_RECORDS);

const PUBLIC_SITEMAP_PATHS = Object.freeze([
  "/",
  "/download",
  "/docs",
  ...TUTORIALS.map((tutorial) => `/docs/${encodeURIComponent(tutorial.id)}`),
  "/catalog",
  "/disciplines",
  ...DISCIPLINES.map((discipline) => `/disciplines/${encodeURIComponent(discipline.id)}`),
  ...CATALOG_RECORDS.map(({ item }) => `/items/${encodeURIComponent(item.id)}`),
  "/planning",
  "/architecture",
  "/extensions",
]);

function normalizeCopy(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => HTML_ESCAPES[character]);
}

function formatScalar(value) {
  if (typeof value === "boolean") return value ? "是" : "否";
  return normalizeCopy(value);
}

function labelFor(key) {
  if (FIELD_LABELS[key]) return FIELD_LABELS[key];
  return String(key)
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .trim();
}

function isScalar(value) {
  return value === null || value === undefined || ["string", "number", "boolean"].includes(typeof value);
}

function titleEntry(record) {
  for (const key of TITLE_KEYS) {
    if (isScalar(record?.[key]) && normalizeCopy(record[key])) return [key, record[key]];
  }
  return null;
}

function renderPrimitiveList(values, ordered = false) {
  const tag = ordered ? "ol" : "ul";
  const className = ordered ? "ordered-content-list" : "content-list";
  return `<${tag} class="${className}">${values.map((value) => `<li>${escapeHtml(formatScalar(value))}</li>`).join("")}</${tag}>`;
}

function renderDefinitionList(entries) {
  if (!entries.length) return "";
  return `<dl class="definition-grid route-definitions">${entries.map(([key, value]) => `<div><dt>${escapeHtml(labelFor(key))}</dt><dd>${escapeHtml(formatScalar(value))}</dd></div>`).join("")}</dl>`;
}

function renderRecord(record, contextKey = "") {
  const heading = titleEntry(record);
  const omitted = new Set(heading ? [heading[0]] : []);
  const headingHtml = heading ? `<h4>${escapeHtml(formatScalar(heading[1]))}</h4>` : "";
  return `<article class="route-record">${headingHtml}${renderObjectBody(record, omitted, contextKey)}</article>`;
}

function renderStructuredValue(value, key = "") {
  if (value === null || value === undefined) return "";
  if (isScalar(value)) return `<p>${escapeHtml(formatScalar(value))}</p>`;

  if (Array.isArray(value)) {
    if (!value.length) return `<p class="state-copy">暂无记录。</p>`;
    if (value.every(isScalar)) return renderPrimitiveList(value, ORDERED_KEYS.has(key));
    return `<div class="route-record-list">${value.map((entry) => isScalar(entry)
      ? `<p>${escapeHtml(formatScalar(entry))}</p>`
      : renderRecord(entry, key)).join("")}</div>`;
  }

  return renderRecord(value, key);
}

function renderObjectBody(record, omittedKeys = new Set(), contextKey = "") {
  const narrative = [];
  const definitions = [];
  const nested = [];

  for (const [key, value] of Object.entries(record || {})) {
    if (omittedKeys.has(key) || value === null || value === undefined) continue;
    if (isScalar(value)) {
      if (!normalizeCopy(value) && value !== 0 && value !== false) continue;
      if (NARRATIVE_KEYS.has(key)) narrative.push(`<p class="route-copy">${escapeHtml(formatScalar(value))}</p>`);
      else definitions.push([key, value]);
      continue;
    }

    const nestedTitle = !Array.isArray(value) && normalizeCopy(value.title)
      ? value.title
      : labelFor(key);
    const nestedValue = !Array.isArray(value) && normalizeCopy(value.title)
      ? renderObjectBody(value, new Set(["title"]), key)
      : renderStructuredValue(value, key);
    nested.push(`<section class="route-subgroup"><h5>${escapeHtml(nestedTitle)}</h5>${nestedValue}</section>`);
  }

  return `${narrative.join("")}${renderDefinitionList(definitions)}${nested.join("")}`;
}

function renderBreadcrumbs(crumbs) {
  return `<nav class="breadcrumbs" aria-label="面包屑"><ol>${crumbs.map((crumb, index) => {
    const current = index === crumbs.length - 1;
    return current
      ? `<li aria-current="page">${escapeHtml(crumb.label)}</li>`
      : `<li><a href="${escapeHtml(crumb.href)}">${escapeHtml(crumb.label)}</a></li>`;
  }).join("")}</ol></nav>`;
}

function renderRouteHero({ crumbs, title, description, label = "工业手册", actions = "", meta = "" }) {
  return `<section class="route-hero"><div class="shell">${renderBreadcrumbs(crumbs)}<div class="route-hero-layout"><div class="route-hero-copy"><p class="kicker">${escapeHtml(label)}</p><h1>${escapeHtml(title)}</h1><p class="route-lead">${escapeHtml(description)}</p>${actions ? `<div class="hero-actions">${actions}</div>` : ""}</div>${meta ? `<aside class="route-hero-meta">${meta}</aside>` : ""}</div></div></section>`;
}

function statusLabel(status) {
  const normalized = status === "implemented" ? "implemented" : "planned";
  return `<span class="status-label is-${normalized}">${escapeHtml(STATUS_LABELS[normalized])}</span>`;
}

function renderTutorialArticle(tutorial) {
  const notes = Array.isArray(tutorial.notes) && tutorial.notes.length
    ? `<aside class="tutorial-note"><h3>操作备注</h3>${renderPrimitiveList(tutorial.notes)}</aside>`
    : "";
  const diagnosis = Array.isArray(tutorial.diagnosis) && tutorial.diagnosis.length
    ? `<aside class="tutorial-diagnosis"><h3>故障诊断</h3>${renderStructuredValue(tutorial.diagnosis, "diagnosis")}</aside>`
    : "";
  return `<article class="tutorial-reader route-reader"><h2>${escapeHtml(tutorial.title)}</h2><p class="tutorial-summary">${escapeHtml(tutorial.summary)}</p><h3>操作步骤</h3><ol class="tutorial-steps">${tutorial.steps.map((step) => {
    if (isScalar(step)) return `<li><p>${escapeHtml(formatScalar(step))}</p></li>`;
    return `<li><div>${step.title ? `<h4>${escapeHtml(step.title)}</h4>` : ""}${step.detail ? `<p>${escapeHtml(step.detail)}</p>` : renderObjectBody(step)}</div></li>`;
  }).join("")}</ol>${notes}${diagnosis}</article>`;
}

function renderHomePage() {
  const implementedDisciplines = DISCIPLINES.filter((discipline) => discipline.status === "implemented").length;
  const routeEntries = [
    {
      id: "plugin",
      path: "/download",
      label: "插件与下载",
      detail: SITE_CONTENT.download.detail,
      meta: SITE_CONTENT.download.availability,
    },
    {
      id: "tutorials",
      path: "/docs",
      label: "教程",
      detail: "沿服主真实操作顺序，从安装、持久化和向导书推进到电网、云端配对与扩展排错。",
      meta: `${TUTORIALS.length} 篇可独立阅读教程`,
    },
    {
      id: "catalog",
      path: "/catalog",
      label: "资料库",
      detail: "逐项检索物品的学科、层级、用途、配方线索与实装状态，不把路线图写成现状。",
      meta: `${CATALOG_STATS.itemCount} 个物品条目`,
    },
    {
      id: "architecture",
      path: "/architecture",
      label: "架构",
      detail: "分开阅读 Paper 插件运行时与 Cloudflare 多租户控制面，查看它们各自承担的职责。",
      meta: "本地执行与云端控制分层",
    },
    {
      id: "extensions",
      path: "/extensions",
      label: "扩展",
      detail: SAAS_ARCHITECTURE.extensions.lead,
      meta: SAAS_ARCHITECTURE.extensions.status,
    },
    {
      id: "console",
      path: "/console",
      label: "控制台",
      detail: "登录后创建服务器、生成十分钟一次性配对码、读取最近快照并管理受限云端扩展。",
      meta: "私有数据仅由同源 API 加载",
    },
  ];

  const body = `<section class="hero shell route-home-hero" id="top" aria-labelledby="hero-title"><div class="hero-copy"><p class="kicker">${escapeHtml(SITE_CONTENT.hero.eyebrow)}</p><h1 id="hero-title">${escapeHtml(SITE_CONTENT.hero.title)}</h1><p class="hero-summary">${escapeHtml(SITE_CONTENT.hero.description)}</p><div class="hero-actions"><a class="button button-primary" href="/download">${escapeHtml(SITE_CONTENT.hero.primaryAction)}</a><a class="button button-secondary" href="/architecture">${escapeHtml(SITE_CONTENT.hero.secondaryAction)}</a></div></div><figure class="hero-visual"><img src="/assets/voxel-industrial-lab-hero.webp" alt="原创方块工业实验室内的熔炉核心、管线、储罐与多方块机器" width="1536" height="1024" fetchpriority="high"><figcaption>${escapeHtml(SITE_CONTENT.brand.visualStatement)}</figcaption></figure></section><section class="fact-band" aria-label="一期关键现状"><dl class="shell fact-grid"><div><dt>已落地学科根目录</dt><dd>${escapeHtml(implementedDisciplines)}</dd></div><div><dt>资料库条目</dt><dd>${escapeHtml(CATALOG_STATS.itemCount)}</dd></div><div><dt>已实装物品</dt><dd>${escapeHtml(CATALOG_STATS.implementedCount)}</dd></div><div><dt>运行基线</dt><dd>Paper 26.1.2 · Java 25</dd></div></dl></section><section class="section shell route-home-directory" aria-labelledby="home-directory-title"><header class="section-heading"><h2 id="home-directory-title">按任务进入，不在首页翻完整手册</h2><p>${escapeHtml(SITE_CONTENT.brand.tagline)}</p></header><div class="route-directory">${routeEntries.map((entry) => `<section class="route-directory-row" id="${escapeHtml(entry.id)}"><div><h3><a href="${escapeHtml(entry.path)}">${escapeHtml(entry.label)}</a></h3><p>${escapeHtml(entry.detail)}</p></div><div class="route-directory-meta"><span>${escapeHtml(entry.meta)}</span><a class="text-button" href="${escapeHtml(entry.path)}" aria-label="进入${escapeHtml(entry.label)}">进入章节</a></div></section>`).join("")}</div></section>`;

  return {
    title: "TalexSoulTech | 灵魂科技工业手册",
    description: "TalexSoulTech 插件官网：查看 Paper 运行基线、下载安装、教程、27 学科资料库、技术架构、扩展运行时与服务器控制台。",
    canonicalPath: "/",
    kind: "home",
    body,
  };
}

function renderDownloadPage() {
  const compatibilityRows = SITE_CONTENT.compatibility.matrix.map((row) => `<tr><th scope="row" data-label="目标">${escapeHtml(row.target)}</th><td data-label="要求">${escapeHtml(row.requirement)}</td><td data-label="状态">${escapeHtml(row.status)}</td><td data-label="说明">${escapeHtml(row.detail)}</td></tr>`).join("");
  const actions = `<a class="button button-primary" href="${DOWNLOAD_URL}" download>下载 JAR</a><a class="button button-secondary" href="${MANIFEST_URL}">查看构件清单</a>`;
  const meta = `<dl class="route-meta-list"><div><dt>发行版本</dt><dd>3.0.0-SNAPSHOT</dd></div><div><dt>运行时</dt><dd>Java 25</dd></div><div><dt>服务端基线</dt><dd>Paper 26.1.2</dd></div></dl>`;
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "下载" }],
    title: SITE_CONTENT.download.title,
    description: SITE_CONTENT.download.detail,
    label: SITE_CONTENT.download.availability,
    actions,
    meta,
  })}<section class="section shell route-section" aria-labelledby="artifact-title"><div class="download-station route-download-station"><div><p class="station-label">可部署构件</p><h2 id="artifact-title">${escapeHtml(SITE_CONTENT.download.artifact)}</h2><p>下载地址与构件清单保持独立，部署前应核对版本、大小与 SHA-256。</p></div><div class="artifact-meta"><dl><div><dt>JAR</dt><dd>${escapeHtml(DOWNLOAD_URL)}</dd></div><div><dt>Manifest</dt><dd>${escapeHtml(MANIFEST_URL)}</dd></div></dl></div><a class="button button-inverse" href="${DOWNLOAD_URL}" download>下载 JAR</a></div><div class="route-two-column"><article class="manual-block"><h2>${escapeHtml(SITE_CONTENT.quickInstall.title)}</h2><p class="route-copy">${escapeHtml(SITE_CONTENT.quickInstall.lead)}</p><ol class="route-step-list">${SITE_CONTENT.quickInstall.steps.map((step) => `<li><span>${escapeHtml(step.step)}</span><div><h3>${escapeHtml(step.title)}</h3><p>${escapeHtml(step.detail)}</p></div></li>`).join("")}</ol></article><aside class="route-callout"><h2>取得产物后</h2>${renderPrimitiveList(SITE_CONTENT.download.steps, true)}<p>${escapeHtml(SITE_CONTENT.download.integrityRule)}</p></aside></div></section><section class="section shell route-section" aria-labelledby="compatibility-page-title"><header class="section-heading compact-heading"><h2 id="compatibility-page-title">${escapeHtml(SITE_CONTENT.compatibility.title)}</h2><p>${escapeHtml(SITE_CONTENT.compatibility.lead)}</p></header><div class="table-frame"><table class="data-table route-static-table"><thead><tr><th scope="col">目标</th><th scope="col">要求</th><th scope="col">状态</th><th scope="col">说明</th></tr></thead><tbody>${compatibilityRows}</tbody></table></div></section>`;

  return {
    title: "下载与兼容基线 | TalexSoulTech",
    description: "下载 TalexSoulTech 3.0.0-SNAPSHOT JAR，并核对 Java 25、Paper 26.1.2 与 MySQL 部署前置条件。",
    canonicalPath: "/download",
    kind: "download",
    body,
  };
}

function renderDocsIndexPage() {
  const firstTutorial = TUTORIALS[0];
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "教程" }],
    title: "从安装到稳定运行",
    description: "每篇教程对应一条可独立完成的服主任务。先确认本地运行边界，再接入云端配对与受限扩展。",
    label: `${TUTORIALS.length} 篇操作教程`,
  })}<section class="section shell route-section" id="tutorials" aria-labelledby="tutorials-title"><header class="section-heading compact-heading"><h2 id="tutorials-title">教程索引</h2><p>左侧选择任务；每篇教程都有独立 URL，可在禁用 JavaScript 时完整阅读。</p></header><div class="tutorial-workbench route-docs-workbench"><nav class="tutorial-index route-doc-index" id="tutorial-index" aria-label="教程章节">${TUTORIALS.map((tutorial, index) => `<a href="/docs/${encodeURIComponent(tutorial.id)}"${index === 0 ? ' aria-current="page"' : ""}><span>${String(index + 1).padStart(2, "0")}</span><strong>${escapeHtml(tutorial.label)}</strong><small>${escapeHtml(tutorial.summary)}</small></a>`).join("")}</nav><div id="tutorial-reader" tabindex="-1" aria-live="polite">${renderTutorialArticle(firstTutorial)}</div></div></section>`;

  return {
    title: "教程索引 | TalexSoulTech",
    description: "按安装、MySQL、向导书、机器、电网、云端配对、云端扩展与排错顺序阅读 TalexSoulTech 教程。",
    canonicalPath: "/docs",
    kind: "docs",
    scripts: ["/app.js"],
    body,
  };
}

function renderTutorialPage(tutorial) {
  const index = TUTORIALS.indexOf(tutorial);
  const previous = index > 0 ? TUTORIALS[index - 1] : null;
  const next = index < TUTORIALS.length - 1 ? TUTORIALS[index + 1] : null;
  const body = `${renderRouteHero({
    crumbs: [
      { label: "首页", href: "/" },
      { label: "教程", href: "/docs" },
      { label: tutorial.label },
    ],
    title: tutorial.title,
    description: tutorial.summary,
    label: `教程 ${String(index + 1).padStart(2, "0")} / ${String(TUTORIALS.length).padStart(2, "0")}`,
  })}<section class="section shell route-section route-reader-shell">${renderTutorialArticle(tutorial)}<nav class="route-pagination" aria-label="教程翻页">${previous ? `<a class="button button-secondary" rel="prev" href="/docs/${encodeURIComponent(previous.id)}">上一章 · ${escapeHtml(previous.label)}</a>` : "<span></span>"}${next ? `<a class="button button-secondary" rel="next" href="/docs/${encodeURIComponent(next.id)}">下一章 · ${escapeHtml(next.label)}</a>` : ""}</nav></section>`;

  return {
    title: `${tutorial.title} | TalexSoulTech 教程`,
    description: tutorial.summary,
    canonicalPath: `/docs/${encodeURIComponent(tutorial.id)}`,
    kind: "tutorial",
    body,
  };
}

function normalizedCatalogQuery(url) {
  const q = normalizeCopy(url.searchParams.get("q"));
  const requestedDiscipline = normalizeCopy(url.searchParams.get("discipline"));
  const requestedStatus = normalizeCopy(url.searchParams.get("status"));
  const discipline = DISCIPLINE_BY_ID.has(requestedDiscipline) ? requestedDiscipline : "all";
  const status = ["implemented", "planned"].includes(requestedStatus) ? requestedStatus : "all";
  const rawPage = normalizeCopy(url.searchParams.get("page"));
  const page = /^\d+$/.test(rawPage) && Number(rawPage) > 0 ? Number(rawPage) : 1;
  return { q, discipline, status, page };
}

function catalogHref({ q, discipline, status, page }) {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (discipline !== "all") params.set("discipline", discipline);
  if (status !== "all") params.set("status", status);
  if (page > 1) params.set("page", String(page));
  const query = params.toString();
  return query ? `/catalog?${query}` : "/catalog";
}

function renderCatalogPage(url) {
  const query = normalizedCatalogQuery(url);
  const needle = query.q.toLocaleLowerCase("zh-CN");
  const filtered = CATALOG_RECORDS.filter(({ item, searchText }) => {
    if (query.discipline !== "all" && item.disciplineId !== query.discipline) return false;
    if (query.status !== "all" && item.status !== query.status) return false;
    return !needle || searchText.includes(needle);
  });
  const totalPages = Math.max(1, Math.ceil(filtered.length / CATALOG_PAGE_SIZE));
  const page = Math.min(query.page, totalPages);
  const pageQuery = { ...query, page };
  const pageItems = filtered.slice((page - 1) * CATALOG_PAGE_SIZE, page * CATALOG_PAGE_SIZE);
  const options = DISCIPLINES.map((discipline) => `<option value="${escapeHtml(discipline.id)}"${discipline.id === query.discipline ? " selected" : ""}>${escapeHtml(discipline.name)}</option>`).join("");
  const chips = [
    `<a class="discipline-chip" href="${escapeHtml(catalogHref({ ...pageQuery, discipline: "all", page: 1 }))}" data-discipline="all" aria-pressed="${query.discipline === "all"}">全部学科</a>`,
    ...DISCIPLINES.map((discipline) => `<a class="discipline-chip" href="${escapeHtml(catalogHref({ ...pageQuery, discipline: discipline.id, page: 1 }))}" data-discipline="${escapeHtml(discipline.id)}" aria-pressed="${discipline.id === query.discipline}">${escapeHtml(discipline.name)}</a>`),
  ].join("");
  const rows = pageItems.map(({ item, discipline }) => `<tr><td data-label="物品"><a class="cell-title" href="/items/${encodeURIComponent(item.id)}">${escapeHtml(item.name)}</a><span class="cell-code">${escapeHtml(item.id)}</span></td><td data-label="学科"><a href="/disciplines/${encodeURIComponent(discipline.id)}">${escapeHtml(discipline.name)}</a></td><td data-label="层级">${escapeHtml(item.tier)} · ${escapeHtml(item.type)}</td><td data-label="状态">${statusLabel(item.status)}</td><td data-label="操作"><a class="text-button" href="/items/${encodeURIComponent(item.id)}">查看详情</a></td></tr>`).join("");
  const previousHref = page > 1 ? catalogHref({ ...pageQuery, page: page - 1 }) : "";
  const nextHref = page < totalPages ? catalogHref({ ...pageQuery, page: page + 1 }) : "";
  const resultCopy = query.q
    ? `“${query.q}”找到 ${filtered.length} 个条目`
    : `找到 ${filtered.length} 个条目`;
  const stats = [
    ["学科总数", CATALOG_STATS.disciplineCount],
    ["物品总数", CATALOG_STATS.itemCount],
    ["已实装", CATALOG_STATS.implementedCount],
    ["规划中", CATALOG_STATS.plannedCount],
  ];
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "资料库" }],
    title: "27 学科资料库",
    description: "检索 810 个物品，逐项区分已实装能力与后续规划。筛选、分页和详情链接在禁用 JavaScript 时仍然可用。",
    label: `${CATALOG_STATS.itemCount} 个可核对条目`,
    actions: '<a class="button button-secondary" href="/disciplines">按学科浏览</a><a class="button button-secondary" href="/planning">查看完整策划</a>',
  })}<section class="section shell catalog-section route-catalog" id="catalog" aria-labelledby="catalog-title"><header class="section-heading compact-heading"><h2 id="catalog-title">物品索引</h2><p>名称、用途、配方线索、学科与研发状态共同参与筛选。</p></header><dl class="catalog-stats" id="catalog-stats" aria-label="资料库统计">${stats.map(([label, value]) => `<div class="catalog-stat"><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>`).join("")}</dl><form class="catalog-controls" id="catalog-controls" action="/catalog" method="get" role="search"><div class="field field-search"><label for="catalog-search">检索名称、用途或配方线索</label><input id="catalog-search" name="q" type="search" value="${escapeHtml(query.q)}" autocomplete="off" enterkeyhint="search" placeholder="输入物品名称或关键词"></div><div class="field"><label for="discipline-filter">学科</label><select id="discipline-filter" name="discipline"><option value="all"${query.discipline === "all" ? " selected" : ""}>全部学科</option>${options}</select></div><div class="field"><label for="status-filter">研发状态</label><select id="status-filter" name="status"><option value="all"${query.status === "all" ? " selected" : ""}>全部状态</option><option value="implemented"${query.status === "implemented" ? " selected" : ""}>已实装</option><option value="planned"${query.status === "planned" ? " selected" : ""}>规划中</option></select></div><div class="catalog-submit-group"><button class="button button-primary" type="submit">应用筛选</button><a class="button button-secondary reset-filter" id="catalog-reset" href="/catalog">重置</a></div></form><div class="discipline-strip" id="discipline-strip" aria-label="按学科快速筛选">${chips}</div><div class="catalog-result-bar" aria-live="polite"><p id="catalog-result-count" tabindex="-1">${escapeHtml(resultCopy)}</p><p id="catalog-page-status">第 ${page} / ${totalPages} 页</p></div><div class="table-frame" id="catalog-table-frame"${pageItems.length ? "" : " hidden"}><table class="data-table catalog-table"><thead><tr><th scope="col">物品</th><th scope="col">学科</th><th scope="col">层级</th><th scope="col">状态</th><th scope="col"><span class="sr-only">操作</span></th></tr></thead><tbody id="catalog-body">${rows}</tbody></table></div><div class="empty-state" id="catalog-empty"${pageItems.length ? " hidden" : ""}><h3>没有匹配的物品</h3><p>缩短关键词或清除一个筛选条件后再试。</p><a class="button button-secondary" id="catalog-empty-reset" href="/catalog">清除筛选</a></div><nav class="pagination" aria-label="资料库分页">${previousHref ? `<a class="button button-secondary" id="catalog-prev" rel="prev" href="${escapeHtml(previousHref)}">上一页</a>` : '<a class="button button-secondary is-disabled" id="catalog-prev" aria-disabled="true">上一页</a>'}${nextHref ? `<a class="button button-secondary" id="catalog-next" rel="next" href="${escapeHtml(nextHref)}">下一页</a>` : '<a class="button button-secondary is-disabled" id="catalog-next" aria-disabled="true">下一页</a>'}</nav></section>`;

  return {
    title: "27 学科资料库 | TalexSoulTech",
    description: "检索 TalexSoulTech 的 27 学科与 810 个物品条目，按学科、状态和关键词筛选并查看独立物品详情。",
    canonicalPath: "/catalog",
    kind: "catalog",
    scripts: ["/app.js"],
    body,
  };
}

function renderDisciplinesPage() {
  const stages = new Map();
  for (const discipline of DISCIPLINES) {
    if (!stages.has(discipline.stageLabel)) stages.set(discipline.stageLabel, []);
    stages.get(discipline.stageLabel).push(discipline);
  }
  const groups = [...stages.entries()].map(([stageLabel, disciplines]) => `<section class="discipline-stage"><header><h2>${escapeHtml(stageLabel)}</h2><p>${disciplines.filter((discipline) => discipline.status === "implemented").length} 门已实装 · ${disciplines.filter((discipline) => discipline.status === "planned").length} 门规划中</p></header><div class="route-directory">${disciplines.map((discipline) => `<article class="route-directory-row"><div><h3><a href="/disciplines/${encodeURIComponent(discipline.id)}">${escapeHtml(discipline.name)}</a></h3><p>${escapeHtml(discipline.tagline)}</p></div><div class="route-directory-meta">${statusLabel(discipline.status)}<span>${discipline.items.length} 个物品</span></div></article>`).join("")}</div></section>`).join("");
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "学科" }],
    title: "27 门学科的完整边界",
    description: SITE_CONTENT.planning.lead,
    label: `${CATALOG_STATS.disciplineCount} 门学科 · 四阶段`,
    actions: '<a class="button button-secondary" href="/catalog">检索全部物品</a><a class="button button-secondary" href="/planning">阅读策划原则</a>',
  })}<section class="section shell route-section discipline-index" aria-label="学科索引">${groups}</section>`;

  return {
    title: "学科索引 | TalexSoulTech",
    description: "浏览 TalexSoulTech 四个阶段的 27 门学科，核对每门学科的状态、学习目标、概念家族与 30 个物品条目。",
    canonicalPath: "/disciplines",
    kind: "disciplines",
    body,
  };
}

function renderDisciplinePage(discipline) {
  const families = discipline.families.map((family) => `<tr><th scope="row" data-label="概念家族">${escapeHtml(family.name)}</th><td data-label="标识"><code>${escapeHtml(family.id)}</code></td><td data-label="设计职责">${escapeHtml(family.concept)}</td></tr>`).join("");
  const items = discipline.items.map((item) => `<tr><td data-label="物品"><a class="cell-title" href="/items/${encodeURIComponent(item.id)}">${escapeHtml(item.name)}</a><span class="cell-code">${escapeHtml(item.id)}</span></td><td data-label="家族">${escapeHtml(item.family)}</td><td data-label="层级">${escapeHtml(item.tier)} · ${escapeHtml(item.type)}</td><td data-label="状态">${statusLabel(item.status)}</td><td data-label="操作"><a class="text-button" href="/items/${encodeURIComponent(item.id)}">查看详情</a></td></tr>`).join("");
  const body = `${renderRouteHero({
    crumbs: [
      { label: "首页", href: "/" },
      { label: "学科", href: "/disciplines" },
      { label: discipline.name },
    ],
    title: discipline.name,
    description: discipline.tagline,
    label: discipline.stageLabel,
    meta: `<dl class="route-meta-list"><div><dt>研发状态</dt><dd>${escapeHtml(STATUS_LABELS[discipline.status])}</dd></div><div><dt>概念家族</dt><dd>${discipline.families.length}</dd></div><div><dt>物品容量</dt><dd>${discipline.items.length}</dd></div></dl>`,
  })}<section class="section shell route-section discipline-detail"><div class="route-two-column"><article class="route-prose"><h2>学科职责</h2><p>${escapeHtml(discipline.overview)}</p></article><aside class="route-callout"><h2>学习目标</h2>${renderPrimitiveList(discipline.learningGoals, true)}</aside></div><section class="route-inner-section" aria-labelledby="families-title"><header class="section-heading compact-heading"><h2 id="families-title">十个概念家族</h2><p>概念家族定义内容职责，不等于已经存在十台机器或十条可生产配方。</p></header><div class="table-frame"><table class="data-table route-static-table"><thead><tr><th scope="col">概念家族</th><th scope="col">标识</th><th scope="col">设计职责</th></tr></thead><tbody>${families}</tbody></table></div></section><section class="route-inner-section catalog-section" aria-labelledby="discipline-items-title"><header class="section-heading compact-heading"><h2 id="discipline-items-title">${discipline.items.length} 个物品条目</h2><p>每个条目都有独立详情页，并明确用途、配方线索与研发状态。</p></header><div class="table-frame"><table class="data-table"><thead><tr><th scope="col">物品</th><th scope="col">家族</th><th scope="col">层级</th><th scope="col">状态</th><th scope="col"><span class="sr-only">操作</span></th></tr></thead><tbody>${items}</tbody></table></div></section></section>`;

  return {
    title: `${discipline.name} | TalexSoulTech 学科`,
    description: `${discipline.tagline} 查看学习目标、概念家族与 ${discipline.items.length} 个物品条目。`,
    canonicalPath: `/disciplines/${encodeURIComponent(discipline.id)}`,
    kind: "discipline",
    body,
  };
}

function renderItemPage(record) {
  const { item, discipline } = record;
  const siblings = discipline.items.filter((candidate) => candidate.family === item.family && candidate.id !== item.id).slice(0, 5);
  const siblingList = siblings.length
    ? `<ul class="route-link-list">${siblings.map((sibling) => `<li><a href="/items/${encodeURIComponent(sibling.id)}">${escapeHtml(sibling.name)}</a>${statusLabel(sibling.status)}</li>`).join("")}</ul>`
    : `<p class="state-copy">当前家族没有其他条目。</p>`;
  const body = `${renderRouteHero({
    crumbs: [
      { label: "首页", href: "/" },
      { label: "资料库", href: "/catalog" },
      { label: discipline.name, href: `/disciplines/${encodeURIComponent(discipline.id)}` },
      { label: item.name },
    ],
    title: item.name,
    description: item.purpose,
    label: `${discipline.name} · ${item.family}`,
    meta: `<dl class="route-meta-list"><div><dt>研发状态</dt><dd>${escapeHtml(STATUS_LABELS[item.status])}</dd></div><div><dt>层级</dt><dd>${escapeHtml(item.tier)}</dd></div><div><dt>类型</dt><dd>${escapeHtml(item.type)}</dd></div></dl>`,
  })}<section class="section shell route-section item-detail"><div class="route-two-column"><article class="route-prose"><h2>条目说明</h2><dl class="definition-grid route-definitions"><div><dt>内部标识</dt><dd><code>${escapeHtml(item.id)}</code></dd></div><div><dt>所属学科</dt><dd><a href="/disciplines/${encodeURIComponent(discipline.id)}">${escapeHtml(discipline.name)}</a></dd></div><div><dt>概念家族</dt><dd>${escapeHtml(item.family)}</dd></div><div><dt>用途</dt><dd>${escapeHtml(item.purpose)}</dd></div></dl><section class="route-callout route-recipe"><h3>配方线索</h3><p>${escapeHtml(item.recipeHint)}</p></section></article><aside class="route-related"><h2>同一家族</h2>${siblingList}<a class="text-button" href="${escapeHtml(catalogHref({ q: "", discipline: discipline.id, status: "all", page: 1 }))}">查看该学科全部物品</a></aside></div></section>`;

  return {
    title: `${item.name} | TalexSoulTech 资料库`,
    description: `${item.name}：${item.purpose} 所属${discipline.name}，${STATUS_LABELS[item.status]}。`,
    canonicalPath: `/items/${encodeURIComponent(item.id)}`,
    kind: "item",
    body,
  };
}

function renderContentSection(content, id) {
  return `<section class="route-inner-section" id="${escapeHtml(id)}"><header class="section-heading compact-heading"><h2>${escapeHtml(content.title)}</h2>${content.lead ? `<p>${escapeHtml(content.lead)}</p>` : ""}</header><div class="route-structured-content">${renderObjectBody(content, new Set(["title", "lead"]))}</div></section>`;
}

function renderPlanningPage() {
  const planning = SITE_CONTENT.planning;
  const curriculumRows = planning.curriculum.map((entry) => `<tr><td data-label="序号" class="cell-code">${escapeHtml(entry.order)}</td><th scope="row" data-label="学科">${escapeHtml(entry.name)}</th><td data-label="状态">${escapeHtml(entry.state)}</td><td data-label="职责">${escapeHtml(entry.role)}</td></tr>`).join("");
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "完整策划" }],
    title: planning.title,
    description: planning.lead,
    label: "游戏策划与运行边界",
    actions: '<a class="button button-secondary" href="#core-loop">核心循环</a><a class="button button-secondary" href="#curriculum">学科容量</a>',
  })}<section class="section shell route-section planning-manual"><nav class="route-anchor-index" aria-label="本页索引"><a href="#guidebook">向导书</a><a href="#machines">机器</a><a href="#power">电力</a><a href="#core-loop">核心循环</a><a href="#progression">成长节奏</a><a href="#curriculum">学科容量</a><a href="#economy">经济与恢复</a></nav>${renderContentSection(SITE_CONTENT.guidebook, "guidebook")}${renderContentSection(SITE_CONTENT.machines, "machines")}${renderContentSection(SITE_CONTENT.power, "power")}${renderContentSection(SITE_CONTENT.coreLoop, "core-loop")}${renderContentSection(SITE_CONTENT.progression, "progression")}<section class="route-inner-section" id="curriculum"><header class="section-heading compact-heading"><h2>${escapeHtml(planning.title)}</h2><p>${escapeHtml(planning.lead)}</p></header><div class="route-two-column route-accounting"><div><h3>容量口径</h3>${renderStructuredValue(planning.accounting, "accounting")}</div><aside class="route-callout"><h3>策划原则</h3>${renderPrimitiveList(planning.principles)}</aside></div><div class="table-frame route-curriculum-table"><table class="data-table route-static-table"><thead><tr><th scope="col">序号</th><th scope="col">学科</th><th scope="col">状态</th><th scope="col">职责</th></tr></thead><tbody>${curriculumRows}</tbody></table></div></section>${renderContentSection(SITE_CONTENT.economy, "economy")}</section>`;

  return {
    title: "完整游戏策划 | TalexSoulTech",
    description: "阅读 TalexSoulTech 的向导书、机器、电力、核心循环、成长节奏、27 学科容量、经济平衡与故障恢复原则。",
    canonicalPath: "/planning",
    kind: "planning",
    body,
  };
}

function renderArchitecturePanel(data, id, labelledBy) {
  const groups = Object.entries(data).filter(([key, value]) => !["title", "lead"].includes(key) && value !== null && value !== undefined);
  return `<section class="architecture-panel" id="${escapeHtml(id)}" role="tabpanel" aria-labelledby="${escapeHtml(labelledBy)}"><p class="hero-summary">${escapeHtml(data.lead)}</p><div class="architecture-map">${groups.map(([key, value]) => {
    const groupTitle = !Array.isArray(value) && value && normalizeCopy(value.title) ? value.title : labelFor(key);
    const groupContent = !Array.isArray(value) && value && normalizeCopy(value.title)
      ? renderObjectBody(value, new Set(["title"]), key)
      : renderStructuredValue(value, key);
    return `<section class="architecture-group"><h3>${escapeHtml(groupTitle)}</h3><div class="architecture-content">${groupContent}</div></section>`;
  }).join("")}</div></section>`;
}

function renderArchitecturePage() {
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "架构" }],
    title: "一期技术架构",
    description: "插件运行时负责 Paper 服内规则与本地数据；Cloudflare SaaS 负责身份、归属、配对、顺序化快照和受能力约束的扩展控制面。",
    label: "执行面与控制面分层",
  })}<section class="section shell route-section" id="architecture" aria-labelledby="architecture-title"><header class="section-heading compact-heading"><h2 id="architecture-title">两套运行边界</h2><p>启用 JavaScript 后可用页签切换；禁用 JavaScript 时两套架构都保持完整可读。</p></header><div class="architecture-switch" role="tablist" aria-label="架构视图"><button class="architecture-tab" id="architecture-plugin-tab" type="button" role="tab" aria-selected="true" aria-controls="architecture-plugin">插件运行时</button><button class="architecture-tab" id="architecture-saas-tab" type="button" role="tab" aria-selected="false" aria-controls="architecture-saas" tabindex="-1">Cloudflare SaaS</button></div><div class="route-architecture-stack">${renderArchitecturePanel(TECH_ARCHITECTURE, "architecture-plugin", "architecture-plugin-tab")}${renderArchitecturePanel(SAAS_ARCHITECTURE, "architecture-saas", "architecture-saas-tab")}</div></section>`;

  return {
    title: "技术架构 | TalexSoulTech",
    description: "查看 TalexSoulTech 一期 Paper 插件架构与 Cloudflare SaaS 架构，包括事件、电力、配对、同步、租户隔离和扩展运行时。",
    canonicalPath: "/architecture",
    kind: "architecture",
    scripts: ["/app.js"],
    body,
  };
}

function renderExtensionsPage() {
  const extensions = SAAS_ARCHITECTURE.extensions;
  const lifecycle = [extensions.context, extensions.dependencyTopology, extensions.disposal].filter(Boolean);
  const permissions = ["log", "schedule", "events", "commands", "kv", "catalog"];
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "扩展" }],
    title: extensions.title,
    description: extensions.lead,
    label: extensions.status,
    actions: '<a class="button button-primary" href="/console">进入控制台</a><a class="button button-secondary" href="#control-plane">查看控制面</a>',
  })}<section class="section extension-system" id="extensions" aria-labelledby="extensions-title"><div class="shell"><header class="section-heading compact-heading extension-heading"><p class="toolbar-label" id="extension-public-status">${escapeHtml(extensions.status)}</p><h2 id="extensions-title">${escapeHtml(extensions.title)}</h2><p id="extension-public-lead">${escapeHtml(extensions.lead)}</p></header><div class="extension-runtime"><ol class="extension-runtime-flow" id="extension-runtime-flow" aria-label="扩展生命周期">${lifecycle.map((phase, index) => `<li><span class="extension-step-index" aria-hidden="true">${String(index + 1).padStart(2, "0")}</span><div><h3>${escapeHtml(phase.title)}</h3><p>${escapeHtml(phase.detail)}</p></div></li>`).join("")}</ol><aside class="extension-security"><div class="extension-security-heading"><p class="toolbar-label">安全边界</p><h3 id="extension-security-title">${escapeHtml(extensions.capabilities.title)}</h3></div><dl class="extension-security-list" id="extension-security-list">${extensions.sandboxes.map((sandbox) => `<div><dt>${escapeHtml(sandbox.runtime)}</dt><dd>${escapeHtml(sandbox.detail)}</dd></div>`).join("")}<div><dt>能力白名单</dt><dd><p>${escapeHtml(extensions.capabilities.detail)}</p><ul class="extension-tag-list">${permissions.map((permission) => `<li class="extension-tag">${escapeHtml(permission)}</li>`).join("")}</ul><ul class="extension-security-rules">${extensions.capabilities.rules.map((rule) => `<li>${escapeHtml(rule)}</li>`).join("")}</ul></dd></div></dl></aside></div><div class="extension-update"><div class="extension-update-copy"><h3 id="extension-update-title">${escapeHtml(extensions.hotUpdate.title)}</h3><p id="extension-update-detail">${escapeHtml(extensions.hotUpdate.detail)}</p></div><ol class="extension-update-steps" id="extension-update-steps" aria-label="热更新步骤">${extensions.hotUpdate.flow.map((step, index) => `<li><strong>${String(index + 1).padStart(2, "0")}</strong><span>${escapeHtml(step)}</span></li>`).join("")}</ol></div><div class="route-extension-details" id="control-plane">${[extensions.controlPlane, extensions.audit].map((section) => `<section class="route-record"><h3>${escapeHtml(section.title)}</h3><p class="route-copy">${escapeHtml(section.detail)}</p>${renderObjectBody(section, new Set(["title", "detail"]))}</section>`).join("")}</div></div></section>`;

  return {
    title: "云端扩展运行时 | TalexSoulTech",
    description: "了解 TalexSoulTech 的扩展 Context、依赖拓扑、LIFO 释放、原子热更新、last-known-good、Lua/JS 沙箱、能力白名单和审计。",
    canonicalPath: "/extensions",
    kind: "extensions",
    scripts: ["/app.js"],
    body,
  };
}

function renderConsoleBody() {
  return `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "控制台" }],
    title: "多服务器控制台",
    description: "登录后管理自己创建的 Paper 服务器。Cookie、服务器列表、快照与扩展数据只由同源 API 在浏览器中加载，不写入 SSR。",
    label: "租户私有控制面",
  })}<section class="section console-section route-console" id="console" aria-labelledby="console-title"><div class="shell"><header class="section-heading console-heading"><h2 id="console-title">连接你的 Paper 服务器</h2><p>配对码十分钟失效，API Key 不会在网页中回显；每个服务器的扩展与快照保持租户隔离。</p></header><div class="console-loading" id="console-loading" aria-live="polite"><div class="content-skeleton" aria-label="正在检查登录状态"><span></span><span></span><span></span></div><p>正在检查登录状态...</p></div><div class="auth-gate" id="auth-gate" hidden><div class="auth-intro"><h3>连接你的 Paper 服务器</h3><p>未登录时仍可浏览全部学科与教程。登录只用于服务器配对和快照管理。</p><ul><li>会话仅存于 HttpOnly 安全 Cookie</li><li>服务器 API Key 只保存哈希</li><li>每个服务器独立串行同步</li></ul></div><div class="auth-panel"><div class="auth-tabs" role="tablist" aria-label="账号操作"><button id="login-tab" type="button" role="tab" aria-selected="true" aria-controls="login-panel">登录</button><button id="register-tab" type="button" role="tab" aria-selected="false" aria-controls="register-panel" tabindex="-1">注册</button></div><div class="form-message form-message-error" id="auth-error" role="alert" hidden></div><form class="auth-form" id="login-panel" role="tabpanel" aria-labelledby="login-tab" novalidate><div class="field"><label for="login-username">用户名</label><input id="login-username" name="username" type="text" autocomplete="username" minlength="3" maxlength="32" pattern="[A-Za-z0-9_-]+" required><p class="field-help">使用创建账号时的用户名。</p></div><div class="field"><label for="login-password">密码</label><input id="login-password" name="password" type="password" autocomplete="current-password" minlength="8" maxlength="128" required></div><button class="button button-primary" type="submit">登录控制台</button></form><form class="auth-form" id="register-panel" role="tabpanel" aria-labelledby="register-tab" hidden novalidate><div class="field"><label for="register-username">用户名</label><input id="register-username" name="username" type="text" autocomplete="username" minlength="3" maxlength="32" pattern="[A-Za-z0-9_-]+" required><p class="field-help">3-32 个字母、数字、下划线或连字符。</p></div><div class="field"><label for="register-password">密码</label><input id="register-password" name="password" type="password" autocomplete="new-password" minlength="8" maxlength="128" required><p class="field-help">至少 8 位，避免使用服务器面板中的现有密码。</p></div><button class="button button-primary" type="submit">创建账号</button></form></div></div><div class="server-console" id="server-console" hidden><header class="console-toolbar"><div><p class="toolbar-label">当前账号</p><h3 id="account-name"></h3></div><button class="button button-secondary" id="logout-button" type="button">退出登录</button></header><div class="console-notice form-message form-message-error" id="console-error" role="alert" hidden></div><form class="server-create" id="server-create-form" novalidate><div class="field"><label for="server-name">新服务器名称</label><input id="server-name" name="name" type="text" maxlength="64" autocomplete="off" placeholder="例如：主生存服" required></div><button class="button button-primary" type="submit">创建服务器</button></form><div class="console-grid"><section class="server-list-panel" aria-labelledby="server-list-title"><div class="panel-heading"><div><h3 id="server-list-title">我的服务器</h3><p id="server-list-summary"></p></div><button class="text-button" id="reload-servers" type="button">重新载入</button></div><div class="table-frame"><table class="data-table server-table"><thead><tr><th scope="col">名称</th><th scope="col">配对</th><th scope="col">最近同步</th><th scope="col"><span class="sr-only">操作</span></th></tr></thead><tbody id="server-list-body"></tbody></table></div><div class="empty-state compact-empty" id="server-empty" hidden><h4>还没有服务器</h4><p>先命名服务器，再生成配对码并在游戏内完成连接。</p></div></section><aside class="server-detail-panel" id="server-detail" aria-labelledby="server-detail-title"><div class="empty-state compact-empty" id="server-detail-empty"><h3 id="server-detail-title">选择一台服务器</h3><p>查看配对状态、生成一次性配对码或读取最近快照。</p></div><div id="server-detail-content" hidden><div class="panel-heading detail-heading"><div><p class="toolbar-label">服务器</p><h3 id="selected-server-name"></h3><p id="selected-server-id"></p></div><button class="button button-primary" id="pairing-button" type="button">生成配对码</button></div><div class="server-facts" id="server-facts"></div><div class="pairing-result" id="pairing-result" hidden aria-live="polite"><div><p class="toolbar-label">一次性配对码</p><output id="pairing-code"></output><p id="pairing-expiry"></p></div><button class="button button-secondary" id="copy-pairing-code" type="button">复制配对码</button></div><section class="snapshot-panel" aria-labelledby="snapshot-title"><div class="panel-heading"><div><h4 id="snapshot-title">最近同步快照</h4><p id="snapshot-summary"></p></div><button class="text-button" id="refresh-snapshot" type="button">刷新快照</button></div><div id="snapshot-content" aria-live="polite"></div></section></div></aside></div><section class="extension-manager" id="extension-manager" aria-labelledby="extension-manager-title" aria-busy="false" hidden><header class="extension-manager-toolbar"><div><p class="toolbar-label">当前服务器扩展</p><h3 id="extension-manager-title">云端扩展</h3><p class="extension-manager-summary" id="extension-manager-summary">选择服务器后管理独立脚本版本。</p></div><div class="extension-manager-actions"><button class="button button-secondary" id="extension-reload-button" type="button">重新载入</button><button class="button button-primary" id="extension-create-button" type="button">创建扩展</button></div></header><div class="form-message extension-feedback" id="extension-feedback" role="status" aria-live="polite" hidden></div><div class="extension-loading" id="extension-loading" aria-live="polite" hidden><div class="content-skeleton" aria-label="正在读取扩展列表"><span></span><span></span><span></span></div><p>正在读取这台服务器的扩展...</p></div><div class="inline-error extension-error" id="extension-error" role="alert" hidden><div><h4>扩展列表加载失败</h4><p id="extension-error-message"></p></div><button class="button button-secondary" id="extension-retry-button" type="button">重试</button></div><div class="empty-state extension-empty" id="extension-empty" hidden><h4>还没有云端扩展</h4><p>创建第一份 Lua 或 JavaScript 脚本，选择最小权限；Paper 端会在下一次刷新时安全拉取。</p><button class="button button-primary" id="extension-empty-create-button" type="button">创建第一个扩展</button></div><div class="table-frame extension-table-frame" id="extension-table-frame" hidden><table class="data-table extension-table"><thead><tr><th scope="col">扩展</th><th scope="col">引擎 / 版本</th><th scope="col">依赖 / 权限</th><th scope="col">状态</th><th scope="col"><span class="sr-only">操作</span></th></tr></thead><tbody id="extension-list-body"></tbody></table></div></section></div></div></section>${renderExtensionDialogs()}<div class="toast-region" id="toast-region" aria-live="polite" aria-atomic="true"></div>`;
}

function renderExtensionDialogs() {
  return `<dialog class="item-dialog extension-dialog" id="extension-dialog" aria-labelledby="extension-dialog-title"><form class="extension-form" id="extension-form" novalidate><div class="dialog-heading extension-dialog-heading"><div><p class="toolbar-label" id="extension-dialog-mode">创建扩展</p><h2 id="extension-dialog-title">扩展 manifest 与源码</h2></div><button class="dialog-close" id="extension-dialog-close" type="button" aria-label="关闭扩展编辑器">关闭</button></div><div class="item-dialog-body extension-dialog-body"><div class="form-message form-message-error extension-form-error" id="extension-form-error" role="alert" hidden></div><div class="extension-form-grid"><div class="field"><label for="extension-id">扩展 ID</label><input id="extension-id" name="id" type="text" pattern="[a-z0-9]+(?:-[a-z0-9]+)*" maxlength="64" autocomplete="off" placeholder="例如：ore-monitor" required><p class="field-help">只允许小写字母、数字与中间连字符；创建后不可修改。</p></div><div class="field"><label for="extension-name">显示名称</label><input id="extension-name" name="name" type="text" maxlength="80" autocomplete="off" placeholder="例如：矿脉监控" required></div><div class="field"><label for="extension-version">版本</label><input id="extension-version" name="version" type="text" maxlength="48" autocomplete="off" placeholder="1.0.0" required></div><div class="field"><label for="extension-engine">脚本引擎</label><select id="extension-engine" name="engine" required><option value="lua">Lua · LuaJ</option><option value="javascript">JavaScript · Rhino</option></select></div><div class="field"><label for="extension-entry">入口文件</label><input id="extension-entry" name="entry" type="text" maxlength="128" autocomplete="off" placeholder="main.lua" required></div><div class="field extension-dependencies-field"><label for="extension-dependencies">依赖扩展</label><textarea id="extension-dependencies" name="dependencies" rows="3" autocomplete="off" placeholder="每行一个扩展 ID" aria-describedby="extension-dependencies-help"></textarea><p class="field-help" id="extension-dependencies-help">按行填写；启用时将先校验依赖拓扑。</p></div></div><fieldset class="extension-permissions" aria-describedby="extension-permissions-help"><legend>能力权限</legend><p class="field-help" id="extension-permissions-help">默认无能力。只勾选脚本运行必需的接口。</p><div class="extension-permission-grid"><label><input type="checkbox" name="permissions" value="log"><span><strong>log</strong><small>输出受控日志</small></span></label><label><input type="checkbox" name="permissions" value="schedule"><span><strong>schedule</strong><small>注册限时调度</small></span></label><label><input type="checkbox" name="permissions" value="events"><span><strong>events</strong><small>订阅只读事件 DTO</small></span></label><label><input type="checkbox" name="permissions" value="commands"><span><strong>commands</strong><small>注册扩展子命令</small></span></label><label><input type="checkbox" name="permissions" value="kv"><span><strong>kv</strong><small>访问扩展私有键值</small></span></label><label><input type="checkbox" name="permissions" value="catalog"><span><strong>catalog</strong><small>只读学科资料库</small></span></label></div></fieldset><div class="field extension-source-field"><label for="extension-source">源码</label><textarea id="extension-source" name="source" maxlength="131072" rows="18" wrap="off" spellcheck="false" autocapitalize="off" autocomplete="off" aria-describedby="extension-source-help" required></textarea><p class="field-help" id="extension-source-help"><span id="extension-source-count">0</span> / 131,072 字节；源码只作为文本提交，不在浏览器执行。</p></div><label class="extension-enabled-control" for="extension-enabled"><input id="extension-enabled" name="enabled" type="checkbox"><span>保存并启用</span></label></div><footer class="extension-dialog-actions"><button class="button button-secondary" id="extension-dialog-cancel" type="button">取消</button><button class="button button-primary" id="extension-save-button" type="submit">保存扩展</button></footer></form></dialog><dialog class="item-dialog extension-delete-dialog" id="extension-delete-dialog" aria-labelledby="extension-delete-title"><form class="extension-delete-form" id="extension-delete-form" novalidate><div class="dialog-heading extension-dialog-heading"><div><p class="toolbar-label">不可撤销操作</p><h2 id="extension-delete-title">删除云端扩展</h2></div><button class="dialog-close" id="extension-delete-close" type="button" aria-label="关闭删除确认">关闭</button></div><div class="item-dialog-body extension-delete-body"><p>删除会让 Paper 端在下一次同步时停用扩展，并按 LIFO 释放全部注册。此操作不能撤销。</p><p>请输入 <code id="extension-delete-id"></code> 以确认。</p><div class="field"><label for="extension-delete-confirmation">扩展 ID</label><input id="extension-delete-confirmation" name="confirmation" type="text" autocomplete="off" required></div><div class="form-message form-message-error extension-delete-error" id="extension-delete-error" role="alert" hidden></div></div><footer class="extension-dialog-actions"><button class="button button-secondary" id="extension-delete-cancel" type="button">保留扩展</button><button class="button button-primary extension-delete-button" id="extension-delete-button" type="submit" disabled>确认删除扩展</button></footer></form></dialog>`;
}

function renderConsolePage() {
  return {
    title: "服务器控制台 | TalexSoulTech",
    description: "登录 TalexSoulTech 控制台，管理自己的 Paper 服务器、一次性配对码、最近同步快照与受限云端扩展。",
    canonicalPath: "/console",
    kind: "console",
    scripts: ["/app.js"],
    private: true,
    noindex: true,
    body: renderConsoleBody(),
  };
}

function renderSetupPage() {
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "平台初始化" }],
    title: "平台管理员只通过 CLI 初始化",
    description: "网页不会提供管理员初始化 mutation。首位 admin 必须在受控终端中直接写入 D1；单例约束会阻止第二个 admin。",
    label: "安全引导 · 无公开写接口",
  })}<section class="section shell route-section setup-shell"><div class="route-two-column"><article class="route-prose"><h2>远端环境（默认）</h2><p>在仓库的 site 目录执行脚本。密码只能通过环境变量提供，命令不会打印明文。</p><pre class="command-block"><code>cd site
wrangler d1 migrations apply soultech --remote --config wrangler.jsonc
IFS= read -r -s SOULTECH_ADMIN_PASSWORD
echo
export SOULTECH_ADMIN_PASSWORD
node scripts/init-admin.mjs &lt;username&gt;
unset SOULTECH_ADMIN_PASSWORD</code></pre><h2>本地 D1</h2><p>只有明确初始化本地 Wrangler 数据库时才附加 <code>--local</code>。</p><pre class="command-block"><code>cd site
wrangler d1 migrations apply soultech --local --config wrangler.jsonc
IFS= read -r -s SOULTECH_ADMIN_PASSWORD
echo
export SOULTECH_ADMIN_PASSWORD
node scripts/init-admin.mjs &lt;username&gt; --local
unset SOULTECH_ADMIN_PASSWORD</code></pre></article><aside class="route-callout"><h2>执行前确认</h2><ul class="content-list"><li>用户名使用 3–32 个字母、数字、下划线或连字符。</li><li>密码只存在于当前进程环境，不写入命令参数、网页或日志。</li><li>远端是默认目标；不要把本地成功误认为生产已初始化。</li><li>已有 admin 时脚本必须失败关闭，不覆盖或降级现有管理员。</li></ul><a class="button button-secondary" href="/admin">打开管理员入口</a></aside></div></section>`;

  return {
    title: "平台管理员初始化 | TalexSoulTech",
    description: "TalexSoulTech 平台管理员仅通过受控 CLI 和环境变量初始化，不开放网页管理员创建接口。",
    canonicalPath: "/setup",
    kind: "setup",
    private: true,
    noindex: true,
    body,
  };
}

function renderAdminPage() {
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "平台管理" }],
    title: "平台管理总览",
    description: "本页只提供无数据 SSR 挂载点。管理员身份、平台统计和错误状态由同源 API 在浏览器中加载。",
    label: "仅 admin 可读",
  })}<section class="section shell route-section admin-shell" aria-labelledby="admin-title"><header class="console-toolbar"><div><p class="toolbar-label">平台范围</p><h2 id="admin-title">资源摘要</h2><p>统计用户、服务器、配对、快照与扩展规模，不返回 Cookie、密码、API Key 或扩展源码。</p></div><button class="button button-secondary" id="admin-refresh" type="button">刷新摘要</button></header><div class="console-loading" id="admin-loading" aria-live="polite"><div class="content-skeleton" aria-label="正在检查管理员权限"><span></span><span></span><span></span></div><p>正在检查管理员权限...</p></div><div class="inline-error" id="admin-error" role="alert" hidden></div><section class="admin-summary-grid" id="admin-summary" aria-live="polite" aria-busy="false" hidden></section><noscript><div class="noscript-message">平台摘要需要 JavaScript 调用同源管理员 API；SSR 不会嵌入任何私有数据。</div></noscript></section>`;

  return {
    title: "平台管理 | TalexSoulTech",
    description: "TalexSoulTech 平台管理员的资源摘要入口，私有统计仅通过同源管理员 API 加载。",
    canonicalPath: "/admin",
    kind: "admin",
    scripts: ["/admin.js"],
    private: true,
    noindex: true,
    body,
  };
}

function renderNotFoundPage(pathname) {
  const body = `${renderRouteHero({
    crumbs: [{ label: "首页", href: "/" }, { label: "页面不存在" }],
    title: "这条手册路径不存在",
    description: "请求的地址没有对应页面。可以返回索引，或从教程、学科与资料库重新定位内容。",
    label: "404 · 路由未登记",
  })}<section class="section shell route-section not-found-shell"><p>未找到：<code>${escapeHtml(pathname)}</code></p><div class="hero-actions"><a class="button button-primary" href="/">返回首页</a><a class="button button-secondary" href="/docs">浏览教程</a><a class="button button-secondary" href="/catalog">检索资料库</a></div></section>`;

  return {
    title: "页面不存在 | TalexSoulTech",
    description: "请求的 TalexSoulTech 页面不存在，请返回首页、教程或资料库。",
    canonicalPath: pathname,
    kind: "not-found",
    status: 404,
    noindex: true,
    body,
  };
}

function decodeRouteSegment(segment) {
  try {
    return decodeURIComponent(segment);
  } catch {
    return null;
  }
}

function normalizedPathname(pathname) {
  if (pathname === "/") return pathname;
  return pathname.replace(/\/+$/, "") || "/";
}

function navigationPath(pathname) {
  if (pathname === "/") return "/";
  if (pathname.startsWith("/docs")) return "/docs";
  if (pathname.startsWith("/disciplines")) return "/disciplines";
  if (pathname.startsWith("/items") || pathname.startsWith("/catalog")) return "/catalog";
  if (pathname.startsWith("/download")) return "/download";
  if (pathname.startsWith("/architecture")) return "/architecture";
  if (pathname.startsWith("/extensions")) return "/extensions";
  if (pathname.startsWith("/console")) return "/console";
  return "";
}

function renderHeader(pathname) {
  const activePath = navigationPath(pathname);
  return `<a class="skip-link" href="#main-content">跳到主要内容</a><header class="site-header" id="site-header"><div class="shell header-inner"><a class="brand-link" href="/" aria-label="返回 TalexSoulTech 首页"><img src="/favicon.svg" alt="" width="34" height="34"><span class="brand-wordmark">TalexSoulTech</span></a><button class="nav-toggle" id="nav-toggle" type="button" aria-expanded="false" aria-controls="primary-nav"><span class="sr-only">打开导航</span><span aria-hidden="true"></span><span aria-hidden="true"></span></button><nav class="primary-nav" id="primary-nav" aria-label="主导航">${NAVIGATION.map((item) => `<a href="${item.path}"${activePath === item.path ? ' aria-current="page"' : ""}>${item.label}</a>`).join("")}<a class="nav-download" href="${DOWNLOAD_URL}" download>下载 JAR</a></nav></div></header>`;
}

function renderFooter() {
  return `<footer class="site-footer"><div class="shell footer-layout"><div><a class="brand-link" href="/"><img src="/favicon.svg" alt="" width="30" height="30"><span class="brand-wordmark">TalexSoulTech</span></a><p>${escapeHtml(SITE_CONTENT.footer.statement)}</p></div><nav aria-label="页脚导航"><a href="/docs">教程</a><a href="/disciplines">学科</a><a href="/catalog">资料库</a><a href="/architecture">架构</a><a href="/extensions">扩展</a><a href="/console">控制台</a></nav><p class="footer-meta">${escapeHtml(SITE_CONTENT.footer.privacy)}</p></div></footer>`;
}

function renderDocument(page, url) {
  const canonicalUrl = new URL(url.origin);
  canonicalUrl.pathname = page.canonicalPath;
  const canonical = canonicalUrl.href;
  const scripts = ["/shell.js", ...(page.scripts || [])];
  const robots = page.noindex ? '<meta name="robots" content="noindex, nofollow">' : '<meta name="robots" content="index, follow">';
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#171313"><meta name="color-scheme" content="dark"><meta name="description" content="${escapeHtml(page.description)}">${robots}<link rel="canonical" href="${escapeHtml(canonical)}"><meta property="og:type" content="website"><meta property="og:locale" content="zh_CN"><meta property="og:site_name" content="TalexSoulTech"><meta property="og:title" content="${escapeHtml(page.title)}"><meta property="og:description" content="${escapeHtml(page.description)}"><meta property="og:url" content="${escapeHtml(canonical)}"><meta property="og:image" content="${escapeHtml(new URL("/assets/voxel-industrial-lab-hero.webp", url.origin).href)}"><title>${escapeHtml(page.title)}</title><link rel="icon" href="/favicon.svg" type="image/svg+xml"><link rel="stylesheet" href="/styles.css"><link rel="stylesheet" href="/routes.css">${scripts.map((source) => `<script type="module" src="${escapeHtml(source)}"></script>`).join("")}</head><body class="route-page route-${escapeHtml(page.kind)}">${renderHeader(page.canonicalPath)}<main id="main-content">${page.body}</main>${renderFooter()}</body></html>`;
}

function htmlResponse(request, page, url) {
  const body = renderDocument(page, url);
  const headers = new Headers({
    "cache-control": page.private
      ? "private, no-store, max-age=0"
      : "public, max-age=300, s-maxage=3600, stale-while-revalidate=86400",
    "content-language": "zh-CN",
    "content-type": "text/html; charset=UTF-8",
    "cross-origin-resource-policy": "same-origin",
    "permissions-policy": "camera=(), microphone=(), geolocation=()",
    "referrer-policy": "strict-origin-when-cross-origin",
    "x-content-type-options": "nosniff",
    "x-frame-options": "DENY",
  });
  return new Response(request.method === "HEAD" ? null : body, {
    status: page.status || 200,
    headers,
  });
}

function sitemapResponse(request, url) {
  const origin = url.origin.replace(/\/$/, "");
  const locations = PUBLIC_SITEMAP_PATHS.map((path) => `<url><loc>${escapeHtml(`${origin}${path}`)}</loc></url>`).join("");
  const body = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${locations}</urlset>\n`;
  return new Response(request.method === "HEAD" ? null : body, {
    headers: {
      "cache-control": "public, max-age=3600, s-maxage=86400",
      "content-type": "application/xml; charset=UTF-8",
      "x-content-type-options": "nosniff",
    },
  });
}

function robotsResponse(request, url) {
  const body = `User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /console\nDisallow: /admin\nDisallow: /setup\nSitemap: ${url.origin.replace(/\/$/, "")}/sitemap.xml\n`;
  return new Response(request.method === "HEAD" ? null : body, {
    headers: {
      "cache-control": "public, max-age=3600, s-maxage=86400",
      "content-type": "text/plain; charset=UTF-8",
      "x-content-type-options": "nosniff",
    },
  });
}

function isAssetPath(pathname) {
  if (["/app.js", "/admin.js", "/shell.js", "/styles.css", "/routes.css", "/favicon.svg"].includes(pathname)) return true;
  if (["/assets/", "/data/", "/downloads/"].some((prefix) => pathname.startsWith(prefix))) return true;
  const lastSegment = pathname.slice(pathname.lastIndexOf("/") + 1);
  return /\.[a-z0-9]+$/i.test(lastSegment);
}

function resolvePage(pathname, url) {
  if (pathname === "/") return renderHomePage();
  if (pathname === "/download") return renderDownloadPage();
  if (pathname === "/docs") return renderDocsIndexPage();
  if (pathname === "/catalog") return renderCatalogPage(url);
  if (pathname === "/disciplines") return renderDisciplinesPage();
  if (pathname === "/planning") return renderPlanningPage();
  if (pathname === "/architecture") return renderArchitecturePage();
  if (pathname === "/extensions") return renderExtensionsPage();
  if (pathname === "/console") return renderConsolePage();
  if (pathname === "/setup") return renderSetupPage();
  if (pathname === "/admin") return renderAdminPage();

  const tutorialMatch = /^\/docs\/([^/]+)$/.exec(pathname);
  if (tutorialMatch) {
    const tutorialId = decodeRouteSegment(tutorialMatch[1]);
    const tutorial = tutorialId === null ? null : TUTORIAL_BY_ID.get(tutorialId);
    return tutorial ? renderTutorialPage(tutorial) : renderNotFoundPage(pathname);
  }

  const disciplineMatch = /^\/disciplines\/([^/]+)$/.exec(pathname);
  if (disciplineMatch) {
    const disciplineId = decodeRouteSegment(disciplineMatch[1]);
    const discipline = disciplineId === null ? null : DISCIPLINE_BY_ID.get(disciplineId);
    return discipline ? renderDisciplinePage(discipline) : renderNotFoundPage(pathname);
  }

  const itemMatch = /^\/items\/([^/]+)$/.exec(pathname);
  if (itemMatch) {
    const itemId = decodeRouteSegment(itemMatch[1]);
    const item = itemId === null ? null : ITEM_BY_ID.get(itemId);
    return item ? renderItemPage(item) : renderNotFoundPage(pathname);
  }

  return null;
}

export async function renderSsrRequest(request, env, url) {
  void env;
  if (!request || !["GET", "HEAD"].includes(request.method)) return null;
  const requestUrl = url instanceof URL ? url : new URL(request.url);
  const pathname = normalizedPathname(requestUrl.pathname);

  if (pathname === "/sitemap.xml") return sitemapResponse(request, requestUrl);
  if (pathname === "/robots.txt") return robotsResponse(request, requestUrl);
  if (pathname.startsWith("/api/") || pathname === "/api") return null;

  const page = resolvePage(pathname, requestUrl);
  if (page) return htmlResponse(request, page, requestUrl);
  if (isAssetPath(pathname)) return null;
  return htmlResponse(request, renderNotFoundPage(pathname), requestUrl);
}
