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

- **Context.** Page HTML embeds fragment and XF content. Publishing a fragment invalidates Dispatcher stat files only along `/content/dam/...` (statfileslevel 2), so cached pages keep the old answer. Legal needs changes visible within 300 s worst case (A-02).
- **Options.**
  - (a) Lower `statfileslevel` so any activation flushes all HTML.
  - (b) TTL budget across all layers.
  - (c) Targeted invalidation: on fragment/XF publish, resolve referencing pages, then SCD `INVALIDATE` + CDN purge by `Surrogate-Key` (I-13).
  - (d) Load fragments client-side via the persisted query.
- **Decision.** (b) built now, (c) designed (story S-14).
- **Budget (AR-01).** The worst case is when each layer fetches just before the previous one expires:

  Dispatcher TTL 60 + CDN `max-age` 150 + CDN `stale-while-revalidate` 30 + browser `max-age` 60 = **300 s**.

  - `DISABLE_DEFAULT_CACHING` is defined in `global.vars`, so the Adobe CDN default (300 s for HTML) does not apply and the project owns every header.
  - The CDN keeps `Age`, so a browser does not cache a 149 s-old object for another 60 s beyond the budget.
  - `stale-if-error=86400` is outside the budget on purpose: it only applies when the origin is failing, and serving the last good legal copy is better than an error page.
- **Trade-offs.** (b) is simple and has no new moving parts. It costs more origin renders (about one per page per minute per Dispatcher), which is negligible for this page. (a) flushes the whole site on every DAM publish, causing cache-hit collapse. (c) gives near-instant freshness but needs a reference resolver, a service user and a CDN purge key. (d) hurts LCP and SEO, and the JSON-LD would no longer be server-rendered (SE-01).
- **Why the AEM filter emits the headers, not Dispatcher `mod_headers` (AR-23).** `enableTTL` reads `Cache-Control` from the AEM response. `mod_headers` values are added after caching, so the Dispatcher TTL would not see them.
- **Consequences.**
  - `HtmlCacheHeadersFilter` (publish only; GET/HEAD, `.html`, status 200, set lazily) emits:
    - `Cache-Control: max-age=60`
    - `Surrogate-Control: max-age=150, stale-while-revalidate=30, stale-if-error=86400`
    - `Surrogate-Key: airline-page-<hash>`, which is ready for I-13.
  - Dispatcher `/enableTTL "1"` honours the AEM headers, and `/headers` passes `Surrogate-*` through.
  - Persisted queries use the same values.

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
- **Consequences (revised after AR-06).**
  - **Both markets use tag mode with *all*** (audience + market). This is already true for the AU demo page.
  - A shared rule is one fragment tagged `market/au` and `market/nz`. A market-specific variant is a separate fragment with the same `sortOrder` and only its market tag, and the shared fragment loses that market's tag.
  - The rollout config `airline-nz-rollout` excludes the accordion's `tags` property from inheritance. The NZ author sets `market/nz` once.
  - Core XF localisation resolves the NZ header/footer automatically.
  - Persisted query `travel-questions-by-tags-all` serves both markets.

---

## ADR-007 — Hero renders DAM renditions through its own model (not Core Image delegation)

- **Context.** HB-03 requires "a Sling Model you wrote, behind an interface". The hero image is the LCP element and must be eager, sized and responsive. Web-optimised delivery / Dynamic Media is not available on the local SDK.
- **Options.**
  - (a) Model builds `src`/`srcset`/`width`/`height` from DAM renditions and metadata.
  - (b) Delegate to Core Image v3 (adaptive image servlet, `.coreimg.`).
  - (c) Dynamic Media URLs.
