const loadingNode = document.getElementById("admin-loading");
const errorNode = document.getElementById("admin-error");
const summaryNode = document.getElementById("admin-summary");
const refreshButton = document.getElementById("admin-refresh");

class ApiRequestError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function createTextElement(tagName, text, className) {
  const node = document.createElement(tagName);
  node.textContent = text;
  if (className) {
    node.className = className;
  }
  return node;
}

function createActionLink(href, label, primary = false) {
  const link = document.createElement("a");
  link.href = href;
  link.className = primary ? "button button-primary" : "button button-secondary";
  link.textContent = label;
  return link;
}

function resetVisibleState() {
  errorNode.hidden = true;
  errorNode.textContent = "";
  summaryNode.hidden = true;
  summaryNode.replaceChildren();
}

function setBusy(busy) {
  loadingNode.hidden = !busy;
  refreshButton.disabled = busy;
  if (busy) {
    refreshButton.setAttribute("aria-busy", "true");
    refreshButton.textContent = "刷新中...";
  } else {
    refreshButton.removeAttribute("aria-busy");
    refreshButton.textContent = "刷新";
  }
}

function renderMessageState(title, description, actions) {
  const panel = document.createElement("article");
  panel.className = "manual-block";
  panel.append(
    createTextElement("h2", title),
    createTextElement("p", description, "state-copy"),
  );

  const actionRow = document.createElement("div");
  actionRow.className = "hero-actions";
  for (const action of actions) {
    actionRow.append(createActionLink(action.href, action.label, action.primary));
  }
  panel.append(actionRow);

  summaryNode.replaceChildren(panel);
  summaryNode.hidden = false;
}

function renderUninitialized() {
  renderMessageState(
    "平台管理员尚未初始化",
    "请在受信任的终端使用 CLI 写入首个管理员。网页不会提供管理员初始化接口。",
    [
      { href: "/setup", label: "查看初始化说明", primary: true },
      { href: "/console", label: "返回控制台", primary: false },
    ],
  );
}

function renderSignedOut() {
  renderMessageState(
    "请先登录管理员账号",
    "管理员会话与普通控制台共用安全 Cookie。登录成功后返回此页并刷新。",
    [{ href: "/console", label: "前往控制台登录", primary: true }],
  );
}

function renderForbidden() {
  renderMessageState(
    "当前账号没有管理员权限",
    "普通服主账号只能管理自己的服务器，不能读取平台汇总。",
    [{ href: "/console", label: "返回控制台", primary: true }],
  );
}

function readCount(summary, key) {
  const count = summary?.[key];
  if (!Number.isSafeInteger(count) || count < 0) {
    throw new Error("管理员汇总响应格式不正确。");
  }
  return count;
}

function renderAdminSummary(summary) {
  const heading = document.createElement("article");
  heading.className = "manual-block";
  heading.append(
    createTextElement("h2", "平台汇总"),
    createTextElement("p", "仅展示全局聚合数量，不包含用户名、密钥或个人快照。", "state-copy"),
  );
  const actions = document.createElement("div");
  actions.className = "hero-actions";
  actions.append(createActionLink("/console", "打开控制台"));
  heading.append(actions);

  const metrics = [
    ["账号", readCount(summary, "users")],
    ["服务器", readCount(summary, "servers")],
    ["已配对服务器", readCount(summary, "pairedServers")],
    ["快照", readCount(summary, "snapshots")],
    ["扩展", readCount(summary, "extensions")],
    ["已启用扩展", readCount(summary, "enabledExtensions")],
  ];
  const cards = metrics.map(([label, value]) => {
    const card = document.createElement("article");
    card.className = "manual-block";
    card.append(
      createTextElement("p", label, "station-label"),
      createTextElement("strong", String(value), "metric-value"),
    );
    return card;
  });

  summaryNode.replaceChildren(heading, ...cards);
  summaryNode.hidden = false;
}

function showFailure(message) {
  summaryNode.hidden = true;
  summaryNode.replaceChildren();
  errorNode.textContent = message;
  errorNode.hidden = false;
}

async function fetchJson(path) {
  const response = await fetch(path, {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
    headers: { accept: "application/json" },
  });

  let body;
  try {
    body = await response.json();
  } catch {
    throw new ApiRequestError(response.status, "服务器返回了无法识别的响应。");
  }

  if (!response.ok) {
    const message = typeof body?.error?.message === "string"
      ? body.error.message
      : "管理员接口请求失败。";
    throw new ApiRequestError(response.status, message);
  }
  return body;
}

async function loadAdmin() {
  resetVisibleState();
  setBusy(true);

  try {
    const status = await fetchJson("/api/admin/status");
    if (typeof status?.initialized !== "boolean") {
      throw new Error("管理员状态响应格式不正确。");
    }
    if (!status.initialized) {
      renderUninitialized();
      return;
    }

    let auth;
    try {
      auth = await fetchJson("/api/auth/me");
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        renderSignedOut();
        return;
      }
      throw error;
    }

    if (auth?.user?.role !== "admin") {
      renderForbidden();
      return;
    }

    let summary;
    try {
      summary = await fetchJson("/api/admin/summary");
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        renderSignedOut();
        return;
      }
      if (error instanceof ApiRequestError && error.status === 403) {
        renderForbidden();
        return;
      }
      throw error;
    }
    renderAdminSummary(summary?.summary);
  } catch (error) {
    showFailure(error instanceof Error ? error.message : "管理员数据加载失败，请稍后重试。");
  } finally {
    setBusy(false);
  }
}

if (loadingNode && errorNode && summaryNode && refreshButton) {
  refreshButton.addEventListener("click", loadAdmin);
  void loadAdmin();
}
