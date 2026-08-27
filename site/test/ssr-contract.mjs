import assert from "node:assert/strict";
import test from "node:test";
import { renderSsrRequest } from "../src/ssr.js";

import { DISCIPLINES, CATALOG_STATS } from "../public/data/catalog.js";
import { CAMPAIGN, CAMPAIGN_ACTS, CAMPAIGN_WAVES } from "../public/data/progression.js";
import { LEGACY_BASELINE_RUNTIME_IDS, LEGACY_RUNTIME_MAPPINGS } from "../public/data/legacy-runtime-mappings.js";
import { RUNTIME_ITEMS, RUNTIME_RELEASE } from "../public/data/runtime-catalog.js";

const ORIGIN = "https://ssr-contract.invalid";
const HTML_CONTENT_TYPE = /^text\/html;\s*charset=UTF-8$/i;
const PUBLIC_CACHE_CONTROL = /^public,\s*max-age=\d+/;
const PRIVATE_CACHE_CONTROL = "private, no-store, max-age=0";
const INDEXABLE_ROBOTS = '<meta name="robots" content="index, follow">';
const NOINDEX_ROBOTS = '<meta name="robots" content="noindex, nofollow">';

const EXPECTED_WAVE_DISCIPLINES = Object.freeze({
  W1: ["basic", "materials", "technology"],
  W2: ["botany", "agroecology", "hydrology"],
  W3: ["defense", "construction", "energy"],
  W4: ["logistics", "automation", "commerce"],
  W5: ["geology", "metallurgy", "mechanics"],
  W6: ["chemistry", "environment", "exploration"],
  W7: ["magic", "space", "gravity"],
  W8: ["astral", "ender", "dimensional"],
  W9: ["quantum", "chronology", "civic"],
});
const EXPECTED_ACT_WAVES = Object.freeze([
  ["W1", "W2"],
  ["W3", "W4", "W5"],
  ["W6", "W7"],
  ["W8", "W9"],
]);
const ANCHOR_ITEM_ID = "basic.wood-compression.plank-9";
const NON_NARRATIVE_ITEM_ID = "basic.log-compression.log";

const PUBLIC_ROUTES = [
  { path: "/", marker: 'id="top"' },
  { path: "/download", marker: 'href="/downloads/TalexSoulTech-3.0.0-SNAPSHOT.jar" download' },
  { path: "/docs", marker: 'id="tutorials"' },
  { path: "/docs/quick-install", marker: '<article class="tutorial-reader route-reader">' },
  { path: "/disciplines", marker: 'href="/disciplines/materials"' },
  { path: "/disciplines/materials", marker: 'href="/items/materials.fire-materials.fire-rod"' },
  { path: "/items/materials.fire-materials.fire-rod", marker: '<code>materials.fire-materials.fire-rod</code>' },
  { path: "/planning", marker: 'id="campaign"' },
  { path: "/runtime", marker: 'id="runtime-catalog"' },
  { path: "/architecture", marker: 'id="architecture"' },
  { path: "/extensions", marker: 'id="extensions"' },
];

const PRIVATE_ROUTES = [
  { path: "/console", marker: 'id="console"' },
  { path: "/setup", marker: 'href="/admin"' },
  { path: "/admin", marker: 'id="admin-summary"' },
];

function staticSsrEnvironment() {
  return new Proxy({}, {
    get(_target, property) {
      assert.fail(`SSR must not read env.${String(property)} while rendering static documents.`);
    },
  });
}

async function render(path) {
  const url = new URL(path, ORIGIN);
  const request = new Request(url.href);
  const response = await renderSsrRequest(request, staticSsrEnvironment(), url);
  assert.ok(response, `${path}: SSR must handle the request.`);

  return { response, body: await response.text(), url };
}

function canonicalHref(url) {
  return new URL(url.pathname, url.origin).href;
}

function assertCanonical(body, url, contract) {
  assert.ok(
    body.includes(`<link rel="canonical" href="${canonicalHref(url)}">`),
    `${contract}: canonical link changed.`,
  );
}

function assertPublicHtml(result, contract) {
  assert.equal(result.response.status, 200, `${contract}: expected HTTP 200.`);
  assert.match(
    result.response.headers.get("content-type") ?? "",
    HTML_CONTENT_TYPE,
    `${contract}: expected an HTML content type.`,
  );
  assert.match(
    result.response.headers.get("cache-control") ?? "",
    PUBLIC_CACHE_CONTROL,
    `${contract}: public document must be cacheable.`,
  );
  assert.ok(result.body.includes(INDEXABLE_ROBOTS), `${contract}: public document must be indexable.`);
  assertCanonical(result.body, result.url, contract);
}

function assertPrivateHtml(result, contract) {
  assert.equal(result.response.status, 200, `${contract}: expected HTTP 200.`);
  assert.match(
    result.response.headers.get("content-type") ?? "",
    HTML_CONTENT_TYPE,
    `${contract}: expected an HTML content type.`,
  );
  assert.equal(
    result.response.headers.get("cache-control"),
    PRIVATE_CACHE_CONTROL,
    `${contract}: private document must not be stored.`,
  );
  assert.ok(result.body.includes(NOINDEX_ROBOTS), `${contract}: private document must not be indexed.`);
  assertCanonical(result.body, result.url, contract);
}