- **Decision.** (a).
- **Trade-offs.** (a) gives full control of eager loading, `fetchpriority` and dimensions, all unit-testable, but depends on renditions existing (processing profile). (b) is lazy by default, and getting eager plus `fetchpriority` needs policy tweaks and a second resource. (c) is not runnable locally (Q-02).
- **Consequences (revised after AR-11).**
  - The processing profile `airline-web` produces `cq5dam.web.640.640`, `cq5dam.web.960.960` and `cq5dam.web.1280.1280`.
  - The **original is never in `srcset`**, so a 4 MB upload cannot become the LCP image. If no rendition exists, `src` is the largest available web rendition, or the hero renders text only (and edit mode warns).
  - Rendition URLs are unversioned, so they are cached for 1 day. Dispatcher is invalidated on asset publish, and a rights takedown triggers a CDN purge.
  - **Production path:** if Dynamic Media with OpenAPI is licensed (Q-02), swap the URL builder in `HeroBannerImpl` for DM delivery URLs. That gives AVIF/WebP, smart crop and versioned URLs that can be cached for a year. The model interface does not change.

## ADR-008 — Landmarks through the Style System's default element (revised during build)

- **Context.** PT-02 needs `header`, `nav`, `main` and `footer` landmarks. Core Container v1 renders `<div>` (with an optional ARIA role).
- **Options.**
  - (a) Use Core's `roleAttribute` (`banner`/`main`/`contentinfo`).
  - (b) Proxy container overriding `responsiveGrid.html`/`simple.html` with `data-sly-element` from a policy.
  - (c) Override page `body.html`.
  - (d) Policy property `cq:styleDefaultElement` (AEM Style System), which turns the component's wrapper element into `header`/`main`/`footer`.
