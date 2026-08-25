import assert from "node:assert/strict";
import test from "node:test";

import { renderSsrRequest } from "../src/ssr.js";

const ORIGIN = "https://ssr-contract.invalid";
const HTML_CONTENT_TYPE = /^text\/html;\s*charset=UTF-8$/i;
const PUBLIC_CACHE_CONTROL = /^public,\s*max-age=\d+/;
const PRIVATE_CACHE_CONTROL = "private, no-store, max-age=0";
const INDEXABLE_ROBOTS = '<meta name="robots" content="index, follow">';
const NOINDEX_ROBOTS = '<meta name="robots" content="noindex, nofollow">';

const PUBLIC_ROUTES = [
  { path: "/", marker: 'id="top"' },
  { path: "/download", marker: 'href="/downloads/TalexSoulTech-3.0.0-SNAPSHOT.jar" download' },
  { path: "/docs", marker: 'id="tutorials"' },
  { path: "/docs/quick-install", marker: '<article class="tutorial-reader route-reader">' },
  { path: "/disciplines", marker: 'href="/disciplines/materials"' },
  { path: "/disciplines/materials", marker: 'href="/items/materials.fire-materials.fire-rod"' },
  { path: "/items/materials.fire-materials.fire-rod", marker: '<code>materials.fire-materials.fire-rod</code>' },
  { path: "/planning", marker: 'href="#core-loop"' },
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

function selectedCatalogFilters(body) {
  return [...body.matchAll(/<option value="([^"]+)" selected>/g)].map((match) => match[1]);
}

function catalogItemLinkCount(body) {
  return [...body.matchAll(/<a class="cell-title" href="\/items\/[^\"]+">/g)].length;
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
    assert.deepEqual(
      selectedCatalogFilters(result.body),
      ["materials", "implemented"],
      "/catalog valid filters: selected controls changed.",
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
    assert.deepEqual(
      selectedCatalogFilters(result.body),
      ["all", "all"],
      "/catalog invalid filters: unsupported filters were not normalized to all.",
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
