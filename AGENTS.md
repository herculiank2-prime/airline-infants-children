# Airline travel information: agent context

AEM as a Cloud Service project (archetype 58) for a fictional airline's "Flying with infants and children" page. Read `README.md` for the requirement trace and `docs/04-decision-log.md` before changing a design decision. Owner of this file: the tech lead. Change it through a PR like any code.

## Ground rules

- **Never** put PII, credentials, client data, Salesforce schemas or unapproved legal copy in prompts, code or fixtures. The exception is the sample content (`ui.content.sample`) and the site theme, which reuse the reference page's copy, images and styling with the owner's permission (ADR-029, superseding A-02). Keep that content out of `core`, `ui.apps` and test fixtures beyond the asserted titles.
- Record each design-changing choice as an ADR in `docs/04-decision-log.md` (Context, Options, Decision, Trade-offs, Consequences), numbered after the last one.
- Each PR gets `docs/pull-requests/PR-NNN.md` with Why, What changed, Verification, and AI assistance (agent used, what a human checked).
- Tag code and tests with requirement IDs (`AC-01`, `HD-02`, ...) from `docs/01-analysis-discovery.md`.
- Don't weaken a gate (JaCoCo 80 %, axe serious/critical, console-error fixture, Dispatcher allow-list) to make a change pass. Fix the cause or write an ADR.

## Modules

- `core`: Sling Models (interfaces in `models`, implementations in `models.impl`), `services` (`TravelQuestionService`), `richtext`, `structureddata`, `filters`. JUnit 5 + wcm.io AEM Mocks. Compile target is release 11: no records, no `switch` expressions with patterns.
- `ui.apps`: components under `/apps/airline/components`. HTL only renders; no business logic. Core Components are used through proxies and delegation (ADR-003).
- `ui.config`: OSGi configs by run mode (`config`, `config.publish`).
- `ui.content`: code-owned structure (template, policies, CF model, GraphQL endpoint and persisted queries, tags). `ui.content.sample`: demo pages and fragments (ADR-019). FileVault `merge` does not update existing nodes.
- `ui.frontend`: TypeScript + SCSS, built into clientlibs. Data-layer events go through `pushEvent` in `datalayer.ts` with `airline:` names (ADR-017).
- `dispatcher`: allow-list filters (ADR-015); every new endpoint needs a filter and a cache rule.
- `ui.tests`: Playwright + axe-core against publish (ADR-012, ADR-026).

## Build, deploy, test

- Full build: `mvn clean install` (must stay green). Author: `-PautoInstallSinglePackage`. Publish: `-PautoInstallSinglePackagePublish`.
- UI tests: `cd ui.tests/test-module; npm test` with `AEM_PUBLISH_URL` (default `http://localhost:4503`).
- Local HTTP calls to AEM: `scripts/aem-http.ps1` (ADR-021). Use `curl.exe` for persisted queries, because .NET rewrites `%2F` (ADR-022).
- On Windows PowerShell 5.1, read and write UTF-8 with `[IO.File]::ReadAllText` / `WriteAllText` and `UTF8Encoding($false)`.

## Project-specific rules and skills

- `.cursor/rules/`: `java-sling-models.mdc`, `htl-components.mdc`, `dispatcher.mdc`, `tests.mdc`.
- `.cursor/skills/new-aem-component/SKILL.md`: scaffold a component to project standards.
- `.ai/prompts/`: Dispatcher review, accessibility review, ADR draft, authoring-guide section.
## Important resources

Note: there are significant architectural differences between AEM as a Cloud Service and Adobe Managed Services/on-prem deployments. The resources below have been curated to ensure they apply to this project. Use them in preference to generic AEM resources.
- [Core Concepts](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/overview/architecture)
- [AEM Project Structure](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-project-content-package-structure)
- [AEM Technical Foundations](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-technologies)
- [AEM Development Guidelines](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/development-guidelines)
- [Java API Best Practices](https://experienceleague.adobe.com/en/docs/experience-manager-learn/foundation/development/understand-java-api-best-practices)
- [The AEM as a Cloud Service SDK](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/aem-as-a-cloud-service-sdk)
- [Sling Adapters](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/sling-adapters)
- [Sling Resource Merger](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/sling-resource-merger)
- [Getting Started with HTL](https://experienceleague.adobe.com/en/docs/experience-manager-htl/content/getting-started)
- [Overlays](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/overlays)
- [Templates](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/components-templates/templates)
- [Core Components Introduction](https://experienceleague.adobe.com/en/docs/experience-manager-core-components/using/introduction)
- [Components Reference Guide](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/components-templates/reference)
- [Manage digital assets with the Adobe Experience Manager Assets HTTP API](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/assets/admin/mac-api-assets)
- [Deprecated and Removed Features and APIs](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/release-notes/deprecated-removed-features)
- [Best Practices for Sling Service User Mapping and Service User Definition](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/security/best-practices-for-sling-service-user-mapping-and-service-user-definition)
- [Using Client-Side Libraries](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/clientlibs)
- [Universal Editor in AEM](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/getting-started)
- [Content Fragments](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/sites/administering/content-fragments/overview)
- [Experience Fragments](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/sites/authoring/fragments/experience-fragments)
- [AEM APIs for Structured Content Delivery and Management](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/headless/apis-headless-and-content-fragments)
- [Developing and Extending Workflows](https://experienceleague.adobe.com/en/docs/experience-manager-65/content/implementing/developing/extending-aem/extending-workflows/workflows)
- [Replication](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/replication)
- [Content Search and Indexing](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/indexing)
- [CDN in AEM as a Cloud Service](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/content-delivery/cdn)
- [Deployment and Maintenance](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/deploying/overview)
- [API Reference Materials](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/reference-materials)
- [WCM.io AEM Mocks](https://wcm.io/testing/aem-mock/)
- [Sling Mocks](https://sling.apache.org/documentation/development/sling-mock.html)
- [Validating and Debugging using Dispatcher Tools](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/content-delivery/validation-debug)