function selectedCatalogControl(body, id) {
  const control = body.match(new RegExp(`<select\\b[^>]*\\bid="${id}"[^>]*>([\\s\\S]*?)<\\/select>`));
  assert.ok(control, `/catalog: ${id} control is missing.`);

  const selectedValues = [...control[1].matchAll(/<option value="([^"]+)" selected>/g)].map((match) => match[1]);
  assert.equal(selectedValues.length, 1, `/catalog: ${id} must have exactly one selected option.`);
  return selectedValues[0];
}

function catalogItemIds(body) {
  return [...body.matchAll(/<a class="cell-title" href="\/items\/([^"]+)">/g)].map((match) => decodeURIComponent(match[1]));
}

function catalogItemLinkCount(body) {
  return catalogItemIds(body).length;
}

function runtimeItemIds(body) {
  return [...body.matchAll(/<span class="cell-code">([^<]+)<\/span>/g)].map((match) => match[1]);
}

function selectedRuntimeGroups(body) {
  const control = body.match(/<select id="runtime-group"[^>]*>(.*?)<\/select>/);
  assert.ok(control, "/runtime: runtime group control is missing.");
  return [...control[1].matchAll(/<option value="([^"]+)" selected>/g)].map((match) => match[1]);
}
function allCatalogRecords() {
  return DISCIPLINES.flatMap((discipline) => discipline.items.map((item, index) => ({ discipline, item, index })));
}

function catalogRecordForItem(id) {
  const record = allCatalogRecords().find(({ item }) => item.id === id);
  assert.ok(record, `catalog: expected stable item ${id}.`);
  return record;
}

function attributeValue(tag, name) {
  return tag.match(new RegExp(`\\b${name}="([^"]*)"`))?.[1] ?? null;
}

function hasClass(tag, className) {
  return (attributeValue(tag, "class") ?? "").split(/\s+/).includes(className);
}

function allOpeningTags(body) {
  return [...body.matchAll(/<([a-z][a-z0-9:-]*)\b[^>]*>/gi)];
}

function findOpeningTag(body, predicate, contract) {
  const tag = allOpeningTags(body).find(predicate);
  assert.ok(tag, `${contract}: expected element is missing.`);
  return tag;
}

function elementBodyById(body, id, contract) {
  const tag = findOpeningTag(body, (candidate) => attributeValue(candidate[0], "id") === id, contract);
  const start = tag.index + tag[0].length;
  const closingTag = `</${tag[1]}>`;
  const end = body.indexOf(closingTag, start);
  assert.notEqual(end, -1, `${contract}: ${id} has no closing tag.`);
  return body.slice(start, end);
}

function campaignActBody(body, actId) {
  const act = findOpeningTag(
    body,
    (candidate) => hasClass(candidate[0], "campaign-act") && attributeValue(candidate[0], "data-act-id") === actId,
    `/planning act ${actId}`,
  );
  const start = act.index + act[0].length;
  const nextAct = allOpeningTags(body).find(
    (candidate) => candidate.index > start && hasClass(candidate[0], "campaign-act"),
  );
  return body.slice(start, nextAct?.index);
}

function campaignWaveIds(body) {
  return allOpeningTags(body)
    .filter((tag) => tag[1].toLowerCase() === "article" && hasClass(tag[0], "campaign-wave"))
    .map((tag) => attributeValue(tag[0], "data-wave-id"));
}

function campaignWaveBody(body, waveId) {
  const wave = findOpeningTag(
    body,
    (candidate) => candidate[1].toLowerCase() === "article" &&
      hasClass(candidate[0], "campaign-wave") &&
      attributeValue(candidate[0], "data-wave-id") === waveId &&
      attributeValue(candidate[0], "id") === `wave-${waveId}`,
    `/planning wave ${waveId}`,
  );
  const start = wave.index + wave[0].length;
  const end = body.indexOf("</article>", start);
  assert.notEqual(end, -1, `/planning wave ${waveId}: missing closing article.`);
  return body.slice(start, end);
}

function disciplineLinks(body) {
  return [...body.matchAll(/href="\/disciplines\/([^"]+)"/g)].map((match) => decodeURIComponent(match[1]));
}

function paginationHref(body, id) {
  const tag = findOpeningTag(
    body,
    (candidate) => candidate[1].toLowerCase() === "a" && attributeValue(candidate[0], "id") === id,
    `/catalog ${id}`,
  );
  const href = attributeValue(tag[0], "href");
  assert.ok(href, `/catalog ${id}: href is missing.`);
  return new URL(href.replaceAll("&amp;", "&"), ORIGIN);
}

function definitionTerms(body) {
  return [...body.matchAll(/<dt>([^<]+)<\/dt>/g)].map((match) => match[1]);
}

function escapedHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}


