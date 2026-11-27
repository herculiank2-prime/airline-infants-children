# AI notes

**Tools.** Cursor's agent (Claude models) in the IDE, with terminal access to the local AEM SDK. No client data was available, so none was put into prompts. All copy and images are fictional or generated (assumptions A-02 and A-03).

**How it was used.**
- *Analysis and design:* first drafts of `docs/01`, `docs/02` and `SOLUTION_DESIGN.md` from the brief. I rewrote the integration numbering, NFR figures and delivery plan, then checked them in a separate architect-review pass (`docs/03`, 30 findings).
- *Build:* scaffolding of Sling Models, HTL, dialogs, the CF model, persisted queries, Playwright specs and test fixtures, generated in small slices, one PR at a time (PR-002 to PR-005).
- *Operations:* driving the SDK over HTTP (`scripts/aem-http.ps1`, ADR-021): installing packages, persisting queries, tree-activating airline paths only, and reading `error.log`.
- *Docs:* README, AUTHORING (screenshots captured by a Playwright script), ADR drafts with `.ai/prompts/adr-draft.md`.

**Where it was wrong, and how that was caught.**
- It claimed the AI artefacts were committed in PR-001 (AR-28 resolution) when they weren't. Found in the final doc review and corrected (ADR-028).
- It believed persisted queries needed `indexedData`. The real cause was partial URL encoding, found by testing every variant on author and publish (ADR-022).
- It used `String!` for the `_path` filter (needs `ID!`), produced UTF-8 mojibake and BOMs on Windows PowerShell 5.1, and wrote a test title with a suffix the design never specified. The compiler, the unit tests or Playwright caught each one.
- On the first UI-test run it would have been easy to loosen tests until they passed. Instead each failure was classified as a product defect, a test defect or an environment issue. The environment and deviation cases are recorded in ADR-026 and ADR-027, and LCP is reported as missed rather than tuned away.

**Machine gates that AI output must pass.** `mvn clean install` with JaCoCo at 80 % or more (actual: 99.1 % line), Playwright and axe on publish (30 of 30), a console-error fixture, and the Dispatcher allow-list. The Dispatcher SDK validator could not be run locally (see README).

**Not delegated.** Choosing between ADR options, legal and answer wording, security and PII decisions (SD-11 data flows, CSP, secrets), what counts as an acceptable deviation, and anything that touches other organisations' content on shared instances.

**Team use (SD-14).** `AGENTS.md` (owner: tech lead), `.cursor/rules/*.mdc` for Java, HTL, Dispatcher and tests, `.cursor/skills/new-aem-component` (owner: senior developer A), and `.ai/prompts/` for Dispatcher review, accessibility review, ADR drafts and authoring sections (owners: senior developer B and QA). They are versioned and reviewed like code. Each PR description states what the agent did and what a human checked.

**Ownership.** I own every line here. The agent was fast at the first 80 % and confidently wrong at the edges: platform quirks, encoding and claims about its own earlier work. Every claim in the README was re-checked against a running instance before it was written down.
