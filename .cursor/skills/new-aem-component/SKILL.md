---
name: new-aem-component
description: Scaffold a new AEM component in the airline project to team standards (Sling Model behind an interface, HTL, dialog, policy, front end, data layer, unit and UI tests, docs). Use when asked to create, add or scaffold a component under /apps/airline/components.
---

# New AEM component (airline project)

Owner: Senior developer A. Changes go through a PR.

## 1. Ask, or confirm from the request

- Component name (kebab-case) and title, plus the requirement ID it serves.
- Does it extend a Core Component (proxy and delegation, ADR-003) or is it custom? If custom, draft an ADR with `.ai/prompts/adr-draft.md`.
- Content source: dialog fields, a Content Fragment, or a page property.
- Interactions and data-layer events (`airline:<component>:<action>`).

## 2. Create these files

| File | Notes |
|---|---|
| `core/.../models/<Name>.java` | Interface, Javadoc with the requirement ID, extends `ComponentExporter` |
| `core/.../models/impl/<Name>Impl.java` | See `.cursor/rules/java-sling-models.mdc`; copy the structure from `TravelQuestionsAccordionImpl` |
| `core/src/test/.../models/impl/<Name>ImplTest.java` | AEM Mocks: happy path, empty, invalid policy, data-layer JSON |
| `ui.apps/.../components/<name>/.content.xml` | `jcr:primaryType="cq:Component"`, `componentGroup="Airline - Content"`, `sling:resourceSuperType` for a proxy |
| `ui.apps/.../components/<name>/_cq_dialog/.content.xml` | Plain-English labels, a `fieldDescription` per field |
| `ui.apps/.../components/<name>/<name>.html` | `data-sly-use` the model, placeholder template when empty |
| `ui.content/.../conf/airline/settings/wcm/policies/.content.xml` | Policy node, and add the component to the container's allowed list only if the template needs it |
| `ui.frontend/src/main/webpack/components/<name>.ts` and `_<name>.scss` | Only if there is behaviour or styling |
| `ui.tests/test-module/tests/<name>.spec.ts` | Keyboard, axe, data layer |
| `ui.content.sample/...` | An instance on the `component-states` page, showing empty and edge cases |

## 3. Check

1. `mvn clean install -PautoInstallSinglePackage` is green, with coverage of 80 % or more.
2. The component renders on author with `?wcmmode=disabled` and in the editor (placeholder when empty).
3. `npm test` in `ui.tests/test-module` passes at both widths.
4. Add a section to `AUTHORING.md` with `.ai/prompts/authoring-guide-section.md` and a screenshot.
5. Write `docs/pull-requests/PR-NNN.md`, including an **AI assistance** section.

## Don't

- Put logic in HTL, copy Core HTL, or hard-code heading levels.
- Add Dispatcher rules without `.cursor/rules/dispatcher.mdc`.
- Use real client copy or PII in samples.
