# 01 — Analysis and discovery

| Item | Value |
|---|---|
| Assignment | AEM Exercise — "Flying with infants and children" page for a fictional client ("the airline") |
| Source of truth | `AEM-Exercise.docx` (Part A solution design, Part B build, assessment weights) |
| Reference page | `https://www.qantas.com/en-au/travel-information/flying-infants-children` — used only for content structure, layout and behaviour |
| Repository | `C:\Users\krikumar24\source\repos\airline-infants-children` |
| Phase | 1 of 8 (analysis → design → architect review → build → tests → deploy → docs) |

## 1. Problem statement

The airline runs AEM as a Cloud Service (Sites + Assets) behind the Adobe-managed CDN. The infants and children travel information page is being rebuilt and no reusable components exist for it. The page must be authorable by a central team of six authors, its infant travel rules are legally reviewed, it must feed Adobe Analytics through the Adobe Client Data Layer (ACDL), surface well in search engines and AI crawlers, and be ready for a New Zealand version next quarter. A Joey Club enquiry to Salesforce is designed but not built.

## 2. Requirements breakdown

### 2.1 Part A — solution design (`SOLUTION_DESIGN.md`)

| ID | Requirement (condensed) | Planned deliverable |
|---|---|---|
| SD-01 | Context, scope in/out, extra assumptions | §1 of SOLUTION_DESIGN |
| SD-02 | C4 context + container views with all named actors/systems, numbered integrations | `docs/diagrams/c4-context.mmd`, `c4-container.mmd` |
| SD-03 | Diagram as code + rendered image, re-renderable | Mermaid `.mmd` + rendered `.svg` + `scripts/render-diagrams.ps1` |
| SD-04 | Impact coding (existing/new/changed) with legend on every element and integration | Mermaid `classDef` colours + `[E]/[N]/[C]` text tags + legend |
| SD-05 | Component register (purpose, resource type, Core base/custom, data source, caching) | Table |
| SD-06 | Integration register (source, target, protocol, direction, auth, frequency, errors, status) | Table keyed by integration number |
| SD-07 | Content model: template + policies, CF model, tags, DAM metadata | Tables + JCR diagram |
| SD-08 | Numeric NFR targets with reasons (LCP, CLS, INP, weight, WCAG 2.1 AA, cache lifetimes + invalidation, security headers) | Table |
| SD-09 | ≥ 4 decision records (2 fixed: HTL vs headless; accordion items as page content vs CF) | ADR-001..ADR-006 (full text in `docs/04-decision-log.md`) |
| SD-10 | NZ rollout approach + cost of changing later | Section |
| SD-11 | Joey Club enquiry sequence (validation, secrets, retries, PII) — designed only | Sequence diagram + table |
| SD-12 | Risk & open-question register with owner and next step | Table |
| SD-13 | Delivery plan: stories, estimates (days), sequence, dependencies, mid-level hand-offs | Table + dependency graph |
| SD-14 | AI-enabled delivery for a team of five | Section |

### 2.2 Part B — build

| ID | Requirement (condensed) | Acceptance criterion highlights |
|---|---|---|
| HB-01 | Hero banner | Title, intro, DAM image, alt; "On this page" jump links from a multifield; title is the only H1 |
| HB-02 | Alt text from DAM | Blank alt → asset `dc:description`; "decorative" option |
| HB-03 | Hero implementation | Own Sling Model behind an interface; no business logic in HTL; empty → editor placeholder, nothing broken |
| TS-01 | Joey Club teaser | Title, rich text, image, link label + target; heading level from template policy; `_blank` → `rel="noopener noreferrer"` |
| TS-02 | Teaser build choice | Core Teaser proxy + delegation **or** custom, justified by ADR |
| AC-01 | Accordion | Section heading "Booking for infants (under 2 years)", the six reference item titles; buttons with `aria-expanded`/`aria-controls`; Tab/Enter/Space |
| PT-01 | Template | Editable template; header/footer locked placeholder XFs; only needed components allowed |
| PT-02 | Landmarks & layout | header, nav, main, footer landmarks; skip link; correct at 375 px and 1280 px |
| HD-01 | Accordion from CFs | CF model (question, answer, tags); six items are CFs; accordion selects by folder **or** tag via dialog |
| HD-02 | Persisted query | Persisted GraphQL query returns the same items; README shows request/response |
| SE-01 | Structured data | Server-side FAQPage JSON-LD from the same fragments; passes Schema.org validator |
| SE-02 | Page metadata | Title, meta description, canonical authorable and rendered |
| DL-01 | Data layer | Accordion expand/collapse + teaser link click push ACDL events with component ID + title; names match design |
| DP-01 | Dispatcher | Allow-list filters incl. persisted query; cache rules/lifetimes match SD-08; passes SDK validator |
| PF-01 | Performance | Measured vs SD-08 in preview mode, mobile profile; hero not lazy, others lazy; results in README |
| EN-01 | Project set-up | AEM Project Archetype for AEMaaCS; version + parameters in README |
| EN-02 | One-command deploy | `mvn clean install -PautoInstallSinglePackage` to a clean author incl. sample page + fragments |
| EN-03 | Unit tests | JUnit 5 + AEM Mocks: every Sling Model, fragment selection, structured data; build fails on test failure |
| EN-04 | UI tests | Playwright/Cypress: accordion keyboard, data layer events, axe-core scan with no serious/critical |
| EN-05 | Git practice | ≥ 3 small PRs with reviewer-ready descriptions |
| EN-06 | Clean runtime | No browser console errors; no `error.log` entries from our code |
| AI-01..04 | AI practice | Committed context/rules/prompts/skills; team artefact + note; `AI_NOTES.md` ≤ 1 page; ownership |
| Docs | README, AUTHORING (with screenshots), AI_NOTES, SOLUTION_DESIGN | See §2.3 |

