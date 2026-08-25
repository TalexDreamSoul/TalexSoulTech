import { pbkdf2Sync, randomBytes } from "node:crypto";
import { spawnSync } from "node:child_process";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const DATABASE_NAME = "soultech";
const PASSWORD_ENV = "SOULTECH_ADMIN_PASSWORD";
const PBKDF2_ITERATIONS = 100_000;
const USERNAME_PATTERN = /^[A-Za-z0-9_-]{3,32}$/;
const MIN_PASSWORD_BYTES = 8;
const MAX_PASSWORD_BYTES = 128;

function usageError(message) {
  throw new Error(`${message}\n用法：npm run init:admin -- <username> [--local]`);
}

function parseArguments(argumentsList) {
  let username;
  let local = false;

  for (const argument of argumentsList) {
    if (argument === "--local") {
      if (local) {
        usageError("--local 只能指定一次。");
      }
      local = true;
      continue;
    }

    if (argument.startsWith("--")) {
      usageError("存在不支持的参数。");
    }
    if (username !== undefined) {
      usageError("只能指定一个用户名。");
    }
    username = argument;
  }

  if (!username) {
    usageError("缺少管理员用户名。");
  }
  if (!USERNAME_PATTERN.test(username)) {
    usageError("用户名需为 3 至 32 位字母、数字、下划线或连字符。");
  }

  return { username, local };
}

function readPassword() {
  const password = process.env[PASSWORD_ENV];
  const byteLength = typeof password === "string" ? Buffer.byteLength(password, "utf8") : 0;
  if (byteLength < MIN_PASSWORD_BYTES || byteLength > MAX_PASSWORD_BYTES) {
    throw new Error(`${PASSWORD_ENV} 必须是 8 至 128 字节的密码。`);
  }
  return password;
}

function sqlQuote(value) {
  return `'${String(value).replaceAll("'", "''")}'`;
}

function buildInsertSql(username, password) {
  const userId = `usr_${randomBytes(16).toString("base64url")}`;
  const saltBytes = randomBytes(16);
  const salt = saltBytes.toString("base64url");
  const hash = pbkdf2Sync(password, saltBytes, PBKDF2_ITERATIONS, 32, "sha256").toString(
    "base64url",
  );
  const now = new Date().toISOString();

  return `INSERT INTO users (
  id, username, password_hash, password_salt, password_iterations, role, created_at, updated_at
) VALUES (
  ${sqlQuote(userId)},
  ${sqlQuote(username)},
  ${sqlQuote(hash)},
  ${sqlQuote(salt)},
  ${sqlQuote(PBKDF2_ITERATIONS)},
  ${sqlQuote("admin")},
  ${sqlQuote(now)},
  ${sqlQuote(now)}
);`;
}

function runWrangler(sql, local) {
  const siteRoot = fileURLToPath(new URL("../", import.meta.url));
  const configPath = path.join(siteRoot, "wrangler.jsonc");
  const executable = "wrangler";
  const childEnvironment = Object.fromEntries(
    Object.entries(process.env).filter(([key]) => key.toUpperCase() !== PASSWORD_ENV),
  );

  const temporaryDirectory = mkdtempSync(path.join(tmpdir(), "soultech-admin-"));
  const sqlPath = path.join(temporaryDirectory, "init-admin.sql");

  try {
    writeFileSync(sqlPath, sql, { encoding: "utf8", flag: "wx", mode: 0o600 });
    return spawnSync(
      executable,
      [
        "d1",
        "execute",
        DATABASE_NAME,
        local ? "--local" : "--remote",
        "--config",
        configPath,
        "--file",
        sqlPath,
      ],
      {
        cwd: siteRoot,
        encoding: "utf8",
        env: childEnvironment,
        maxBuffer: 1024 * 1024,
        shell: false,
        stdio: ["ignore", "pipe", "pipe"],
      },
    );
  } finally {
    rmSync(temporaryDirectory, { force: true, recursive: true });
  }
}

function describeWranglerFailure(result) {
  if (result.error) {
    return "无法启动 Wrangler。请确认 Wrangler 已在当前 npm 环境中可用。";
  }

  const output = `${result.stdout ?? ""}\n${result.stderr ?? ""}`;
  if (/UNIQUE constraint failed:\s*users\.role/i.test(output)) {
    return "平台管理员已初始化；数据库拒绝写入第二个管理员。";
  }
  if (/UNIQUE constraint failed:\s*users\.username/i.test(output)) {
    return "该用户名已存在；未创建管理员。";
  }
  return "Wrangler D1 写入失败。请确认迁移已应用、Cloudflare 登录有效且目标数据库可用。";
}

try {
  if (process.platform === "win32") {
    throw new Error("原生 Windows 不支持安全启动 Wrangler；请使用 WSL、Linux 或 macOS。");
  }

  const { username, local } = parseArguments(process.argv.slice(2));
  const password = readPassword();
  const result = runWrangler(buildInsertSql(username, password), local);

  if (result.status !== 0) {
    throw new Error(describeWranglerFailure(result));
  }

  console.log(`平台管理员初始化成功（${local ? "本地 D1" : "远程 D1"}）。`);
} catch (error) {
  console.error(error instanceof Error ? error.message : "平台管理员初始化失败。");
  process.exitCode = 1;
}
