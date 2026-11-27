# Airline travel information: "Flying with infants and children"

An AEM as a Cloud Service project for a fictional airline's "Flying with infants and children" page. It has three components: a hero banner, a Joey Club teaser and a "Booking for infants (under 2 years)" accordion. The accordion is driven by Content Fragments, which also feed FAQPage JSON-LD and a persisted GraphQL query for non-AEM apps.

| Document | What it is for |
|---|---|
| [`SOLUTION_DESIGN.md`](SOLUTION_DESIGN.md) | Part A solution design (architecture, NFRs, integrations, risks, delivery plan, AI practice) |
| [`AUTHORING.md`](AUTHORING.md) | How authors build and change the page, with screenshots |
| [`AI_NOTES.md`](AI_NOTES.md) | How AI was used on this build, and what was not delegated |
| [`docs/01-analysis-discovery.md`](docs/01-analysis-discovery.md) | Requirement IDs, assumptions, environment findings |
| [`docs/02-design.md`](docs/02-design.md) | Technical design for the build |
| [`docs/03-architect-review.md`](docs/03-architect-review.md) | Independent architect review and its resolution |
| [`docs/04-decision-log.md`](docs/04-decision-log.md) | ADR-001 to ADR-028 |
| [`docs/05-build-deploy-test.md`](docs/05-build-deploy-test.md) | Build, deploy, replicate and test, step by step, with results |
| [`docs/pull-requests/`](docs/pull-requests/) | PR-001 to PR-006 descriptions (ADR-013: no Git remote) |

## Quick start

Prerequisites: Java 21, Maven 3.9, an AEM SDK 2026.2 author on `localhost:4502` and, optionally, a publish instance on `localhost:4503`. Node and npm are downloaded by the build.

```powershell
mvn clean install -PautoInstallSinglePackage          # build, test, deploy to author 4502
mvn clean install -PautoInstallSinglePackagePublish   # same, to publish 4503
```

The single package installs code, configuration, the Content Fragment model, the GraphQL endpoint, persisted queries, the six fragments and the demo pages. No manual steps are needed (EN-02). On publish, replicate the content from author afterwards (see [`docs/05-build-deploy-test.md`](docs/05-build-deploy-test.md) section 3).

| Demo | URL |
|---|---|
| Page on author (preview) | <http://localhost:4502/content/airline/au/en/travel-information/flying-infants-children.html?wcmmode=disabled> |
| Page in the editor | <http://localhost:4502/editor.html/content/airline/au/en/travel-information/flying-infants-children.html> |
| Page on publish | <http://localhost:4503/content/airline/au/en/travel-information/flying-infants-children.html> |
| Component states (empty, decorative and folder-mode cases) | <http://localhost:4503/content/airline/au/en/travel-information/component-states.html> |
| Persisted query on publish | <http://localhost:4503/graphql/execute.json/airline/travel-questions-by-folder%3Bfolder%3D%2Fcontent%2Fdam%2Fairline%2Ftravel-questions%2Finfants> |

## Project set-up (EN-01)

Generated with `com.adobe.aem:aem-project-archetype:58`: `appId=airline`, `groupId=com.airline.travel`, `package=com.airline.travel`, `aemVersion=cloud`, `frontendModule=general`, `datalayer=y`, `singleCountry=y`, `language=en`, `country=au`, `includeDispatcherConfig=y`, `includeExamples=n`, `includeErrorHandler=n`, `amp=n`, `enableSSR=n`. Java 21 (`.cloudmanager/java-version`), with sources compiled for release 11. The build uses SDK API `2026.2.24678` (ADR-011), Core Components 2.28.0 and Node 24 (ADR-010). ADR-009 records the Windows symlink patch to the archetype.

| Module | Contents |
|---|---|
| `core` | Sling Models, `TravelQuestionService`, rich-text sanitizer, FAQ schema builder, cache-header filter, with JUnit 5 and AEM Mocks tests |
| `ui.apps` | Components `hero`, `teaser`, `travelquestions`, page and container proxies, HTL |
| `ui.config` | OSGi configuration (service limits, cache headers, CORS for GraphQL, publish URL mapping) |
| `ui.content` | Template, policies, CF model, GraphQL endpoint and persisted queries, tags, XF placeholders |
| `ui.content.sample` | Demo pages, six fragments, images (ADR-019) |
| `ui.frontend` | TypeScript and SCSS: accordion behaviour, data-layer events |
| `dispatcher` | Allow-list filters, cache rules and TTLs (ADR-015) |
| `ui.tests` | Playwright and axe-core tests against publish (ADR-012) |

## Requirement trace

