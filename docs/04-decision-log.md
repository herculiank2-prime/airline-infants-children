# 04 — Decision log (ADR)

Format: context → options → decision → trade-offs → consequences. Status: **Accepted** unless noted. ADR-001..006 are the Part A decision records (summarised in `SOLUTION_DESIGN.md` §9). ADR-007 onwards are engineering decisions taken during the build. Integration numbers refer to the integration register (SOLUTION_DESIGN §5).

---

## ADR-001 — Server-rendered HTL, not a headless React front end

- **Context.** A public content page whose main consumers are visitors on mobile and search/AI crawlers (I-08). Six authors use the AEM page editor. The header and footer already exist as HTL experience fragments. A non-AEM consumer needs the accordion data (HD-02).
- **Options.**
  - (a) HTL + Core Components, rendered on publish and cached by Dispatcher and CDN.
  - (b) React with the SPA Editor.
  - (c) Headless Next.js reading CFs via GraphQL, with Universal Editor.
- **Decision.** (a) HTL.
- **Trade-offs.** (a) gives complete HTML at first byte (best LCP and crawlability), zero extra runtime, existing editor workflows and XF reuse. It is weaker for highly interactive or app-like UI, and front-end developers must know HTL. (b) is deprecated for new projects. (c) needs a Node runtime, a separate deployment and CDN rules, and Universal Editor training for six authors; it duplicates header/footer in another stack.
- **Consequences.** Headless reuse is still delivered through persisted queries (I-07). Interactivity is limited to progressive-enhancement TypeScript. If a future app-like experience is needed, the CF model and persisted queries are already the contract.

## ADR-002 — Accordion items as Content Fragments, not page content

- **Context.** Infant rules are legally reviewed, appear on the web page, in FAQ structured data and in an app (HD-02), and will be reused for NZ (SD-10).
- **Options.**
  - (a) Core Accordion with child components authored on the page.
  - (b) Content Fragments (model `travel-question`) selected by folder or tag.
  - (c) Experience fragments per item.
- **Decision.** (b).
- **Trade-offs.** (b) is a single source for web, JSON-LD and GraphQL. Legal reviews a fragment once, and it is reusable across pages and markets. It costs a custom component, a cache-freshness problem (ADR-004), and authors working in two places (Assets for copy, Sites for placement). (a) is fastest to build but duplicates copy per page and market and cannot feed GraphQL. (c) has no structured fields, so no clean JSON-LD.
- **Consequences.** We add `sortOrder` to the model (ADR-016) so all consumers order identically. Authors need the "Managing travel questions" section of `AUTHORING.md`.

## ADR-003 — Joey Club teaser extends Core Teaser via proxy + delegation

- **Context.** TS-01 needs title, rich text, image, link label and target, a policy-set heading level, and `rel="noopener noreferrer"` for new-tab links. Core `LinkImpl` 2.28 does not emit `rel`.
- **Options.**
  - (a) Proxy `airline/components/teaser` with a delegating Sling Model that decorates `getLink()`/`getActions()`.
  - (b) Custom component and model.
  - (c) Client-side JS adding `rel`.
- **Decision.** (a).
- **Trade-offs.** (a) keeps the Core dialog, image handling (lazy, responsive), data layer, policy `titleType` and upgrades, for ~120 lines of Java. The risk is a Core HTL change that bypasses `link.htmlAttributes`, which tests catch. (b) duplicates mature Core code. (c) is incorrect for crawlers and non-JS clients and is a security smell.
- **Consequences.** Core HTL is untouched. A test asserts `rel` on both the CTA and the title link. The upgrade checklist includes re-running teaser tests.

## ADR-004 — Caching and invalidation of fragment-driven content

- **Context.** Page HTML embeds fragment content. Publishing a fragment invalidates Dispatcher stat files only along `/content/dam/...` (statfileslevel 2), so cached pages keep the old answer. Legal needs changes visible within 5 minutes (A-02).
- **Options.**
  - (a) Lower `statfileslevel` so any activation flushes all HTML.
  - (b) TTL bound: HTML cached ≤ 300 s at Dispatcher (`enableTTL` + `max-age` from AEM) and CDN (`s-maxage=300`).
  - (c) Targeted invalidation: on fragment publish, resolve referencing pages and send SCD `INVALIDATE` + CDN purge (I-13).
  - (d) Load fragments client-side via the persisted query.
- **Decision.** (b) built now, (c) designed (story S-14).
- **Trade-offs.** (b) is simple, has no new moving parts and bounds staleness at 5 min. It costs more origin renders (one per page per 5 min per Dispatcher), which is negligible for this page. (a) flushes the whole site on every DAM publish, causing cache-hit collapse. (c) gives instant freshness but needs a reference resolver, service user and CDN purge key. (d) hurts LCP and SEO, and the JSON-LD would no longer be server-rendered (SE-01).
- **Consequences.** `HtmlCacheHeadersFilter` (publish only) emits `Cache-Control: max-age=60, s-maxage=300, stale-while-revalidate=300, stale-if-error=86400` for `/content/airline/**.html`. Dispatcher `/enableTTL "1"` honours it. Persisted queries use the same 300 s.

