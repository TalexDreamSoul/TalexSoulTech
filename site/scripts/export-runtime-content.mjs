import { createHash } from 'node:crypto';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import { DISCIPLINES, CATALOG_STATS } from '../public/data/catalog.js';
import { CAMPAIGN_WAVES } from '../public/data/progression.js';
import { LEGACY_RUNTIME_MAPPINGS, LEGACY_BASELINE_RUNTIME_IDS } from '../public/data/legacy-runtime-mappings.js';

const SCRIPT_PATH = fileURLToPath(import.meta.url);
const SCRIPT_DIR = dirname(SCRIPT_PATH);
const REPOSITORY_ROOT = resolve(SCRIPT_DIR, '../..');
const OUTPUT_PATH = resolve(REPOSITORY_ROOT, 'src/main/resources/talexsoultech/content/catalog-runtime.json');
const CATALOG_PATH = resolve(REPOSITORY_ROOT, 'site/public/data/catalog.js');
const PROGRESSION_PATH = resolve(REPOSITORY_ROOT, 'site/public/data/progression.js');
const MAPPING_PATH = resolve(REPOSITORY_ROOT, 'site/public/data/legacy-runtime-mappings.js');

const FAMILY_KINDS = Object.freeze([
  'research',
  'resource',
  'processing',
  'plant',
  'defense',
  'machine',
  'energy',
  'magic',
  'space',
  'gravity',
  'logistics',
  'construction',
  'fluid',
  'commerce',
  'quantum',
]);
const FORM_ORDER = Object.freeze({
  research: Object.freeze(['probe', 'analyzer', 'station']),
  resource: Object.freeze(['fragment', 'alloy', 'block']),
  processing: Object.freeze(['reagent', 'core', 'vat']),
  plant: Object.freeze(['seed', 'culture', 'greenhouse']),
  defense: Object.freeze(['plate', 'armor', 'bastion']),
  machine: Object.freeze(['part', 'drive', 'workstation']),
  energy: Object.freeze(['coil', 'cell', 'unit']),
  magic: Object.freeze(['rune', 'wand', 'array']),
  space: Object.freeze(['shard', 'anchor', 'gate']),
  gravity: Object.freeze(['mass', 'gauntlet', 'field']),
  logistics: Object.freeze(['tag', 'sorter', 'relay']),
  construction: Object.freeze(['brick', 'frame', 'workshop']),
  fluid: Object.freeze(['filter', 'pump', 'network']),
  commerce: Object.freeze(['token', 'contract', 'exchange']),
  quantum: Object.freeze(['bit', 'core', 'gate']),
});

const KIND_PALETTES = Object.freeze({
  research: Object.freeze(['minecraft:paper', 'minecraft:book']),
  resource: Object.freeze(['minecraft:stone', 'minecraft:iron_ingot']),
  processing: Object.freeze(['minecraft:glass_bottle', 'minecraft:redstone']),
  plant: Object.freeze(['minecraft:wheat_seeds', 'minecraft:bone_meal']),
  defense: Object.freeze(['minecraft:iron_ingot', 'minecraft:leather']),
  machine: Object.freeze(['minecraft:iron_ingot', 'minecraft:redstone']),
  energy: Object.freeze(['minecraft:copper_ingot', 'minecraft:redstone']),
  magic: Object.freeze(['minecraft:amethyst_shard', 'minecraft:paper']),
  space: Object.freeze(['minecraft:ender_pearl', 'minecraft:iron_ingot']),
  gravity: Object.freeze(['minecraft:iron_ingot', 'minecraft:feather']),
  logistics: Object.freeze(['minecraft:paper', 'minecraft:chest']),
  construction: Object.freeze(['minecraft:bricks', 'minecraft:iron_ingot']),
  fluid: Object.freeze(['minecraft:glass', 'minecraft:iron_ingot']),
  commerce: Object.freeze(['minecraft:emerald', 'minecraft:paper']),
  quantum: Object.freeze(['minecraft:amethyst_shard', 'minecraft:redstone']),
});

