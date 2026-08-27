import { createHash } from "node:crypto";
import { copyFile, mkdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";
import {
  ASSET_GENERATION_COUNTS,
  BASELINE_RUNTIME_COUNT,
  buildRuntimeCatalogSource,
  createGeneratedModels,
  expectedFallbackKind,
  loadAssetManifest,
  validateAssetModels,
} from "./catalog-asset-generation.mjs";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const siteDirectory = resolve(scriptDirectory, "..");
const source = resolve(siteDirectory, "..", "target", "talex-soul-tech-3.0.0-SNAPSHOT.jar");
const downloadsDirectory = resolve(siteDirectory, "public", "downloads");
const filename = "TalexSoulTech-3.0.0-SNAPSHOT.jar";
const destination = resolve(downloadsDirectory, filename);
const runtimeCatalogDestination = resolve(siteDirectory, "public", "data", "runtime-catalog.js");
const runtimeManifestSource = resolve(
  siteDirectory,
  "..",
  "src",
  "main",
  "resources",
  "talexsoultech",
  "content",
  "catalog-runtime.json",
);

const resourcePackVersion = "26.1.2";
const resourcePackFormat = 84;
const resourcePackName = `TalexSoulTech-${resourcePackVersion}-resource-pack.zip`;
const resourcePackSourceDirectory = resolve(siteDirectory, "public", "assets", "talexsoultech-resource-pack");
const resourcePackDestination = resolve(siteDirectory, "public", "assets", resourcePackName);
const resourcePackManifestDestination = resolve(siteDirectory, "public", "assets", "TalexSoulTech-resource-pack.manifest.json");
const poweredEquipmentCatalogSource = resolve(
  siteDirectory,
  "..",
  "src",
  "main",
  "java",
  "pubsher",
  "talexsoultech",
  "talex",
  "items",
  "equipment",
  "ElectricalEquipmentCatalog.java",
);

const palette = {
  ".": [0, 0, 0, 0],
  K: [13, 18, 29, 255],
  D: [35, 45, 58, 255],
  S: [82, 102, 119, 255],
  L: [188, 204, 214, 255],
  T: [39, 209, 196, 255],
  V: [123, 92, 246, 255],
  P: [184, 164, 255, 255],
  C: [170, 104, 72, 255],
  B: [105, 60, 35, 255],
  O: [239, 133, 44, 255],
  A: [218, 151, 56, 255],
  Y: [255, 211, 85, 255],
  R: [183, 57, 43, 255],
  G: [66, 190, 111, 255],
  N: [37, 76, 126, 255],
  W: [238, 245, 247, 255],
};

const baseCustomModels = [
  {
    id: "guide_book",
    baseMaterial: "book",
    selector: "talexsoultech:guide_book",
    pixels: [
      "................",
      ".....KKKKKK.....",
      "....KDDDDDDK....",
      "...VKDDDDDDLK...",
      "...VKDLLLLLKL...",
      "...VKDLLLLLKL...",
      "...VKDLLOOLKL...",
      "...VKDLLTOLKL...",
      "...VKDLLTOLKL...",
      "...VKDLLLLLKL...",
      "...VKDLLLLLLKK..",
      "....KDLLLLLLKK..",
      "....KDDDDDDLK...",
      ".....KKKKKKK....",
      "................",
      "................",
    ],
  },
  {
    id: "guider_book",
    baseMaterial: "book",
    selector: "talexsoultech:guider_book",
    pixels: [
      "................",
      "....KKKKKKK.....",
      "...KPVVVVVK.....",
      "..PKVVVVVVLLK...",
      "..PKVLLLLLLKL...",
      "..PKVLLLLLLKL...",
      "..PKVLLTCTKLL...",
      "..PKVLLCCCKLL...",
      "..PKVLLTCTKLL...",
      "..PKVLLLLLLKL...",
      "..PKVLLLLLLKKL..",
      "...KVVVVVVLLKK..",
      "....KKKKKKKKKK..",
      ".....KKKKKKKK...",
      "................",
      "................",
    ],
  },
  {
    id: "copper_box",
    baseMaterial: "chest",
    selector: "talexsoultech:copper_box",
    pixels: [
      "................",
      ".....KKKKKK.....",
      "....KCCCCCCK....",
      "...KCOOOOOOCK...",
      "...KCCCCCCCCK...",
      "..KCSDDDDDDSCK..",
      "..KCDDDDDDDDCK..",
      "..KCDDDTDDDDCK..",
      "..KCDDDVDDDDCK..",
      "..KCDDDDDDDDCK..",
      "..KCSDDDDDDSCK..",
      "...KCCCCCCCCK...",
      "....KCCCCCCK....",
      ".....KKKKKK.....",
      "................",
      "................",
    ],
  },
  {
    id: "iron_box",
    baseMaterial: "trapped_chest",
    selector: "talexsoultech:iron_box",
    pixels: [
      "................",
      ".....KKKKKK.....",
      "....KSSSSSSK....",
      "...KSLLLLLLSK...",
      "...KSSSSSSSSK...",
      "..KSLDDDDDDLSK..",
      "..KSDDDDDDDDSK..",
      "..KSDDDLTVDDSK..",
      "..KSDDDLTVDDSK..",
      "..KSDDDDDDDDSK..",
      "..KSLDDDDDDLSK..",
      "...KSSSSSSSSK...",
      "...KSLLLLLLSK...",
      "....KSSSSSSK....",
      ".....KKKKKK.....",
      "................",
    ],
  },
  {
    id: "void_box",
    baseMaterial: "barrel",
    selector: "talexsoultech:void_box",
    pixels: [
      "................",
      ".....KKKKKK.....",
      "....KDVVVVDK....",
      "...KDVPPPPVDK...",
      "...KDVVVVVVDK...",
      "..KDVPDDDDPVDK..",
      "..KDVDDDDDDVDK..",
      "..KDVDDTTVVDDK..",
      "..KDVDDTTVVDDK..",
      "..KDVDDDDDDVDK..",
      "..KDVPDDDDPVDK..",
      "...KDVVVVVVDK...",
      "...KDVPPPPVDK...",
      "....KDVVVVDK....",
      ".....KKKKKK.....",
      "................",
    ],
  },
  {
    id: "fire_ingot",
    baseMaterial: "nether_brick",
    selector: "talexsoultech:fire_ingot",
    pixels: [
      "................",
      "................",
      ".....KKKKKK.....",
      "....KROOOORK....",
      "...KROOOOOORK...",
      "..KROYYYYYYORK..",
      "..KROYYTVYYORK..",
      "..KROYYYYYYORK..",
      "...KROOOOOORK...",
      "....KROOOORK....",
      ".....KRRRRK.....",
      "......KBBK......",
      "................",
      "................",
      "................",
      "................",
    ],
  },
  {
    id: "space_dust",
    baseMaterial: "glowstone_dust",
    selector: "talexsoultech:space_dust",
    pixels: [
      "................",
      ".......KK.......",
      "......KPVK......",
      ".....KPVVPK.....",
      "....KPVVVTPK....",
      "...KPVTTTTVPK...",
      "...KPVTTTTVPK...",
      "....KPVVVTPK....",
      ".....KPVVPK.....",
      "......KPVK......",
      ".......KK.......",
      "................",
      "................",
      "................",
      "................",
      "................",
    ],
  },
  {
    id: "resin",
    baseMaterial: "slime_ball",
    selector: "talexsoultech:resin",
    pixels: [
      "................",
      "................",
      "........B.......",
      ".......BAB......",
      "......BAAAB.....",
      ".....BBOAAYB....",
      "....BBAOYYYYB...",
      "....BAOYYYYYB...",
      ".....BAOYYYYB...",
      "......BAYYYB....",
      ".......BYYB.....",
      "........BB......",
      "................",
      "................",
      "................",
      "................",
    ],
  },
];

function equipmentKind(id, machine) {
  if (machine) return "machine";
  if (id.includes("battery")) return "battery";
  if (/backpack|chestplate|leggings|boots|helmet|jetpack|harness/.test(id)) return "armor";
  if (/charger|receiver|generator|recall|scanner|analyzer|collector|flashlight/.test(id)) return "device";
  return "tool";
}

function createEquipmentPixels(id, tier, machine) {
  const grid = Array.from({ length: 16 }, () => Array(16).fill("."));
  const digest = createHash("sha256").update(id).digest();
  const tierColors = ["C", "G", "T", "V", "P"];
  const accents = ["Y", "O", "T", "G", "P", "R", "W", "N"];
  const base = tierColors[tier - 1];
  const accent = accents[digest[0] % accents.length];
  const secondary = accents[digest[1] % accents.length];
  const set = (x, y, value) => {
    if (x >= 0 && x < 16 && y >= 0 && y < 16) grid[y][x] = value;
  };
  const rect = (x1, y1, x2, y2, value) => {
    for (let y = y1; y <= y2; y += 1) {
      for (let x = x1; x <= x2; x += 1) set(x, y, value);
    }
  };
  const outlineRect = (x1, y1, x2, y2, fill) => {
    rect(x1, y1, x2, y2, "K");
    rect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
  };

  switch (equipmentKind(id, machine)) {
    case "battery":
      outlineRect(5, 2, 10, 13, "D");
      rect(7, 1, 8, 1, "L");
      rect(6, 4, 9, 11, base);
      rect(7, 5, 8, 10, accent);
      set(6, 12, "S");
      set(9, 12, "S");
      break;
    case "armor":
      rect(4, 3, 11, 4, "K");
      rect(3, 4, 12, 6, "K");
      rect(5, 5, 10, 12, "K");
      rect(4, 6, 11, 9, base);
      rect(6, 5, 9, 11, "D");
      rect(7, 6, 8, 9, accent);
      set(5, 10, secondary);
      set(10, 10, secondary);
      break;
    case "machine":
      outlineRect(2, 4, 13, 13, "D");
      rect(3, 5, 12, 7, base);
      outlineRect(5, 7, 10, 12, "N");
      rect(6, 8, 9, 10, accent);
      set(3, 12, "L");
      set(12, 12, "L");
      rect(5, 2, 10, 3, "K");
      rect(6, 2, 9, 2, secondary);
      break;
    case "device":
      for (let offset = 0; offset < 5; offset += 1) {
        rect(7 - offset, 3 + offset, 8 + offset, 12 - offset, "K");
      }
      for (let offset = 0; offset < 3; offset += 1) {
        rect(7 - offset, 5 + offset, 8 + offset, 10 - offset, base);
      }
      rect(7, 6, 8, 9, accent);
      set(4, 7, secondary);
      set(11, 8, secondary);
      break;
    default:
      for (let step = 0; step < 9; step += 1) {
        set(3 + step, 13 - step, "K");
        if (step < 8) set(3 + step, 12 - step, "B");
      }
      outlineRect(8, 2, 13, 6, base);
      rect(9, 3, 12, 4, accent);
      set(4, 11, secondary);
      set(5, 12, secondary);
      break;
  }

  for (let bit = 0; bit < 10; bit += 1) {
    const value = (digest[2 + (bit >> 3)] >> (bit & 7)) & 1;
    set(3 + bit, 14, value === 1 ? accent : "S");
  }
  return grid.map((row) => row.join(""));
}

async function loadEquipmentCustomModels() {
  const source = await readFile(poweredEquipmentCatalogSource, "utf8");
  const portablePattern = /item\(\s*"([a-z0-9_]+)"\s*,\s*"[^"]*"\s*,\s*Material\.([A-Z0-9_]+)\s*,\s*([1-5])/g;
  const chargerPattern = /charger\(\s*"([a-z0-9_]+)"\s*,\s*"[^"]*"\s*,\s*Material\.([A-Z0-9_]+)\s*,\s*([1-5])/g;
  const portable = [...source.matchAll(portablePattern)].map((match) => ({
    id: match[1],
    baseMaterial: match[2].toLowerCase(),
    tier: Number(match[3]),
    machine: false,
  }));
  const chargers = [...source.matchAll(chargerPattern)].map((match) => ({
    id: match[1],
    baseMaterial: match[2].toLowerCase(),
    tier: Number(match[3]),
    machine: true,
  }));
  if (portable.length !== 47 || chargers.length !== 3) {
    throw new Error(`电力装备材质目录必须为 47+3，当前 ${portable.length}+${chargers.length}`);
  }
  const definitions = [...portable, ...chargers];
  if (new Set(definitions.map((definition) => definition.id)).size !== 50) {
    throw new Error("电力装备材质 ID 必须全局唯一");
  }
  return definitions.map((definition) => ({
    id: definition.id,
    baseMaterial: definition.baseMaterial,
    selector: `talexsoultech:${definition.id}`,
    pixels: createEquipmentPixels(definition.id, definition.tier, definition.machine),
  }));
}

const equipmentCustomModels = await loadEquipmentCustomModels();
const runtimeManifest = await loadAssetManifest(runtimeManifestSource);
const generatedCustomModels = createGeneratedModels(runtimeManifest.manifest);
const preservedCustomModels = [...baseCustomModels, ...equipmentCustomModels].map((model) => ({
  ...model,
  generated: false,
  legacy: true,
}));
const modelClosure = validateAssetModels({
  manifest: runtimeManifest.manifest,
  existingModels: preservedCustomModels,
  generatedModels: generatedCustomModels,
});
const customModels = modelClosure.allModels.sort((left, right) => (left.id < right.id ? -1 : left.id > right.id ? 1 : 0));

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ ((crc & 1) === 1 ? 0xedb88320 : 0);
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function pngChunk(type, content) {
  const typeBuffer = Buffer.from(type, "ascii");
  const header = Buffer.alloc(8);
  header.writeUInt32BE(content.length, 0);
  typeBuffer.copy(header, 4);
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE(crc32(Buffer.concat([typeBuffer, content])), 0);
  return Buffer.concat([header, content, checksum]);
}

function createPixelTexture(rows) {
  const width = 16;
  const height = 16;
  if (rows.length !== height || rows.some((row) => row.length !== width)) {
    throw new Error("物品像素纹理必须是 16×16");
  }

  const raw = Buffer.alloc(height * (1 + width * 4));
  let offset = 0;
  for (const row of rows) {
    raw[offset] = 0;
    offset += 1;
    for (const pixel of row) {
      const color = palette[pixel];
      if (!color) {
        throw new Error(`未知像素调色板字符: ${pixel}`);
      }
      raw.set(color, offset);
      offset += 4;
    }
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    pngChunk("IHDR", ihdr),
    pngChunk("IDAT", deflateSync(raw, { level: 9 })),
    pngChunk("IEND", Buffer.alloc(0)),
  ]);
}

