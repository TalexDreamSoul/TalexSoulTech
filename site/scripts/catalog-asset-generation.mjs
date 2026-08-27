import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { BASELINE_RUNTIME_ITEMS } from "./runtime-baseline.mjs";

const EXPECTED_COUNTS = Object.freeze({
  catalog: 810,
  baseline: 150,
  legacyMappings: 34,
  newRegistrations: 776,
  runtimeTotal: 926,
  families: 270,
  familyKinds: 15,
});

const MATERIALS = new Set([
  "PAPER",
  "STONE",
  "GLASS_BOTTLE",
  "WHEAT_SEEDS",
  "IRON_CHESTPLATE",
  "IRON_BLOCK",
  "REDSTONE",
  "AMETHYST_SHARD",
  "ENDER_PEARL",
  "BRICKS",
  "GLASS",
  "EMERALD",
  "LECTERN",
  "CAULDRON",
  "BEACON",
  "SMITHING_TABLE",
  "REDSTONE_BLOCK",
  "ENCHANTING_TABLE",
  "LODESTONE",
  "BARREL",
  "COPPER_BLOCK",
  "EMERALD_BLOCK",
]);

const FACILITY_BASE_MATERIALS = Object.freeze({
  station: "LECTERN",
  block: "IRON_BLOCK",
  vat: "CAULDRON",
  greenhouse: "GLASS",
  bastion: "BEACON",
  workstation: "SMITHING_TABLE",
  unit: "REDSTONE_BLOCK",
  array: "ENCHANTING_TABLE",
  gate: "LODESTONE",
  field: "IRON_BLOCK",
  relay: "BARREL",
  workshop: "BRICKS",
  network: "COPPER_BLOCK",
  exchange: "EMERALD_BLOCK",
});

const STACK_LIMIT_ONE_FORMS = new Set([
  "probe",
  "analyzer",
  "station",
  "wand",
  "array",
  "anchor",
  "gate",
  "gauntlet",
  "field",
  "tag",
  "sorter",
  "relay",
  "token",
  "contract",
  "exchange",
  "filter",
  "pump",
  "network",
  "armor",
  "bastion",
  "workstation",
  "workshop",
  "greenhouse",
  "vat",
  "unit",
  "drive",
  "cell",
  "core",
]);

const STACK_LIMIT_SIXTY_FOUR_FORMS = new Set([
  "seed",
  "culture",
  "rune",
  "reagent",
  "fragment",
  "alloy",
  "block",
  "brick",
  "frame",
  "plate",
  "part",
  "coil",
  "shard",
  "mass",
  "bit",
]);

const FAMILY_KINDS = new Set([
  "research",
  "resource",
  "processing",
  "plant",
  "defense",
  "machine",
  "energy",
  "magic",
  "space",
  "gravity",
  "logistics",
  "construction",
  "fluid",
  "commerce",
  "quantum",
]);

const KIND_GROUP_LABELS = Object.freeze({
  research: "研究与记录",
  resource: "资源与材料",
  processing: "处理与反应",
  plant: "种植与培育",
  defense: "防御与维护",
  machine: "机器与设施",
  energy: "能源与储存",
  magic: "魔法与意图",
  space: "空间与航路",
  gravity: "引力与场域",
  logistics: "物流与路由",
  construction: "建造与修复",
  fluid: "流体与来源",
  commerce: "交易与公共服务",
  quantum: "量子与回滚",
});

const KIND_PALETTE = Object.freeze({
  research: ["P", "T", "W"],
  resource: ["B", "C", "A"],
  processing: ["N", "T", "O"],
  plant: ["G", "Y", "W"],
  defense: ["R", "L", "S"],
  machine: ["D", "N", "O"],
  energy: ["T", "Y", "N"],
  magic: ["V", "P", "T"],
  space: ["V", "P", "W"],
  gravity: ["S", "N", "P"],
  logistics: ["A", "O", "W"],
  construction: ["C", "B", "L"],
  fluid: ["T", "N", "W"],
  commerce: ["G", "Y", "P"],
  quantum: ["V", "T", "W"],
});

