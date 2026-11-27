import { test as base, expect, Page } from "@playwright/test";

export const PAGE_PATH = "/content/airline/au/en/travel-information/flying-infants-children.html";

/** The six titles from the reference page (AC-01), in sortOrder. */
export const QUESTIONS = [
    "Infants on domestic flights",
    "Infants on international flights",
    "Booking a seat for your infant",
    "Travelling with more than one infant per adult",
    "How soon can infants travel after birth?",
    "My infant isn\u2019t born yet, how do I book?",
];

export type DataLayerEvent = { event: string; eventInfo: Record<string, unknown> };

/**
 * Short URLs (/au/en/...) only resolve through the Dispatcher rewrite (design section 12), so without
 * AEM_DISPATCHER=1 their 404s on a bare publish instance are expected and not reported.
 */
const behindDispatcher = process.env.AEM_DISPATCHER === "1";

/** Every console error, uncaught exception and failed same-origin request seen while the test ran (EN-06). */
export const test = base.extend<{ consoleErrors: string[] }>({
    consoleErrors: async ({ page, baseURL }, use) => {
        const errors: string[] = [];
        page.on("console", (message) => {
            if (message.type() === "error" && !message.text().startsWith("Failed to load resource")) {
                errors.push(message.text());
            }
        });
        page.on("response", (response) => {
            const url = new URL(response.url());
            if (response.status() < 400 || !baseURL || !response.url().startsWith(baseURL)) {
                return;
            }
            const shortUrl = !/^\/(content|etc\.clientlibs|libs|graphql)\//.test(url.pathname);
            if (behindDispatcher || !shortUrl) {
                errors.push(`${response.status()} ${url.pathname}`);
            }
        });
        page.on("pageerror", (error) => errors.push(error.message));
        await use(errors);
    },
});

export { expect };

export async function airlineEvents(page: Page): Promise<DataLayerEvent[]> {
    return page.evaluate(() =>
        ((window as unknown as { adobeDataLayer?: unknown[] }).adobeDataLayer || [])
            .filter((entry): entry is { event: string; eventInfo: Record<string, unknown> } =>
                typeof entry === "object" && entry !== null && typeof (entry as { event?: unknown }).event === "string"
                && (entry as { event: string }).event.startsWith("airline:"))
            .map((entry) => ({ event: entry.event, eventInfo: entry.eventInfo })),
    );
}