test("SSR public routes return cacheable, indexable documents", async (t) => {
  for (const route of PUBLIC_ROUTES) {
    await t.test(route.path, async () => {
      const result = await render(route.path);
      assertPublicHtml(result, route.path);
      assert.ok(result.body.includes(route.marker), `${route.path}: expected route content is missing.`);
    });
  }
});

test("SSR preserves historical home anchors", async () => {
  const { body } = await render("/");

  for (const id of ["plugin", "tutorials", "catalog", "architecture", "extensions", "console"]) {
    assert.ok(body.includes(`id="${id}"`), `/: historical #${id} anchor is missing.`);
  }
});

test("SSR private routes are no-store and noindex", async (t) => {
  for (const route of PRIVATE_ROUTES) {
    await t.test(route.path, async () => {
      const result = await render(route.path);
      assertPrivateHtml(result, route.path);
      assert.ok(result.body.includes(route.marker), `${route.path}: expected private route content is missing.`);
    });
  }
});

test("SSR catalog filters results, normalizes controls, and pages 24 items", async (t) => {
  await t.test("renders an item link for valid normalized filters", async () => {
    const result = await render("/catalog?q=fire-rod&discipline=materials&status=implemented&page=1");
    assertPublicHtml(result, "/catalog valid filters");
    assert.equal(
      selectedCatalogControl(result.body, "discipline-filter"),
      "materials",
      "/catalog valid filters: selected discipline changed.",
    );
    assert.equal(
      selectedCatalogControl(result.body, "status-filter"),
      "implemented",
      "/catalog valid filters: selected status changed.",
    );
    assert.ok(
      result.body.includes('href="/items/materials.fire-materials.fire-rod"'),
      "/catalog valid filters: expected filtered item link is missing.",
    );
  });

  await t.test("normalizes whitespace, unknown filters, and an invalid page", async () => {
    const result = await render(
      `/catalog?q=${encodeURIComponent(" fire-rod ")}&discipline=not-a-discipline&status=unknown&page=0`,
    );
    assertPublicHtml(result, "/catalog invalid filters");
    assert.ok(result.body.includes('value="fire-rod"'), "/catalog invalid filters: search text was not normalized.");
    assert.equal(
      selectedCatalogControl(result.body, "discipline-filter"),
      "all",
      "/catalog invalid filters: unsupported discipline was not normalized to all.",
    );
    assert.equal(
      selectedCatalogControl(result.body, "status-filter"),
      "all",
      "/catalog invalid filters: unsupported status was not normalized to all.",
    );
    assert.match(
      result.body,
      /id="catalog-page-status">第 1 \/ /,
      "/catalog invalid filters: invalid page was not normalized to page 1.",
    );
  });

  await t.test("renders a full page of result links on adjacent pages", async () => {
    const firstPage = await render("/catalog");
    const secondPage = await render("/catalog?page=2");
    assertPublicHtml(firstPage, "/catalog");
    assertPublicHtml(secondPage, "/catalog?page=2");
    assert.equal(catalogItemLinkCount(firstPage.body), 24, "/catalog: expected 24 item links per page.");
    assert.equal(catalogItemLinkCount(secondPage.body), 24, "/catalog?page=2: expected 24 item links per page.");
    assert.ok(
      secondPage.body.includes('id="catalog-prev" rel="prev" href="/catalog"'),
      "/catalog?page=2: previous-page link is missing.",
    );
    assert.ok(
      secondPage.body.includes('id="catalog-next" rel="next" href="/catalog?page=3"'),
      "/catalog?page=2: next-page link is missing.",
    );
  });
});