function createZip(files) {
  const localRecords = [];
  const centralRecords = [];
  let offset = 0;

  for (const file of files) {
    const name = Buffer.from(file.path, "utf8");
    const checksum = crc32(file.content);
    const localHeader = Buffer.alloc(30);
    localHeader.writeUInt32LE(0x04034b50, 0);
    localHeader.writeUInt16LE(20, 4);
    localHeader.writeUInt16LE(0, 6);
    localHeader.writeUInt16LE(0, 8);
    localHeader.writeUInt16LE(0, 10);
    localHeader.writeUInt16LE(33, 12);
    localHeader.writeUInt32LE(checksum, 14);
    localHeader.writeUInt32LE(file.content.length, 18);
    localHeader.writeUInt32LE(file.content.length, 22);
    localHeader.writeUInt16LE(name.length, 26);
    localHeader.writeUInt16LE(0, 28);
    const localRecord = Buffer.concat([localHeader, name, file.content]);
    localRecords.push(localRecord);

    const centralHeader = Buffer.alloc(46);
    centralHeader.writeUInt32LE(0x02014b50, 0);
    centralHeader.writeUInt16LE(20, 4);
    centralHeader.writeUInt16LE(20, 6);
    centralHeader.writeUInt16LE(0, 8);
    centralHeader.writeUInt16LE(0, 10);
    centralHeader.writeUInt16LE(0, 12);
    centralHeader.writeUInt16LE(33, 14);
    centralHeader.writeUInt32LE(checksum, 16);
    centralHeader.writeUInt32LE(file.content.length, 20);
    centralHeader.writeUInt32LE(file.content.length, 24);
    centralHeader.writeUInt16LE(name.length, 28);
    centralHeader.writeUInt16LE(0, 30);
    centralHeader.writeUInt16LE(0, 32);
    centralHeader.writeUInt16LE(0, 34);
    centralHeader.writeUInt16LE(0, 36);
    centralHeader.writeUInt32LE(0, 38);
    centralHeader.writeUInt32LE(offset, 42);
    centralRecords.push(Buffer.concat([centralHeader, name]));
    offset += localRecord.length;
  }

  const centralDirectory = Buffer.concat(centralRecords);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(files.length, 8);
  end.writeUInt16LE(files.length, 10);
  end.writeUInt32LE(centralDirectory.length, 12);
  end.writeUInt32LE(offset, 16);
  end.writeUInt16LE(0, 20);
  return Buffer.concat([...localRecords, centralDirectory, end]);
}

