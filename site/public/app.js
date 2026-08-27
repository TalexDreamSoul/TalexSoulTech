const DOWNLOAD_URL = "/downloads/TalexSoulTech-3.0.0-SNAPSHOT.jar";
const MANIFEST_URL = "/downloads/manifest.json";
const CATALOG_PAGE_SIZE = 24;

const STATUS_LABELS = Object.freeze({
  implemented: "已实装",
  planned: "规划中",
});

const NARRATIVE_LABELS = Object.freeze({ all: "全部故事", anchor: "故事锚点" });

// The data modules total ~330KB. Every page renders its content server-side, so each enhancement
// path pulls only the modules it actually reads and slim pages transfer none of them.
let DISCIPLINES = null;
let CATALOG_STATS = null;
let SITE_CONTENT = {};
let TECH_ARCHITECTURE = null;
let SAAS_ARCHITECTURE = null;
let TUTORIALS = null;
let CAMPAIGN_WAVE_LIST = Object.freeze([]);
let CAMPAIGN_WAVE_IDS = new Set();

let catalogDataPromise = null;
let contentDataPromise = null;

function toCampaignWaveList(waves) {
  return Object.freeze(
    (Array.isArray(waves) ? waves : Object.values(waves || {}))
      .filter((wave) => wave && wave.id)
      .map((wave) => Object.freeze({
        id: String(wave.id),
        title: String(wave.title || wave.name || wave.id),
        disciplineIds: Array.isArray(wave.disciplineIds) ? wave.disciplineIds.map(String) : [],
      })),
  );
}

function loadCatalogData() {
  if (!catalogDataPromise) {
    catalogDataPromise = Promise.all([
      import("./data/catalog.js"),
      import("./data/progression.js"),
    ]).then(([catalog, progression]) => {
      DISCIPLINES = catalog.DISCIPLINES;
      CATALOG_STATS = catalog.CATALOG_STATS;
      CAMPAIGN_WAVE_LIST = toCampaignWaveList(progression.CAMPAIGN_WAVES);
      CAMPAIGN_WAVE_IDS = new Set(CAMPAIGN_WAVE_LIST.map((wave) => wave.id));
    });
  }
  return catalogDataPromise;
}

function loadContentData() {
  if (!contentDataPromise) {
    contentDataPromise = import("./data/content.js").then((content) => {
      SITE_CONTENT = content.SITE_CONTENT;
      TECH_ARCHITECTURE = content.TECH_ARCHITECTURE;
      SAAS_ARCHITECTURE = content.SAAS_ARCHITECTURE;
      TUTORIALS = content.TUTORIALS;
    });
  }
  return contentDataPromise;
}

function hasNarrativeStory(item) {
  return item?.story !== null && item?.story !== undefined;
}

function waveControlOptions() {
  return [{ id: "all", title: "全部波次" }, ...CAMPAIGN_WAVE_LIST];
}

function waveForDiscipline(disciplineId) {
  return CAMPAIGN_WAVE_LIST.find((wave) => wave.disciplineIds.includes(String(disciplineId)))?.id || "";
}

function waveById(waveId) {
  return CAMPAIGN_WAVE_LIST.find((wave) => wave.id === waveId) || null;
}

function catalogField(id, name, fallbackName = name) {
  return byId(id)
    || byId("catalog-controls")?.querySelector(`[name="${name}"]`)
    || (fallbackName ? byId("catalog-controls")?.querySelector(`[name="${fallbackName}"]`) : null);
}

const KEY_LABELS = Object.freeze({
  implemented: "当前已实现",
  boundaries: "明确边界",
  matrix: "兼容项目",
  steps: "操作步骤",
  currentTables: "当前数据表",
  configuration: "配置项",
  safeguards: "运行保障",
  useFlow: "玩家路径",
  currentTypes: "当前机器",
  operatingRules: "运行规则",
  components: "组成",
  settlement: "结算流程",
  playerGuidance: "玩家排查",
  stages: "循环阶段",
  designRule: "设计约束",
  phases: "进程阶段",
  accounting: "容量口径",
  curriculum: "学科版图",
  principles: "策划原则",
  economy: "经济原则",
  balance: "平衡原则",
  recovery: "失败恢复",
  layers: "职责分层",
  power: "电网",
  ui: "库存界面",
  events: "事件组合",
  lifecycle: "生命周期",
  domain: "领域模型",
  flow: "执行流程",
  invariants: "不变量",
  composition: "组合关系",
  scope: "一期范围",
  nonClaims: "不作承诺",
  api: "API 边界",
  tenantIsolation: "租户隔离",
  pairing: "配对",
  sync: "快照同步",
  extensions: "扩展运行时",
  roadmap: "后续路线",
  context: "扩展 Context",
  dependencyTopology: "依赖拓扑",
  disposal: "资源释放",
  hotUpdate: "原子热更新",
  sandboxes: "沙箱",
  capabilities: "能力权限",
  controlPlane: "控制面",
  audit: "审计",
  guarantees: "保证",
  rules: "规则",
  lifecycleOrder: "生命周期顺序",
  actions: "控制动作",
  eventsList: "审计事件",
  items: "项目",
  sequence: "同步序列",
  sentAt: "插件发送时间",
  receivedAt: "云端接收时间",
  serverId: "服务器 ID",
  server: "服务器",
  players: "玩家",
  systems: "系统",
  catalog: "目录",
  name: "名称",
  paperVersion: "Paper 版本",
  pluginVersion: "插件版本",
  onlineCount: "在线人数",
  maxPlayers: "人数上限",
  names: "在线玩家",
  worldCount: "世界数量",
  machines: "机器统计",
  disciplines: "学科数量",
  itemCount: "物品数量",
  routes: "路由",
  current: "当前状态",
  status: "状态",
  state: "状态",
  requirement: "要求",
  purpose: "用途",
  role: "作用",
  meaning: "说明",
  focus: "重点",
  playerGoal: "玩家目标",
  outcome: "结果",
  detail: "说明",
  clarification: "口径说明",
  playerPromise: "玩家承诺",
  integrityRule: "完整性规则",
  operatorMeaning: "运维含义",
});

const TITLE_KEYS = Object.freeze([
  "title",
  "name",
  "target",
  "phase",
  "boundary",
  "group",
  "source",
  "table",
  "key",
  "area",
  "runtime",
  "action",
  "stage",
  "label",
]);

const PROSE_KEYS = new Set([
  "lead",
  "description",
  "detail",
  "summary",
  "statement",
  "privacy",
  "clarification",
  "playerPromise",
  "designRule",
  "boundary",
  "integrityRule",
  "operatorMeaning",
]);

const byId = (id) => document.getElementById(id);

const state = {
  catalog: {
    allItems: [],
    query: "",
    wave: "all",
    discipline: "all",
    narrative: "all",
    status: "all",
    page: 1,
  },
  auth: {
    status: "loading",
    user: null,
    generation: 0,
    controller: new AbortController(),
  },
  servers: [],
  serversStatus: "idle",
  serversRequest: 0,
  selectedServerId: null,
  selectedServer: null,
  pairing: null,
  snapshot: null,
  detailRequest: 0,
  snapshotRequest: 0,
  extensions: {
    items: [],
    status: "idle",
    error: null,
    request: 0,
    editing: null,
    deleting: null,
    mutation: false,
  },
};

class ApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

function normalizeCopy(value) {
  return String(value ?? "")
    .replace(/[—–]/g, "-")
    .replace(/\s*·\s*/g, " / ")
    .trim();
}

function createElement(tagName, options = {}) {
  const node = document.createElement(tagName);
  if (options.className) node.className = options.className;
  if (options.text !== undefined) node.textContent = normalizeCopy(options.text);
  if (options.attributes) {
    for (const [name, value] of Object.entries(options.attributes)) {
      if (value !== undefined && value !== null) node.setAttribute(name, String(value));
    }
  }
  return node;
}

function setText(nodeOrId, value, fallback = "") {
  const node = typeof nodeOrId === "string" ? byId(nodeOrId) : nodeOrId;
  if (!node) return;
  node.textContent = normalizeCopy(value || fallback);
}

function formatScalar(value) {
  if (value === null || value === undefined || value === "") return "未提供";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number") return new Intl.NumberFormat("zh-CN").format(value);
  return normalizeCopy(value);
}

function humanizeKey(key) {
  if (KEY_LABELS[key]) return KEY_LABELS[key];
  return normalizeCopy(key.replace(/([a-z0-9])([A-Z])/g, "$1 $2"));
}

function objectTitle(record) {
  for (const key of TITLE_KEYS) {
    const value = record?.[key];
    if (typeof value === "string" || typeof value === "number") {
      return { key, value: formatScalar(value) };
    }
  }
  return null;
}

function renderDefinitionRows(rows) {
  const list = createElement("dl", { className: "definition-grid" });
  for (const [key, value] of rows) {
    const wrapper = createElement("div");
    wrapper.append(
      createElement("dt", { text: humanizeKey(key) }),
      createElement("dd", { text: formatScalar(value) }),
    );
    list.append(wrapper);
  }
  return list;
}

function renderScalarList(values, ordered = false) {
  const list = createElement(ordered ? "ol" : "ul", {
    className: ordered ? "ordered-content-list" : "content-list",
  });
  for (const value of values) {
    list.append(createElement("li", { text: formatScalar(value) }));
  }
  return list;
}

function renderObjectEntry(record) {
  const article = createElement("article", { className: "content-group" });
  const title = objectTitle(record);
  if (title) article.append(createElement("h4", { text: title.value }));

  const scalarRows = [];
  for (const [key, value] of Object.entries(record)) {
    if (title?.key === key || value === null || value === undefined) continue;

    if (PROSE_KEYS.has(key) && ["string", "number", "boolean"].includes(typeof value)) {
      article.append(createElement("p", { text: formatScalar(value) }));
      continue;
    }

    if (["string", "number", "boolean"].includes(typeof value)) {
      scalarRows.push([key, value]);
      continue;
    }

    const group = createElement("div", { className: "content-group" });
    group.append(createElement("h4", { text: humanizeKey(key) }));
    group.append(renderDataValue(value, { ordered: key === "steps" || key === "flow" }));
    article.append(group);
  }

  if (scalarRows.length) article.append(renderDefinitionRows(scalarRows));
  return article;
}

function renderDataValue(value, options = {}) {
  if (value === null || value === undefined) {
    return createElement("p", { className: "state-copy", text: "暂无内容" });
  }

  if (["string", "number", "boolean"].includes(typeof value)) {
    return createElement("p", { text: formatScalar(value) });
  }

  if (Array.isArray(value)) {
    if (!value.length) return createElement("p", { className: "state-copy", text: "暂无内容" });
    if (value.every((entry) => ["string", "number", "boolean"].includes(typeof entry))) {
      return renderScalarList(value, options.ordered);
    }

    const stack = createElement("div", { className: "content-stack" });
    for (const entry of value) {
      if (entry && typeof entry === "object") stack.append(renderObjectEntry(entry));
      else stack.append(createElement("p", { text: formatScalar(entry) }));
    }
    return stack;
  }

  const stack = createElement("div", { className: "content-stack" });
  const scalarRows = [];
  for (const [key, nested] of Object.entries(value)) {
    if (nested === null || nested === undefined) continue;
    if (key === "title") continue;

    if (PROSE_KEYS.has(key) && ["string", "number", "boolean"].includes(typeof nested)) {
      stack.append(createElement("p", { text: formatScalar(nested) }));
      continue;
    }

    if (["string", "number", "boolean"].includes(typeof nested)) {
      scalarRows.push([key, nested]);
      continue;
    }

    const group = createElement("section", { className: "content-group" });
    group.append(createElement("h4", { text: humanizeKey(key) }));
    group.append(renderDataValue(nested, { ordered: key === "steps" || key === "flow" }));
    stack.append(group);
  }
  if (scalarRows.length) stack.prepend(renderDefinitionRows(scalarRows));
  return stack;
}