const BASE_MATERIALS = Object.freeze({
  research: 'PAPER',
  resource: 'STONE',
  processing: 'GLASS_BOTTLE',
  plant: 'WHEAT_SEEDS',
  defense: 'IRON_CHESTPLATE',
  machine: 'IRON_BLOCK',
  energy: 'REDSTONE',
  magic: 'AMETHYST_SHARD',
  space: 'ENDER_PEARL',
  gravity: 'IRON_BLOCK',
  logistics: 'PAPER',
  construction: 'BRICKS',
  fluid: 'GLASS',
  commerce: 'EMERALD',
  quantum: 'AMETHYST_SHARD',
});
const FACILITY_BASE_MATERIALS = Object.freeze({
  station: 'LECTERN',
  block: 'IRON_BLOCK',
  vat: 'CAULDRON',
  greenhouse: 'GLASS',
  bastion: 'BEACON',
  workstation: 'SMITHING_TABLE',
  unit: 'REDSTONE_BLOCK',
  array: 'ENCHANTING_TABLE',
  gate: 'LODESTONE',
  field: 'IRON_BLOCK',
  relay: 'BARREL',
  workshop: 'BRICKS',
  network: 'COPPER_BLOCK',
  exchange: 'EMERALD_BLOCK',
});
const PLACEABLE_FACILITY_MATERIALS = new Set(Object.values(FACILITY_BASE_MATERIALS));
const STACK_LIMIT_ONE_FORMS = new Set([
  'probe', 'analyzer', 'station', 'wand', 'array', 'anchor', 'gate', 'gauntlet', 'field',
  'tag', 'sorter', 'relay', 'token', 'contract', 'exchange', 'filter', 'pump', 'network',
  'armor', 'bastion', 'workstation', 'workshop', 'greenhouse', 'vat', 'unit', 'drive', 'cell', 'core',
]);
const STACK_LIMIT_SIXTY_FOUR_FORMS = new Set([
  'seed', 'culture', 'rune', 'reagent', 'fragment', 'alloy', 'block', 'brick', 'frame', 'plate', 'part', 'coil',
  'shard', 'mass', 'bit',
]);

const BEHAVIOR_ACTIONS = Object.freeze({
  research: 'observe_and_record',
  resource: 'extract_and_process',
  processing: 'process_finite_input',
  plant: 'cultivate_bounded_growth',
  defense: 'protect_bounded_area',
  machine: 'operate_bounded_workstation',
  energy: 'settle_finite_energy',
  magic: 'apply_owner_bound_intent',
  space: 'route_known_endpoint',
  gravity: 'apply_capacity_limited_field',
  logistics: 'route_one_batch',
  construction: 'patch_loaded_world_area',
  fluid: 'transfer_source_ledger',
  commerce: 'settle_owner_bound_exchange',
  quantum: 'commit_or_rollback_state',
});

const KIND_FACILITY_FOOTPRINT = Object.freeze({
  research: 'SINGLE',
  resource: 'THREE_BY_THREE',
  processing: 'THREE_BY_THREE',
  plant: 'THREE_BY_THREE',
  defense: 'THREE_BY_THREE',
  machine: 'THREE_BY_THREE',
  energy: 'THREE_BY_THREE',
  magic: 'SINGLE',
  space: 'FIVE_BY_FIVE',
  gravity: 'THREE_BY_THREE',
  logistics: 'THREE_BY_THREE',
  construction: 'FIVE_BY_FIVE',
  fluid: 'THREE_BY_THREE',
  commerce: 'THREE_BY_THREE',
  quantum: 'FIVE_BY_FIVE',
});

const VANILLA_REFERENCE = /^minecraft:[a-z0-9_]+$/;
const RUNTIME_REFERENCE = /^[a-z0-9_]+$/;
const MATERIALS = new Set([
  'PAPER',
  'STONE',
  'GLASS_BOTTLE',
  'WHEAT_SEEDS',
  'IRON_CHESTPLATE',
  'IRON_BLOCK',
  'REDSTONE',
  'AMETHYST_SHARD',
  'ENDER_PEARL',
  'BRICKS',
  'GLASS',
  'EMERALD',
  'LECTERN',
  'CAULDRON',
  'BEACON',
  'SMITHING_TABLE',
  'REDSTONE_BLOCK',
  'ENCHANTING_TABLE',
  'LODESTONE',
  'BARREL',
  'COPPER_BLOCK',
  'EMERALD_BLOCK',
]);