function vanillaFallback(baseMaterial) {
  const model = (path) => ({ type: "minecraft:model", model: path });
  const explicitModels = {
    beacon: "minecraft:block/beacon",
    clock: "minecraft:item/clock_00",
    compass: "minecraft:item/compass_16",
    lightning_rod: "minecraft:block/lightning_rod",
    lodestone: "minecraft:block/lodestone",
    recovery_compass: "minecraft:item/recovery_compass_16",
    redstone_block: "minecraft:block/redstone_block",
    respawn_anchor: "minecraft:block/respawn_anchor_0",
  };
  if (explicitModels[baseMaterial]) return model(explicitModels[baseMaterial]);
  if (baseMaterial === "chest" || baseMaterial === "trapped_chest") {
    const texture = baseMaterial === "chest" ? "normal" : "trapped";
    return {
      type: "minecraft:select",
      cases: [
        {
          model: {
            type: "minecraft:special",
            base: `minecraft:item/${baseMaterial}`,
            model: { type: "minecraft:chest", texture: "christmas" },
          },
          when: ["12-24", "12-25", "12-26"],
        },
      ],
      fallback: {
        type: "minecraft:special",
        base: `minecraft:item/${baseMaterial}`,
        model: { type: "minecraft:chest", texture },
      },
      pattern: "MM-dd",
      property: "minecraft:local_time",
    };
  }
  const kind = expectedFallbackKind(baseMaterial);
  return model(`minecraft:${kind}/${baseMaterial}`);
}