function renderInto(nodeOrId, value, options = {}) {
  const node = typeof nodeOrId === "string" ? byId(nodeOrId) : nodeOrId;
  if (!node) return;
  node.replaceChildren(renderDataValue(value, options));
}

function setInlineMessage(node, message, retryHandler) {
  if (!node) return;
  node.replaceChildren(createElement("span", { text: message }));
  if (retryHandler) {
    const button = createElement("button", {
      className: "text-button",
      text: "重试",
      attributes: { type: "button" },
    });
    button.addEventListener("click", retryHandler, { once: true });
    node.append(button);
  }
  node.hidden = false;
}

function hideMessage(node) {
  if (!node) return;
  node.hidden = true;
  node.replaceChildren();
}

function showToast(message) {
  const region = byId("toast-region");
  if (!region) return;
  const toast = createElement("div", { className: "toast", text: message });
  region.append(toast);
  window.setTimeout(() => toast.remove(), 4200);
}

function formatDate(value, fallback = "尚未同步") {
  if (!value) return fallback;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return normalizeCopy(value);
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatBytes(value) {
  const bytes = Number(value);
  if (!Number.isFinite(bytes) || bytes < 0) return null;
  const units = ["B", "KiB", "MiB", "GiB"];
  let amount = bytes;
  let unit = units[0];
  for (let index = 1; index < units.length && amount >= 1024; index += 1) {
    amount /= 1024;
    unit = units[index];
  }
  return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: amount >= 10 ? 1 : 2 }).format(amount)} ${unit}`;
}

function errorMessage(error, fallback) {
  if (error instanceof ApiError && error.message) return normalizeCopy(error.message);
  if (error instanceof Error && error.message) return normalizeCopy(error.message);
  return fallback;
}

async function requestApi(path, options = {}) {
  const headers = new Headers({ Accept: "application/json" });
  const hasBody = options.body !== undefined;
  if (hasBody || options.json) headers.set("Content-Type", "application/json");

  const response = await fetch(path, {
    method: options.method || "GET",
    credentials: "same-origin",
    headers,
    body: hasBody ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    throw new ApiError("服务器返回了无法识别的响应。", response.status, "invalid_json");
  }

  if (!response.ok) {
    const apiError = payload?.error;
    throw new ApiError(
      apiError?.message || "请求未完成，请稍后重试。",
      response.status,
      apiError?.code || "request_failed",
    );
  }

  return payload;
}

function setButtonBusy(button, busy, busyLabel = "处理中...") {
  if (!button) return;
  if (busy) {
    if (!button.dataset.idleLabel) button.dataset.idleLabel = button.textContent;
    button.textContent = busyLabel;
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
  } else {
    button.textContent = button.dataset.idleLabel || button.textContent;
    button.disabled = false;
    button.removeAttribute("aria-busy");
  }
}

function renderSharedContent() {
  const { brand = {}, hero = {}, overview = {}, compatibility = {}, download = {}, quickInstall = {}, mysql = {}, footer = {} } = SITE_CONTENT;

  setText("hero-kicker", hero.eyebrow);
  setText("hero-title", hero.title, brand.name);
  setText("hero-summary", hero.description, brand.tagline);
  setText("hero-caption", brand.visualStatement);
  setText("plugin-title", overview.title, "插件与安装");
  setText("plugin-intro", overview.lead);
  setText("download-title", download.title || download.artifact, "TalexSoulTech JAR");
  setText("download-description", download.detail);
  setText("quick-install-title", quickInstall.title, "快速安装");
  setText("mysql-title", mysql.title, "MySQL 与持久化");
  setText("footer-summary", footer.statement, brand.tagline);
  setText("footer-meta", footer.privacy);

  const overviewBody = {
    implemented: overview.implemented,
    boundaries: overview.boundaries,
  };
  renderInto("overview-content", overviewBody);
  renderInto("compatibility-content", {
    lead: compatibility.lead,
    matrix: compatibility.matrix,
  });
  renderInto("quick-install-content", {
    lead: quickInstall.lead,
    steps: quickInstall.steps,
  });
  renderInto("mysql-content", {
    lead: mysql.lead,
    currentTables: mysql.currentTables,
    configuration: mysql.configuration,
    safeguards: mysql.safeguards,
  });

  const stats = CATALOG_STATS || {};
  setText("fact-disciplines", Number.isFinite(stats.disciplineCount) ? `${stats.disciplineCount} 门` : "资料已载入");
  setText("fact-items", Number.isFinite(stats.itemCount) ? `${stats.itemCount} 件` : "资料已载入");

  const matrix = Array.isArray(compatibility.matrix) ? compatibility.matrix : [];
  const java = matrix.find((entry) => normalizeCopy(entry.target).includes("Java"))?.requirement;
  const paper = matrix.find((entry) => normalizeCopy(entry.target).includes("Paper"))?.requirement;
  setText("fact-runtime", [java, paper].filter(Boolean).join(" / "), "查看兼容矩阵");
  setText("fact-sync", SAAS_ARCHITECTURE?.sync?.title, "按服务器顺序同步");

  const brandName = normalizeCopy(brand.name || "TalexSoulTech");
  if (byId("hero-title")) document.title = `${brandName} | 灵魂科技工业手册`;

  const navTargets = {
    overview: "plugin",
    play: "planning",
    install: "tutorials",
    catalog: "catalog",
    architecture: "architecture",
    console: "console",
  };
  const navItems = Array.isArray(SITE_CONTENT.nav) ? SITE_CONTENT.nav : [];
  for (const item of navItems) {
    const target = navTargets[item.id];
    if (!target) continue;
    const link = document.querySelector(`#primary-nav a[href="#${target}"]`);
    if (link) setText(link, item.label);
  }
}

async function loadArtifactManifest() {
  const meta = byId("artifact-meta");
  if (!meta) return;

  await loadContentData().catch(() => {});

  try {
    const response = await fetch(MANIFEST_URL, {
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) throw new Error("构件清单暂不可用");
    const payload = await response.json();
    const artifact = payload.artifact && typeof payload.artifact === "object" ? payload.artifact : payload;
    const rows = [];
    const fileName = artifact.fileName || artifact.filename || artifact.name || SITE_CONTENT.download?.artifact;
    const size = formatBytes(artifact.size ?? artifact.bytes ?? artifact.sizeBytes);
    const sha256 = artifact.sha256 || artifact.sha256Hex || artifact.digest;
    if (fileName) rows.push(["文件", fileName]);
    if (size) rows.push(["体积", size]);
    if (sha256) rows.push(["SHA-256", sha256]);

    meta.replaceChildren(
      rows.length
        ? renderDefinitionRows(rows)
        : createElement("p", { className: "state-copy", text: "构件已准备，可直接下载。" }),
    );
  } catch {
    meta.replaceChildren(
      createElement("p", {
        className: "state-copy",
        text: "构件清单暂不可用，下载入口仍指向本站受控产物。",
      }),
    );
  }

  for (const link of document.querySelectorAll(`a[href="${DOWNLOAD_URL}"]`)) {
    link.setAttribute("download", "");
  }
}

function initTutorials() {
  const index = byId("tutorial-index");
  const reader = byId("tutorial-reader");
  if (!index || !reader) return;

  index.replaceChildren();
  if (!Array.isArray(TUTORIALS) || !TUTORIALS.length) {
    index.replaceChildren();
    reader.replaceChildren(
      createElement("div", { className: "inline-error", text: "教程数据暂不可用。" }),
    );
    return;
  }

  const tabs = TUTORIALS.map((tutorial, position) => {
    const button = createElement("button", {
      attributes: {
        type: "button",
        role: "tab",
        id: `tutorial-tab-${tutorial.id || position}`,
        "aria-controls": "tutorial-reader",
        "aria-selected": position === 0 ? "true" : "false",
        tabindex: position === 0 ? "0" : "-1",
      },
    });
    button.dataset.tutorialIndex = String(position);
    button.append(
      createElement("span", { text: tutorial.label || `章节 ${position + 1}` }),
      createElement("span", { text: tutorial.title }),
    );
    index.append(button);
    return button;
  });

  const selectTutorial = (position, focus = false) => {
    const tutorial = TUTORIALS[position];
    if (!tutorial) return;
    tabs.forEach((tab, tabIndex) => {
      const selected = tabIndex === position;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
    });

    const fragment = document.createDocumentFragment();
    fragment.append(createElement("h3", { text: tutorial.title }));
    if (tutorial.summary) fragment.append(createElement("p", { className: "tutorial-summary", text: tutorial.summary }));

    if (Array.isArray(tutorial.steps) && tutorial.steps.length) {
      fragment.append(createElement("h4", { text: "操作步骤" }));
      const steps = createElement("ol", { className: "tutorial-steps" });
      for (const step of tutorial.steps) {
        const item = createElement("li");
        if (step && typeof step === "object") item.append(renderObjectEntry(step));
        else item.append(createElement("span", { text: formatScalar(step) }));
        steps.append(item);
      }
      fragment.append(steps);
    }

    if (tutorial.notes) {
      const note = createElement("aside", { className: "tutorial-note" });
      note.append(createElement("h4", { text: "操作提示" }), renderDataValue(tutorial.notes));
      fragment.append(note);
    }

    if (tutorial.diagnosis) {
      const diagnosis = createElement("aside", { className: "tutorial-diagnosis" });
      diagnosis.append(createElement("h4", { text: "故障诊断" }), renderDataValue(tutorial.diagnosis));
      fragment.append(diagnosis);
    }

    reader.replaceChildren(fragment);
    reader.setAttribute("aria-labelledby", tabs[position].id);
    if (focus) tabs[position].focus();
  };

  index.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-tutorial-index]");
    if (button) selectTutorial(Number(button.dataset.tutorialIndex));
  });

  index.addEventListener("keydown", (event) => {
    const active = event.target.closest("button[data-tutorial-index]");
    if (!active) return;
    const current = Number(active.dataset.tutorialIndex);
    let next = current;
    if (event.key === "ArrowRight" || event.key === "ArrowDown") next = (current + 1) % tabs.length;
    else if (event.key === "ArrowLeft" || event.key === "ArrowUp") next = (current - 1 + tabs.length) % tabs.length;
    else if (event.key === "Home") next = 0;
    else if (event.key === "End") next = tabs.length - 1;
    else return;
    event.preventDefault();
    selectTutorial(next, true);
  });

  selectTutorial(0);
}

function renderPlanning() {
  const container = byId("planning-content");
  if (!container) return;

  const sections = [
    SITE_CONTENT.guidebook,
    SITE_CONTENT.machines,
    SITE_CONTENT.power,
    SITE_CONTENT.coreLoop,
    SITE_CONTENT.progression,
    SITE_CONTENT.planning,
    SITE_CONTENT.economy,
  ].filter(Boolean);

  if (!sections.length) {
    container.replaceChildren(createElement("div", { className: "inline-error", text: "游戏策划数据暂不可用。" }));
    return;
  }

  const fragment = document.createDocumentFragment();
  for (const section of sections) {
    const article = createElement("article", { className: "planning-block" });
    article.append(createElement("h3", { text: section.title }));
    article.append(renderDataValue(section));
    fragment.append(article);
  }
  container.replaceChildren(fragment);
}

