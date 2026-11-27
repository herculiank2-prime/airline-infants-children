# Prompt: Authoring-guide section for a component

Owner: QA, with a content author as reviewer. The output goes into `AUTHORING.md`.

```text
Write an AUTHORING.md section for the <component> component in the airline AEM project.
Audience: content authors who don't read code. Plain English, short sentences, second person.
Match the structure and tone of the existing sections in AUTHORING.md.

Inputs:
- Dialog XML: <paste _cq_dialog/.content.xml>
- Model behaviour that authors notice (empty states, limits, sanitising, fallbacks): <summary or Java excerpt>
- Screenshot file names in docs/authoring/: <list>

Include:
1. Where the component is and what it is for (one or two sentences).
2. A table of tab, field and what to enter, using the dialog's exact labels.
3. What happens when it is empty or misconfigured, in the editor and on the published page.
4. Publishing dependencies (fragments, tags, images).
5. Troubleshooting rows (what you see, cause, fix).
Only describe behaviour that the inputs show. List anything you had to assume at the end, for the
reviewer to confirm.
```
