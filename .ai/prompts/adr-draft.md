# Prompt: Draft an ADR

Owner: Tech lead. The agent drafts; the tech lead decides and signs off. Final decisions are never delegated (SD-14).

```text
Draft an Architecture Decision Record for docs/04-decision-log.md in the airline AEM project.
Read the last three ADRs first and match their format and tone exactly:
"## ADR-NNN - <decision as a short statement>", then bullets **Context.**, **Options.** (lettered),
**Decision.**, **Trade-offs.**, **Consequences.**

Problem: <one or two sentences>
Constraints: <requirement IDs, NFRs from SOLUTION_DESIGN.md section 8, platform limits>
Options considered so far: <list, or "propose 2-4">

Rules:
- Use facts from the repo or from Adobe documentation only. Mark anything unverified with "(unverified)".
- Give at least one real downside for the chosen option.
- Consequences must name the files, tests or docs that change.
- Don't pick the decision if I wrote "options only". End with open questions for the tech lead.
```