function renderArchitecturePanel(node, data) {
  if (!node) return;

  const fragment = document.createDocumentFragment();
  if (data?.lead) fragment.append(createElement("p", { className: "hero-summary", text: data.lead }));

  const map = createElement("div", { className: "architecture-map" });
  for (const [key, value] of Object.entries(data || {})) {
    if (key === "title" || key === "lead" || value === null || value === undefined) continue;
    const group = createElement("section", { className: "architecture-group" });
    const title = value && typeof value === "object" && !Array.isArray(value) && value.title
      ? value.title
      : humanizeKey(key);
    const content = createElement("div", { className: "architecture-content" });
    content.append(renderDataValue(value));
    group.append(createElement("h3", { text: title }), content);
    map.append(group);
  }
  fragment.append(map);
  node.replaceChildren(fragment);
}

function initArchitecture() {
  const pluginPanel = byId("architecture-plugin");
  const saasPanel = byId("architecture-saas");
  renderArchitecturePanel(pluginPanel, TECH_ARCHITECTURE);
  renderArchitecturePanel(saasPanel, SAAS_ARCHITECTURE);

  const tabs = [byId("architecture-plugin-tab"), byId("architecture-saas-tab")];
  const panels = [pluginPanel, saasPanel];
  if (tabs.some((tab) => !tab) || panels.some((panel) => !panel)) return;

  const selectTab = (index, focus = false) => {
    tabs.forEach((tab, tabIndex) => {
      const selected = index === tabIndex;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      panels[tabIndex].hidden = !selected;
    });
    if (focus) tabs[index].focus();
  };

  tabs.forEach((tab, index) => tab.addEventListener("click", () => selectTab(index)));
  tabs.forEach((tab, index) => tab.addEventListener("keydown", (event) => {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    let next = index;
    if (event.key === "ArrowLeft") next = (index - 1 + tabs.length) % tabs.length;
    if (event.key === "ArrowRight") next = (index + 1) % tabs.length;
    if (event.key === "Home") next = 0;
    if (event.key === "End") next = tabs.length - 1;
    selectTab(next, true);
  }));
  selectTab(0);
}

function initializeCatalogData() {
  if (!Array.isArray(DISCIPLINES)) throw new Error("学科模块没有提供可读取的 DISCIPLINES 数组。");
  const flattened = [];
  const ids = new Set();

  for (const discipline of DISCIPLINES) {
    const disciplineId = normalizeCopy(discipline.id);
    const disciplineWaveId = normalizeCopy(discipline.waveId || waveForDiscipline(disciplineId));
    const disciplineWave = waveById(disciplineWaveId);
    const items = Array.isArray(discipline.items) ? discipline.items : [];
    for (const item of items) {
      const id = normalizeCopy(item.id);
      if (!id || ids.has(id)) throw new Error("资料库包含缺失或重复的物品 ID。");
      ids.add(id);
      const waveId = normalizeCopy(item.waveId || disciplineWaveId);
      const story = item.story && typeof item.story === "object" ? item.story : null;
      const enriched = {
        ...item,
        id,
        disciplineId: normalizeCopy(item.disciplineId || disciplineId),
        disciplineName: discipline.name,
        disciplineStage: discipline.stage,
        waveId,
        waveTitle: waveById(waveId)?.title || disciplineWave?.title || "",
        discipline,
      };
      enriched.searchText = [
        enriched.id,
        enriched.name,
        enriched.type,
        enriched.purpose,
        enriched.family,
        enriched.recipeHint,
        enriched.disciplineName,
        enriched.disciplineStage,
        enriched.waveId,
        enriched.waveTitle,
        story?.text,
        story?.anchorReason,
      ].map(normalizeCopy).join(" ").toLocaleLowerCase("zh-CN");
      flattened.push(enriched);
    }
  }

  state.catalog.allItems = flattened;
}

function renderCatalogStats() {
  const container = byId("catalog-stats");
  if (!container) return;

  const computedImplemented = state.catalog.allItems.filter((item) => item.status === "implemented").length;
  const rows = [
    ["学科总数", CATALOG_STATS?.disciplineCount ?? DISCIPLINES.length],
    ["物品总数", CATALOG_STATS?.itemCount ?? state.catalog.allItems.length],
    ["已实装", CATALOG_STATS?.implementedCount ?? computedImplemented],
    ["规划中", CATALOG_STATS?.plannedCount ?? state.catalog.allItems.length - computedImplemented],
  ];
  const fragment = document.createDocumentFragment();
  for (const [label, value] of rows) {
    const entry = createElement("div", { className: "catalog-stat" });
    entry.append(createElement("dt", { text: label }), createElement("dd", { text: formatScalar(value) }));
    fragment.append(entry);
  }
  container.replaceChildren(fragment);
}

function catalogHref(overrides = {}) {
  const query = {
    q: state.catalog.query,
    wave: state.catalog.wave,
    discipline: state.catalog.discipline,
    narrative: state.catalog.narrative,
    status: state.catalog.status,
    page: state.catalog.page,
    ...overrides,
  };
  const normalizedQuery = normalizeCopy(query.q);
  const params = new URLSearchParams();
  if (normalizedQuery) params.set("q", normalizedQuery);
  if (CAMPAIGN_WAVE_IDS.has(String(query.wave))) params.set("wave", String(query.wave));
  if (query.discipline && query.discipline !== "all") params.set("discipline", query.discipline);
  if (query.narrative === "anchor") params.set("narrative", "anchor");
  if (query.status && query.status !== "all") params.set("status", query.status);
  if (query.page > 1) params.set("page", String(query.page));
  const serialized = params.toString();
  return serialized ? `/catalog?${serialized}` : "/catalog";
}

function renderCatalogSelect(select, choices) {
  if (!select) return;
  const fragment = document.createDocumentFragment();
  for (const choice of choices) {
    fragment.append(createElement("option", {
      text: choice.title || choice.name,
      attributes: { value: choice.id },
    }));
  }
  select.replaceChildren(fragment);
}
function renderDisciplineControls() {
  const disciplineChoices = [{ id: "all", name: "全部学科" }, ...DISCIPLINES];
  renderCatalogSelect(catalogField("discipline-filter", "discipline"), disciplineChoices);

  const waveChoices = waveControlOptions();
  renderCatalogSelect(catalogField("wave-filter", "wave"), waveChoices);

  const narrativeChoices = Object.entries(NARRATIVE_LABELS).map(([id, title]) => ({ id, title }));
  renderCatalogSelect(catalogField("narrative-filter", "narrative"), narrativeChoices);

  const disciplineStrip = byId("discipline-strip");
  if (disciplineStrip) {
    const stripFragment = document.createDocumentFragment();
    for (const discipline of disciplineChoices) {
      const id = String(discipline.id);
      const link = createElement("a", {
        className: "discipline-chip",
        text: discipline.name,
        attributes: {
          href: catalogHref({ discipline: id, page: 1 }),
          "aria-pressed": id === state.catalog.discipline ? "true" : "false",
        },
      });
      link.dataset.discipline = id;
      stripFragment.append(link);
    }
    disciplineStrip.replaceChildren(stripFragment);
  }

  const waveStrip = byId("wave-strip");
  if (waveStrip) {
    const stripFragment = document.createDocumentFragment();
    for (const wave of waveChoices) {
      const id = String(wave.id);
      const link = createElement("a", {
        className: "wave-chip",
        text: id === "all" ? wave.title : `${id} · ${wave.title}`,
        attributes: {
          href: catalogHref({ wave: id, page: 1 }),
          "aria-pressed": id === state.catalog.wave ? "true" : "false",
        },
      });
      link.dataset.wave = id;
      stripFragment.append(link);
    }
    waveStrip.replaceChildren(stripFragment);
  }
}


function filteredCatalogItems() {
  const query = normalizeCopy(state.catalog.query).toLocaleLowerCase("zh-CN");
  return state.catalog.allItems.filter((item) => {
    if (state.catalog.wave !== "all" && item.waveId !== state.catalog.wave) return false;
    if (state.catalog.discipline !== "all" && item.disciplineId !== state.catalog.discipline) return false;
    if (state.catalog.narrative === "anchor" && !hasNarrativeStory(item)) return false;
    if (state.catalog.status !== "all" && item.status !== state.catalog.status) return false;
    return !query || item.searchText.includes(query);
  });
}

function initializeCatalogStateFromUrl() {
  const params = new URL(window.location.href).searchParams;
  const disciplineIds = new Set(DISCIPLINES.map((discipline) => String(discipline.id)));
  const requestedDiscipline = normalizeCopy(params.get("discipline") || "all");
  const requestedWave = normalizeCopy(params.get("wave") || "all");
  const requestedNarrative = normalizeCopy(params.get("narrative") || "all");
  const requestedStatus = normalizeCopy(params.get("status") || "all");
  const rawPage = normalizeCopy(params.get("page"));
  const parsedPage = rawPage && /^\d+$/.test(rawPage) ? Number(rawPage) : 1;
  const queryValue = params.has("q") ? params.get("q") : params.get("query");

  state.catalog.query = normalizeCopy(queryValue);
  state.catalog.wave = CAMPAIGN_WAVE_IDS.has(requestedWave) ? requestedWave : "all";
  state.catalog.discipline = disciplineIds.has(requestedDiscipline) ? requestedDiscipline : "all";
  state.catalog.narrative = requestedNarrative === "anchor" ? "anchor" : "all";
  state.catalog.status = Object.prototype.hasOwnProperty.call(STATUS_LABELS, requestedStatus)
    ? requestedStatus
    : "all";
  state.catalog.page = parsedPage > 0 ? parsedPage : 1;
}

function setCatalogControlValues() {
  const search = catalogField("catalog-search", "q", "query");
  const wave = catalogField("wave-filter", "wave");
  const discipline = catalogField("discipline-filter", "discipline");
  const narrative = catalogField("narrative-filter", "narrative");
  const status = catalogField("status-filter", "status");
  const page = byId("catalog-controls")?.querySelector('[name="page"]');
  if (search) search.value = state.catalog.query;
  if (wave) wave.value = state.catalog.wave;
  if (discipline) discipline.value = state.catalog.discipline;
  if (narrative) narrative.value = state.catalog.narrative;
  if (status) status.value = state.catalog.status;
  if (page) page.value = String(state.catalog.page);
}

function replaceCatalogUrl() {
  if (!window.history?.replaceState) return;
  window.history.replaceState(
    window.history.state,
    "",
    `${catalogHref()}${window.location.hash}`,
  );
}

function configureCatalogPageLink(link, page, enabled, rel) {
  if (!link) return;
  if (link.tagName === "A") {
    link.classList.toggle("is-disabled", !enabled);
    if (enabled) {
      link.href = catalogHref({ page });
      link.rel = rel;
      link.removeAttribute("aria-disabled");
      link.removeAttribute("tabindex");
    } else {
      link.removeAttribute("href");
      link.removeAttribute("rel");
      link.setAttribute("aria-disabled", "true");
      link.setAttribute("tabindex", "-1");
    }
    return;
  }
  link.disabled = !enabled;
}

