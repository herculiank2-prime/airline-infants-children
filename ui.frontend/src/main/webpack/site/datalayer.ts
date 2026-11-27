/**
 * Single entry point for pushing project events to the Adobe Client Data Layer (DL-01, ADR-017).
 * Event names are namespaced "airline:*" so AEP Tags rules never double count Core's cmp:* events.
 */
export interface AirlineEventInfo {
    path: string;
    componentId: string;
    title?: string;
    [key: string]: unknown;
}

type DataLayerEntry = { event: string; eventInfo: AirlineEventInfo };

declare global {
    interface Window {
        adobeDataLayer?: unknown[];
    }
}

export function pushEvent(event: string, eventInfo: AirlineEventInfo): void {
    window.adobeDataLayer = window.adobeDataLayer || [];
    const entry: DataLayerEntry = { event, eventInfo };
    window.adobeDataLayer.push(entry);
}

/** Reads Core's data-cmp-data-layer JSON ({ "<id>": { "dc:title": ... } }) from an element. */
export function readComponentData(element: Element | null): { id: string; title?: string; parentId?: string } | null {
    const raw = element?.getAttribute("data-cmp-data-layer");
    if (!raw) {
        return null;
    }
    try {
        const parsed = JSON.parse(raw) as Record<string, { "dc:title"?: string; parentId?: string }>;
        const id = Object.keys(parsed)[0];
        if (!id) {
            return null;
        }
        return { id, title: parsed[id]["dc:title"], parentId: parsed[id].parentId };
    } catch {
        return null;
    }
}
