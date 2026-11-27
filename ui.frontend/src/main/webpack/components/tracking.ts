import { pushEvent, readComponentData } from "../site/datalayer";

export const TRACK_EVENT_ATTR = "data-airline-track-event";
const TRACK_LABEL_ATTR = "data-airline-track-label";

/**
 * Pushes the author-configured event (Analytics tab) for clicks on links and buttons inside a tracked
 * component (DL-01, ADR-031). The server has already normalised the name into the airline: namespace.
 */
document.addEventListener("click", (event: MouseEvent) => {
    const target = event.target as Element | null;
    const control = target && typeof target.closest === "function" ? target.closest("a[href], button") : null;
    const tracked = control ? control.closest(`[${TRACK_EVENT_ATTR}]`) : null;
    const name = tracked?.getAttribute(TRACK_EVENT_ATTR);
    if (!control || !tracked || !name) {
        return;
    }
    const data = readComponentData(control.closest("[data-cmp-data-layer]"));
    const componentId = data?.id || control.id || tracked.id;
    pushEvent(name, {
        path: `component.${componentId}`,
        componentId,
        title: data?.title,
        label: tracked.getAttribute(TRACK_LABEL_ATTR) ?? undefined,
        linkUrl: control.getAttribute("href") ?? "",
        linkText: control.textContent?.trim() ?? "",
    });
});