function assert(condition, message) {
  if (!condition) throw new Error(`catalog runtime export invariant failed: ${message}`);
}
function compareIds(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function normalizeRuntimeId(planningId) {
  const runtimeId = planningId.replace(/[.-]+/g, '_').replace(/_+/g, '_').toLowerCase();
  assert(runtimeId.length > 0 && !runtimeId.startsWith('st_'), `invalid generated runtime ID for ${planningId}`);
  return runtimeId;
}

function canonicalAuthoringHash(sources) {
  const payload = sources
    .map(({ name, content }) => `${name}\n${content.length}\n${content}\n`)
    .join('');
  return createHash('sha256').update(payload, 'utf8').digest('hex');
}

function allCatalogItems() {
  return DISCIPLINES.flatMap((discipline) => discipline.items.map((item) => ({
    ...item,
    disciplineName: discipline.name,
    disciplineStage: discipline.stage,
  })));
}

function createMappingIndex() {
  const byPlanningId = new Map();
  const byRuntimeId = new Map();
  for (const mapping of LEGACY_RUNTIME_MAPPINGS) {
    assert(!byPlanningId.has(mapping.planningId), `duplicate legacy planning ID ${mapping.planningId}`);
    assert(!byRuntimeId.has(mapping.runtimeId), `duplicate legacy runtime ID ${mapping.runtimeId}`);
    byPlanningId.set(mapping.planningId, mapping.runtimeId);
    byRuntimeId.set(mapping.runtimeId, mapping.planningId);
  }
  return { byPlanningId, byRuntimeId };
}

function entryMetadata(item, mappingIndex) {
  const legacyRuntimeId = mappingIndex.byPlanningId.get(item.id) ?? null;
  const runtimeId = legacyRuntimeId ?? normalizeRuntimeId(item.id);
  const familyKind = item.familyKind;
  const form = item.form;
  assert(FAMILY_KINDS.includes(familyKind), `${item.id} has invalid family kind ${familyKind}`);
  assert(typeof form === 'string' && form.length > 0, `${item.id} has no form metadata`);
  assert(typeof item.waveId === 'string' && /^W[1-9]$/.test(item.waveId), `${item.id} has invalid wave`);
  assert(typeof item.disciplineId === 'string' && item.disciplineId.length > 0, `${item.id} has invalid discipline`);
  assert(typeof item.familyId === 'string' && item.familyId.length > 0, `${item.id} has invalid family`);
  const slug = item.id.slice(item.id.lastIndexOf('.') + 1);
  assert(slug.length > 0 && item.id.endsWith(`.${slug}`), `${item.id} has invalid slug`);
  const baseMaterial = FACILITY_BASE_MATERIALS[form] ?? BASE_MATERIALS[familyKind];
  assert(MATERIALS.has(baseMaterial), `${item.id} has invalid base material`);
  if (FACILITY_BASE_MATERIALS[form]) {
    assert(PLACEABLE_FACILITY_MATERIALS.has(baseMaterial), `${item.id} facility base material is placeable`);
  }

  assert(STACK_LIMIT_ONE_FORMS.has(form) || STACK_LIMIT_SIXTY_FOUR_FORMS.has(form), `${item.id} has unsupported stack-limit form ${form}`);
  const stackLimit = STACK_LIMIT_ONE_FORMS.has(form) ? 1 : 64;
  return {
    planningId: item.id,
    runtimeId,
    legacyRuntimeId,
    newRegistration: legacyRuntimeId === null,
    runtimeKind: 'SOULTECH_ITEM',
    wave: item.waveId,
    discipline: item.disciplineId,
    family: item.family,
    familyId: item.familyId,
    familyKey: item.familyKey,
    slug,
    tier: item.tier,
    type: item.type,
    name: item.name,
    familyKind,
    form,
    baseMaterial,
    modelKey: `talexsoultech.item.${runtimeId}`,
    stackLimit,
    recipe: null,
    behavior: null,
    facility: null,
    recovery: {
      stop: 'STOP_BEFORE_COMMIT',
      rollback: 'RESTORE_INPUT_AND_RELEASE_ENERGY',
      retry: 'RESUME_FROM_CHECKPOINT',
    },
    isNarrativeAnchor: item.isNarrativeAnchor,
    story: item.story ? { ...item.story } : null,
    previousItemId: item.previousItemId,
    nextItemId: item.nextItemId,
  };
}

function formRank(entry) {
  const rank = FORM_ORDER[entry.familyKind]?.indexOf(entry.form) ?? -1;
  assert(rank >= 0 && rank <= 2, `${entry.planningId} has invalid form rank`);
  return rank;
}

function buildRecipes(entries, sourceById, entryByPlanningId) {
  const byWave = new Map();
  for (const entry of entries) {
    const waveEntries = byWave.get(entry.wave) ?? [];
    waveEntries.push(entry);
    byWave.set(entry.wave, waveEntries);
  }
  for (const waveEntries of byWave.values()) waveEntries.sort((left, right) => compareIds(left.planningId, right.planningId));
  const waveIndex = new Map(CAMPAIGN_WAVES.map((wave, index) => [wave.id, index]));
  const entryByRuntimeId = new Map(entries.map((entry) => [entry.runtimeId, entry]));

  for (const entry of entries) {
    const source = sourceById.get(entry.planningId);
    const rank = formRank(entry);
    const ingredients = [];
    if (rank > 0) {
      assert(source.previousItemId, `${entry.planningId} rank ${rank} is missing previous item`);
      const previous = entryByPlanningId.get(sourceById.get(source.previousItemId).id);
      assert(previous, `${entry.planningId} previous item does not resolve`);
      ingredients.push({ kind: 'RUNTIME', reference: previous.runtimeId, amount: 1 });
    } else if (waveIndex.get(entry.wave) > 0) {
      const previousWave = CAMPAIGN_WAVES[waveIndex.get(entry.wave) - 1].id;
      const catalyst = byWave.get(previousWave)?.[0];
      assert(catalyst, `${entry.planningId} has no previous-wave catalyst`);
      ingredients.push({ kind: 'RUNTIME', reference: catalyst.runtimeId, amount: 1 });
    }
    const palette = KIND_PALETTES[entry.familyKind];
    assert(palette, `${entry.planningId} has no typed ingredient palette`);
    for (const reference of palette) ingredients.push({ kind: 'VANILLA', reference, amount: rank + 1 });

    entry.recipe = {
      workstation: rank === 0 ? 'CRAFTING_TABLE' : 'ADVANCED_WORKBENCH',
      ingredients,
      outputAmount: 1,
    };
    entry.behavior = {
      kind: entry.familyKind,
      action: `${BEHAVIOR_ACTIONS[entry.familyKind]}_${entry.form}`,
      bounds: {
        radius: rank === 2 ? 5 : 2,
        maxTargets: entry.familyKind === 'defense' ? 8 : 1,
        durationTicks: rank === 2 ? 200 : 80,
        maxBlocks: entry.familyKind === 'construction' ? (rank === 2 ? 25 : 9) : 1,
        maxEntities: entry.familyKind === 'research' || entry.familyKind === 'defense' ? 8 : 1,
      },
      cost: {
        energyMilliSe: entry.familyKind === 'energy' ? 100 : rank === 2 ? 50 : 10,
        inputAmount: rank + 1,
        cooldownTicks: 20 * (rank + 1),
      },
      statePolicy: 'FINITE_STOP_ON_EMPTY_OR_FULL',
    };
    if (rank === 2) {
      entry.facility = {
        form: entry.form,
        footprint: KIND_FACILITY_FOOTPRINT[entry.familyKind],
        ports: entry.familyKind === 'energy' || entry.familyKind === 'fluid' ? 4 : 2,
        operation: {
          intervalTicks: 20,
          maxBatch: 1,
          inputSlots: 3,
          outputSlots: 1,
        },
      };
    }
  }
}

function validateStoryAndWaveIdentity(sourceItems) {
  const waveById = new Map(CAMPAIGN_WAVES.map((wave) => [wave.id, wave]));
  const storyIds = new Set();
  for (const item of sourceItems) {
    const wave = waveById.get(item.waveId);
    assert(wave, `${item.id} points to unknown wave ${item.waveId}`);
    assert(wave.disciplineIds.includes(item.disciplineId), `${item.id} discipline is not in ${item.waveId}`);
    if (item.story) {
      assert(!storyIds.has(item.id), `duplicate story item ${item.id}`);
      storyIds.add(item.id);
      const anchor = wave.anchors.find((candidate) => candidate.familyKey === item.familyKey);
      assert(anchor, `${item.id} story family is not an anchor`);
      assert(anchor.stories.some((story) => story.itemId === item.id && story.order === item.story.order), `${item.id} story identity drifted`);
    }
  }
  assert(storyIds.size === 162, `expected 162 stories, received ${storyIds.size}`);
}

function validateManifest(manifest, sourceItems, mappingIndex) {
  assert(CATALOG_STATS.itemCount === 810 && sourceItems.length === 810, 'catalog must contain exactly 810 entries');
  assert(CATALOG_STATS.familyCount === 270, 'catalog must contain exactly 270 families');
  assert(CATALOG_STATS.waveCount === 9, 'catalog must contain exactly 9 waves');
  assert(CATALOG_STATS.implementedCount === 810, 'catalog must mark all 810 entries implemented');
  assert(CATALOG_STATS.plannedCount === 0, 'catalog must not retain planned entries after runtime cutover');
  assert(new Set(LEGACY_BASELINE_RUNTIME_IDS).size === 150 && LEGACY_BASELINE_RUNTIME_IDS.length === 150, 'baseline runtime snapshot must contain 150 unique IDs');
  assert(LEGACY_RUNTIME_MAPPINGS.length === 34, 'legacy mapping table must contain 34 entries');
  validateStoryAndWaveIdentity(sourceItems);

  const entriesByPlanningId = new Map();
  const entriesByRuntimeId = new Map();
  const allEntriesByRuntimeId = new Map(manifest.entries.map((entry) => [entry.runtimeId, entry]));
  const families = new Set();
  const familyKinds = new Set();
  const baselineRuntimeIds = new Set(LEGACY_BASELINE_RUNTIME_IDS);
  for (const entry of manifest.entries) {
    assert(!entriesByPlanningId.has(entry.planningId), `duplicate planning ID ${entry.planningId}`);
    assert(!entriesByRuntimeId.has(entry.runtimeId), `duplicate runtime ID ${entry.runtimeId}`);
    entriesByPlanningId.set(entry.planningId, entry);
    entriesByRuntimeId.set(entry.runtimeId, entry);
    families.add(`${entry.discipline}.${entry.family}`);
    familyKinds.add(entry.familyKind);
    assert(entry.runtimeKind === 'SOULTECH_ITEM', `${entry.planningId} has a non-SoulTech runtime kind`);
    assert(entry.newRegistration === (entry.legacyRuntimeId === null), `${entry.planningId} registration flag drifted`);
    if (entry.newRegistration) {
      assert(entry.runtimeId === normalizeRuntimeId(entry.planningId), `${entry.planningId} generated ID drifted`);
      assert(!baselineRuntimeIds.has(entry.runtimeId), `${entry.planningId} collides with baseline runtime ID ${entry.runtimeId}`);
    } else {
      assert(entry.legacyRuntimeId === entry.runtimeId, `${entry.planningId} legacy runtime ID must be immutable`);
      assert(baselineRuntimeIds.has(entry.runtimeId), `${entry.planningId} maps outside the baseline runtime catalog`);
      assert(mappingIndex.byPlanningId.get(entry.planningId) === entry.runtimeId, `${entry.planningId} is not in the explicit mapping table`);
    }
    assert(MATERIALS.has(entry.baseMaterial), `${entry.planningId} has unknown base material`);
    if (FACILITY_BASE_MATERIALS[entry.form]) assert(PLACEABLE_FACILITY_MATERIALS.has(entry.baseMaterial), `${entry.planningId} facility base material is not placeable`);
    assert(entry.modelKey === `talexsoultech.item.${entry.runtimeId}`, `${entry.planningId} model key drifted`);
    assert((STACK_LIMIT_ONE_FORMS.has(entry.form) ? 1 : STACK_LIMIT_SIXTY_FOUR_FORMS.has(entry.form) ? 64 : 0) === entry.stackLimit, `${entry.planningId} stackLimit does not match form policy`);
    assert(entry.recipe && entry.recipe.outputAmount === 1, `${entry.planningId} has no real recipe`);
    assert(entry.behavior && FAMILY_KINDS.includes(entry.behavior.kind), `${entry.planningId} has invalid behavior`);
    assert(entry.behavior.bounds.radius > 0 && entry.behavior.bounds.maxTargets > 0 && entry.behavior.bounds.durationTicks > 0 && entry.behavior.bounds.maxBlocks > 0 && entry.behavior.bounds.maxEntities > 0, `${entry.planningId} has unbounded behavior`);
    assert(entry.behavior.cost.energyMilliSe >= 0 && entry.behavior.cost.inputAmount > 0 && entry.behavior.cost.cooldownTicks > 0, `${entry.planningId} has invalid behavior cost`);
    if (entry.facility) {
      assert(entry.facility.ports > 0, `${entry.planningId} facility has no ports`);
      for (const key of ['intervalTicks', 'maxBatch', 'inputSlots', 'outputSlots']) assert(entry.facility.operation[key] > 0, `${entry.planningId} facility has invalid ${key}`);
    }
    for (const ingredient of entry.recipe.ingredients) {
      assert(ingredient.amount > 0, `${entry.planningId} has non-positive ingredient amount`);
      if (ingredient.kind === 'VANILLA') assert(VANILLA_REFERENCE.test(ingredient.reference), `${entry.planningId} has invalid vanilla ingredient ${ingredient.reference}`);
      else if (ingredient.kind === 'RUNTIME') assert(RUNTIME_REFERENCE.test(ingredient.reference) && allEntriesByRuntimeId.has(ingredient.reference), `${entry.planningId} has unresolved runtime ingredient ${ingredient.reference}`);
      else throw new Error(`catalog runtime export invariant failed: ${entry.planningId} has invalid ingredient kind ${ingredient.kind}`);
    }
  }
  assert(manifest.entries.length === 810, `manifest must contain 810 entries, received ${manifest.entries.length}`);
  assert(entriesByPlanningId.size === 810 && entriesByRuntimeId.size === 810, 'manifest IDs must be unique');
  assert(families.size === 270, `manifest must contain 270 families, received ${families.size}`);
  assert(familyKinds.size === 15 && FAMILY_KINDS.every((kind) => familyKinds.has(kind)), 'manifest must contain all 15 family kinds');
  assert(manifest.counts.catalog === 810, 'catalog count drifted');
  assert(manifest.counts.baseline === 150, 'baseline count drifted');
  assert(manifest.counts.legacyMappings === 34, 'legacy mapping count drifted');
  assert(manifest.counts.newRegistrations === 776, 'new registration count drifted');
  assert(manifest.counts.runtimeTotal === 926, 'runtime total drifted');
  assert(manifest.entries.filter((entry) => entry.newRegistration).length === 776, 'new registration rows drifted');
  assert(manifest.entries.filter((entry) => !entry.newRegistration).length === 34, 'legacy rows drifted');
  assert(manifest.counts.runtimeTotal === baselineRuntimeIds.size + manifest.counts.newRegistrations, 'runtime total is not baseline plus new registrations');
  for (const mapping of LEGACY_RUNTIME_MAPPINGS) {
    const entry = entriesByPlanningId.get(mapping.planningId);
    assert(entry && entry.legacyRuntimeId === mapping.runtimeId, `legacy mapping ${mapping.planningId} is missing from manifest`);
  }
  for (const runtimeId of baselineRuntimeIds) {
    if (!mappingIndex.byRuntimeId.has(runtimeId)) continue;
    assert(entriesByRuntimeId.get(runtimeId)?.legacyRuntimeId === runtimeId, `baseline mapping ${runtimeId} was overwritten`);
  }
}

export async function buildManifest() {
  const [catalogSource, progressionSource, mappingSource] = await Promise.all([
    readFile(CATALOG_PATH, 'utf8'),
    readFile(PROGRESSION_PATH, 'utf8'),
    readFile(MAPPING_PATH, 'utf8'),
  ]);
  const authoringHash = canonicalAuthoringHash([
    { name: 'catalog.js', content: catalogSource },
    { name: 'progression.js', content: progressionSource },
    { name: 'legacy-runtime-mappings.js', content: mappingSource },
  ]);
  const sourceItems = allCatalogItems();
  const mappingIndex = createMappingIndex();
  for (const mapping of LEGACY_RUNTIME_MAPPINGS) assert(sourceItems.some((item) => item.id === mapping.planningId), `mapping references unknown planning ID ${mapping.planningId}`);
  const sourceById = new Map(sourceItems.map((item) => [item.id, item]));
  const entries = sourceItems
    .map((item) => entryMetadata(item, mappingIndex))
    .sort((left, right) => compareIds(left.planningId, right.planningId));
  buildRecipes(entries, sourceById, new Map(entries.map((entry) => [entry.planningId, entry])));
  const manifest = {
    schemaVersion: 1,
    authoringHash,
    counts: {
      catalog: 810,
      baseline: 150,
      legacyMappings: 34,
      newRegistrations: 776,
      runtimeTotal: 926,
      families: 270,
      familyKinds: 15,
    },
    entries,
  };
  validateManifest(manifest, sourceItems, mappingIndex);
  return manifest;
}

export async function writeManifest() {
  const manifest = await buildManifest();
  await mkdir(dirname(OUTPUT_PATH), { recursive: true });
  const serialized = `${JSON.stringify(manifest, null, 2)}\n`;
  await writeFile(OUTPUT_PATH, serialized, 'utf8');
  return { outputPath: OUTPUT_PATH, authoringHash: manifest.authoringHash, counts: manifest.counts };
}

if (pathToFileURL(process.argv[1] ?? '').href === import.meta.url) {
  const result = await writeManifest();
  process.stdout.write(`${JSON.stringify(result)}\n`);
}
