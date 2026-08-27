const METRIC_LABELS = [
  ["produce", "产出"],
  ["machine_op", "机器操作"],
  ["tool_use", "装备使用"],
  ["charge", "充能"],
  ["session_seconds", "在线秒数"],
  ["unique_players", "活跃玩家"],
  ["unlock", "解锁"],
];
const TOP_LABELS = [
  ["produce", "产出 Top 10"],
  ["machine_op", "机器操作 Top 10"],
  ["tool_use", "装备使用 Top 10"],
];
const EMPTY_STATE_TEXT = "等待生产插件上报";

const panel = document.getElementById("telemetry-panel");

function createTextElement(tagName, text, className) {
  const node = document.createElement(tagName);
  node.textContent = text;
  if (className) {
    node.className = className;
  }
  return node;
}

function createCell(tagName, text, className) {
  const cell = createTextElement(tagName, text, className);
  if (tagName === "th") {
    cell.scope = "col";
  }
  return cell;
}

function createTable(caption, headers, rows) {
  const frame = document.createElement("div");
  frame.className = "table-frame";

  const table = document.createElement("table");
  table.className = "data-table";
  table.append(createTextElement("caption", caption, "station-label"));

  const head = document.createElement("thead");
  const headRow = document.createElement("tr");
  headRow.append(...headers.map((header) => createCell("th", header)));
  head.append(headRow);

  const body = document.createElement("tbody");
  for (const row of rows) {
    const bodyRow = document.createElement("tr");
    bodyRow.append(
      createCell("td", row[0], "cell-code"),
      ...row.slice(1).map((value) => createCell("td", value)),
    );
    body.append(bodyRow);
  }

  table.append(head, body);
  frame.append(table);
  return frame;
}

function readNumber(value) {
  return Number.isFinite(value) ? String(value) : "0";
}

function renderEmpty() {
  const empty = document.createElement("div");
  empty.className = "empty-state compact-empty";
  empty.append(createTextElement("p", EMPTY_STATE_TEXT, "state-copy"));
  panel.replaceChildren(empty);
}

function renderError(message) {
  const failure = document.createElement("div");
  failure.className = "inline-error";
  failure.append(createTextElement("p", message, "state-copy"));
  panel.replaceChildren(failure);
}

function renderTelemetry(data) {
  const days = Array.isArray(data?.days) ? data.days : [];
  const servers = Array.isArray(data?.servers) ? data.servers : [];
  const top = data?.top ?? {};

  if (days.length === 0 && servers.length === 0) {
    renderEmpty();
    return;
  }

  const sections = [
    createTable(
      "每日聚合",
      ["日期", ...METRIC_LABELS.map(([, label]) => label)],
      days.map((entry) => [
        String(entry?.day ?? ""),
        ...METRIC_LABELS.map(([metric]) => readNumber(entry?.totals?.[metric])),
      ]),
    ),
  ];

  for (const [metric, caption] of TOP_LABELS) {
    const entries = Array.isArray(top?.[metric]) ? top[metric] : [];
    if (entries.length > 0) {
      sections.push(
        createTable(
          caption,
          ["标识", "计数"],
          entries.map((entry) => [String(entry?.key ?? ""), readNumber(entry?.value)]),
        ),
      );
    }
  }

  sections.push(
    createTable(
      "上报服务器",
      ["服务器", "最近上报日"],
      servers.map((server) => [String(server?.serverId ?? ""), String(server?.lastDay ?? "")]),
    ),
  );

  panel.replaceChildren(...sections);
}

async function loadTelemetry(endpoint) {
  const response = await fetch(endpoint, {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: { accept: "application/json" },
  });

  let body;
  try {
    body = await response.json();
  } catch {
    renderError("遥测接口返回了无法识别的响应。");
    return;
  }

  if (!response.ok) {
    renderError(
      typeof body?.error?.message === "string" ? body.error.message : "遥测数据加载失败，请稍后重试。",
    );
    return;
  }

  renderTelemetry(body);
}

if (panel) {
  const endpoint = panel.dataset.endpoint ?? "";
  if (endpoint.startsWith("/api/")) {
    loadTelemetry(endpoint).catch(() => renderError("遥测数据加载失败，请稍后重试。"));
  } else {
    renderError("遥测面板缺少有效的数据接口配置。");
  }
}