| ID | Requirement | Where it is built | Evidence |
|---|---|---|---|
| HB-01..03 | Hero: title (only h1), intro, DAM image, alt from DAM or decorative, jump links | `components/hero`, `HeroBannerImpl` | `HeroBannerImplTest`; Playwright skip link, one h1 and eager hero |
| TS-01..02 | Joey Club teaser, `rel="noopener noreferrer"` on `_blank` | Core Teaser proxy and delegation (ADR-003) | `TeaserImplTest`, `RelSafeLinkTest` (guard: fails if Core starts emitting `rel`) |
| AC-01 | Accordion, six titles, ARIA, keyboard | `components/travelquestions`, `travelquestions.ts` | `accordion.spec.ts` (order, ARIA, Tab, Enter, Space, find-in-page) |
| PT-01..02 | Editable template, locked XF header and footer, landmarks, 375 px and 1280 px | `ui.content` template and policies | `accessibility.spec.ts` (landmarks, no horizontal scroll at both widths) |
| HD-01 | CF model; select by folder or tag | `travel-question` model, `TravelQuestionServiceImpl` | `TravelQuestionServiceImplTest` (folder, tags any or all, root check, limits) |
| HD-02 | Persisted query returns the same items | Three queries in `/conf/airline/settings/graphql/persistentQueries` (ADR-024) | Example below; `structured-data.spec.ts` contract test |
| SE-01 | FAQPage JSON-LD from the same fragments | `FaqStructuredDataImpl` in `<head>` (ADR-014) | `FaqStructuredDataImplTest`, Playwright compares JSON-LD with the accordion |
| SE-02 | Title, description, canonical | Core Page and page properties | Playwright metadata test (canonical: see departures) |
| DL-01 | Data-layer events with ID and title | `airline:accordion:expand` and `collapse`, `airline:teaser:click` (ADR-017) | `datalayer.spec.ts` |
| DP-01 | Allow-list Dispatcher, caching per SD-08 | `dispatcher/src` (ADR-015, ADR-004) | Persisted-query headers verified on publish; SDK validator not run (see departures) |
| PF-01 | Performance against SD-08 | Eager hero with `fetchpriority=high`, lazy other images | Lighthouse results below |
| EN-03 | Unit tests | `core/src/test` | 60 tests, 99.1 % line and 88.6 % branch coverage, JaCoCo gate 80 % |
| EN-04 | UI tests | `ui.tests/test-module` | 30 of 30 passed on publish (15 per viewport) |
| EN-05 | Small PRs | `docs/pull-requests/` | Six PRs merged with `--no-ff` |
| EN-06 | Clean runtime | Console-error fixture | Playwright: no console errors; no `error.log` entries from `com.airline` |
| AI-01..04 | AI practice | `AGENTS.md`, `.cursor/`, `.ai/prompts/`, `AI_NOTES.md` | See [`AI_NOTES.md`](AI_NOTES.md) |

## Persisted query (HD-02)

Three queries are shipped: `travel-questions-by-folder` (`$folder: ID!`), `travel-questions-by-tag` (`$tag`) and `travel-questions-by-tags-all` (`$audience`, `$market`). Each sorts by `sortOrder`, then `question`, and returns at most 20 items. Encode the whole variable tail, or none of it (ADR-022). Encoding only the values returns no items.

```powershell
curl.exe -i "http://localhost:4503/graphql/execute.json/airline/travel-questions-by-folder%3Bfolder%3D%2Fcontent%2Fdam%2Fairline%2Ftravel-questions%2Finfants"
```

Response (verified on publish, 27 Nov 2026; answers shortened):

```http
HTTP/1.1 200 OK
Cache-Control: max-age=60, public, s-maxage=150, stale-while-revalidate=30, stale-if-error=86400
```

```json
{
  "data": {
    "travelQuestionList": {
      "items": [
        {
          "_path": "/content/dam/airline/travel-questions/infants/infants-domestic-flights",
          "question": "Infants on domestic flights",
          "answer": { "html": "<p>On domestic flights an infant under 2 years can travel on your lap at no charge. …</p>", "plaintext": "On domestic flights …" },
          "tags": ["airline:travel/infants", "airline:market/au", "airline:market/nz"],
          "sortOrder": 10
        },
        { "question": "Infants on international flights", "sortOrder": 20 },
        { "question": "Booking a seat for your infant", "sortOrder": 30 },
        { "question": "Travelling with more than one infant per adult", "sortOrder": 40 },
        { "question": "How soon can infants travel after birth?", "sortOrder": 50 },
        { "question": "My infant isn't born yet, how do I book?", "sortOrder": 60 }
      ]
    }
  }
}
```

The tag query takes an unencoded tag ID as well: `.../travel-questions-by-tags-all;audience=airline:travel/infants;market=airline:market/au`.

## Test and quality results

| Check | Result |
|---|---|
| `mvn clean install` | BUILD SUCCESS (12 modules) |
| Unit tests (`core`) | 60 passed, 0 failed |
| JaCoCo (`core`) | 99.1 % line, 88.6 % branch; the build fails below 80 % |
| Playwright and axe on publish | 30 passed: axe WCAG 2.1 AA (no serious or critical issues, panels collapsed and expanded), keyboard, data layer, JSON-LD, metadata, persisted-query contract |
| `error.log` | No entries from `com.airline`. One Adobe WARN from `/libs` xfpage `body.js` |

