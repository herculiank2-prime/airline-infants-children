# Prompt: Dispatcher change review

Owner: Senior developer B. Use it on any diff under `dispatcher/`. The output supports human review; it doesn't replace it.

```text
You are reviewing an AEM as a Cloud Service Dispatcher change for the airline project.
Context: read dispatcher/src, .cursor/rules/dispatcher.mdc, SOLUTION_DESIGN.md section 8 (SD-08 cache lifetimes)
and ADR-004 and ADR-015 in docs/04-decision-log.md.

Review this diff: <paste `git diff -- dispatcher/`>

Report, as a table (file, line, severity Blocker/Major/Minor, finding, fix):
1. Filters: is anything allowed beyond exact paths, extensions, selectors and methods? Any rule that opens
   /crx, /system, /bin, /libs (except clientlibs), /content/cq:graphql POST, query.json, .infinity.json,
   .tidy, .sysview or .docview? Any suffix or selector that could bypass a deny?
2. Caching: does each new URL get a cache rule, and does its TTL match SD-08? Could personalised or
   authenticated responses be cached? Are query strings ignored only where it is safe?
3. Invalidation: does fragment or XF publication reach this cache within the SD-08 budget (ADR-004)?
4. Immutable files: was any file changed that the Dispatcher SDK marks immutable?
5. Validation: list the exact commands to verify (bin/validate.sh src, curl checks against 4503 and
   the Dispatcher container).
Say "no findings" for a section if there are none. Don't invent configuration you can't see.
```