function packJson(value) {
  return Buffer.from(`${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function fileHash(content) {
  return createHash("sha256").update(content).digest("hex");
}

async function prepareResourcePack() {
  await rm(resourcePackSourceDirectory, { recursive: true, force: true });

  const files = new Map();
  files.set(
    "pack.mcmeta",
    packJson({
      pack: {
        description: "TalexSoulTech 机械炼金物品模型",
        min_format: resourcePackFormat,
        max_format: resourcePackFormat,
      },
    }),
  );

  const byMaterial = new Map();
  const modelRecords = [];
  for (const customModel of customModels) {
    const itemDefinition = `assets/minecraft/items/${customModel.baseMaterial}.json`;
    const directItemDefinition = `assets/talexsoultech/items/${customModel.id}.json`;
    const modelPath = `assets/talexsoultech/models/item/${customModel.id}.json`;
    const texturePath = `assets/talexsoultech/textures/item/${customModel.id}.png`;
    const texture = createPixelTexture(customModel.pixels);
    files.set(
      modelPath,
      packJson({
        parent: "minecraft:item/generated",
        textures: { layer0: `talexsoultech:item/${customModel.id}` },
      }),
    );
    files.set(
      directItemDefinition,
      packJson({
        model: {
          type: "minecraft:model",
          model: `talexsoultech:item/${customModel.id}`,
        },
      }),
    );
    files.set(texturePath, texture);
    const materialModels = byMaterial.get(customModel.baseMaterial) ?? [];
    materialModels.push(customModel);
    byMaterial.set(customModel.baseMaterial, materialModels);
    modelRecords.push({
      runtimeId: customModel.runtimeId ?? customModel.id,
      planningId: customModel.planningId ?? null,
      selector: customModel.selector,
      baseMaterial: customModel.baseMaterial,
      generated: Boolean(customModel.generated),
      legacy: Boolean(customModel.legacy),
      itemDefinition,
      directItemDefinition,
      model: modelPath,
      texture: texturePath,
      textureSha256: fileHash(texture),
    });
  }

  const fallbackRecords = [];
  for (const baseMaterial of [...byMaterial.keys()].sort()) {
    const materialModels = byMaterial.get(baseMaterial).sort((left, right) => (left.selector < right.selector ? -1 : left.selector > right.selector ? 1 : 0));
    const fallback = vanillaFallback(baseMaterial);
    const fallbackText = JSON.stringify(fallback);
    if (!fallback || fallbackText.includes("undefined") || !fallbackText.includes("minecraft:")) {
      throw new Error(`missing safe vanilla fallback for ${baseMaterial}`);
    }
    const itemDefinition = `assets/minecraft/items/${baseMaterial}.json`;
    files.set(
      itemDefinition,
      packJson({
        model: {
          type: "minecraft:select",
          property: "minecraft:custom_model_data",
          index: 0,
          cases: materialModels.map((customModel) => ({
            when: customModel.selector,
            model: {
              type: "minecraft:model",
              model: `talexsoultech:item/${customModel.id}`,
            },
          })),
          fallback,
        },
      }),
    );
    fallbackRecords.push({
      baseMaterial,
      itemDefinition,
      kind: expectedFallbackKind(baseMaterial),
      selectorCount: materialModels.length,
      valid: true,
    });
  }

  const archiveFiles = [...files]
    .map(([path, content]) => ({ path, content }))
    .sort((left, right) => (left.path < right.path ? -1 : left.path > right.path ? 1 : 0));
  for (const file of archiveFiles) {
    const output = resolve(resourcePackSourceDirectory, file.path);
    await mkdir(dirname(output), { recursive: true });
    await writeFile(output, file.content);
  }

  const archive = createZip(archiveFiles);
  await mkdir(dirname(resourcePackDestination), { recursive: true });
  await writeFile(resourcePackDestination, archive);

  const filePaths = new Set(archiveFiles.map((file) => file.path));
  for (const record of modelRecords) {
    for (const path of [record.itemDefinition, record.directItemDefinition, record.model, record.texture]) {
      if (!filePaths.has(path)) throw new Error(`resource pack closure missing ${path}`);
    }
  }
  if (modelRecords.length !== customModels.length || modelRecords.filter((record) => record.generated).length !== ASSET_GENERATION_COUNTS.newRegistrations) {
    throw new Error("resource pack model closure does not cover preserved and generated assets");
  }
  if (new Set(modelRecords.map((record) => record.selector)).size !== modelRecords.length) {
    throw new Error("resource pack selectors must be globally unique");
  }
  if (new Set(modelRecords.map((record) => record.model)).size !== modelRecords.length || new Set(modelRecords.map((record) => record.texture)).size !== modelRecords.length) {
    throw new Error("resource pack models and textures must be globally unique");
  }
  if (fallbackRecords.some((record) => !record.valid || record.selectorCount < 1)) {
    throw new Error("resource pack fallback closure is incomplete");
  }
  const generatedCount = modelRecords.filter((record) => record.generated).length;
  const preservedCount = modelRecords.length - generatedCount;
  const resourcePackManifest = {
    filename: resourcePackName,
    version: resourcePackVersion,
    target: "Minecraft Java 26.1.2 / Paper 26.1.2",
    resourcePackFormat: { major: resourcePackFormat, minor: 0 },
    sourceManifest: {
      path: "src/main/resources/talexsoultech/content/catalog-runtime.json",
      sha256: runtimeManifest.sourceManifestHash,
      authoringHash: runtimeManifest.manifest.authoringHash,
    },
    counts: {
      mapped: ASSET_GENERATION_COUNTS.catalog,
      catalogMapped: ASSET_GENERATION_COUNTS.catalog,
      newAssets: generatedCount,
      newRegistrations: ASSET_GENERATION_COUNTS.newRegistrations,
      runtime: ASSET_GENERATION_COUNTS.runtimeTotal,
      runtimeTotal: ASSET_GENERATION_COUNTS.runtimeTotal,
      baseline: BASELINE_RUNTIME_COUNT,
      preservedAssets: preservedCount,
      customModels: modelRecords.length,
      files: archiveFiles.length,
      baseMaterialDefinitions: fallbackRecords.length,
    },
    closure: {
      files: { count: archiveFiles.length, complete: true },
      models: { count: modelRecords.length, generated: generatedCount, preserved: preservedCount, complete: true },
      directItemDefinitions: { count: modelRecords.length, complete: true },
      textures: { count: modelRecords.length, generated: generatedCount, size: "16x16", complete: true },
      selectors: { count: modelRecords.length, unique: true, generated: generatedCount, preserved: preservedCount, complete: true },
      fallbacks: { count: fallbackRecords.length, valid: true, complete: true },
    },
    archive: {
      path: `assets/${resourcePackName}`,
      size: archive.length,
      sha256: fileHash(archive),
    },
    models: modelRecords.sort((left, right) => (left.runtimeId < right.runtimeId ? -1 : left.runtimeId > right.runtimeId ? 1 : 0)),
    fallbacks: fallbackRecords,
    files: archiveFiles.map((file) => ({
      path: file.path,
      size: file.content.length,
      sha256: fileHash(file.content),
    })),
  };
  await writeFile(
    resourcePackManifestDestination,
    `${JSON.stringify(resourcePackManifest, null, 2)}\n`,
    "utf8",
  );
  return resourcePackManifest;
}

const resourcePackManifest = await prepareResourcePack();

await mkdir(downloadsDirectory, { recursive: true });
await copyFile(source, destination);

const bytes = await readFile(destination);
const metadata = await stat(destination);
const jarSha256 = createHash("sha256").update(bytes).digest("hex");
const manifest = {
  filename,
  version: "3.0.0-SNAPSHOT",
  target: "Paper 26.1.2 / Java 25",
  size: metadata.size,
  sha256: jarSha256,
};

await writeFile(
  runtimeCatalogDestination,
  buildRuntimeCatalogSource({
    manifest: runtimeManifest.manifest,
    jarSha256,
    observedAt: "2026-08-25",
    sourceManifestHash: runtimeManifest.sourceManifestHash,
  }),
  "utf8",
);

await writeFile(
  resolve(downloadsDirectory, "manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
  "utf8",
);

console.log(`${filename} ${manifest.sha256}`);
console.log(`${resourcePackName} ${resourcePackManifest.archive.sha256}`);