test("Progression data covers canonical waves, anchors, and catalog-bound stories", () => {
  const records = allCatalogRecords();
  const recordById = new Map(records.map((record) => [record.item.id, record]));
  const familyByKey = new Map(
    DISCIPLINES.flatMap((discipline) => discipline.families.map((family) => [family.key, { discipline, family }])),
  );

  assert.equal(DISCIPLINES.length, 27, "catalog: expected exactly 27 disciplines.");
  assert.equal(CATALOG_STATS.itemCount, 810, "catalog: planned item capacity changed.");
  assert.equal(records.length, 810, "catalog: rendered discipline items no longer total 810.");
  assert.equal(CAMPAIGN_WAVES.length, 9, "progression: expected nine waves.");
  assert.deepEqual(CAMPAIGN_WAVES.map((wave) => wave.id), Object.keys(EXPECTED_WAVE_DISCIPLINES));
  assert.equal(CAMPAIGN_ACTS.length, 4, "progression: expected four acts.");
  assert.deepEqual(
    CAMPAIGN_ACTS.map((act) => [act.order, act.waveIds]),
    EXPECTED_ACT_WAVES.map((waveIds, index) => [index + 1, waveIds]),
    "progression: the four-act wave grouping changed.",
  );

  const campaignDisciplineIds = [];
  for (const wave of CAMPAIGN_WAVES) {
    const expectedDisciplineIds = EXPECTED_WAVE_DISCIPLINES[wave.id];
    assert.deepEqual(wave.disciplineIds, expectedDisciplineIds, `${wave.id}: discipline mapping changed.`);
    assert.equal(wave.anchors.length, 6, `${wave.id}: expected two anchor families per discipline.`);
    campaignDisciplineIds.push(...wave.disciplineIds);

    for (const disciplineId of wave.disciplineIds) {
      const discipline = DISCIPLINES.find((candidate) => candidate.id === disciplineId);
      assert.ok(discipline, `${wave.id}: unknown discipline ${disciplineId}.`);
      assert.equal(discipline.waveId, wave.id, `${disciplineId}: catalog wave binding diverged.`);
    }
  }
  assert.equal(new Set(campaignDisciplineIds).size, 27, "progression: a discipline appears in multiple waves.");
  assert.deepEqual(
    [...campaignDisciplineIds].sort(),
    Object.values(EXPECTED_WAVE_DISCIPLINES).flat().sort(),
    "progression: waves no longer cover the canonical disciplines exactly once.",
  );

  const anchorFamilies = [...familyByKey.values()].filter(({ family }) => family.isNarrativeAnchor);
  assert.equal(anchorFamilies.length, 54, "catalog: expected 54 narrative anchor families.");
  assert.equal(new Set(anchorFamilies.map(({ family }) => family.key)).size, 54, "catalog: narrative anchor family keys must be unique.");

  const storyRecords = records.filter(({ item }) => item.story !== null);
  assert.equal(storyRecords.length, 162, "catalog: expected 162 story items.");
  assert.equal(new Set(storyRecords.map(({ item }) => item.id)).size, 162, "catalog: story item IDs must be unique.");

  const tierRank = new Map([["I", 1], ["II", 2], ["III", 3], ["IV", 4], ["V", 5], ["VI", 6], ["VII", 7], ["VIII", 8], ["IX", 9], ["X", 10], ["XI", 11], ["XII", 12]]);
  for (const { discipline, family } of familyByKey.values()) {
    const familyRecords = records.filter(({ item }) => item.disciplineId === discipline.id && item.familyId === family.id);
    const ordered = [...familyRecords].sort((left, right) => {
      const leftRank = tierRank.get(left.item.tier);
      const rightRank = tierRank.get(right.item.tier);
      assert.ok(leftRank, `${left.item.id}: expected a Roman tier.`);
      assert.ok(rightRank, `${right.item.id}: expected a Roman tier.`);
      return leftRank - rightRank || left.index - right.index;
    });
    for (const [index, record] of ordered.entries()) {
      assert.equal(record.item.familyKey, family.key, `${record.item.id}: family key drifted from its catalog family.`);
      assert.equal(record.item.previousItemId, ordered[index - 1]?.item.id ?? null, `${record.item.id}: previous family sequence link is wrong.`);
      assert.equal(record.item.nextItemId, ordered[index + 1]?.item.id ?? null, `${record.item.id}: next family sequence link is wrong.`);
    }
  }

  const campaignAnchors = CAMPAIGN_WAVES.flatMap((wave) => wave.anchors);
  const storyIdsFromAnchors = new Set();
  assert.equal(campaignAnchors.length, 54, "progression: expected 54 anchor records.");
  assert.equal(new Set(campaignAnchors.map((anchor) => anchor.familyKey)).size, 54, "progression: anchor families must be unique.");
  for (const anchor of campaignAnchors) {
    const familyRecord = familyByKey.get(anchor.familyKey);
    assert.ok(familyRecord, `${anchor.familyKey}: anchor family is absent from the catalog.`);
    assert.equal(familyRecord.discipline.id, anchor.disciplineId, `${anchor.familyKey}: anchor discipline changed.`);
    assert.equal(familyRecord.family.id, anchor.familyId, `${anchor.familyKey}: anchor family ID changed.`);
    assert.equal(familyRecord.family.anchorReason, anchor.reason, `${anchor.familyKey}: anchor reason diverged.`);
    assert.deepEqual(anchor.stories.map((story) => story.order), [1, 2, 3], `${anchor.familyKey}: story orders must stay 1–3.`);

    for (const story of anchor.stories) {
      const record = recordById.get(story.itemId);
      assert.ok(record, `${story.itemId}: story references an item outside the catalog.`);
      assert.equal(record.item.disciplineId, anchor.disciplineId, `${story.itemId}: story crossed disciplines.`);
      assert.equal(record.item.familyId, anchor.familyId, `${story.itemId}: story crossed families.`);
      assert.equal(record.item.familyKey, anchor.familyKey, `${story.itemId}: story family key changed.`);
      assert.equal(record.item.isNarrativeAnchor, true, `${story.itemId}: story item lost its anchor marker.`);
      assert.equal(record.item.story?.order, story.order, `${story.itemId}: story order drifted from the campaign anchor.`);
      assert.equal(record.item.story?.text, story.text, `${story.itemId}: story text drifted from the campaign anchor.`);
      assert.equal(record.item.story?.anchorReason, anchor.reason, `${story.itemId}: story reason drifted from the campaign anchor.`);
      assert.ok(!Object.hasOwn(record.item.story, "tier"), `${story.itemId}: story must derive tier from its catalog item.`);
      assert.ok(tierRank.has(record.item.tier), `${story.itemId}: story item no longer has a catalog tier.`);
      assert.ok(!storyIdsFromAnchors.has(story.itemId), `${story.itemId}: story item appears in more than one anchor.`);
      storyIdsFromAnchors.add(story.itemId);
    }
  }
  assert.deepEqual(
    [...storyIdsFromAnchors].sort(),
    storyRecords.map(({ item }) => item.id).sort(),
    "progression: campaign anchors and catalog story items diverged.",
  );

  for (const wave of CAMPAIGN_WAVES) {
    for (const link of wave.familyLinks) {
      assert.equal(link.kind, "supports", `${wave.id}: family links must remain soft supports relationships.`);
      assert.ok(familyByKey.has(link.from), `${wave.id}: family link source ${link.from} is unknown.`);
      assert.ok(familyByKey.has(link.to), `${wave.id}: family link target ${link.to} is unknown.`);
    }
  }
});