const MARKER_PALETTE = Object.freeze([
  "K",
  "D",
  "S",
  "L",
  "T",
  "V",
  "P",
  "C",
  "B",
  "O",
  "A",
  "Y",
  "R",
  "G",
  "N",
  "W",
]);

const RUNTIME_ID_PATTERN = /^[a-z][a-z0-9_]+$/;
const MODEL_KEY_PATTERN = /^[a-z][a-z0-9_.-]*$/;
const SHA256_PATTERN = /^[0-9a-f]{64}$/;

const assert = (condition, message) => {
  if (!condition) throw new Error(`catalog asset generation invariant failed: ${message}`);
};

const sha256 = (bytes) => createHash("sha256").update(bytes).digest("hex");

function stableCompare(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function readSelector(modelKey) {
  assert(typeof modelKey === "string" && MODEL_KEY_PATTERN.test(modelKey), `invalid modelKey ${modelKey}`);
  return modelKey;
}

function validateStackPolicy(entry) {
  const expected = STACK_LIMIT_ONE_FORMS.has(entry.form)
    ? 1
    : STACK_LIMIT_SIXTY_FOUR_FORMS.has(entry.form)
      ? 64
      : null;
  assert(expected !== null, `${entry.planningId} has unsupported form ${entry.form}`);
  assert(entry.stackLimit === expected, `${entry.planningId} stackLimit must be ${expected}`);
}

function validateManifestShape(manifest) {
  assert(manifest && typeof manifest === "object", "manifest is an object");
  assert(manifest.schemaVersion === 1, "manifest schemaVersion is 1");
  assert(SHA256_PATTERN.test(manifest.authoringHash ?? ""), "manifest authoringHash is sha256");
  assert(Array.isArray(manifest.entries), "manifest entries are an array");
  for (const [key, expected] of Object.entries(EXPECTED_COUNTS)) {
    assert(manifest.counts?.[key] === expected, `manifest counts.${key} must be ${expected}`);
  }
  assert(manifest.entries.length === EXPECTED_COUNTS.catalog, "manifest has 810 entries");
  assert(BASELINE_RUNTIME_ITEMS.length === EXPECTED_COUNTS.baseline, "frozen baseline has 150 records");
  const baselineIds = new Set();
  for (const [runtimeId, name] of BASELINE_RUNTIME_ITEMS) {
    assert(RUNTIME_ID_PATTERN.test(runtimeId), `baseline runtime ID ${runtimeId} is valid`);
    assert(typeof name === "string" && name.length > 0, `baseline ${runtimeId} has a display name`);
    assert(!baselineIds.has(runtimeId), `baseline runtime ID ${runtimeId} is unique`);
    baselineIds.add(runtimeId);
  }

  const planningIds = new Set();
  const runtimeIds = new Set();
  const families = new Set();
  const kinds = new Set();
  const legacyRuntimeIds = new Set();
  let newCount = 0;
  let legacyCount = 0;
  for (const entry of manifest.entries) {
    assert(entry && typeof entry === "object", "manifest entry is an object");
    assert(typeof entry.planningId === "string" && entry.planningId.length > 0, "planningId is present");
    assert(!planningIds.has(entry.planningId), `planning ID ${entry.planningId} is unique`);
    planningIds.add(entry.planningId);
    assert(RUNTIME_ID_PATTERN.test(entry.runtimeId), `${entry.planningId} runtimeId is valid`);
    assert(!runtimeIds.has(entry.runtimeId), `runtime ID ${entry.runtimeId} is unique`);
    runtimeIds.add(entry.runtimeId);
    assert(MATERIALS.has(entry.baseMaterial), `${entry.planningId} base material ${entry.baseMaterial} is valid`);
    assert(entry.modelKey === `talexsoultech.item.${entry.runtimeId}`, `${entry.planningId} modelKey follows runtime ID`);
    readSelector(entry.modelKey);
    assert(FAMILY_KINDS.has(entry.familyKind), `${entry.planningId} familyKind is supported`);
    kinds.add(entry.familyKind);
    families.add(entry.familyKey);
    validateStackPolicy(entry);
    if (entry.newRegistration) {
      newCount += 1;
      assert(entry.legacyRuntimeId === null, `${entry.planningId} new row has no legacy ID`);
      assert(!baselineIds.has(entry.runtimeId), `${entry.planningId} new runtime ID does not collide with baseline`);
    } else {
      legacyCount += 1;
      assert(typeof entry.legacyRuntimeId === "string", `${entry.planningId} has a legacy runtime ID`);
      assert(entry.legacyRuntimeId === entry.runtimeId, `${entry.planningId} preserves legacy runtime ID`);
      assert(baselineIds.has(entry.runtimeId), `${entry.planningId} legacy ID belongs to baseline`);
      assert(!legacyRuntimeIds.has(entry.runtimeId), `${entry.runtimeId} has one legacy mapping`);
      legacyRuntimeIds.add(entry.runtimeId);
    }
    if (entry.facility !== null) {
      assert(entry.facility && entry.facility.form === entry.form, `${entry.planningId} facility form matches entry`);
      assert(FACILITY_BASE_MATERIALS[entry.form] === entry.baseMaterial, `${entry.planningId} facility material is placeable`);
      assert(["SINGLE", "THREE_BY_THREE", "FIVE_BY_FIVE"].includes(entry.facility.footprint), `${entry.planningId} facility footprint is valid`);
      assert(Number.isInteger(entry.facility.ports) && entry.facility.ports >= 0, `${entry.planningId} facility ports are bounded`);
      assert(entry.facility.operation && Number.isInteger(entry.facility.operation.intervalTicks) && entry.facility.operation.intervalTicks > 0, `${entry.planningId} facility interval is bounded`);
      assert(Number.isInteger(entry.facility.operation.maxBatch) && entry.facility.operation.maxBatch > 0, `${entry.planningId} facility batch is bounded`);
    }
  }
  assert(newCount === EXPECTED_COUNTS.newRegistrations, "manifest has 776 new registrations");
  assert(legacyCount === EXPECTED_COUNTS.legacyMappings, "manifest has 34 legacy mappings");
  assert(families.size === EXPECTED_COUNTS.families, "manifest has 270 families");
  assert(kinds.size === EXPECTED_COUNTS.familyKinds, "manifest has 15 family kinds");
  assert(runtimeIds.size === EXPECTED_COUNTS.catalog, "manifest runtime IDs are globally unique");
  assert(BASELINE_RUNTIME_ITEMS.length + newCount === EXPECTED_COUNTS.runtimeTotal, "baseline plus new runtime count is 926");
  return { baselineIds, planningIds, runtimeIds, legacyRuntimeIds };
}

export async function loadAssetManifest(manifestPath) {
  const bytes = await readFile(manifestPath);
  let manifest;
  try {
    manifest = JSON.parse(bytes.toString("utf8"));
  } catch (error) {
    throw new Error(`catalog runtime manifest JSON is invalid: ${error.message}`);
  }
  const indexes = validateManifestShape(manifest);
  return Object.freeze({
    manifest,
    bytes,
    sourceManifestHash: sha256(bytes),
    ...indexes,
  });
}

function setPixel(grid, x, y, value) {
  if (x >= 0 && x < 16 && y >= 0 && y < 16) grid[y][x] = value;
}

function rect(grid, x1, y1, x2, y2, value) {
  for (let y = y1; y <= y2; y += 1) {
    for (let x = x1; x <= x2; x += 1) setPixel(grid, x, y, value);
  }
}

function outlineRect(grid, x1, y1, x2, y2, fill) {
  rect(grid, x1, y1, x2, y2, "K");
  rect(grid, x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
}

function drawFamilySilhouette(grid, entry, colors, digest) {
  const [primary, accent, highlight] = colors;
  switch (entry.familyKind) {
    case "research":
      outlineRect(grid, 3, 3, 12, 12, primary);
      rect(grid, 5, 5, 10, 6, highlight);
      rect(grid, 5, 8, 10, 8, accent);
      rect(grid, 5, 10, 8, 10, "W");
      break;
    case "resource":
      rect(grid, 4, 6, 11, 11, "K");
      rect(grid, 5, 5, 10, 10, primary);
      rect(grid, 6, 4, 9, 9, accent);
      rect(grid, 7, 3, 8, 4, highlight);
      break;
    case "processing":
      outlineRect(grid, 3, 5, 12, 12, primary);
      rect(grid, 5, 3, 10, 5, highlight);
      rect(grid, 6, 7, 9, 10, accent);
      rect(grid, 4, 12, 5, 13, "L");
      rect(grid, 10, 12, 11, 13, "L");
      break;
    case "plant":
      rect(grid, 6, 9, 9, 12, primary);
      rect(grid, 7, 7, 8, 9, accent);
      setPixel(grid, 5, 8, highlight);
      setPixel(grid, 6, 7, highlight);
      setPixel(grid, 9, 8, highlight);
      setPixel(grid, 8, 6, "G");
      setPixel(grid, 8, 5, "Y");
      break;
    case "defense":
      rect(grid, 4, 3, 11, 5, "K");
      rect(grid, 3, 5, 12, 8, "K");
      rect(grid, 5, 5, 10, 11, primary);
      rect(grid, 6, 7, 9, 10, accent);
      setPixel(grid, 7, 6, highlight);
      setPixel(grid, 8, 6, highlight);
      break;
    case "machine":
      outlineRect(grid, 2, 4, 13, 13, primary);
      rect(grid, 4, 5, 11, 6, highlight);
      outlineRect(grid, 5, 7, 10, 11, "N");
      rect(grid, 6, 8, 9, 10, accent);
      setPixel(grid, 3 + (digest[0] % 10), 12, "L");
      break;
    case "energy":
      outlineRect(grid, 5, 2, 10, 13, primary);
      rect(grid, 6, 4, 9, 11, accent);
      rect(grid, 7, 3, 8, 4, highlight);
      setPixel(grid, 6, 12, "S");
      setPixel(grid, 9, 12, "S");
      break;
    case "magic":
      for (let offset = 0; offset < 5; offset += 1) {
        rect(grid, 7 - offset, 3 + offset, 8 + offset, 12 - offset, "K");
      }
      rect(grid, 5, 5, 10, 10, primary);
      rect(grid, 7, 6, 8, 9, accent);
      setPixel(grid, 4, 7, highlight);
      setPixel(grid, 11, 8, highlight);
      break;
    case "space":
      for (let offset = 0; offset < 5; offset += 1) {
        rect(grid, 7 - offset, 3 + offset, 8 + offset, 12 - offset, "K");
      }
      rect(grid, 6, 5, 9, 10, primary);
      rect(grid, 7, 6, 8, 9, accent);
      setPixel(grid, 4, 6, highlight);
      setPixel(grid, 11, 10, highlight);
      break;
    case "gravity":
      for (let y = 4; y <= 11; y += 1) {
        const width = y <= 7 ? y - 3 : 12 - y;
        rect(grid, 8 - width, y, 7 + width, y, "K");
      }
      rect(grid, 6, 6, 9, 9, primary);
      rect(grid, 7, 7, 8, 8, accent);
      setPixel(grid, 5, 10, highlight);
      setPixel(grid, 10, 5, highlight);
      break;
    case "logistics":
      rect(grid, 3, 6, 12, 9, "K");
      rect(grid, 4, 7, 11, 8, primary);
      rect(grid, 5, 4, 7, 6, accent);
      rect(grid, 8, 9, 10, 11, highlight);
      setPixel(grid, 12, 7, "W");
      break;
    case "construction":
      outlineRect(grid, 3, 3, 12, 12, primary);
      rect(grid, 5, 5, 10, 10, accent);
      rect(grid, 6, 6, 9, 9, highlight);
      setPixel(grid, 4, 4, "L");
      setPixel(grid, 11, 11, "L");
      break;
    case "fluid":
      setPixel(grid, 8, 2, "K");
      for (let y = 3; y <= 11; y += 1) {
        const width = y < 7 ? y - 2 : 12 - y;
        rect(grid, 8 - width, y, 8 + width, y, "K");
      }
      rect(grid, 6, 6, 10, 10, primary);
      rect(grid, 7, 7, 9, 9, accent);
      setPixel(grid, 8, 8, highlight);
      break;
    case "commerce":
      outlineRect(grid, 3, 3, 12, 12, primary);
      rect(grid, 5, 5, 10, 10, accent);
      rect(grid, 6, 6, 9, 9, highlight);
      setPixel(grid, 7, 7, "G");
      setPixel(grid, 8, 8, "Y");
      break;
    case "quantum":
      for (let offset = 0; offset < 6; offset += 1) {
        setPixel(grid, 8, 2 + offset, "K");
        setPixel(grid, 8, 13 - offset, "K");
        setPixel(grid, 2 + offset, 8, "K");
        setPixel(grid, 13 - offset, 8, "K");
      }
      rect(grid, 6, 6, 9, 9, primary);
      rect(grid, 7, 7, 8, 8, accent);
      setPixel(grid, 8, 7, highlight);
      break;
    default:
      throw new Error(`unsupported family kind ${entry.familyKind}`);
  }
}

export function createManifestPixels(entry) {
  assert(entry && typeof entry.runtimeId === "string", "texture entry has runtimeId");
  const digest = createHash("sha256")
    .update(`${entry.planningId}|${entry.runtimeId}|${entry.familyKey}|${entry.familyKind}|${entry.tier}`)
    .digest();
  const palette = KIND_PALETTE[entry.familyKind];
  assert(palette, `${entry.planningId} has a texture palette`);
  const tierIndex = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"].indexOf(entry.tier);
  const colors = [palette[(tierIndex + digest[0]) % palette.length], palette[(tierIndex + 1 + digest[1]) % palette.length], palette[(tierIndex + 2 + digest[2]) % palette.length]];
  const grid = Array.from({ length: 16 }, () => Array(16).fill("."));
  drawFamilySilhouette(grid, entry, colors, digest);
  // Encode the identity digest in two visible marker rows. This prevents same-family
  // textures from collapsing while retaining a real silhouette and transparent border.
  for (let index = 0; index < 32; index += 1) {
    const byte = digest[index >> 1];
    const nibble = (index & 1) === 0 ? byte >> 4 : byte & 0x0f;
    setPixel(grid, index % 16, 14 + Math.floor(index / 16), MARKER_PALETTE[nibble]);
  }
  const pixels = grid.map((row) => row.join(""));
  assert(pixels.length === 16 && pixels.every((row) => row.length === 16), `${entry.planningId} texture is 16x16`);
  assert(new Set(pixels.join("")).size >= 4, `${entry.planningId} texture is not a solid/blank placeholder`);
  return pixels;
}

export function createGeneratedModels(manifest) {
  const models = manifest.entries
    .filter((entry) => entry.newRegistration)
    .map((entry) => ({
      id: entry.runtimeId,
      runtimeId: entry.runtimeId,
      planningId: entry.planningId,
      baseMaterial: entry.baseMaterial.toLowerCase(),
      selector: readSelector(entry.modelKey),
      pixels: createManifestPixels(entry),
      generated: true,
      legacy: false,
      familyKind: entry.familyKind,
      tier: entry.tier,
      fallback: entry.baseMaterial.toLowerCase(),
    }))
    .sort((left, right) => stableCompare(left.id, right.id));
  assert(models.length === EXPECTED_COUNTS.newRegistrations, "generated model count is 776");
  return models;
}

function baselineClassification(id) {
  if (id.endsWith("charge_pad") || id.endsWith("charge_beacon") || id.endsWith("charge_pylon")) return "无线充电多方块";
  if (/^(powered_|electric_|advanced_|compact_|elite_|energy_|induction_|kinetic_|magnetic_|personal_|pocket_|quantum_|repair_|resin_tapper|field_|mining_|ore_scanner|crop_harvester|lumber_axe|plasma_cutter|arc_welder|capacitor_|wireless_charge_receiver|universal_matter_tool|excavation_hammer|precision_drill|mob_stunner|jetpack|gravitic_harness|shield_chestplate|scout_helmet|servo_leggings|powered_boots|magnetic_boots)/.test(id)) return "便携电力装备";
  if (["copper_box", "iron_box", "void_box"].includes(id)) return "受保护存储箱";
  if (/industry_|magic_(resonance_array|void_distiller|elemental_infusion_altar|astral_loom|echo_gate)|space_(item_router|compressor)|folded_storage_core|phase_transmitter|dimensional_anchor|gravity_(attractor|repulsor|separator)|item_accretion_machine|singularity_compressor/.test(id)) return "供电多方块机器";
  if (/machine|generator|storage|maker|extractor|workbench|compressor/.test(id)) return "旧式机器与设施";
  return "材料、工具与功能物品";
}

function runtimeRowFromManifest(entry, baselineName) {
  const kindGroup = KIND_GROUP_LABELS[entry.familyKind];
  return {
    id: entry.runtimeId,
    runtimeId: entry.runtimeId,
    name: baselineName ?? entry.name,
    group: kindGroup,
    kindGroup,
    familyKind: entry.familyKind,
    kind: entry.familyKind,
    planningId: entry.planningId,
    wave: entry.wave,
    discipline: entry.discipline,
    family: entry.family,
    familyId: entry.familyId,
    tier: entry.tier,
    generated: entry.newRegistration,
    legacy: !entry.newRegistration,
    status: entry.newRegistration ? "generated" : "legacy",
    source: entry.newRegistration ? "catalog-runtime" : "baseline-runtime",
  };
}

export function buildRuntimeCatalogSource({ manifest, jarSha256, observedAt, sourceManifestHash }) {
  assert(SHA256_PATTERN.test(jarSha256 ?? ""), "JAR SHA-256 is valid");
  assert(SHA256_PATTERN.test(sourceManifestHash ?? ""), "source manifest hash is valid");
  const baselineById = new Map(BASELINE_RUNTIME_ITEMS.map(([id, name]) => [id, name]));
  const manifestByRuntimeId = new Map(manifest.entries.map((entry) => [entry.runtimeId, entry]));
  const rows = BASELINE_RUNTIME_ITEMS.map(([id, name]) => {
    const entry = manifestByRuntimeId.get(id);
    return entry
      ? runtimeRowFromManifest(entry, name)
      : {
          id,
          runtimeId: id,
          name,
          group: baselineClassification(id),
          kindGroup: null,
          kind: null,
          generated: false,
          legacy: true,
          status: "legacy",
          source: "baseline-runtime",
        };
  });
  for (const entry of manifest.entries.filter((candidate) => candidate.newRegistration)) {
    assert(!baselineById.has(entry.runtimeId), `${entry.runtimeId} generated runtime row does not collide with baseline`);
    rows.push(runtimeRowFromManifest(entry));
  }
  rows.sort((left, right) => stableCompare(left.runtimeId, right.runtimeId));
  assert(rows.length === EXPECTED_COUNTS.runtimeTotal, "runtime catalog has 926 records");
  assert(new Set(rows.map((row) => row.runtimeId)).size === EXPECTED_COUNTS.runtimeTotal, "runtime catalog IDs are unique");
  assert(rows.filter((row) => row.generated).length === EXPECTED_COUNTS.newRegistrations, "runtime catalog has 776 generated records");
  assert(rows.filter((row) => row.legacy).length === EXPECTED_COUNTS.baseline, "runtime catalog preserves 150 baseline records");
  const groups = [...new Set(rows.map((row) => row.group))].sort(stableCompare);
  const release = {
    version: "3.0.0-SNAPSHOT",
    jarSha256,
    sourceManifestHash,
    observedAt,
    itemCount: rows.length,
    catalogCount: EXPECTED_COUNTS.catalog,
    baselineCount: EXPECTED_COUNTS.baseline,
    legacyMappingCount: EXPECTED_COUNTS.legacyMappings,
    newRegistrationCount: EXPECTED_COUNTS.newRegistrations,
    electricalEntryCount: 50,
  };
  return `// Generated by site/scripts/prepare-assets.mjs from catalog-runtime.json; do not edit.\nconst RAW_RUNTIME_ITEMS = ${JSON.stringify(rows, null, 2)};\n\nexport const RUNTIME_RELEASE = Object.freeze(${JSON.stringify(release, null, 2)});\n\nexport const RUNTIME_ITEMS = Object.freeze(RAW_RUNTIME_ITEMS.map((item) => Object.freeze(item)));\n\nexport const RUNTIME_GROUPS = Object.freeze(${JSON.stringify(groups, null, 2)});\n\nif (RUNTIME_ITEMS.length !== RUNTIME_RELEASE.itemCount || new Set(RUNTIME_ITEMS.map((item) => item.runtimeId)).size !== RUNTIME_RELEASE.itemCount || RUNTIME_ITEMS.some((item) => /[\\u0000-\\u001f\\u007f]/.test(item.name))) {\n  throw new Error("runtime catalog must contain exactly 926 unique registered item IDs");\n}\n`;
}

export function validateAssetModels({ manifest, existingModels, generatedModels }) {
  const allModels = [...existingModels, ...generatedModels];
  const modelIds = new Set();
  const selectors = new Set();
  for (const model of allModels) {
    assert(!modelIds.has(model.id), `duplicate model ID ${model.id}`);
    modelIds.add(model.id);
    assert(typeof model.selector === "string" && model.selector.length > 0, `${model.id} selector is present`);
    assert(!selectors.has(model.selector), `duplicate selector ${model.selector}`);
    selectors.add(model.selector);
    if (model.generated) {
      assert(MATERIALS.has(model.baseMaterial.toUpperCase()), `${model.id} generated base material is known`);
    } else {
      assert(/^[a-z][a-z0-9_]+$/.test(model.baseMaterial), `${model.id} preserved base material is valid`);
    }
    assert(Array.isArray(model.pixels) && model.pixels.length === 16 && model.pixels.every((row) => row.length === 16), `${model.id} texture is 16x16`);
  }
  const generatedById = new Map(generatedModels.map((model) => [model.id, model]));
  const newEntries = manifest.entries.filter((entry) => entry.newRegistration);
  assert(generatedById.size === EXPECTED_COUNTS.newRegistrations, "generated IDs are unique");
  for (const entry of newEntries) {
    const model = generatedById.get(entry.runtimeId);
    assert(model, `${entry.runtimeId} has a generated model`);
    assert(model.selector === entry.modelKey, `${entry.runtimeId} selector matches modelKey`);
    assert(model.baseMaterial.toUpperCase() === entry.baseMaterial, `${entry.runtimeId} selector uses manifest base material`);
  }
  return { allModels, modelIds, selectors };
}

export function expectedFallbackKind(baseMaterial) {
  const blockMaterials = new Set([
    "barrel",
    "beacon",
    "bricks",
    "cauldron",
    "copper_block",
    "emerald_block",
    "enchanting_table",
    "glass",
    "iron_block",
    "lectern",
    "lodestone",
    "redstone_block",
    "smithing_table",
    "stone",
  ]);
  return blockMaterials.has(baseMaterial) ? "block" : "item";
}

export const ASSET_GENERATION_COUNTS = EXPECTED_COUNTS;
export const BASELINE_RUNTIME_COUNT = BASELINE_RUNTIME_ITEMS.length;
export const KIND_GROUPS = KIND_GROUP_LABELS;