## ADR-005 — Joey Club enquiry reaches Salesforce through an AEM servlet, synchronously, with bounded retries

- **Context.** Enquiries contain PII (parent name, email, child's age). There is a Salesforce REST API. Secrets must not reach the browser. The AEM publish tier is horizontally scaled and stateless.
- **Options.**
  - (a) Browser posts directly to Salesforce Web-to-Lead.
  - (b) AEM servlet validates and calls Salesforce REST synchronously, with ≤ 3 retries in a 4 s budget.
  - (c) AEM persists to a Sling Job queue and a worker posts to Salesforce.
  - (d) Adobe App Builder action as middleware.
- **Decision.** (b).
- **Trade-offs.** (b) keeps secrets server-side (Cloud Manager secret variables), makes validation authoritative and holds no PII at rest. The cost is that a Salesforce outage shows the user a retry message, so enquiries are lost if the user does not retry. (c) survives outages but persists PII in the publish JCR (backups, retention, GDPR/Privacy Act scope). (a) exposes the org ID, offers no server validation and limits field mapping. (d) adds a runtime and team skill set for low volume.
- **Consequences.** Monitor the failure rate. Revisit (c) with encryption, or (d), if the outage impact proves material.

## ADR-006 — New Zealand via MSM live copy of `au/en`

- **Context.** NZ launches next quarter. Same language, mostly the same rules. The project was generated single-country (`/content/airline/au/en`).
- **Options.**
  - (a) MSM live copy `au/en` → `nz/en`.
  - (b) Introduce language masters now.
  - (c) Language copy + translation.
  - (d) Independent NZ site.
- **Decision.** (a), with market tags on fragments.
- **Trade-offs.** (a) is cheap now (~2 days) and inherits structure and component configuration. Local differences break inheritance per component. Moving later to language masters costs re-parenting (~3–5 days). (b) has the lowest long-term cost but restructures a live AU site now. (c) does not apply (same language). (d) duplicates legally reviewed content.
- **Consequences.** Fragments stay shared. NZ-only rules are new fragments tagged `airline:market/nz`, and the NZ accordion uses tag mode. Core XF localisation resolves NZ header/footer automatically.

---

## ADR-007 — Hero renders DAM renditions through its own model (not Core Image delegation)

- **Context.** HB-03 requires "a Sling Model you wrote, behind an interface". The hero image is the LCP element and must be eager, sized and responsive. Web-optimised delivery / Dynamic Media is not available on the local SDK.
- **Options.**
  - (a) Model builds `src`/`srcset`/`width`/`height` from DAM renditions and metadata.
  - (b) Delegate to Core Image v3 (adaptive image servlet, `.coreimg.`).
  - (c) Dynamic Media URLs.
- **Decision.** (a).
- **Trade-offs.** (a) gives full control of eager loading, `fetchpriority` and dimensions, all unit-testable, but depends on renditions existing (processing profile). (b) is lazy by default, and getting eager plus `fetchpriority` needs policy tweaks and a second resource. (c) is not runnable locally (Q-02).
- **Consequences.** A processing profile must produce `cq5dam.web.640.640` and `cq5dam.web.1280.1280`. If none exist, the model falls back to the original.

## ADR-008 — Landmarks through a container proxy with a policy-driven element

- **Context.** PT-02 needs `header`, `nav`, `main` and `footer` landmarks. Core Container v1 renders `<div>` (with an optional ARIA role).
- **Options.**
  - (a) Use Core's `roleAttribute` (`banner`/`main`/`contentinfo`).
  - (b) Proxy container overriding `responsiveGrid.html`/`simple.html` with `data-sly-element` from a policy.
  - (c) Override page `body.html`.
- **Decision.** (b).
- **Trade-offs.** (b) produces semantic elements (preferred over roles, per the first rule of ARIA use) for two small HTL overrides and a model, but must be kept in sync with Core container HTL on upgrade. (a) has no code but uses ARIA where native HTML exists. (c) couples the page to the structure.
- **Consequences.** `data-sly-element` only accepts an allow-list of elements, and the model normalises anything else to `div`.

## ADR-009 — Archetype 58 post-generate script patched for Windows symlinks

- **Context.** Archetype 58's post-generate script creates symlinks for Dispatcher `enabled_*` files. On Windows without Developer Mode this fails ("A required privilege is not held by the client"). Plugin 3.2.1 also fails, on Groovy `XmlSlurper`.
- **Options.**
  - (a) Enable Developer Mode (needs admin).
  - (b) Generate in WSL/Docker (not available).
  - (c) Patch a local copy of the archetype to copy instead of symlinking, installed as `58-winfix`.
- **Decision.** (c).
- **Trade-offs.** Generated content is otherwise byte-identical to archetype 58. The `enabled_*` files are copies, not symlinks, so they must be kept identical to `available_*`. Cloud Manager accepts regular files in flexible mode.
- **Consequences.** Recorded in the README (EN-01). If the project later moves to Linux CI, recreate the symlinks with `ln -sf`.

## ADR-010 — Node.js raised from archetype default v16.17.0 to v24 LTS

- **Context.** The archetype pins Node 16 (end of life). Playwright needs Node ≥ 18. The assignment asks for a current LTS.
- **Decision.** Use `v24.22.3` / npm `11.19.1` for `ui.frontend` and `ui.tests` via `frontend-maven-plugin`.
- **Trade-offs.** Some older webpack-era dev dependencies may print deprecation warnings. The build is verified green.
- **Consequences.** Cloud Manager uses the plugin-downloaded Node, so no pipeline change is needed.

## ADR-011 — Compile against the local SDK's API version

- **Context.** The archetype resolved `aem.sdk.api` to `2026.11.29643…` (latest on Maven Central). The local SDK is `2026.2.24678…`. Compiling against a newer API can produce OSGi `Import-Package` ranges the older runtime does not satisfy.
- **Decision.** `aem.sdk.api = 2026.2.24678.20260226T154829Z-260200`.
- **Consequences.** Bump together with the local SDK and the Cloud Manager environment version. The AEM analyser still validates against the latest SDK feature model in the build.

## ADR-012 — Playwright replaces the archetype's Cypress UI-test module

- **Context.** EN-04 allows Playwright or Cypress. The Cypress module took 8 of the baseline build's 13 minutes (`npm ci` of the Cypress binary). Playwright can drive the installed Edge/Chrome without downloading browsers, and `@axe-core/playwright` is first-class.
- **Decision.** Playwright in `ui.tests/test-module`, keeping the Cloud Manager Docker-context packaging.
- **Consequences.** The default `mvn install` only packages the test module. Tests run with `npm test` against a running AEM (README).

## ADR-013 — Pull requests without a remote

- **Context.** EN-05 requires ≥ 3 small PRs. No Git remote or hosting was provisioned, and creating one on the developer's account is outside the brief.
- **Decision.** Feature branches merged with `git merge --no-ff`. Each merge commit carries the PR title, and `docs/pull-requests/PR-00N.md` holds the reviewer-ready description (scope, changes, testing, risks, checklist).
- **Consequences.** Branches and merge history are reviewable with `git log --graph`. Next step: push to the client's Git and replay them as hosted PRs.

## ADR-014 — FAQPage JSON-LD rendered in `<head>` by a page-level model

- **Context.** SE-01 wants one FAQPage per page, generated server-side from the same fragments.
- **Options.**
  - (a) Accordion component emits its own JSON-LD in the body.
  - (b) Page `customheaderlibs.html` + `FaqStructuredData` aggregating all `travelquestions` components on the page.
- **Decision.** (b).
- **Trade-offs.** (b) guarantees a single FAQPage even with two accordions and keeps the head as the metadata home. It costs a second call to `TravelQuestionService` per accordion (bounded, ≤ 200 nodes). (a) can produce multiple FAQPage blocks.
- **Consequences.** Both models build `QuestionSelection` from the same resource properties via one factory, so the outputs cannot diverge.

## ADR-015 — Dispatcher allow-list instead of including `default_filters.any`

- **Context.** The archetype's defaults allow a broad set of extensions under `/content/*` plus forms, commerce, screens and `model.json`. DP-01 asks for only what the page needs.
- **Decision.** `filters.any` starts with deny-all and adds explicit rules. `default_filters.any` stays in the repo (immutable file) but is not included.
- **Trade-offs.** Smaller attack surface. New features must add rules deliberately, which is documented in the README.
- **Consequences.** `model.json` and other selectors are blocked on publish. Author is unaffected.

## ADR-016 — `sortOrder` field on the travel-question model

- **Context.** GraphQL cannot sort by JCR node order. The accordion and the persisted query must return the same order (HD-02).
- **Decision.** Add an optional integer `sortOrder`. Sort by `sortOrder`, then `question`.
- **Consequences.** A small departure from "question, answer and tags", recorded in the README departures table.

## ADR-017 — Namespaced data-layer event names

- **Context.** Core Components already push `cmp:click`, `cmp:show` and `cmp:hide`. DL-01 asks for event names that match the design.
- **Decision.** `airline:accordion:expand`, `airline:accordion:collapse`, `airline:teaser:click`, with `eventInfo.path = component.<id>`.
- **Trade-offs.** Clear ownership and no double counting with Core events, but Tags rules must subscribe to our names.
- **Consequences.** The contract lives in SOLUTION_DESIGN §8.3, and Playwright tests assert the exact names.