test("SSR catalog applies wave and narrative filters without losing normalized query state", async (t) => {
  const records = allCatalogRecords();
  const w2AnchorIds = records
    .filter(({ item }) => item.waveId === "W2" && item.isNarrativeAnchor)
    .map(({ item }) => item.id);
  assert.equal(w2AnchorIds.length, 18, "/catalog W2 anchors: expected six complete anchor families.");

  await t.test("returns only W2 narrative anchor rows", async () => {
    const result = await render("/catalog?wave=W2&narrative=anchor");
    assertPublicHtml(result, "/catalog W2 anchors");
    assert.equal(selectedCatalogControl(result.body, "wave-filter"), "W2", "/catalog W2 anchors: selected wave changed.");
    assert.equal(selectedCatalogControl(result.body, "narrative-filter"), "anchor", "/catalog W2 anchors: selected narrative mode changed.");
    assert.deepEqual(
      catalogItemIds(result.body).sort(),
      [...w2AnchorIds].sort(),
      "/catalog W2 anchors: rows include a non-W2 or non-anchor item.",
    );
  });

  await t.test("keeps all active filters while paging server-rendered rows", async () => {
    const result = await render("/catalog?q=botany&wave=W2&discipline=botany&narrative=all&page=2");
    assertPublicHtml(result, "/catalog W2 botany page 2");
    assert.equal(catalogItemLinkCount(result.body), 6, "/catalog W2 botany page 2: expected the remaining six botany rows.");

    const previous = paginationHref(result.body, "catalog-prev");
    assert.equal(previous.searchParams.get("q"), "botany", "/catalog pagination: search query was dropped.");
    assert.equal(previous.searchParams.get("wave"), "W2", "/catalog pagination: wave filter was dropped.");
    assert.equal(previous.searchParams.get("discipline"), "botany", "/catalog pagination: discipline filter was dropped.");
    assert.equal(previous.searchParams.get("narrative"), null, "/catalog pagination: canonical all narrative mode should be omitted.");
    assert.equal(previous.searchParams.get("page"), null, "/catalog pagination: canonical first page should be omitted.");
  });

  await t.test("normalizes unknown new filters without dropping known query fields", async () => {
    const result = await render("/catalog?q=%20botany%20&wave=not-a-wave&discipline=botany&narrative=not-a-mode&status=planned&page=0");
    assertPublicHtml(result, "/catalog unknown wave and narrative");
    assert.ok(result.body.includes('value="botany"'), "/catalog unknown wave and narrative: search text was not normalized.");
    assert.equal(selectedCatalogControl(result.body, "wave-filter"), "all", "/catalog unknown wave and narrative: unknown wave was not normalized.");
    assert.equal(selectedCatalogControl(result.body, "narrative-filter"), "all", "/catalog unknown wave and narrative: unknown narrative mode was not normalized.");
    assert.equal(selectedCatalogControl(result.body, "discipline-filter"), "botany", "/catalog unknown wave and narrative: known discipline was dropped.");
    assert.equal(selectedCatalogControl(result.body, "status-filter"), "planned", "/catalog unknown wave and narrative: known status was dropped.");
    assert.match(result.body, /id="catalog-page-status">第 1 \/ /, "/catalog unknown wave and narrative: invalid page was not normalized to page 1.");
  });
});