function renderCatalog({ updateUrl = false, preserveSearchInput = false } = {}) {
  const items = filteredCatalogItems();
  const totalPages = Math.max(1, Math.ceil(items.length / CATALOG_PAGE_SIZE));
  state.catalog.page = Math.max(1, Math.min(state.catalog.page, totalPages));
  const start = (state.catalog.page - 1) * CATALOG_PAGE_SIZE;
  const pageItems = items.slice(start, start + CATALOG_PAGE_SIZE);
  if (!preserveSearchInput) setCatalogControlValues();

  setText(
    "catalog-result-count",
    state.catalog.query
      ? `“${state.catalog.query}”找到 ${items.length} 个条目`
      : `找到 ${items.length} 个条目`,
  );
  setText("catalog-page-status", items.length ? `第 ${state.catalog.page} / ${totalPages} 页` : "无结果");

  const body = byId("catalog-body");
  const frame = byId("catalog-table-frame");
  const empty = byId("catalog-empty");
  if (frame) frame.hidden = !pageItems.length;
  if (empty) empty.hidden = Boolean(pageItems.length);

  if (body) {
    const fragment = document.createDocumentFragment();
    for (const item of pageItems) {
      const row = createElement("tr");
      const itemHref = `/items/${encodeURIComponent(item.id)}`;

      const nameCell = createElement("th", { attributes: { "data-label": "物品", scope: "row" } });
      nameCell.append(
        createElement("a", {
          className: "cell-title",
          text: item.name,
          attributes: { href: itemHref },
        }),
        createElement("span", { className: "cell-code", text: item.id }),
      );

      const disciplineCell = createElement("td", { attributes: { "data-label": "学科" } });
      disciplineCell.append(createElement("a", {
        text: item.disciplineName,
        attributes: { href: `/disciplines/${encodeURIComponent(item.disciplineId)}` },
      }));
      const waveCell = createElement("td", { attributes: { "data-label": "波次" } });
      waveCell.append(createElement("a", {
        text: item.waveId,
        attributes: { href: `/planning#wave-${encodeURIComponent(item.waveId || "")}` },
      }));
      const tierCell = createElement("td", {
        text: [item.tier, item.type].filter(Boolean).join(" · "),
        attributes: { "data-label": "层级" },
      });
      const statusCell = createElement("td", { attributes: { "data-label": "状态" } });
      statusCell.append(createElement("span", {
        className: `status-label is-${item.status}`,
        text: STATUS_LABELS[item.status] || item.status,
      }));

      const actionCell = createElement("td", { attributes: { "data-label": "操作" } });
      actionCell.append(createElement("a", {
        className: "text-button",
        text: "查看详情",
        attributes: { href: itemHref, "aria-label": `查看 ${item.name} 详情` },
      }));

      row.append(nameCell, disciplineCell, waveCell, tierCell, statusCell, actionCell);
      fragment.append(row);
    }
    body.replaceChildren(fragment);
  }

  configureCatalogPageLink(
    byId("catalog-prev"),
    state.catalog.page - 1,
    state.catalog.page > 1 && Boolean(items.length),
    "prev",
  );
  configureCatalogPageLink(
    byId("catalog-next"),
    state.catalog.page + 1,
    state.catalog.page < totalPages && Boolean(items.length),
    "next",
  );

  const strip = byId("discipline-strip");
  for (const chip of strip?.querySelectorAll(".discipline-chip") || []) {
    const discipline = chip.dataset.discipline || "all";
    chip.setAttribute("aria-pressed", String(discipline === state.catalog.discipline));
    if (chip.tagName === "A") chip.href = catalogHref({ discipline, page: 1 });
  }

  const waveStrip = byId("wave-strip");
  for (const chip of waveStrip?.querySelectorAll(".wave-chip") || []) {
    const wave = chip.dataset.wave || "all";
    chip.setAttribute("aria-pressed", String(wave === state.catalog.wave));
    if (chip.tagName === "A") chip.href = catalogHref({ wave, page: 1 });
  }

  if (updateUrl) replaceCatalogUrl();
}

function resetCatalogFilters() {
  state.catalog.query = "";
  state.catalog.wave = "all";
  state.catalog.discipline = "all";
  state.catalog.narrative = "all";
  state.catalog.status = "all";
  state.catalog.page = 1;
  setCatalogControlValues();
  renderCatalog({ updateUrl: true });
}

function shouldHandleCatalogLink(event) {
  return (
    event.button === 0
    && !event.metaKey
    && !event.ctrlKey
    && !event.shiftKey
    && !event.altKey
  );
}