## Performance (PF-01)

Lighthouse 13.6.0, mobile preset (simulated slow 4G, Moto G Power), headless Edge, on publish 4503 without a Dispatcher or CDN. Report: [`docs/perf/lighthouse-mobile.report.html`](docs/perf/lighthouse-mobile.report.html).

| Metric | SD-08 target | Measured | Status |
|---|---|---|---|
| LCP | ≤ 2.5 s | 3.4 s | Missed |
| CLS | ≤ 0.1 | 0 | Met |
| TBT (proxy for INP) | ≤ 200 ms | 160 ms | Met |
| Page weight | ≤ 1,000 KB | 355 KiB | Met |
| Scores | — | Performance 77, Accessibility 100, Best practices 96, SEO 92 | — |

LCP misses the target because FCP is already 2.9 s. Four render-blocking requests load before the first paint: the ContextHub `cloudsettings.kernel.js` that Core Page adds, and three clientlib CSS files. The SDK serves them without compression or CDN caching. Next steps: drop the ContextHub include (it is not used), inline the critical CSS, and measure again on a Cloud Manager dev environment behind the CDN.

## Departures from the design and known gaps

| Item | Departure | Why, and next step |
|---|---|---|
| DP-01 validator | The Dispatcher SDK validator was not run | The Dispatcher tools are not installed on this machine, and downloading them needs an Adobe Software Distribution login. The Cloud Manager pipeline runs the same validator. Next step: run `bin/validate.sh src` before the first pipeline run |
| PF-01 | LCP 3.4 s against ≤ 2.5 s | See above |
| SE-02 canonical | Root-relative, not absolute (ADR-027) | No Externalizer domain configured. Set it through Cloud Manager environment variables |
| UI tests | Run against publish directly, skipping short-URL 404s (ADR-026) | Set `AEM_DISPATCHER=1` behind the Dispatcher so every 404 counts |
| Fragment reading | JCR `ValueMap`, not the Content Fragment API (ADR-023) | Faster and easier to test. Coupled to the storage layout |
| AI artefacts | The architect-review resolution (AR-28) said they were committed in PR-001; they were committed in PR-006 | Recorded in ADR-028 and in the review resolution |
| Publish instance | Local publish repository reset after corruption (ADR-025) | The old repository is kept as `crx-quickstart.corrupt-20261127` |
| SD-11 Joey Club enquiry | Designed only, as the brief asks | — |

## Self-assessment

| Criterion (weight) | Self-score | Evidence | Weakest point |
|---|---|---|---|
| Solution design and architecture (25 %) | Strong | `SOLUTION_DESIGN.md`, `docs/02-design.md`, 28 ADRs, architect review with 30 findings resolved | Design ran over the page target before the review trimmed it |
| AEM engineering (25 %) | Strong | Sling Models behind interfaces, Core proxy and delegation, CF model, code-owned GraphQL, server-side JSON-LD, allow-list Dispatcher, editable template, one-command deploy | Dispatcher not validated locally; canonical not absolute |
| AI practice (20 %) | Good | `AGENTS.md`, four `.cursor/rules`, `new-aem-component` skill, four prompts, `AI_NOTES.md`, PR records | Artefacts committed late (ADR-028); no second team member to prove the skill |
| Front end, accessibility, performance (10 %) | Good | WAI-ARIA accordion with `until-found`, axe clean at two widths, ACDL events, Lighthouse measured | LCP target missed |
| Testing and quality (10 %) | Strong | 60 unit tests with a JaCoCo gate, 30 Playwright tests, `mvn clean install` green, six small PRs | UI tests not yet run through a Dispatcher |
| Documentation (10 %) | Strong | README, AUTHORING with 10 screenshots, AI_NOTES, docs 01–05, PR descriptions | — |

## Licences

All code is original, built on Apache-2.0 Adobe archetype output. Libraries: Apache Sling and wcm.io AEM Mocks (Apache-2.0), Gson (Apache-2.0, test only), Playwright (Apache-2.0), axe-core (MPL-2.0), TypeScript (Apache-2.0). Images were generated for this exercise (assumption A-03). The accordion answer copy is invented for a fictional airline. Only the six question titles follow the reference page (assumption A-02).

## Next steps

1. Run the Dispatcher validator, then deploy through a Cloud Manager full-stack pipeline.
2. Fix LCP: remove the ContextHub include and inline the critical CSS, then measure behind the CDN.
3. Set the Externalizer `publish` domain and tighten the canonical test.
4. Build the targeted CDN purge on fragment publish (ADR-004 option c) and the Joey Club enquiry (SD-11).
5. Create the NZ live copy (ADR-006).