test("SSR runtime catalog renders every registered ID and preserves compatibility metadata", async () => {
  const expectedIds = new Set(RUNTIME_ITEMS.map((item) => item.id));
  const renderedIds = new Set();
  const pageCount = Math.ceil(RUNTIME_ITEMS.length / 30);

  assert.equal(RUNTIME_RELEASE.itemCount, 926, "/runtime metadata: registered count changed.");
  assert.equal(RUNTIME_RELEASE.baselineCount, 150, "/runtime metadata: compatibility baseline count changed.");
  assert.equal(RUNTIME_RELEASE.newRegistrationCount, 776, "/runtime metadata: generated registration count changed.");
  assert.equal(RUNTIME_RELEASE.catalogCount, 810, "/runtime metadata: mapped planning count changed.");
  assert.equal(RUNTIME_RELEASE.legacyMappingCount, 34, "/runtime metadata: legacy mapping count changed.");
  assert.equal(
    RUNTIME_RELEASE.itemCount,
    RUNTIME_RELEASE.baselineCount + RUNTIME_RELEASE.newRegistrationCount,
    "/runtime metadata: total must remain baseline plus generated registrations.",
  );
  assert.equal(
    RUNTIME_RELEASE.catalogCount,
    RUNTIME_RELEASE.newRegistrationCount + RUNTIME_RELEASE.legacyMappingCount,
    "/runtime metadata: planned mappings must remain generated plus legacy mappings.",
  );

  for (let page = 1; page <= pageCount; page += 1) {
    const result = await render(`/runtime?page=${page}`);
    assertPublicHtml(result, `/runtime?page=${page}`);

    const pageIds = runtimeItemIds(result.body);
    const expectedRowCount = Math.min(30, RUNTIME_ITEMS.length - (page - 1) * 30);
    assert.equal(pageIds.length, expectedRowCount, `/runtime?page=${page}: wrong registered row count.`);
    for (const id of pageIds) {
      assert.ok(expectedIds.has(id), `/runtime?page=${page}: unexpected registered ID ${id}.`);
      assert.ok(!renderedIds.has(id), `/runtime?page=${page}: duplicate registered ID ${id}.`);
      renderedIds.add(id);
    }
  }

  assert.equal(renderedIds.size, RUNTIME_RELEASE.itemCount, "/runtime: rendered ID count diverged from release metadata.");
  assert.deepEqual(
    [...renderedIds].sort(),
    [...expectedIds].sort(),
    "/runtime: rendered IDs diverged from the registered catalog.",
  );

  const baselineItems = RUNTIME_ITEMS.filter((item) => item.source === "baseline-runtime");
  assert.equal(baselineItems.length, RUNTIME_RELEASE.baselineCount, "/runtime: baseline metadata no longer matches catalog entries.");
  assert.deepEqual(
    baselineItems.map((item) => item.runtimeId).sort(),
    [...LEGACY_BASELINE_RUNTIME_IDS].sort(),
    "/runtime: the frozen 150-ID compatibility baseline changed.",
  );
  for (const id of LEGACY_BASELINE_RUNTIME_IDS) {
    assert.ok(renderedIds.has(id), `/runtime: compatibility ID ${id} disappeared from SSR.`);
  }

  const generatedItems = RUNTIME_ITEMS.filter((item) => item.generated);
  const mappedItems = RUNTIME_ITEMS.filter((item) => typeof item.planningId === "string");
  const legacyMappedItems = mappedItems.filter((item) => item.legacy);
  assert.equal(generatedItems.length, RUNTIME_RELEASE.newRegistrationCount, "/runtime: generated entry count diverged from release metadata.");
  assert.equal(mappedItems.length, RUNTIME_RELEASE.catalogCount, "/runtime: planning mapping count diverged from release metadata.");
  assert.equal(new Set(mappedItems.map((item) => item.planningId)).size, RUNTIME_RELEASE.catalogCount, "/runtime: planning mappings must stay one-to-one.");
  assert.equal(legacyMappedItems.length, RUNTIME_RELEASE.legacyMappingCount, "/runtime: legacy planning mapping count diverged from release metadata.");
  assert.equal(LEGACY_RUNTIME_MAPPINGS.length, RUNTIME_RELEASE.legacyMappingCount, "/runtime: frozen legacy mapping table count diverged from release metadata.");
  for (const mapping of LEGACY_RUNTIME_MAPPINGS) {
    const item = RUNTIME_ITEMS.find((candidate) => candidate.runtimeId === mapping.runtimeId);
    assert.ok(item, `/runtime: mapped legacy runtime ID ${mapping.runtimeId} is missing.`);
    assert.equal(item.planningId, mapping.planningId, `/runtime: ${mapping.runtimeId} no longer maps to ${mapping.planningId}.`);
    assert.equal(item.legacy, true, `/runtime: ${mapping.runtimeId} lost its legacy marker.`);
    assert.equal(item.generated, false, `/runtime: ${mapping.runtimeId} must not be reclassified as generated.`);
  }
});

