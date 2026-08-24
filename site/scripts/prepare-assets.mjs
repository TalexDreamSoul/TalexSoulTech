import { createHash } from "node:crypto";
import { copyFile, mkdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const siteDirectory = resolve(scriptDirectory, "..");
const source = resolve(siteDirectory, "..", "target", "talex-soul-tech-3.0.0-SNAPSHOT.jar");
const downloadsDirectory = resolve(siteDirectory, "public", "downloads");
const filename = "TalexSoulTech-3.0.0-SNAPSHOT.jar";
const destination = resolve(downloadsDirectory, filename);

const resourcePackVersion = "26.1.2";
const resourcePackFormat = 84;
const resourcePackName = `TalexSoulTech-${resourcePackVersion}-resource-pack.zip`;
const resourcePackSourceDirectory = resolve(siteDirectory, "public", "assets", "talexsoultech-resource-pack");
const resourcePackDestination = resolve(siteDirectory, "public", "assets", resourcePackName);
const resourcePackManifestDestination = resolve(siteDirectory, "public", "assets", "TalexSoulTech-resource-pack.manifest.json");

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
};

const customModels = [
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
  return model(
    baseMaterial === "barrel" ? "minecraft:block/barrel" : `minecraft:item/${baseMaterial}`,
  );
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
  for (const customModel of customModels) {
    files.set(
      `assets/talexsoultech/models/item/${customModel.id}.json`,
      packJson({
        parent: "minecraft:item/generated",
        textures: { layer0: `talexsoultech:item/${customModel.id}` },
      }),
    );
    files.set(
      `assets/talexsoultech/items/${customModel.id}.json`,
      packJson({
        model: {
          type: "minecraft:model",
          model: `talexsoultech:item/${customModel.id}`,
        },
      }),
    );
    files.set(
      `assets/talexsoultech/textures/item/${customModel.id}.png`,
      createPixelTexture(customModel.pixels),
    );
    const materialModels = byMaterial.get(customModel.baseMaterial) ?? [];
    materialModels.push(customModel);
    byMaterial.set(customModel.baseMaterial, materialModels);
  }

  for (const [baseMaterial, materialModels] of byMaterial) {
    files.set(
      `assets/minecraft/items/${baseMaterial}.json`,
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
          fallback: vanillaFallback(baseMaterial),
        },
      }),
    );
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

  const resourcePackManifest = {
    filename: resourcePackName,
    version: resourcePackVersion,
    target: "Minecraft Java 26.1.2 / Paper 26.1.2",
    resourcePackFormat: { major: 84, minor: 0 },
    archive: {
      path: `assets/${resourcePackName}`,
      size: archive.length,
      sha256: fileHash(archive),
    },
    models: customModels.map((customModel) => ({
      selector: customModel.selector,
      baseMaterial: customModel.baseMaterial,
      itemDefinition: `assets/minecraft/items/${customModel.baseMaterial}.json`,
      directItemDefinition: `assets/talexsoultech/items/${customModel.id}.json`,
      model: `assets/talexsoultech/models/item/${customModel.id}.json`,
      texture: `assets/talexsoultech/textures/item/${customModel.id}.png`,
    })),
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
const manifest = {
  filename,
  version: "3.0.0-SNAPSHOT",
  target: "Paper 26.1.2 / Java 25",
  size: metadata.size,
  sha256: createHash("sha256").update(bytes).digest("hex"),
};

await writeFile(
  resolve(downloadsDirectory, "manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
  "utf8",
);

console.log(`${filename} ${manifest.sha256}`);
console.log(`${resourcePackName} ${resourcePackManifest.archive.sha256}`);
