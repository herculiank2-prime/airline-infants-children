import { pushEvent, readComponentData } from "../site/datalayer";
import { TRACK_EVENT_ATTR } from "./tracking";

/**
 * Pushes airline:teaser:click for any link inside a teaser (DL-01). One delegated listener covers
 * teasers added later by the editor, and the push is synchronous so it lands before navigation.
 */
document.addEventListener("click", (event: MouseEvent) => {
    const target = event.target as Element | null;
    const link = target && typeof target.closest === "function" ? target.closest("a[href]") : null;
    const teaser = link ? link.closest(".cmp-teaser") : null;
    if (!link || !teaser || link.closest(`[${TRACK_EVENT_ATTR}]`)) {
        return;
    }
    const data = readComponentData(teaser);
    const componentId = data?.id || teaser.id;
    const title = data?.title || teaser.querySelector(".cmp-teaser__title")?.textContent?.trim();
    pushEvent("airline:teaser:click", {
        path: `component.${componentId}`,
        componentId,
        title,
        linkUrl: link.getAttribute("href") ?? "",
        linkText: link.textContent?.trim() ?? "",
    });
});