test("SSR runtime catalog normalizes query and group controls", async (t) => {
  const selectedGroup = "便携电力装备";

  await t.test("trims q and retains a known group", async () => {
    const result = await render(
      `/runtime?q=${encodeURIComponent(" advanced_battery ")}&group=${encodeURIComponent(selectedGroup)}`,
    );
    assertPublicHtml(result, "/runtime normalized query");
    assert.ok(
      result.body.includes('value="advanced_battery"'),
      "/runtime normalized query: search text was not normalized.",
    );
    assert.deepEqual(
      selectedRuntimeGroups(result.body),
      [selectedGroup],
      "/runtime normalized query: known group was not selected.",
    );
    assert.deepEqual(
      runtimeItemIds(result.body),
      ["advanced_battery"],
      "/runtime normalized query: filtered production ID is missing.",
    );
  });

  await t.test("falls back to all for an unknown group and invalid page", async () => {
    const firstPage = await render("/runtime");
    const result = await render("/runtime?group=not-a-runtime-group&page=0");
    assertPublicHtml(result, "/runtime invalid controls");
    assert.deepEqual(
      selectedRuntimeGroups(result.body),
      ["all"],
      "/runtime invalid controls: unknown group was selected instead of all.",
    );
    assert.deepEqual(
      runtimeItemIds(result.body),
      runtimeItemIds(firstPage.body),
      "/runtime invalid controls: invalid page did not normalize to the first page.",
    );
  });
});

test("SSR runtime catalog escapes hostile search text", async () => {
  const hostile = "<img src=x onerror=alert(1)>";
  const result = await render(`/runtime?q=${encodeURIComponent(hostile)}`);
  assertPublicHtml(result, "/runtime hostile query");

  assert.ok(
    result.body.includes('value="&lt;img src=x onerror=alert(1)&gt;"'),
    "/runtime hostile query: escaped query text is missing.",
  );
  assert.doesNotMatch(
    result.body,
    /<img src=x onerror=alert\(1\)>/,
    "/runtime hostile query: raw HTML query text was rendered.",
  );
});

test("SSR planning renders the canonical four-act, nine-wave discipline campaign", async () => {
  const result = await render("/planning");
  assertPublicHtml(result, "/planning campaign");
  assert.ok(result.body.includes('id="campaign"'), "/planning: campaign section is missing.");
  assert.ok(result.body.includes('id="vertical-slices"'), "/planning: vertical slice section is missing.");
  assert.ok(result.body.includes('id="curriculum"'), "/planning: curriculum section is missing.");

  for (const field of ["premise", "playerIdentity", "centralQuestion", "ending"]) {
    assert.ok(result.body.includes(escapedHtml(CAMPAIGN[field])), `/planning: campaign ${field} is missing.`);
  }

  for (const [index, act] of CAMPAIGN_ACTS.entries()) {
    const actBody = campaignActBody(result.body, act.id);
    assert.ok(actBody.includes(escapedHtml(act.title)), `/planning ${act.id}: act title is missing.`);
    assert.deepEqual(
      campaignWaveIds(actBody),
      EXPECTED_ACT_WAVES[index],
      `/planning ${act.id}: waves escaped their canonical act.`,
    );
  }

  for (const [waveId, expectedDisciplineIds] of Object.entries(EXPECTED_WAVE_DISCIPLINES)) {
    const waveBody = campaignWaveBody(result.body, waveId);
    assert.deepEqual(
      disciplineLinks(waveBody).sort(),
      [...expectedDisciplineIds].sort(),
      `/planning ${waveId}: discipline routes no longer match the canonical wave.`,
    );
    assert.match(waveBody, /门禁/, `/planning ${waveId}: gate copy is missing.`);
  }
  assert.doesNotMatch(result.body, /食品工艺|光学|终局工程/, "/planning: legacy mismatched wave labels returned.");
});

test("SSR item pages show progression answers and story records only for narrative anchors", async (t) => {
  const anchorRecord = catalogRecordForItem(ANCHOR_ITEM_ID);
  const ordinaryRecord = catalogRecordForItem(NON_NARRATIVE_ITEM_ID);
  assert.ok(anchorRecord.item.story, `${ANCHOR_ITEM_ID}: expected a narrative anchor fixture.`);
  assert.equal(ordinaryRecord.item.story, null, `${NON_NARRATIVE_ITEM_ID}: expected a non-anchor fixture.`);

  await t.test("anchor item renders five answers, its story, and its stable family sequence", async () => {
    const result = await render(`/items/${ANCHOR_ITEM_ID}`);
    assertPublicHtml(result, `/items/${ANCHOR_ITEM_ID}`);
    assert.ok(result.body.includes('id="item-progression"'), `/items/${ANCHOR_ITEM_ID}: progression section is missing.`);
    assert.deepEqual(
      definitionTerms(result.body).filter((term) => ["为何现在", "需要什么", "产出什么", "失败怎么办", "下一步去哪"].includes(term)),
      ["为何现在", "需要什么", "产出什么", "失败怎么办", "下一步去哪"],
      `/items/${ANCHOR_ITEM_ID}: progression questions changed.`,
    );

    const sequence = elementBodyById(result.body, "family-sequence", `/items/${ANCHOR_ITEM_ID}`);
    assert.ok(sequence.includes(anchorRecord.item.familyKey), `/items/${ANCHOR_ITEM_ID}: stable family key is missing.`);
    for (const relationId of [anchorRecord.item.previousItemId, anchorRecord.item.nextItemId].filter(Boolean)) {
      assert.ok(sequence.includes(`href="/items/${relationId}"`), `/items/${ANCHOR_ITEM_ID}: family sequence lost ${relationId}.`);
    }

    const story = elementBodyById(result.body, "restorer-record", `/items/${ANCHOR_ITEM_ID}`);
    assert.ok(story.includes("复原者记录"), `/items/${ANCHOR_ITEM_ID}: story heading is missing.`);
    assert.ok(story.includes(escapedHtml(anchorRecord.item.story.text)), `/items/${ANCHOR_ITEM_ID}: story text is missing.`);
    assert.ok(story.includes(escapedHtml(anchorRecord.item.story.anchorReason)), `/items/${ANCHOR_ITEM_ID}: story reason is missing.`);
  });

  await t.test("ordinary item does not render an invented story record", async () => {
    const result = await render(`/items/${NON_NARRATIVE_ITEM_ID}`);
    assertPublicHtml(result, `/items/${NON_NARRATIVE_ITEM_ID}`);
    assert.ok(!result.body.includes('id="restorer-record"'), `/items/${NON_NARRATIVE_ITEM_ID}: non-anchor item rendered a story record.`);
  });
});

