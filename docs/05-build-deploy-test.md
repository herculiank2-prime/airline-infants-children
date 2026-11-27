# 05 — Build, deploy and test

Step-by-step runbook with the results recorded on 27 November 2026 (Windows 11, PowerShell 5.1, Java 21, Maven 3.9, AEM SDK 2026.2).

## 1. Prerequisites

| Item | Value |
|---|---|
| Author | `C:\aem-sdk\aem-author-p4502.jar`, `http://localhost:4502`, admin/admin (local only) |
| Publish | `C:\aem-sdk-publish\aem-publish-p4503.jar`, `http://localhost:4503` |
| Java | 21 (`.cloudmanager/java-version`); sources compiled with `release` 11 |
| Node and npm | 24.22.3 and 11.19.1, downloaded by `frontend-maven-plugin` (ADR-010) |
| Browser for UI tests | The installed Microsoft Edge (`PW_CHANNEL=msedge`); no Playwright browser download |

Start each instance with `java -jar <jar> -nointeractive` and wait until `/libs/granite/csrf/token.json` returns 200.

## 2. Build and deploy

```powershell
mvn -B clean install                                   # build and unit tests only
mvn -B clean install -PautoInstallSinglePackage        # + deploy the "all" package to author
mvn -B clean install -PautoInstallSinglePackagePublish # + deploy the "all" package to publish
```

| Run | Result |
|---|---|
| `clean install -PautoInstallSinglePackage` | BUILD SUCCESS. Author shows the demo page with six questions and one FAQPage block |
| `clean install -PautoInstallSinglePackagePublish` | BUILD SUCCESS (about 4.5 min) |
| Final `clean install` (PR-006) | BUILD SUCCESS. See section 6 |

FileVault `merge` mode does not overwrite existing nodes (ADR-019). To refresh demo pages after changing `ui.content.sample`, delete the page on author first, then redeploy.

### Publish repository reset (ADR-025)

The first publish deploy failed with a 404 from `/crx/packmgr`. `error.log` showed `SegmentNotFoundException`, which aborted repoinit. The fix:

1. Stop the publish JVM.
2. Rename `C:\aem-sdk-publish\crx-quickstart` to `crx-quickstart.corrupt-20261127`. It is kept, not deleted.
3. Start publish again and wait for the CSRF token and Package Manager to return 200.
4. Redeploy with `-PautoInstallSinglePackagePublish`.

## 3. Replicate content to publish

The package deploys code and code-owned structure to publish. Pages, fragments and tags come from author by replication. Activate the airline paths only:

```powershell
foreach ($path in "/conf/airline", "/content/cq:tags/airline", "/content/dam/airline",
                  "/content/experience-fragments/airline", "/content/cq:graphql/airline", "/content/airline") {
  curl.exe -s -u admin:admin -o NUL -w "%{http_code} $path`n" -F cmd=activate -F "path=$path" `
    -F ignoredeactivated=false -F onlymodified=false http://localhost:4502/libs/replication/treeactivation.html
}
```

All six returned 200.

## 4. Verify publish

| Check | Command or URL | Result |
|---|---|---|
| Demo page | `/content/airline/au/en/travel-information/flying-infants-children.html` | 200, 6 accordion buttons, 1 FAQPage block |
| Component states | `/content/airline/au/en/travel-information/component-states.html` | 200: folder mode with 3 items, unconfigured accordion renders nothing |
| Persisted query | `curl.exe -i "http://localhost:4503/graphql/execute.json/airline/travel-questions-by-folder%3Bfolder%3D%2Fcontent%2Fdam%2Fairline%2Ftravel-questions%2Finfants"` | 200, six items in `sortOrder` order |
| Cache headers | Same response | `Cache-Control: max-age=60, public, s-maxage=150, stale-while-revalidate=30, stale-if-error=86400` |
| `error.log` | Filtered for `com.airline` | No entries |

Short URLs (`/au/en/...`) return 404 on bare publish. That is expected: mapping is outbound only, and the Dispatcher rewrite resolves the short URLs (design section 12, ADR-026).

## 5. Tests

### Unit tests and coverage

`mvn clean install` runs them. The JaCoCo `check` goal fails the build below 80 % line coverage. Report: `core/target/site/jacoco/index.html`.

| Result | Value |
|---|---|
| Tests | 60 run, 0 failures, 0 errors |
| Line coverage | 99.1 % |
| Branch coverage | 88.6 % |

### UI tests (Playwright and axe)

```powershell
cd ui.tests\test-module
$env:PATH = "$(Resolve-Path ..\..\ui.frontend\node);$env:PATH"   # Node from the Maven build
$env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = "1"
npm ci
npm run lint        # tsc --noEmit
npm test            # AEM_PUBLISH_URL defaults to http://localhost:4503
```

Result: 30 passed in 2.3 min (15 tests each in `mobile-375` and `desktop-1280`). Reports are in `target/html-report` and `target/junit/results.xml`.

| Spec | Covers |
|---|---|
| `accordion.spec.ts` | AC-01: heading, six titles in order, ARIA, Tab, Enter and Space, find-in-page, hero jump link |
| `datalayer.spec.ts` | DL-01: expand and collapse events with ID, title and trigger; IDs in data-layer state; teaser click |
| `accessibility.spec.ts` | PT-02, EN-04, EN-06: axe WCAG 2.1 AA with panels collapsed and expanded, landmarks, skip link, no horizontal scroll, eager and lazy images, console errors |
| `structured-data.spec.ts` | SE-01, SE-02, HD-02: FAQPage matches the accordion, metadata, persisted-query contract and headers |

Three test defects were fixed on the first run against publish: an invented title suffix, a `toBeHidden` check on an `until-found` panel (its box stays in the layout), and an assumed absolute canonical (ADR-027). Short-URL 404s were also excluded without a Dispatcher (ADR-026). In Cloud Manager the Docker image (`mcr.microsoft.com/playwright:v1.65.1-noble`) uses the bundled Chromium (`PW_CHANNEL=""`); set `AEM_DISPATCHER=1` there.

### Performance (PF-01)

```powershell
$env:CHROME_PATH = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
npx lighthouse http://localhost:4503/content/airline/au/en/travel-information/flying-infants-children.html `
  --form-factor=mobile --chrome-flags="--headless=new" --output=json --output=html --output-path=docs/perf/lighthouse-mobile
```

Lighthouse 13.6.0 results: LCP 3.4 s (target ≤ 2.5 s, missed), CLS 0, TBT 160 ms, FCP 2.9 s, 355 KiB. Scores: performance 77, accessibility 100, best practices 96, SEO 92. README has the analysis.

### Dispatcher

Not validated locally. The Dispatcher SDK isn't installed and needs an Adobe Software Distribution login. Run `bin/validate.sh src` from the Dispatcher SDK before the first Cloud Manager pipeline run, which validates the same configuration.

## 6. Final build

| Item | Result |
|---|---|
| `mvn -B clean install` on `main` after PR-006 | BUILD SUCCESS. See `docs/build-final.log` |

## 7. Authoring screenshots

```powershell
cd ui.tests\test-module
npx tsx scripts/authoring-screenshots.ts   # writes docs/authoring/*.png from 4502 and 4503
```