async function initCatalog() {
  const controls = byId("catalog-controls");
  const body = byId("catalog-body");
  if (!controls || !body) return;

  try {
    await loadCatalogData();
    initializeCatalogData();
    initializeCatalogStateFromUrl();
    renderCatalogStats();
    renderDisciplineControls();
    renderCatalog();
  } catch (error) {
    const result = byId("catalog-result-count");
    const liveRegion = result?.closest("[aria-live]")
      || byId("catalog")?.querySelector("[aria-live]")
      || document.querySelector(".catalog-result-bar[aria-live]");
    if (liveRegion) {
      setText(result && liveRegion.contains(result) ? result : liveRegion, errorMessage(error, "资料库加载失败。"));
    }
    return;
  }

  controls.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(controls);
    const queryValue = data.has("q") ? data.get("q") : data.get("query");
    const wave = normalizeCopy(data.get("wave") || "all");
    const discipline = normalizeCopy(data.get("discipline") || "all");
    const narrative = normalizeCopy(data.get("narrative") || "all");
    const status = normalizeCopy(data.get("status") || "all");
    state.catalog.query = normalizeCopy(queryValue);
    state.catalog.wave = CAMPAIGN_WAVE_IDS.has(wave) ? wave : "all";
    state.catalog.discipline = DISCIPLINES.some((entry) => String(entry.id) === discipline)
      ? discipline
      : "all";
    state.catalog.narrative = narrative === "anchor" ? "anchor" : "all";
    state.catalog.status = Object.prototype.hasOwnProperty.call(STATUS_LABELS, status)
      ? status
      : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  let searchFrame = 0;
  catalogField("catalog-search", "q", "query")?.addEventListener("input", (event) => {
    window.cancelAnimationFrame(searchFrame);
    searchFrame = window.requestAnimationFrame(() => {
      state.catalog.query = event.target.value;
      state.catalog.page = 1;
      renderCatalog({ updateUrl: true, preserveSearchInput: true });
    });
  });

  catalogField("wave-filter", "wave")?.addEventListener("change", (event) => {
    const wave = normalizeCopy(event.target.value);
    state.catalog.wave = CAMPAIGN_WAVE_IDS.has(wave) ? wave : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  catalogField("discipline-filter", "discipline")?.addEventListener("change", (event) => {
    const discipline = normalizeCopy(event.target.value);
    state.catalog.discipline = DISCIPLINES.some((entry) => String(entry.id) === discipline)
      ? discipline
      : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  catalogField("narrative-filter", "narrative")?.addEventListener("change", (event) => {
    state.catalog.narrative = normalizeCopy(event.target.value) === "anchor" ? "anchor" : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  catalogField("status-filter", "status")?.addEventListener("change", (event) => {
    const status = normalizeCopy(event.target.value);
    state.catalog.status = Object.prototype.hasOwnProperty.call(STATUS_LABELS, status)
      ? status
      : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  controls.addEventListener("reset", (event) => {
    event.preventDefault();
    resetCatalogFilters();
  });
  for (const resetLink of [byId("catalog-reset"), byId("catalog-empty-reset")]) {
    resetLink?.addEventListener("click", (event) => {
      if (!shouldHandleCatalogLink(event)) return;
      event.preventDefault();
      resetCatalogFilters();
    });
  }

  byId("discipline-strip")?.addEventListener("click", (event) => {
    const chip = event.target?.closest?.("[data-discipline]");
    if (!chip || !shouldHandleCatalogLink(event)) return;
    event.preventDefault();
    const discipline = normalizeCopy(chip.dataset.discipline || "all");
    state.catalog.discipline = DISCIPLINES.some((entry) => String(entry.id) === discipline)
      ? discipline
      : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  byId("wave-strip")?.addEventListener("click", (event) => {
    const chip = event.target?.closest?.("[data-wave]");
    if (!chip || !shouldHandleCatalogLink(event)) return;
    event.preventDefault();
    const wave = normalizeCopy(chip.dataset.wave || "all");
    state.catalog.wave = CAMPAIGN_WAVE_IDS.has(wave) ? wave : "all";
    state.catalog.page = 1;
    renderCatalog({ updateUrl: true });
  });

  byId("catalog-prev")?.addEventListener("click", (event) => {
    if (!shouldHandleCatalogLink(event)) return;
    event.preventDefault();
    if (state.catalog.page <= 1) return;
    state.catalog.page -= 1;
    renderCatalog({ updateUrl: true });
    byId("catalog-result-count")?.focus({ preventScroll: true });
    byId("catalog")?.scrollIntoView({ block: "start" });
  });

  byId("catalog-next")?.addEventListener("click", (event) => {
    if (!shouldHandleCatalogLink(event)) return;
    event.preventDefault();
    if (state.catalog.page * CATALOG_PAGE_SIZE >= filteredCatalogItems().length) return;
    state.catalog.page += 1;
    renderCatalog({ updateUrl: true });
    byId("catalog-result-count")?.focus({ preventScroll: true });
    byId("catalog")?.scrollIntoView({ block: "start" });
  });
}

function initAuthTabs() {
  const tabs = [byId("login-tab"), byId("register-tab")];
  const panels = [byId("login-panel"), byId("register-panel")];
  if (tabs.some((tab) => !tab) || panels.some((panel) => !panel)) return;

  const activate = (index, focus = false) => {
    tabs.forEach((tab, tabIndex) => {
      const selected = tabIndex === index;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
      panels[tabIndex].hidden = !selected;
    });
    hideMessage(byId("auth-error"));
    if (focus) tabs[index].focus();
  };

  tabs.forEach((tab, index) => tab.addEventListener("click", () => activate(index)));
  tabs.forEach((tab, index) => tab.addEventListener("keydown", (event) => {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    let next = index;
    if (event.key === "ArrowLeft") next = (index - 1 + tabs.length) % tabs.length;
    if (event.key === "ArrowRight") next = (index + 1) % tabs.length;
    if (event.key === "Home") next = 0;
    if (event.key === "End") next = tabs.length - 1;
    activate(next, true);
  }));
}

function dispatchConsoleEvent(name, detail = {}) {
  document.dispatchEvent(new CustomEvent(name, { detail }));
}

function authUserId(user = state.auth.user) {
  return user?.id === undefined || user?.id === null ? null : String(user.id);
}

function isAbortError(error) {
  return error?.name === "AbortError";
}

let submissionSequence = 0;
const buttonSubmissionTokens = new WeakMap();
const privateMutationSubmissions = new Map();

function beginButtonSubmission(button) {
  const token = ++submissionSequence;
  buttonSubmissionTokens.set(button, token);
  return token;
}

function buttonSubmissionMatches(button, token) {
  return buttonSubmissionTokens.get(button) === token;
}

function releaseButtonSubmission(button, token) {
  if (!buttonSubmissionMatches(button, token)) return false;
  buttonSubmissionTokens.delete(button);
  setButtonBusy(button, false);
  return true;
}

function beginPrivateMutation(operation, button, serverId = state.selectedServerId) {
  const authContext = captureAuthContext(serverId);
  if (!authContextMatches(authContext)) return null;

  const previous = privateMutationSubmissions.get(operation);
  if (previous) releaseButtonSubmission(previous.button, previous.submissionToken);

  const context = {
    ...authContext,
    operation,
    button,
    submissionToken: beginButtonSubmission(button),
  };
  privateMutationSubmissions.set(operation, context);
  return context;
}

function privateMutationMatches(context) {
  return (
    privateMutationSubmissions.get(context.operation) === context
    && buttonSubmissionMatches(context.button, context.submissionToken)
    && authContextMatches(context)
  );
}

function finishPrivateMutation(context) {
  if (!privateMutationMatches(context)) return false;
  privateMutationSubmissions.delete(context.operation);
  return releaseButtonSubmission(context.button, context.submissionToken);
}

function invalidatePrivateMutations() {
  for (const context of privateMutationSubmissions.values()) {
    releaseButtonSubmission(context.button, context.submissionToken);
  }
  privateMutationSubmissions.clear();
  state.extensions.mutation = false;
}

function clearPrivateConsoleViews() {
  state.servers = [];
  state.serversStatus = "idle";
  renderServers();
  resetSelectedServer();
  extensionResetState();
}

function beginAuthTransition() {
  state.auth.controller.abort();
  invalidatePrivateMutations();
  state.auth.generation += 1;
  state.auth.controller = new AbortController();
  state.serversRequest += 1;
  state.detailRequest += 1;
  state.snapshotRequest += 1;
  state.auth.status = "loading";
  state.auth.user = null;
  clearPrivateConsoleViews();
  renderAuthState();
  dispatchConsoleEvent("soultech:auth-changed", { user: null });
  return {
    generation: state.auth.generation,
    signal: state.auth.controller.signal,
  };
}

function authTransitionMatches(context) {
  return !context.signal.aborted && context.generation === state.auth.generation;
}

function captureAuthContext(serverId = state.selectedServerId, request = null) {
  return {
    userId: authUserId(),
    generation: state.auth.generation,
    serverId,
    request,
    signal: state.auth.controller.signal,
  };
}

function authRequestMatches(context, currentRequest = context.request) {
  return (
    !context.signal.aborted
    && context.generation === state.auth.generation
    && context.userId !== null
    && context.userId === authUserId()
    && state.auth.status === "authenticated"
    && context.request === currentRequest
  );
}

function authContextMatches(context, currentRequest = context.request) {
  return (
    authRequestMatches(context, currentRequest)
    && context.serverId === state.selectedServerId
  );
}

function renderAuthState() {
  const loading = byId("console-loading");
  const gate = byId("auth-gate");
  const consolePanel = byId("server-console");

  loading.hidden = state.auth.status !== "loading";
  gate.hidden = !["anonymous", "error"].includes(state.auth.status);
  consolePanel.hidden = state.auth.status !== "authenticated";

  if (state.auth.status === "authenticated") {
    setText("account-name", state.auth.user?.username, "已登录账号");
  }
}

async function loadAuth() {
  const context = beginAuthTransition();
  hideMessage(byId("auth-error"));

  try {
    const payload = await requestApi("/api/auth/me", { signal: context.signal });
    if (!authTransitionMatches(context)) return;

    state.auth.user = payload.user || null;
    state.auth.status = state.auth.user ? "authenticated" : "anonymous";
    renderAuthState();
    dispatchConsoleEvent("soultech:auth-changed", { user: state.auth.user });
    if (state.auth.status === "authenticated") await loadServers();
  } catch (error) {
    if (isAbortError(error) || !authTransitionMatches(context)) return;
    if (error instanceof ApiError && error.status === 401) {
      state.auth.status = "anonymous";
      renderAuthState();
      dispatchConsoleEvent("soultech:auth-changed", { user: null });
      return;
    }
    state.auth.status = "error";
    renderAuthState();
    setInlineMessage(
      byId("auth-error"),
      errorMessage(error, "无法确认登录状态，请检查网络后重试。"),
      loadAuth,
    );
  }
}

async function submitCredentials(form, endpoint, busyLabel) {
  if (!form.reportValidity()) return;
  const button = form.querySelector("button[type='submit']");
  const data = new FormData(form);
  const username = normalizeCopy(data.get("username"));
  const password = String(data.get("password") || "");
  const submissionToken = beginButtonSubmission(button);
  const context = beginAuthTransition();
  hideMessage(byId("auth-error"));
  setButtonBusy(button, true, busyLabel);

  try {
    const payload = await requestApi(endpoint, {
      method: "POST",
      body: { username, password },
      signal: context.signal,
    });
    if (!authTransitionMatches(context)) return;

    state.auth.user = payload.user;
    state.auth.status = "authenticated";
    form.reset();
    renderAuthState();
    dispatchConsoleEvent("soultech:auth-changed", { user: state.auth.user });
    showToast(endpoint.endsWith("register") ? "账号已创建。" : "登录成功。");
    await loadServers();
  } catch (error) {
    if (isAbortError(error) || !authTransitionMatches(context)) return;
    state.auth.status = "anonymous";
    renderAuthState();
    setInlineMessage(byId("auth-error"), errorMessage(error, "账号操作未完成，请重试。"));
  } finally {
    releaseButtonSubmission(button, submissionToken);
  }
}

function serverPaired(server) {
  return Boolean(server.pairedAt || server.softwareVersion);
}

function renderServersLoading() {
  const row = createElement("tr", { className: "loading-row" });
  const cell = createElement("td", {
    text: "正在读取服务器列表...",
    attributes: { colspan: "4" },
  });
  row.append(cell);
  byId("server-list-body").replaceChildren(row);
  byId("server-empty").hidden = true;
  setText("server-list-summary", "读取中");
}

function renderServers() {
  const body = byId("server-list-body");
  const empty = byId("server-empty");
  const fragment = document.createDocumentFragment();

  for (const server of state.servers) {
    const row = createElement("tr", {
      className: server.id === state.selectedServerId ? "is-selected" : "",
    });
    const name = createElement("td", { attributes: { "data-label": "名称" } });
    name.append(
      createElement("div", { className: "cell-title", text: server.name }),
      createElement("div", { className: "cell-code", text: server.softwareVersion || "尚未报告版本" }),
    );

    const paired = createElement("td", { attributes: { "data-label": "配对" } });
    paired.append(createElement("span", {
      className: `status-label ${serverPaired(server) ? "is-paired" : ""}`,
      text: serverPaired(server) ? "已配对" : "待配对",
    }));

    const sync = createElement("td", {
      text: formatDate(server.lastSyncAt),
      attributes: { "data-label": "最近同步" },
    });
    const action = createElement("td", { attributes: { "data-label": "操作" } });
    const button = createElement("button", {
      className: "text-button",
      text: server.id === state.selectedServerId ? "正在查看" : "查看",
      attributes: {
        type: "button",
        "aria-label": `查看服务器 ${server.name}`,
        "aria-current": server.id === state.selectedServerId ? "true" : undefined,
      },
    });
    button.dataset.serverId = server.id;
    action.append(button);
    row.append(name, paired, sync, action);
    fragment.append(row);
  }

  body.replaceChildren(fragment);
  empty.hidden = Boolean(state.servers.length);
  setText("server-list-summary", state.servers.length ? `${state.servers.length} 台服务器` : "等待创建第一台服务器");
}

function resetSelectedServer() {
  invalidatePrivateMutations();
  state.selectedServerId = null;
  state.selectedServer = null;
  state.snapshot = null;
  state.pairing = null;
  byId("server-detail-empty").hidden = false;
  byId("server-detail-content").hidden = true;
  dispatchConsoleEvent("soultech:server-selected", { serverId: null });
}

async function loadServers(preferredServerId = null) {
  const requestNumber = ++state.serversRequest;
  const context = captureAuthContext(state.selectedServerId, requestNumber);
  if (!authContextMatches(context, state.serversRequest)) return;

  state.serversStatus = "loading";
  renderServersLoading();
  hideMessage(byId("console-error"));

  try {
    const payload = await requestApi("/api/servers", { signal: context.signal });
    if (!authContextMatches(context, state.serversRequest)) return;

    const servers = Array.isArray(payload.servers) ? payload.servers : [];
    const nextId = preferredServerId
      || (context.serverId && servers.some((server) => server.id === context.serverId) ? context.serverId : null)
      || servers[0]?.id
      || null;
    state.servers = servers;
    state.serversStatus = "ready";
    renderServers();
    if (nextId) await selectServer(nextId);
    else resetSelectedServer();
  } catch (error) {
    if (isAbortError(error) || !authContextMatches(context, state.serversRequest)) return;
    state.serversStatus = "error";
    state.servers = [];
    renderServers();
    setInlineMessage(
      byId("console-error"),
      errorMessage(error, "服务器列表加载失败。"),
      () => loadServers(),
    );
  }
}

function renderSelectedServer(server) {
  setText("selected-server-name", server.name);
  setText("selected-server-id", server.id);
  const facts = byId("server-facts");
  facts.replaceChildren(renderDefinitionRows([
    ["status", serverPaired(server) ? "已配对" : "待配对"],
    ["softwareVersion", server.softwareVersion || "尚未报告"],
    ["pairedAt", formatDate(server.pairedAt, "尚未配对")],
    ["lastSequence", server.lastSequence ?? "尚无序列"],
    ["lastSyncAt", formatDate(server.lastSyncAt)],
    ["createdAt", formatDate(server.createdAt, "未提供")],
  ]));
  byId("server-detail-empty").hidden = true;
  byId("server-detail-content").hidden = false;
  byId("pairing-result").hidden = true;
}

function renderServerDetailLoading(server) {
  renderSelectedServer(server);
  const snapshot = byId("snapshot-content");
  snapshot.replaceChildren(
    createElement("div", { className: "content-skeleton" }),
    createElement("p", { className: "state-copy", text: "正在读取服务器详情..." }),
  );
}

async function selectServer(serverId) {
  const listServer = state.servers.find((server) => server.id === serverId);
  if (!listServer) return;

  if (state.selectedServerId !== serverId) {
    invalidatePrivateMutations();
    extensionResetState();
  }

  const requestNumber = ++state.detailRequest;
  const context = captureAuthContext(serverId, requestNumber);
  if (!authRequestMatches(context, state.detailRequest)) return;

  state.selectedServerId = serverId;
  state.selectedServer = listServer;
  if (!authContextMatches(context, state.detailRequest)) return;

  state.pairing = null;
  renderServers();
  renderServerDetailLoading(listServer);
  dispatchConsoleEvent("soultech:server-selected", { serverId });

  const detailPromise = requestApi(`/api/servers/${encodeURIComponent(serverId)}`, {
    signal: context.signal,
  });
  const snapshotPromise = loadSnapshot(serverId);

  try {
    const payload = await detailPromise;
    if (!authContextMatches(context, state.detailRequest)) return;
    state.selectedServer = payload.server;
    state.servers = state.servers.map((server) => server.id === serverId ? payload.server : server);
    renderServers();
    renderSelectedServer(payload.server);
  } catch (error) {
    if (isAbortError(error) || !authContextMatches(context, state.detailRequest)) return;
    setInlineMessage(
      byId("snapshot-content"),
      errorMessage(error, "服务器详情加载失败。"),
      () => selectServer(serverId),
    );
  }

  await snapshotPromise;
}

function renderSnapshot(snapshot) {
  const container = byId("snapshot-content");
  const view = createElement("div", { className: "snapshot-view" });
  view.append(renderDefinitionRows([
    ["sequence", snapshot.sequence],
    ["sentAt", formatDate(snapshot.sentAt, "未提供")],
    ["receivedAt", formatDate(snapshot.receivedAt, "未提供")],
    ["serverId", snapshot.serverId],
  ]));

  const groups = [
    ["服务器", snapshot.server],
    ["玩家", snapshot.players],
    ["系统", snapshot.systems],
    ["目录", snapshot.catalog],
  ];
  for (const [label, value] of groups) {
    if (value === null || value === undefined) continue;
    const group = createElement("section", { className: "snapshot-group" });
    group.append(createElement("h5", { text: label }), renderDataValue(value));
    view.append(group);
  }
  container.replaceChildren(view);
  setText("snapshot-summary", `序列 ${formatScalar(snapshot.sequence)} / 接收于 ${formatDate(snapshot.receivedAt, "未知时间")}`);
}

function renderSnapshotEmpty() {
  const empty = createElement("div", { className: "empty-state compact-empty" });
  empty.append(
    createElement("h4", { text: "尚无同步快照" }),
    createElement("p", { text: "完成插件配对并等待一次成功同步后，这里会显示服务器、玩家、系统与目录摘要。" }),
  );
  byId("snapshot-content").replaceChildren(empty);
  setText("snapshot-summary", "等待插件首次同步");
}

async function loadSnapshot(serverId = state.selectedServerId) {
  if (!serverId) return;
  const requestNumber = ++state.snapshotRequest;
  const context = captureAuthContext(serverId, requestNumber);
  if (!authContextMatches(context, state.snapshotRequest)) return;

  const container = byId("snapshot-content");
  container.replaceChildren(
    createElement("div", { className: "content-skeleton" }),
    createElement("p", { className: "state-copy", text: "正在读取最近快照..." }),
  );
  setText("snapshot-summary", "读取中");

  try {
    const payload = await requestApi(`/api/servers/${encodeURIComponent(serverId)}/snapshot`, {
      signal: context.signal,
    });
    if (!authContextMatches(context, state.snapshotRequest)) return;
    state.snapshot = payload.snapshot || null;
    if (state.snapshot) renderSnapshot(state.snapshot);
    else renderSnapshotEmpty();
  } catch (error) {
    if (isAbortError(error) || !authContextMatches(context, state.snapshotRequest)) return;
    if (error instanceof ApiError && error.status === 404) {
      state.snapshot = null;
      renderSnapshotEmpty();
      return;
    }
    setInlineMessage(
      container,
      errorMessage(error, "快照加载失败。"),
      () => loadSnapshot(serverId),
    );
    setText("snapshot-summary", "读取失败");
  }
}

async function createServer(event) {
  event.preventDefault();
  const form = event.currentTarget;
  if (!form.reportValidity()) return;
  const button = form.querySelector("button[type='submit']");
  const context = beginPrivateMutation("server-create", button, state.selectedServerId);
  if (!context) return;

  const name = normalizeCopy(new FormData(form).get("name"));
  hideMessage(byId("console-error"));
  setButtonBusy(button, true, "创建中...");

  try {
    const payload = await requestApi("/api/servers", {
      method: "POST",
      body: { name },
      signal: context.signal,
    });
    if (!privateMutationMatches(context)) return;

    form.reset();
    showToast(`服务器“${payload.server.name}”已创建。`);
    await loadServers(payload.server.id);
  } catch (error) {
    if (isAbortError(error) || !privateMutationMatches(context)) return;
    setInlineMessage(byId("console-error"), errorMessage(error, "服务器创建失败。"));
  } finally {
    finishPrivateMutation(context);
  }
}

async function generatePairingCode() {
  const serverId = state.selectedServerId;
  if (!serverId) return;
  const button = byId("pairing-button");
  const context = beginPrivateMutation("server-pairing", button, serverId);
  if (!context) return;

  setButtonBusy(button, true, "生成中...");
  hideMessage(byId("console-error"));

  try {
    const payload = await requestApi(`/api/servers/${encodeURIComponent(serverId)}/pairing`, {
      method: "POST",
      json: true,
      signal: context.signal,
    });
    if (!privateMutationMatches(context)) return;

    state.pairing = payload;
    setText("pairing-code", payload.code);
    setText("pairing-expiry", `有效期至 ${formatDate(payload.expiresAt, "十分钟后")}`);
    byId("pairing-result").hidden = false;
    showToast("一次性配对码已生成。");
  } catch (error) {
    if (isAbortError(error) || !privateMutationMatches(context)) return;
    setInlineMessage(byId("console-error"), errorMessage(error, "配对码生成失败。"));
  } finally {
    finishPrivateMutation(context);
  }
}

async function copyPairingCode() {
  const code = normalizeCopy(state.pairing?.code);
  if (!code) return;
  const button = byId("copy-pairing-code");
  setButtonBusy(button, true, "复制中...");

  try {
    await navigator.clipboard.writeText(code);
    showToast("配对码已复制。不要在公开频道发送它。");
  } catch {
    const input = createElement("input", { attributes: { type: "text", value: code, "aria-hidden": "true" } });
    input.style.position = "fixed";
    input.style.opacity = "0";
    document.body.append(input);
    input.select();
    const copied = document.execCommand("copy");
    input.remove();
    if (!copied) setInlineMessage(byId("console-error"), "浏览器未允许复制，请手动选择配对码。");
    else showToast("配对码已复制。不要在公开频道发送它。");
  } finally {
    setButtonBusy(button, false);
  }
}

async function logout() {
  const button = byId("logout-button");
  const submissionToken = beginButtonSubmission(button);
  const context = beginAuthTransition();
  setButtonBusy(button, true, "退出中...");
  hideMessage(byId("console-error"));

  try {
    await requestApi("/api/auth/logout", {
      method: "POST",
      json: true,
      signal: context.signal,
    });
    if (!authTransitionMatches(context)) return;

    state.auth.status = "anonymous";
    state.auth.user = null;
    renderAuthState();
    dispatchConsoleEvent("soultech:auth-changed", { user: null });
    showToast("已安全退出。资料库仍可继续浏览。");
  } catch (error) {
    if (isAbortError(error) || !authTransitionMatches(context)) return;
    await loadAuth();
  } finally {
    releaseButtonSubmission(button, submissionToken);
  }
}

function initConsole() {
  const loading = byId("console-loading");
  const gate = byId("auth-gate");
  const consolePanel = byId("server-console");
  const loginPanel = byId("login-panel");
  const registerPanel = byId("register-panel");
  const serverForm = byId("server-create-form");
  const serverList = byId("server-list-body");
  if (!loading || !gate || !consolePanel || !loginPanel || !registerPanel || !serverForm || !serverList) return;

  initAuthTabs();
  loginPanel.addEventListener("submit", (event) => {
    event.preventDefault();
    submitCredentials(event.currentTarget, "/api/auth/login", "登录中...");
  });
  registerPanel.addEventListener("submit", (event) => {
    event.preventDefault();
    submitCredentials(event.currentTarget, "/api/auth/register", "创建中...");
  });
  serverForm.addEventListener("submit", createServer);
  serverList.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-server-id]");
    if (button) selectServer(button.dataset.serverId);
  });
  byId("reload-servers")?.addEventListener("click", () => loadServers(state.selectedServerId));
  byId("pairing-button")?.addEventListener("click", generatePairingCode);
  byId("copy-pairing-code")?.addEventListener("click", copyPairingCode);
  byId("refresh-snapshot")?.addEventListener("click", () => loadSnapshot());
  byId("logout-button")?.addEventListener("click", logout);
  loadAuth();
}


const EXTENSION_SOURCE_MAX_BYTES = 131072;
const EXTENSION_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const EXTENSION_PERMISSIONS = Object.freeze(["log", "schedule", "events", "commands", "kv", "catalog"]);
const EXTENSION_VIEW_IDS = Object.freeze([
  "extension-loading",
  "extension-error",
  "extension-empty",
  "extension-table-frame",
]);
const EXTENSION_ERROR_COPY = Object.freeze({
  invalid_extension_id: "扩展 ID 格式无效。",
  invalid_extension_name: "扩展名称无效。",
  invalid_extension_version: "扩展版本无效。",
  invalid_extension_engine: "脚本引擎必须是 Lua 或 JavaScript。",
  invalid_extension_entry: "入口文件无效。",
  invalid_extension_source: "源码为空或无法通过校验。",
  invalid_extension_dependencies: "依赖列表包含无效扩展 ID。",
  invalid_extension_permissions: "权限列表包含未开放能力。",
  invalid_extension_state: "扩展状态无效。",
  extension_dependency_not_found: "依赖扩展不存在，请先创建依赖。",
  extension_dependency_cycle: "依赖形成循环，无法确定安全启用顺序。",
  extension_not_found: "扩展已不存在，请重新载入列表。",
  extension_update_conflict: "扩展已被其他会话更新，请重新载入后再编辑。",
  payload_too_large: "源码超过 128 KiB 上限。",
});
const EXTENSION_TEXT_ENCODER = new TextEncoder();
const EXTENSION_NUMBER_FORMAT = new Intl.NumberFormat("zh-CN");

async function extensionRenderArchitecture() {
  const runtimeFlow = byId("extension-runtime-flow");
  const securityList = byId("extension-security-list");
  const updateSteps = byId("extension-update-steps");
  const hasPublicArchitecture = Boolean(
    byId("extensions-title") || runtimeFlow || securityList || updateSteps,
  );
  if (!hasPublicArchitecture) return;

  // The SSR document already carries this section; leave it untouched when the module is unreachable.
  try {
    await loadContentData();
  } catch {
    return;
  }

  const content = SAAS_ARCHITECTURE?.extensions;
  if (!content) return;

  setText("extension-public-status", content.status);
  setText("extensions-title", content.title);
  setText("extension-public-lead", content.lead);

  if (runtimeFlow) {
    const lifecycle = [content.context, content.dependencyTopology, content.disposal].filter(Boolean);
    const lifecycleFragment = document.createDocumentFragment();
    lifecycle.forEach((phase, index) => {
      const item = createElement("li");
      const copy = createElement("div");
      copy.append(
        createElement("h3", { text: phase.title }),
        createElement("p", { text: phase.detail }),
      );
      item.append(
        createElement("span", {
          className: "extension-step-index",
          text: String(index + 1).padStart(2, "0"),
          attributes: { "aria-hidden": "true" },
        }),
        copy,
      );
      lifecycleFragment.append(item);
    });
    runtimeFlow.replaceChildren(lifecycleFragment);
  }

  setText("extension-security-title", content.capabilities?.title || "安全边界");
  if (securityList) {
    const securityFragment = document.createDocumentFragment();
    for (const sandbox of content.sandboxes || []) {
      const row = createElement("div");
      row.append(
        createElement("dt", { text: sandbox.runtime }),
        createElement("dd", { text: sandbox.detail }),
      );
      securityFragment.append(row);
    }

    if (content.capabilities) {
      const row = createElement("div");
      const detail = createElement("dd");
      detail.append(createElement("p", { text: content.capabilities.detail }));
      detail.append(extensionCreateTags(EXTENSION_PERMISSIONS, "cap", EXTENSION_PERMISSIONS.length));
      const rules = createElement("ul", { className: "extension-security-rules" });
      for (const rule of content.capabilities.rules || []) rules.append(createElement("li", { text: rule }));
      if (rules.childElementCount) detail.append(rules);
      row.append(createElement("dt", { text: "能力白名单" }), detail);
      securityFragment.append(row);
    }
    securityList.replaceChildren(securityFragment);
  }

  setText("extension-update-title", content.hotUpdate?.title);
  setText("extension-update-detail", content.hotUpdate?.detail);
  if (updateSteps) {
    const updateFragment = document.createDocumentFragment();
    for (const [index, step] of (content.hotUpdate?.flow || []).entries()) {
      const item = createElement("li");
      item.append(
        createElement("strong", { text: String(index + 1).padStart(2, "0"), }),
        createElement("span", { text: step }),
      );
      updateFragment.append(item);
    }
    updateSteps.replaceChildren(updateFragment);
  }
}

function extensionServerId() {
  if (state.auth.status !== "authenticated" || !state.auth.user || !state.selectedServerId) return null;
  return state.selectedServerId;
}

function extensionRequireServer() {
  const serverId = extensionServerId();
  if (!serverId) showToast("请先登录并选择一台服务器。");
  return serverId;
}

function extensionErrorMessage(error, fallback) {
  const detail = errorMessage(error, fallback);
  if (!(error instanceof ApiError) || !error.code) return detail;
  const guidance = EXTENSION_ERROR_COPY[error.code];
  if (!guidance) return `[${error.code}] ${detail}`;
  return `[${error.code}] ${guidance}${detail && detail !== guidance ? ` ${detail}` : ""}`;
}

function extensionSetFeedback(message, isError = false) {
  const feedback = byId("extension-feedback");
  if (!feedback) return;
  feedback.textContent = normalizeCopy(message);
  feedback.classList.toggle("form-message-error", isError);
  feedback.setAttribute("role", isError ? "alert" : "status");
  feedback.hidden = false;
}

function extensionHideFeedback() {
  const feedback = byId("extension-feedback");
  if (!feedback) return;
  feedback.hidden = true;
  feedback.replaceChildren();
  feedback.classList.remove("form-message-error");
  feedback.setAttribute("role", "status");
}

function extensionOpenNativeDialog(dialog, focusTarget) {
  if (!dialog) return;
  if (!dialog.open) {
    if (typeof dialog.showModal === "function") dialog.showModal();
    else dialog.setAttribute("open", "");
  }
  window.requestAnimationFrame(() => focusTarget?.focus());
}

function extensionCloseNativeDialog(dialog) {
  if (!dialog) return;
  if (dialog.open && typeof dialog.close === "function") dialog.close();
  else dialog.removeAttribute("open");
}

function extensionShowView(viewId) {
  for (const id of EXTENSION_VIEW_IDS) {
    const view = byId(id);
    if (view) view.hidden = id !== viewId;
  }
}

function extensionSetToolbarAvailability({ reload = true, create = true } = {}) {
  const reloadButton = byId("extension-reload-button");
  const createButton = byId("extension-create-button");
  if (reloadButton) reloadButton.disabled = !reload;
  if (createButton) createButton.disabled = !create;
}

function extensionResetState() {
  state.extensions.request += 1;
  state.extensions.items = [];
  state.extensions.status = "idle";
  state.extensions.error = null;
  state.extensions.editing = null;
  state.extensions.deleting = null;
  state.extensions.mutation = false;

  const manager = byId("extension-manager");
  if (!manager) return;
  manager.hidden = true;
  manager.setAttribute("aria-busy", "false");
  byId("extension-list-body")?.replaceChildren();
  extensionHideFeedback();
  extensionShowView("");
  extensionSetToolbarAvailability({ reload: false, create: false });
  extensionCloseEditor(true);
  extensionCloseDeleteDialog(true);
}

function extensionRenderLoading() {
  const manager = byId("extension-manager");
  manager.hidden = false;
  manager.setAttribute("aria-busy", "true");
  extensionHideFeedback();
  extensionShowView("extension-loading");
  extensionSetToolbarAvailability({ reload: false, create: false });
  setText("extension-manager-summary", "正在读取扩展清单");
}

function extensionRenderError(error) {
  const manager = byId("extension-manager");
  manager.hidden = false;
  manager.setAttribute("aria-busy", "false");
  setText("extension-error-message", extensionErrorMessage(error, "扩展列表加载失败。"));
  setText("extension-manager-summary", "扩展清单暂不可用");
  extensionShowView("extension-error");
  extensionSetToolbarAvailability({ reload: true, create: false });
}

function extensionEngineLabel(engine) {
  return engine === "lua" ? "Lua / LuaJ" : "JavaScript / Rhino";
}

function extensionCreateTags(values, prefix, visibleLimit = 4) {
  const list = createElement("ul", { className: "extension-tag-list" });
  const limit = Math.min(values.length, visibleLimit);
  for (let index = 0; index < limit; index += 1) {
    list.append(createElement("li", {
      className: "extension-tag",
      text: `${prefix}:${values[index]}`,
    }));
  }
  if (values.length > limit) {
    list.append(createElement("li", {
      className: "extension-tag",
      text: `+${values.length - limit}`,
    }));
  }
  return list;
}

function extensionRenderRow(item) {
  const manifest = item.manifest || {};
  const id = String(manifest.id || "");
  const name = String(manifest.name || id);
  const dependencies = Array.isArray(manifest.dependencies) ? manifest.dependencies : [];
  const permissions = Array.isArray(manifest.permissions) ? manifest.permissions : [];
  const row = createElement("tr");

  const identityCell = createElement("td", { attributes: { "data-label": "扩展" } });
  identityCell.append(
    createElement("span", { className: "extension-name", text: name }),
    createElement("span", { className: "extension-id", text: id }),
  );

  const engineCell = createElement("td", { attributes: { "data-label": "引擎 / 版本" } });
  engineCell.append(
    createElement("span", { text: extensionEngineLabel(manifest.engine) }),
    createElement("span", { className: "extension-engine", text: manifest.version || "未提供版本" }),
  );

  const boundaryCell = createElement("td", { attributes: { "data-label": "依赖 / 权限" } });
  boundaryCell.append(createElement("span", {
    className: "extension-revision",
    text: `依赖 ${dependencies.length} 项 / 权限 ${permissions.length} 项`,
  }));
  if (dependencies.length) boundaryCell.append(extensionCreateTags(dependencies, "dep"));
  if (permissions.length) boundaryCell.append(extensionCreateTags(permissions, "perm"));

  const statusCell = createElement("td", { attributes: { "data-label": "状态" } });
  statusCell.append(
    createElement("span", {
      className: `extension-status ${item.enabled ? "is-enabled" : ""}`,
      text: item.enabled ? "已启用" : "已停用",
    }),
    createElement("span", {
      className: "extension-revision",
      text: `revision ${item.revision ?? 0} / ${formatDate(item.updatedAt, "未提供更新时间")}`,
    }),
  );

  const actionCell = createElement("td", { attributes: { "data-label": "操作" } });
  const actions = createElement("div", { className: "extension-action-group" });
  actions.append(
    createElement("button", {
      className: "text-button",
      text: "编辑",
      attributes: {
        type: "button",
        "data-extension-action": "edit",
        "data-extension-id": id,
        "aria-label": `编辑扩展 ${name}`,
      },
    }),
    createElement("button", {
      className: "text-button",
      text: item.enabled ? "停用" : "启用",
      attributes: {
        type: "button",
        "data-extension-action": "state",
        "data-extension-id": id,
        "aria-label": `${item.enabled ? "停用" : "启用"}扩展 ${name}`,
      },
    }),
    createElement("button", {
      className: "text-button extension-delete-trigger",
      text: "删除",
      attributes: {
        type: "button",
        "data-extension-action": "delete",
        "data-extension-id": id,
        "aria-label": `删除扩展 ${name}`,
      },
    }),
  );
  actionCell.append(actions);
  row.append(identityCell, engineCell, boundaryCell, statusCell, actionCell);
  return row;
}

function extensionRenderReady() {
  const manager = byId("extension-manager");
  const body = byId("extension-list-body");
  const fragment = document.createDocumentFragment();
  const enabledCount = state.extensions.items.filter((item) => item.enabled).length;

  for (const item of state.extensions.items) fragment.append(extensionRenderRow(item));
  body.replaceChildren(fragment);
  manager.hidden = false;
  manager.setAttribute("aria-busy", "false");
  extensionSetToolbarAvailability({ reload: true, create: true });
  setText(
    "extension-manager-summary",
    state.extensions.items.length
      ? `${state.extensions.items.length} 个扩展 / ${enabledCount} 个已启用`
      : "等待创建第一个扩展",
  );
  extensionShowView(state.extensions.items.length ? "extension-table-frame" : "extension-empty");
}

async function extensionLoad(serverId = state.selectedServerId) {
  if (!serverId || extensionServerId() !== serverId) {
    extensionResetState();
    return;
  }

  const requestNumber = ++state.extensions.request;
  const context = captureAuthContext(serverId, requestNumber);
  if (!authContextMatches(context, state.extensions.request)) return;

  state.extensions.status = "loading";
  state.extensions.error = null;
  extensionRenderLoading();

  try {
    const payload = await requestApi(`/api/servers/${encodeURIComponent(serverId)}/extensions`, {
      signal: context.signal,
    });
    if (!authContextMatches(context, state.extensions.request)) return;

    state.extensions.items = Array.isArray(payload.extensions)
      ? payload.extensions.filter((item) => item?.manifest && typeof item.manifest.id === "string")
      : [];
    state.extensions.status = "ready";
    extensionRenderReady();
  } catch (error) {
    if (isAbortError(error) || !authContextMatches(context, state.extensions.request)) return;
    state.extensions.status = "error";
    state.extensions.error = error;
    extensionRenderError(error);
  }
}

function extensionUpdateSourceCount() {
  const source = byId("extension-source");
  if (!source) return 0;

  const bytes = EXTENSION_TEXT_ENCODER.encode(source.value).byteLength;
  setText("extension-source-count", EXTENSION_NUMBER_FORMAT.format(bytes));
  source.toggleAttribute("aria-invalid", bytes > EXTENSION_SOURCE_MAX_BYTES);
  return bytes;
}

function extensionCloseEditor(force = false) {
  if (state.extensions.mutation && !force) return;
  extensionCloseNativeDialog(byId("extension-dialog"));
  state.extensions.editing = null;
  hideMessage(byId("extension-form-error"));
}

function extensionOpenEditor(item = null) {
  if (!extensionRequireServer()) return;
  if (state.extensions.mutation) {
    showToast("请等待当前扩展操作完成。");
    return;
  }

  const form = byId("extension-form");
  const manifest = item?.manifest || {};
  const editing = Boolean(item);
  form.reset();
  hideMessage(byId("extension-form-error"));
  state.extensions.editing = editing ? String(manifest.id) : null;

  const idInput = byId("extension-id");
  idInput.value = editing ? String(manifest.id || "") : "";
  idInput.readOnly = editing;
  idInput.toggleAttribute("aria-readonly", editing);
  byId("extension-name").value = editing ? String(manifest.name || "") : "";
  byId("extension-version").value = editing ? String(manifest.version || "") : "1.0.0";
  byId("extension-engine").value = manifest.engine === "javascript" ? "javascript" : "lua";
  byId("extension-entry").value = editing ? String(manifest.entry || "") : "main.lua";
  byId("extension-dependencies").value = Array.isArray(manifest.dependencies)
    ? manifest.dependencies.join("\n")
    : "";

  const selectedPermissions = new Set(Array.isArray(manifest.permissions) ? manifest.permissions : []);
  for (const checkbox of form.querySelectorAll('input[name="permissions"]')) {
    checkbox.checked = selectedPermissions.has(checkbox.value);
  }

  byId("extension-source").value = typeof item?.source === "string" ? item.source : "";
  byId("extension-enabled").checked = Boolean(item?.enabled);
  setText("extension-dialog-mode", editing ? "编辑扩展" : "创建扩展");
  setText("extension-save-button", editing ? "保存新版本" : "保存扩展");
  byId("extension-save-button").dataset.idleLabel = editing ? "保存新版本" : "保存扩展";
  extensionUpdateSourceCount();
  extensionOpenNativeDialog(byId("extension-dialog"), editing ? byId("extension-name") : idInput);
}

function extensionUpdateEntryForEngine() {
  const entry = byId("extension-entry");
  if (!entry.value || entry.value === "main.lua" || entry.value === "main.js") {
    entry.value = byId("extension-engine").value === "javascript" ? "main.js" : "main.lua";
  }
}

function extensionShowFormError(message, focusTarget) {
  setInlineMessage(byId("extension-form-error"), message);
  focusTarget?.focus();
}

function extensionReadDraft() {
  const idInput = byId("extension-id");
  const id = idInput.value.trim();
  if (!EXTENSION_ID_PATTERN.test(id)) {
    extensionShowFormError("扩展 ID 仅允许小写字母、数字与中间连字符。", idInput);
    return null;
  }
  if (state.extensions.editing && state.extensions.editing !== id) {
    extensionShowFormError("编辑现有扩展时不能修改扩展 ID。", idInput);
    return null;
  }

  const dependencyInput = byId("extension-dependencies");
  const dependencies = [];
  const seenDependencies = new Set();
  for (const value of dependencyInput.value.split(/[\n,]/)) {
    const dependency = value.trim();
    if (!dependency || seenDependencies.has(dependency)) continue;
    if (!EXTENSION_ID_PATTERN.test(dependency)) {
      extensionShowFormError(`依赖 ID “${dependency}” 格式无效。`, dependencyInput);
      return null;
    }
    if (dependency === id) {
      extensionShowFormError("扩展不能依赖自身。", dependencyInput);
      return null;
    }
    dependencies.push(dependency);
    seenDependencies.add(dependency);
  }

  const sourceInput = byId("extension-source");
  const source = sourceInput.value;
  const sourceBytes = extensionUpdateSourceCount();
  if (!source.trim()) {
    extensionShowFormError("请输入扩展源码。", sourceInput);
    return null;
  }
  if (sourceBytes > EXTENSION_SOURCE_MAX_BYTES) {
    extensionShowFormError("源码超过 128 KiB 上限，请精简后再保存。", sourceInput);
    return null;
  }

  const permissions = Array.from(
    byId("extension-form").querySelectorAll('input[name="permissions"]:checked'),
    (checkbox) => checkbox.value,
  ).filter((permission) => EXTENSION_PERMISSIONS.includes(permission));

  return {
    manifest: {
      id,
      name: byId("extension-name").value.trim(),
      version: byId("extension-version").value.trim(),
      engine: byId("extension-engine").value,
      entry: byId("extension-entry").value.trim(),
      dependencies,
      permissions,
    },
    source,
    enabled: byId("extension-enabled").checked,
  };
}

async function extensionSubmitEditor(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const serverId = extensionRequireServer();
  if (!serverId || state.extensions.mutation) return;
  hideMessage(byId("extension-form-error"));
  if (!form.reportValidity()) return;

  const draft = extensionReadDraft();
  if (!draft) return;
  const saveButton = byId("extension-save-button");
  const context = beginPrivateMutation("extension-upsert", saveButton, serverId);
  if (!context) return;

  state.extensions.mutation = true;
  setButtonBusy(saveButton, true, "保存中...");

  try {
    await requestApi(`/api/servers/${encodeURIComponent(serverId)}/extensions`, {
      method: "POST",
      body: draft,
      signal: context.signal,
    });
    if (!privateMutationMatches(context)) return;

    extensionCloseEditor(true);
    showToast(`扩展 ${draft.manifest.name} 已保存${draft.enabled ? "并启用" : "为停用状态"}。`);
    await extensionLoad(serverId);
  } catch (error) {
    if (isAbortError(error) || !privateMutationMatches(context)) return;
    extensionShowFormError(extensionErrorMessage(error, "扩展保存失败。"), saveButton);
  } finally {
    if (privateMutationMatches(context)) {
      state.extensions.mutation = false;
      finishPrivateMutation(context);
    }
  }
}

async function extensionSetEnabled(item, button) {
  const serverId = extensionRequireServer();
  if (!serverId || state.extensions.mutation) return;
  const nextEnabled = !item.enabled;
  const action = nextEnabled ? "启用" : "停用";
  const confirmed = window.confirm(
    `确认${action}扩展 ${item.manifest.name}？Paper 端会在下一次同步后应用该状态。`,
  );
  if (!confirmed) return;

  const context = beginPrivateMutation("extension-state", button, serverId);
  if (!context) return;

  extensionHideFeedback();
  state.extensions.mutation = true;
  setButtonBusy(button, true, `${action}中...`);
  try {
    await requestApi(
      `/api/servers/${encodeURIComponent(serverId)}/extensions/${encodeURIComponent(item.manifest.id)}/state`,
      { method: "POST", body: { enabled: nextEnabled }, signal: context.signal },
    );
    if (!privateMutationMatches(context)) return;

    showToast(`扩展 ${item.manifest.name} 已${action}。`);
    await extensionLoad(serverId);
  } catch (error) {
    if (isAbortError(error) || !privateMutationMatches(context)) return;
    extensionSetFeedback(extensionErrorMessage(error, `扩展${action}失败。`), true);
  } finally {
    if (privateMutationMatches(context)) {
      state.extensions.mutation = false;
      finishPrivateMutation(context);
    }
  }
}

function extensionCloseDeleteDialog(force = false) {
  if (state.extensions.mutation && !force) return;
  extensionCloseNativeDialog(byId("extension-delete-dialog"));
  state.extensions.deleting = null;
  byId("extension-delete-confirmation").value = "";
  byId("extension-delete-button").disabled = true;
  hideMessage(byId("extension-delete-error"));
}

function extensionOpenDeleteDialog(item) {
  if (!extensionRequireServer()) return;
  if (state.extensions.mutation) {
    showToast("请等待当前扩展操作完成。");
    return;
  }
  state.extensions.deleting = item;
  setText("extension-delete-id", item.manifest.id);
  byId("extension-delete-confirmation").value = "";
  byId("extension-delete-button").disabled = true;
  hideMessage(byId("extension-delete-error"));
  extensionOpenNativeDialog(byId("extension-delete-dialog"), byId("extension-delete-confirmation"));
}

function extensionUpdateDeleteConfirmation() {
  const expected = state.extensions.deleting?.manifest?.id || "";
  const matches = Boolean(expected) && byId("extension-delete-confirmation").value === expected;
  byId("extension-delete-button").disabled = state.extensions.mutation || !matches;
}

async function extensionSubmitDelete(event) {
  event.preventDefault();
  const serverId = extensionRequireServer();
  const item = state.extensions.deleting;
  if (!serverId || !item || state.extensions.mutation) return;
  if (byId("extension-delete-confirmation").value !== item.manifest.id) {
    setInlineMessage(byId("extension-delete-error"), "输入的扩展 ID 不匹配，未执行删除。 " );
    extensionUpdateDeleteConfirmation();
    return;
  }

  const deleteButton = byId("extension-delete-button");
  const context = beginPrivateMutation("extension-delete", deleteButton, serverId);
  if (!context) return;

  state.extensions.mutation = true;
  setButtonBusy(deleteButton, true, "删除中...");
  try {
    await requestApi(
      `/api/servers/${encodeURIComponent(serverId)}/extensions/${encodeURIComponent(item.manifest.id)}`,
      { method: "DELETE", json: true, signal: context.signal },
    );
    if (!privateMutationMatches(context)) return;

    extensionCloseDeleteDialog(true);
    showToast(`扩展 ${item.manifest.name} 已删除。`);
    await extensionLoad(serverId);
  } catch (error) {
    if (isAbortError(error) || !privateMutationMatches(context)) return;
    setInlineMessage(byId("extension-delete-error"), extensionErrorMessage(error, "扩展删除失败。"));
  } finally {
    if (privateMutationMatches(context)) {
      state.extensions.mutation = false;
      if (finishPrivateMutation(context)) extensionUpdateDeleteConfirmation();
    }
  }
}

async function extensionHandleListAction(event) {
  const button = event.target.closest("button[data-extension-action]");
  if (!button) return;
  if (state.extensions.mutation) {
    showToast("请等待当前扩展操作完成。");
    return;
  }

  const item = state.extensions.items.find(
    (candidate) => candidate.manifest.id === button.dataset.extensionId,
  );
  if (!item) {
    extensionSetFeedback("扩展列表已经变化，请重新载入。", true);
    return;
  }

  if (button.dataset.extensionAction === "edit") extensionOpenEditor(item);
  if (button.dataset.extensionAction === "state") await extensionSetEnabled(item, button);
  if (button.dataset.extensionAction === "delete") extensionOpenDeleteDialog(item);
}

function initExtensions() {
  extensionRenderArchitecture();
  if (!byId("extension-manager")) return;

  byId("extension-create-button")?.addEventListener("click", () => extensionOpenEditor());
  byId("extension-empty-create-button")?.addEventListener("click", () => extensionOpenEditor());
  byId("extension-reload-button")?.addEventListener("click", () => extensionLoad());
  byId("extension-retry-button")?.addEventListener("click", () => extensionLoad());
  byId("extension-list-body")?.addEventListener("click", (event) => {
    extensionHandleListAction(event);
  });

  byId("extension-engine")?.addEventListener("change", extensionUpdateEntryForEngine);
  byId("extension-source")?.addEventListener("input", extensionUpdateSourceCount);
  byId("extension-form")?.addEventListener("submit", extensionSubmitEditor);
  byId("extension-dialog-close")?.addEventListener("click", () => extensionCloseEditor());
  byId("extension-dialog-cancel")?.addEventListener("click", () => extensionCloseEditor());

  const editorDialog = byId("extension-dialog");
  editorDialog?.addEventListener("cancel", (event) => {
    event.preventDefault();
    extensionCloseEditor();
  });
  editorDialog?.addEventListener("click", (event) => {
    if (event.target === editorDialog) extensionCloseEditor();
  });

  byId("extension-delete-confirmation")?.addEventListener("input", extensionUpdateDeleteConfirmation);
  byId("extension-delete-form")?.addEventListener("submit", extensionSubmitDelete);
  byId("extension-delete-close")?.addEventListener("click", () => extensionCloseDeleteDialog());
  byId("extension-delete-cancel")?.addEventListener("click", () => extensionCloseDeleteDialog());

  const deleteDialog = byId("extension-delete-dialog");
  deleteDialog?.addEventListener("cancel", (event) => {
    event.preventDefault();
    extensionCloseDeleteDialog();
  });
  deleteDialog?.addEventListener("click", (event) => {
    if (event.target === deleteDialog) extensionCloseDeleteDialog();
  });

  document.addEventListener("soultech:auth-changed", (event) => {
    if (!event.detail?.user || !state.selectedServerId) extensionResetState();
    else extensionLoad(state.selectedServerId);
  });
  document.addEventListener("soultech:server-selected", (event) => {
    if (!event.detail?.serverId || state.auth.status !== "authenticated") extensionResetState();
    else extensionLoad(event.detail.serverId);
  });

  extensionResetState();
  extensionUpdateSourceCount();
}

async function initializeSite() {
  const sharedTargets = [
    byId("overview-content"),
    byId("compatibility-content"),
    byId("quick-install-content"),
    byId("mysql-content"),
    byId("download-description"),
  ];
  const sharedErrorTarget = sharedTargets.find(Boolean);
  if (sharedErrorTarget) {
    try {
      await Promise.all([loadContentData(), loadCatalogData()]);
      renderSharedContent();
    } catch (error) {
      sharedErrorTarget.replaceChildren(
        createElement("div", { className: "inline-error", text: errorMessage(error, "站点内容加载失败。") }),
      );
    }
  }

  if (byId("tutorial-index") && byId("tutorial-reader")) {
    try {
      await loadContentData();
      initTutorials();
    } catch (error) {
      byId("tutorial-reader")?.replaceChildren(
        createElement("div", { className: "inline-error", text: errorMessage(error, "教程加载失败。") }),
      );
    }
  }

  if (byId("planning-content")) {
    try {
      await loadContentData();
      renderPlanning();
    } catch (error) {
      byId("planning-content")?.replaceChildren(
        createElement("div", { className: "inline-error", text: errorMessage(error, "策划内容加载失败。") }),
      );
    }
  }

  if (byId("architecture-plugin") || byId("architecture-saas")) {
    try {
      await loadContentData();
      initArchitecture();
    } catch (error) {
      const panel = byId("architecture-plugin") || byId("architecture-saas");
      panel?.replaceChildren(
        createElement("div", { className: "inline-error", text: errorMessage(error, "架构内容加载失败。") }),
      );
    }
  }

  if (byId("catalog-controls") && byId("catalog-body")) await initCatalog();

  if (byId("console-loading") && byId("auth-gate") && byId("server-console")) {
    initConsole();
  }

  if (
    byId("extensions-title")
    || byId("extension-runtime-flow")
    || byId("extension-security-list")
    || byId("extension-update-steps")
    || byId("extension-manager")
  ) initExtensions();
  if (byId("artifact-meta")) loadArtifactManifest();
}

initializeSite();
