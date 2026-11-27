# UI tests (Playwright + axe-core)

End-to-end tests for the infants and children page (EN-04). They run against **AEM publish** and cover:

| Spec | What it checks |
|---|---|
| `accordion.spec.ts` | Six questions in order, collapsed on load, `aria-expanded`/`aria-controls`, Tab/Enter/Space (AC-01) |
| `datalayer.spec.ts` | `airline:accordion:expand`/`collapse` and `airline:teaser:click` with component ID and title (DL-01) |
| `accessibility.spec.ts` | axe-core WCAG 2.1 AA scan (no serious/critical), landmarks, one `h1`, skip link, no overflow at 375/1280, eager hero, no console errors (PT-02, EN-06) |
| `structured-data.spec.ts` | One FAQPage JSON-LD matching the accordion, title/description/canonical, persisted query contract (SE-01, SE-02, HD-02) |

Every test runs twice: at 375 px and at 1280 px wide.

## Run locally

Uses the installed Microsoft Edge, so no browser download is needed.

```powershell
cd ui.tests/test-module
$env:PATH = "$PWD\node;$env:PATH"          # Node installed here by `mvn install`
$env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = "1"
npm ci
npm test                                     # AEM_PUBLISH_URL defaults to http://localhost:4503
npm run report                               # HTML report in target/html-report
```

Set `PW_CHANNEL=""` to use Playwright's bundled Chromium instead of Edge (as the Docker image does).

## Cloud Manager

`mvn install` packages this folder as the UI-test Docker context. The image (`../Dockerfile`) runs `run.sh`, which
calls `npm test`; JUnit results go to `$REPORTS_PATH/junit`.