test("SSR discipline detail exposes the discipline's canonical campaign wave", async () => {
  const discipline = DISCIPLINES.find((candidate) => candidate.id === "botany");
  assert.ok(discipline, "/disciplines/botany: expected stable discipline fixture.");
  assert.equal(discipline.waveId, "W2", "/disciplines/botany: catalog wave changed.");

  const result = await render("/disciplines/botany");
  assertPublicHtml(result, "/disciplines/botany");
  findOpeningTag(
    result.body,
    (candidate) => attributeValue(candidate[0], "data-wave-id") === "W2",
    "/disciplines/botany wave marker",
  );
  assert.ok(result.body.includes('href="/planning#wave-W2"'), "/disciplines/botany: planning wave link is missing.");
});

test("SSR escapes hostile catalog search text", async () => {
  const hostile = "<img src=x onerror=alert(1)>";
  const result = await render(`/catalog?q=${encodeURIComponent(hostile)}`);
  assertPublicHtml(result, "/catalog hostile query");

  assert.ok(
    result.body.includes('value="&lt;img src=x onerror=alert(1)&gt;"'),
    "/catalog hostile query: escaped query text is missing.",
  );
  assert.doesNotMatch(
    result.body,
    /<img src=x onerror=alert\(1\)>/,
    "/catalog hostile query: raw HTML query text was rendered.",
  );
});

test("SSR emits discovery artifacts without private routes", async () => {
  const sitemap = await render("/sitemap.xml");
  assert.equal(sitemap.response.status, 200, "/sitemap.xml: expected HTTP 200.");
  assert.match(
    sitemap.response.headers.get("content-type") ?? "",
    /^application\/xml;\s*charset=UTF-8$/i,
    "/sitemap.xml: expected XML content type.",
  );
  assert.match(
    sitemap.response.headers.get("cache-control") ?? "",
    PUBLIC_CACHE_CONTROL,
    "/sitemap.xml: sitemap must be cacheable.",
  );
  assert.ok(
    sitemap.body.includes(`<loc>${ORIGIN}/items/materials.fire-materials.fire-rod</loc>`),
    "/sitemap.xml: public item route is missing.",
  );
  assert.ok(
    sitemap.body.includes(`<loc>${ORIGIN}/runtime</loc>`),
    "/sitemap.xml: public runtime route is missing.",
  );
  assert.ok(
    !sitemap.body.includes(`<loc>${ORIGIN}/console</loc>`),
    "/sitemap.xml: private console route leaked.",
  );

  const robots = await render("/robots.txt");
  assert.equal(robots.response.status, 200, "/robots.txt: expected HTTP 200.");
  assert.match(
    robots.response.headers.get("content-type") ?? "",
    /^text\/plain;\s*charset=UTF-8$/i,
    "/robots.txt: expected text content type.",
  );
  assert.match(
    robots.response.headers.get("cache-control") ?? "",
    PUBLIC_CACHE_CONTROL,
    "/robots.txt: robots file must be cacheable.",
  );
  for (const path of ["/api/", "/console", "/admin", "/setup"]) {
    assert.ok(robots.body.includes(`Disallow: ${path}`), `/robots.txt: ${path} is not disallowed.`);
  }
  assert.ok(robots.body.includes(`Sitemap: ${ORIGIN}/sitemap.xml`), "/robots.txt: controlled sitemap URL is missing.");
});

test("SSR returns a noindex 404 document for unknown routes", async () => {
  const unknownPath = "/not-a-real-route";
  const result = await render(unknownPath);

  assert.equal(result.response.status, 404, `${unknownPath}: expected HTTP 404.`);
  assert.match(
    result.response.headers.get("content-type") ?? "",
    HTML_CONTENT_TYPE,
    `${unknownPath}: expected an HTML content type.`,
  );
  assert.ok(result.body.includes(NOINDEX_ROBOTS), `${unknownPath}: 404 document must not be indexed.`);
  assert.ok(result.body.includes(`<code>${unknownPath}</code>`), `${unknownPath}: missing-path context is absent.`);
  assertCanonical(result.body, result.url, unknownPath);
});