### 2.3 Documentation deliverables

Assignment-mandated: `SOLUTION_DESIGN.md`, `README.md`, `AUTHORING.md`, `AI_NOTES.md`. Process-mandated (this engagement): `docs/01-analysis-discovery.md`, `docs/02-design.md`, `docs/03-architect-review.md`, `docs/04-decision-log.md`, `docs/05-build-deploy-test.md`, plus build logs and test reports in `docs/`.

## 3. Requirement → evaluation criteria mapping

Assessment weights: Solution design 25 %, AEM engineering 25 %, AI practice 20 %, Front end/a11y/perf 10 %, Testing & quality 10 %, Documentation 10 %.

| Assessment area (weight) | Requirements that feed it | What reviewers look for → how we address it |
|---|---|---|
| Solution design & architecture (25 %) | SD-01..SD-14 | Diagram and registers agree → single integration numbering `I-01..I-14` reused in diagrams, register, ADRs; realistic NFRs → numbers justified and later *measured* (PF-01); delivery plan runnable → stories with days, dependencies and owner level |
| AEM engineering (25 %) | HB-01..03, TS-01..02, AC-01, PT-01..02, HD-01..02, SE-01..02, DP-01, EN-01..02 | Sling Models behind interfaces, Core proxy + delegation, CF model + persisted query, server-side JSON-LD, allow-list Dispatcher, editable template; *design ↔ code traceability* via requirement IDs in README and code-level Javadoc |
| AI practice (20 %) | AI-01..AI-04, SD-14 | Committed `AGENTS.md`, `.cursor/rules`, reusable prompts/skills, a team skill for new components, honest `AI_NOTES.md`, ownership |
| Front end, a11y, performance (10 %) | PT-02, AC-01, DL-01, PF-01, SD-08 | Semantic landmarks, WAI-ARIA accordion pattern, ACDL events, Lighthouse mobile results vs targets |
| Testing & quality (10 %) | EN-03..EN-06 | JUnit 5 + AEM Mocks with JaCoCo coverage gate, Playwright + axe, clean build, small PRs |
| Documentation (10 %) | README, AUTHORING, docs/* | Deploy from README alone; author from AUTHORING alone (with screenshots) |

## 4. Assumptions (beyond the assumed current state)

| # | Assumption | Why it is reasonable |
|---|---|---|
| A-01 | The build covers the hero, Joey Club teaser and the "Booking for infants (under 2 years)" accordion. Other reference-page sections (meals, baggage, bassinets, Bluey) are out of scope. | The Part B scope table lists exactly these three components |
| A-02 | *(Superseded by ADR-029: sample content now uses the reference copy with permission.)* Accordion answer copy is **our own fictional wording** for "the airline"; only the six item titles follow the reference page | Ground rules forbid client content; AC-01 only constrains titles |
| A-03 | Imagery is generated or self-produced for this exercise (no stock or client images) | Ground rules: everything must be our own work or openly licensed |
| A-04 | "Preview mode" (PF-01) means the AEM author page rendered with `wcmmode=disabled` on the local SDK, measured with Lighthouse using its mobile profile (Moto G Power emulation, simulated slow 4G) | No publish/CDN locally; this is the nearest repeatable environment |
| A-05 | Header and footer XFs are placeholders (logo text, primary nav, footer links), not the airline's real global XFs | PT-01 says placeholder XFs |
| A-06 | Single market (AU, `en`) is built; NZ is designed (SD-10) | Assumed current state |
| A-07 | Joey Club enquiry form, Salesforce integration and the CF-publication-driven page invalidator are **designed only** | Part A says "integrations that are designed but not built" |
| A-08 | The local SDK has no Dispatcher, CDN, Asset Compute or Dynamic Media; DAM renditions are pre-generated and packaged | Asset microservices do not run on the SDK |
| A-09 | Authors are trained AEM users; legal review is a workflow/approval step, not code | Assumed current state |
| A-10 | Analytics rules in AEP Tags (mapping ACDL events to Analytics variables) are owned by the analytics team; we deliver events and a contract | Assumed current state (Analytics live via Tags) |

## 5. Open questions and how we resolved them

| # | Open question | Resolution (assumption taken) |
|---|---|---|
| OQ-01 | HB-03 "With no title or image" — is a title-less hero with an image valid? | The **title is mandatory** (it is the page's only H1). No title → placeholder in the editor, no markup outside edit mode. Image is optional: with a title but no image the hero renders text-only. |
| OQ-02 | Where do jump-link anchors point? | Each multifield item has a label and an anchor ID. Components expose an authorable ID (Core `id` property) and the accordion exposes its own ID; the hero renders `href="#<anchor>"` after normalising the ID. |
| OQ-03 | Which heading levels? | Hero title `h1` (fixed). Teaser title level from template policy (default `h2`). Accordion section heading `h2` and item headings `h3` from policy. |
| OQ-04 | What is "the accordion selects them by folder or tag"? | Dialog radio: **Folder** (direct children of a DAM folder) or **Tags** (fragments under a root folder that carry any/all of the selected tags). Optional max items. |
| OQ-05 | Item order? | The CF model adds an optional `sortOrder` number (ascending, then title). The persisted query sorts the same way, so both consumers see the same order. The extra field is a recorded departure from "question, answer, tags". |
| OQ-06 | Event names for DL-01? | `airline:accordion:expand`, `airline:accordion:collapse`, `airline:teaser:click`, with `eventInfo.path = "component.<id>"` and the component ID and title in `eventInfo`. Namespaced to avoid colliding with Core's `cmp:*` events. |
| OQ-07 | Structured data answer format? | `acceptedAnswer.text` carries the fragment's rich-text HTML (Google supports a safe HTML subset). The JSON is serialised with HTML-safe escaping, so it cannot break out of the `<script>` block. |
| OQ-08 | How is the canonical URL authored? | Core Page v3 renders `page.canonicalLink`; verify on the instance. If it's missing, add a small page model. Default canonical = self URL. |
| OQ-09 | How fresh must legal changes be? | Changes visible within **≤ 5 minutes** after publish at every cache layer (TTL bound). Instant targeted invalidation for fragment changes is designed (I-13). |
| OQ-10 | Can the Dispatcher SDK validator run here? | No Docker and no Dispatcher SDK on this machine. The configuration follows the AEMaaCS flexible-mode structure. Running `validate.sh` / Cloud Manager's validator is a recorded next step (see Environment findings). |
| OQ-11 | Real PRs without a remote? | No remote was provisioned. Work is merged through ≥ 3 feature branches using `--no-ff` merges with PR descriptions committed under `docs/pull-requests/`. They can be replayed as GitHub PRs when a remote exists. |

## 6. Inventory — reuse before build

| Capability | Source | Use in this solution |
|---|---|---|
| Page v3 | Core Components 2.28.0 | Proxy `airline/components/page`: title, description, canonical, robots, skip link (`mainContentSelector` policy), data layer, clientlib loading |
| Container v1 | Core | Proxy `airline/components/container`; adds a policy-controlled landmark element (`header`/`main`/`footer`) |
| Experience Fragment v2 | Core | Header and footer, locked in template structure |
| Navigation v2 | Core | `<nav>` landmark inside the header XF |
| Teaser v2 | Core | Joey Club teaser through proxy + delegation (TS-02 / ADR-003) |
| Image v3 | Core | Used by Teaser for lazy-loaded responsive images (`loading="lazy"` by default) |
| Accordion v1 | Core | **Not reused.** Its items are child components (page content); HD-01 needs fragment-driven items (ADR-002). Its WAI-ARIA behaviour is the reference for our implementation |
| Content Fragment List | Core | **Not reused.** It renders fragments by model/tag but cannot produce an accordion, structured data or our data-layer contract |
| Data layer builder (`DataLayerBuilder`, `ComponentUtils`) | Core API | Consistent component IDs and `data-cmp-data-layer` JSON for custom components |
| Adobe Client Data Layer | Core (enabled by CA config) | Event bus for DL-01 |
| CF models, GraphQL, persisted queries | AEMaaCS Assets | HD-01, HD-02 |
| Tagging | AEM | Fragment selection + DAM metadata |
| Archetype 58 | Adobe | Module layout, Dispatcher flexible-mode config, front-end webpack, repoinit, `.cloudmanager/java-version` |
| wcm.io AEM Mocks 5.5.4, JUnit 5, Mockito 4 | Archetype | Unit tests |

## 7. Risks (initial — the full register lives in SOLUTION_DESIGN §12)

| # | Risk | Mitigation |
|---|---|---|
| R-01 | CF-driven HTML goes stale in the Dispatcher cache after a fragment is published (stat-file invalidation only covers the activated path's subtree) | TTL bound on HTML (publish-only cache-header filter, 5 min) now; targeted invalidator I-13 designed |
| R-02 | Dispatcher validator not runnable locally | Flexible-mode structure; validator run is a release gate in Cloud Manager; checklist in README |
| R-03 | SDK lags Maven Central SDK API (2026.2 vs 2026.11) | Compile against the instance's API version (`2026.2.24678…`) so the bundle resolves locally; bump with the SDK |
| R-04 | Local network may block Playwright browser downloads | Use system Edge/Chrome channels |
| R-05 | Lab performance on the SDK author differs from CDN-backed publish | Documented as indicative; real targets measured with CrUX/RUM after go-live |
| R-06 | Infant rules copy is legally sensitive | Copy lives in CFs, so legal reviews fragments (not pages); workflow designed |

## 8. Environment findings

| Item | Finding | Impact |
|---|---|---|
| OS / shell | Windows 11, PowerShell 5 | Windows path and symlink caveats (below) |
| Java | Microsoft OpenJDK 21.0.10 LTS | Matches `.cloudmanager/java-version = 21` |
| Maven | 3.9.6 | OK (≥ 3.9 required) |
| Node.js | **Not installed globally** | `frontend-maven-plugin` provisions Node per module. The archetype default (v16.17.0, EOL) is raised to a current LTS (ADR-010) |
| Git | 2.45.1 | OK |
| Docker | **Not installed** | Dispatcher SDK validator / local Dispatcher cannot run (R-02) |
| Dispatcher SDK | Not present on disk | As above |
| Browsers | Microsoft Edge and Google Chrome installed | Playwright and Lighthouse use system browsers |
| AEM author | `http://localhost:4502`, `admin/admin`, SDK **2026.2.24678.20260226T154829Z**, run modes `author, sdk, crx3, live, s7connect`. Home `C:\aem-sdk` | Deployment target |
| AEM publish | Installed at `C:\aem-sdk-publish` (port 4503) but **not running** | Only author deployment is verified; publish-only configs are unit-tested |
| Instance hygiene | Other projects are installed (WKND, ACS AEM Commons, Groovy console, several client packages); `error.log` is noisy | EN-06 checks filter `error.log` to `com.airline` loggers and our resource types. ACS Commons is **not** a dependency (not part of AEMaaCS) |
| Archetype | `com.adobe.aem:aem-project-archetype:58` (latest on Maven Central). Plugin 3.2.1 failed (Groovy `XmlSlurper`); 3.3.1 failed creating Dispatcher symlinks (Windows privilege) | Ran 58 with its post-generate script patched locally to copy `enabled_*` files instead of symlinking them (ADR-009). Generated content is otherwise identical |
| Archetype quirk | With `country=au`, the CIF clean-up targets `us/en/...`, so CIF sample pages (`my-account`, `products`, ...) remain under `au/en` | Removed manually |
| Baseline build | `mvn clean install` green in 13 min; 8 min spent in the Cypress `ui.tests` module (`npm ci`) | `ui.tests` is replaced with Playwright (EN-04) |
| Generated params | `appId=airline`, `groupId=com.airline.travel`, `package=com.airline.travel`, `aemVersion=cloud`, `frontendModule=general`, `datalayer=y`, `singleCountry=y`, `language=en`, `country=au`, `includeDispatcherConfig=y`, `includeExamples=n`, `includeErrorHandler=n`, `amp=n`, `enableSSR=n` | Recorded in `archetype.properties` and README (EN-01) |