- **Decision.** v1.1 chose (b). **Revised during the build to (d).** The generated archetype policies already use `cq:styleDefaultElement` for the header XF, footer XF and main container, and the platform renders it on publish.
- **Trade-offs.** (d) is zero code and zero HTL overrides: no upgrade drift with Core container HTL, and it is the archetype's own pattern. The element is the component's wrapper rather than the inner `cmp-container` div, so the skip-link target `id="main-content"` sits on the inner container inside `<main>`. Keyboard focus still lands inside the main landmark. (b) gave the same HTML for ~80 lines of code and an upgrade risk (R-05). (a) uses ARIA where native HTML exists.
- **Consequences.**
  - No `LandmarkContainer` model.
  - The page policy sets `mainContentSelector=main-content`, and Core Page v3 renders the skip link `href="#main-content"` (verified in Core's `body.skiptomaincontent.html` on the SDK).
  - Playwright asserts one `<main>` that contains `#main-content`, plus one `<header>` and one `<footer>`. This guards R-05 / AR-30.

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
- **Trade-offs.** (b) guarantees a single FAQPage even with two accordions and keeps the head as the metadata home. The head renders before the body, so it runs the selection first, and the accordion reuses that result from a request attribute (one scan per selection per request, ≤ 100 assets). (a) can produce multiple FAQPage blocks.
- **Consequences.**
  - Both models build `QuestionSelection` from the same resource properties via one factory, so the outputs cannot diverge.
  - FAQPage is for semantics and answer engines. Google restricts FAQ rich results to authoritative government and health sites, so no rich-result uplift is claimed (A-08).

## ADR-015 — Dispatcher allow-list instead of including `default_filters.any`

- **Context.** The archetype's defaults allow a broad set of extensions under `/content/*` plus forms, commerce, screens and `model.json`. DP-01 asks for only what the page needs.
- **Decision.** `filters.any` starts with deny-all and adds explicit rules. `default_filters.any` stays in the repo (immutable file) but is not included.
- **Trade-offs.** Smaller attack surface. New features must add rules deliberately, which is documented in the README.
- **Consequences.** `model.json` and other selectors are blocked on publish. Author is unaffected.

## ADR-016 — `sortOrder` field on the travel-question model

- **Context.** GraphQL cannot sort by JCR node order. The accordion and the persisted query must return the same order (HD-02).
- **Decision.** Add a **required** integer `sortOrder` (default 100). Sort by `sortOrder`, then `question` using case-sensitive natural string order in both Java and GraphQL, so the two engines agree (AR-02).
- **Consequences.** A small departure from "question, answer and tags", recorded in the README departures table.

## ADR-017 — Namespaced data-layer event names

- **Context.** Core Components already push `cmp:click`, `cmp:show` and `cmp:hide`. DL-01 asks for event names that match the design.
- **Decision.** `airline:accordion:expand`, `airline:accordion:collapse`, `airline:teaser:click`, with `eventInfo.path = component.<id>`.
- **Trade-offs.** Clear ownership and no double counting with Core events, but Tags rules must subscribe to our names.
- **Consequences.** The contract lives in SOLUTION_DESIGN §8.3, and Playwright tests assert the exact names. Item IDs come from `ComponentUtils.generateId` on the fragment path, so they survive reordering (AR-20). `eventInfo.trigger` separates clicks from find-in-page reveals.

## ADR-018 — Collapsed accordion panels use `hidden="until-found"`

- **Context.** Answers must be in the HTML for crawlers and JSON-LD parity. The collapsed state must be rendered on the server (no CLS). Users searching the page with Ctrl+F should find text inside collapsed panels.
- **Options.**
  - (a) `hidden` attribute.
  - (b) CSS-only collapse (`max-height: 0`).
  - (c) `hidden="until-found"` + `beforematch` handler.
  - (d) `<details>`/`<summary>`.
- **Decision.** (c).
- **Trade-offs.**
  - (c) keeps panels out of the accessibility tree while collapsed (like `hidden`) and lets find-in-page reveal them. Browsers without support treat it as plain `hidden`, so it degrades safely.
  - (b) leaves collapsed content in the accessibility tree.
  - (d) does not provide the heading + button semantics of the WAI-ARIA accordion pattern, and analytics hooks differ.
- **Consequences.**
  - `beforematch` sets `aria-expanded="true"` and pushes `airline:accordion:expand` with `trigger: "find-in-page"`.
  - Without JS, a `<noscript><style>` in the page head shows all panels, so the content is never locked away.

## ADR-019 — Demo content in a separate `ui.content.sample` package

- **Context.** The exercise ships sample pages, fragments and images (EN-02). In a real programme, code packages must never overwrite authored content (AR-05).
- **Options.**
  - (a) Everything in `ui.content` with `mode="merge"`.
  - (b) Split code-owned structure (`ui.content`: templates, policies, CF model, persisted queries, tags) from demo content (`ui.content.sample`).
- **Decision.** (b).
- **Trade-offs.** (b) has one more module, but the filter modes become honest: `update` for templates/policies, `replace` for the model and queries, and `merge` for sample content. Production drops the sample embed from `all`.
- **Consequences.** The README says how to exclude the sample package. Filter roots for DAM and tags are explicit paths, not `/content/dam/airline`.

## ADR-020 — Hero in the template structure, not the allowed-components list

- **Context.** HB-01 requires exactly one `h1`. If the hero is a freely addable component, authors can add two, or none (AR-24).
- **Options.**
  - (a) Allowed component plus an author guideline.
  - (b) A single unlocked hero node in the template structure.
- **Decision.** (b).
- **Trade-offs.** (b) guarantees one hero per page. Authors cannot remove it, only leave it empty, in which case it renders nothing and the page lacks an `h1`. The empty state shows an editor warning, and Playwright asserts one `h1` on the demo page.
- **Consequences.** The `content` container allows only teaser and travel questions.

## ADR-021 — Keep `scripts/aem-http.ps1` as the shared local AEM HTTP helper

- **Context.** An earlier, parallel session left an untracked `scripts/aem-http.ps1`. This session owns the repository and the instance now and needed reliable scripted calls to the local SDK (Sling POST, package manager, persisted-query `PUT`). `Invoke-WebRequest` in Windows PowerShell 5.1 hides error bodies and cannot set some headers cleanly.
- **Options.**
  - (a) Delete it and use ad hoc `curl.exe` calls.
  - (b) Keep and commit it as a dot-sourced helper.
  - (c) Replace it with a Node or Python CLI.
- **Decision.** (b). It was reviewed line by line: basic auth, `Referer` and CSRF token, form or raw bodies, status and body returned without throwing.
- **Trade-offs.** (b) is small, has no dependencies and matches the PowerShell scripts already in `scripts/`. It uses the SDK's default `admin`/`admin` and must never point at a shared environment. Its `StringContent` adds `charset=utf-8`, which the persisted-query endpoint rejects, so that one call sets the content type itself. (a) repeats auth and CSRF handling everywhere. (c) adds a runtime for a local convenience.
- **Consequences.** Used for content clean-up and verification in PR-004 to PR-006. GraphQL checks use `curl.exe`, because .NET normalises `%2F` in URLs (ADR-022).

## ADR-022 — Persisted-query variables: encode the whole tail or nothing

- **Context.** The v1.1 design (§7) showed `…/travel-questions-by-tags-all;audience=airline%3Atravel%2Finfants;market=…`. On the SDK that request returns a validation error (`market` is null) or an empty list: AEM splits on the raw `;` and `/` but does not decode the values the same way.
- **Options.**
  - (a) Encode the values only (design v1.1).
  - (b) Encode the whole `;name=value` tail, including `;` and `=`.
  - (c) Leave the tail unencoded.
  - (d) POST the variables in a JSON body.
- **Decision.** Document (b) as the contract and allow (c) for readability in examples. Measured on author and publish: (b) and (c) return the six items; (a) fails.
- **Trade-offs.** (b) is what Adobe recommends for CDN cache keys and works with any HTTP client. (c) breaks with tag IDs that contain characters outside the URL path set. (d) is not cacheable at the CDN.
- **Consequences.** README, Playwright contract test and design §7 use (b). Clients must not double-encode (`%253A` returns no items).

## ADR-023 — Read fragments through the JCR resource tree, not the Content Fragment API

- **Context.** The design said each asset is "adapted to `ContentFragment`". The service walks up to 100 assets per request on publish and must be unit-testable with AEM Mocks.
- **Options.**
  - (a) `resource.adaptTo(ContentFragment.class)`, then `getElement("question")`.
  - (b) Read `jcr:content/data/@cq:model` and `jcr:content/data/master` as a `ValueMap`.
- **Decision.** (b).
- **Trade-offs.** (b) is one property read per field with no adapter cost, and the test fixture writes exactly the JCR shape the package installs. It couples the service to the structured-fragment layout (`data/master`, `_strucVersion` 2). (a) is the supported API and hides the layout, but adds adapter overhead per asset and needs more mock set-up.
- **Consequences.** If Adobe changes the storage layout, only `TravelQuestionServiceImpl.read()` changes. The upgrade checklist includes running the service tests against the new SDK.

## ADR-024 — GraphQL endpoint and persisted queries are code-owned

- **Context.** On a clean SDK, persisting a query fails until `/conf/airline/settings/graphql/persistentQueries` and an endpoint for the `airline` configuration exist. Authors normally create both in the Configuration Browser and GraphiQL.
- **Options.**
  - (a) Manual set-up steps in the README.
  - (b) Ship the endpoint (`/content/cq:graphql/airline/endpoint`, `merge`) and the queries (`replace`) in `ui.content`.
- **Decision.** (b), keeping EN-02's "no manual steps".
- **Trade-offs.** (b) makes the query text reviewable in Git and identical in every environment. Queries edited in GraphiQL on an environment are overwritten on the next deploy, which is intended. The query text is stored as a binary property (`_jcr_data.binary`), so diffs show it as a file.
- **Consequences.** Change a query by editing its `.binary` file. The endpoint must be published with the queries for publish-side `execute.json` (done by the package install on publish).