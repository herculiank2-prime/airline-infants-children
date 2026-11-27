/**
 * Captures the screenshots used in AUTHORING.md from a local author (4502) and publish (4503).
 * Run from ui.tests/test-module: npx tsx scripts/authoring-screenshots.ts
 */
import { chromium, Page } from "@playwright/test";
import * as path from "path";

const author = process.env.AEM_AUTHOR_URL || "http://localhost:4502";
const publish = process.env.AEM_PUBLISH_URL || "http://localhost:4503";
const user = process.env.AEM_AUTHOR_USERNAME || "admin";
const password = process.env.AEM_AUTHOR_PASSWORD || "admin";
const out = path.resolve(__dirname, "../../../docs/authoring");
const page = "/content/airline/au/en/travel-information/flying-infants-children";
const components = `${page}/jcr:content/root/container`;

async function shot(p: Page, name: string, url: string, settle = 4000): Promise<void> {
    await p.goto(url, { waitUntil: "load", timeout: 120_000 });
    await p.waitForTimeout(settle);
    await p.screenshot({ path: path.join(out, `${name}.png`) });
    console.log(`${name}.png`);
}

async function dialog(p: Page, name: string, resource: string, tab?: string): Promise<void> {
    await p.goto(`${author}/editor.html${page}.html`, { waitUntil: "load", timeout: 120_000 });
    await p.waitForFunction(() => {
        const g = (window as unknown as { Granite?: { author?: { editables?: unknown[] } } }).Granite;
        return !!g?.author?.editables && g.author.editables.length > 0;
    }, undefined, { timeout: 60_000 });
    await p.evaluate((target) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const author = (window as any).Granite.author;
        const editable = author.editables.find(target)[0];
        author.DialogFrame.openDialog(new author.edit.Dialog(editable));
    }, resource);
    const dialogElement = p.locator("coral-dialog.cq-Dialog[open], .cq-dialog-floating, form.cq-dialog").first();
    await dialogElement.waitFor({ timeout: 60_000 });
    await p.waitForTimeout(2500);
    if (tab) {
        await p.locator("coral-tab", { hasText: tab }).first().click();
        await p.waitForTimeout(1000);
    }
    await p.screenshot({ path: path.join(out, `${name}.png`) });

    console.log(`${name}.png`);
}

(async () => {
    const browser = await chromium.launch({ channel: process.env.PW_CHANNEL ?? "msedge" });
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const p = await context.newPage();
    await p.goto(`${author}/libs/granite/core/content/login.html`);
    await p.fill("#username", user);
    await p.fill("#password", password);
    await Promise.all([p.waitForNavigation(), p.click("#submit-button")]);

    await shot(p, "01-sites-console", `${author}/sites.html${page.substring(0, page.lastIndexOf("/"))}`);
    await shot(p, "02-page-editor", `${author}/editor.html${page}.html`, 8000);
    await dialog(p, "03-hero-dialog", `${components}/hero`);
    await dialog(p, "04-teaser-dialog", `${components}/content/joey_club`);
    await dialog(p, "05-accordion-dialog-content", `${components}/content/booking_infants`);
    await dialog(p, "06-accordion-dialog-questions", `${components}/content/booking_infants`, "Questions");
    await shot(p, "07-fragment-folder", `${author}/assets.html/content/dam/airline/travel-questions/infants`);
    await shot(p, "08-fragment-editor", `${author}/editor.html/content/dam/airline/travel-questions/infants/infant-seat`, 8000);
    await shot(p, "09-page-properties", `${author}/mnt/overlay/wcm/core/content/sites/properties.html?item=${page}`);

    const pub = await context.newPage();
    await pub.goto(`${publish}${page}.html`, { timeout: 120_000 });
    const accordion = pub.locator("#booking-infants");
    await accordion.scrollIntoViewIfNeeded();
    await accordion.locator(".cmp-travelquestions__button").first().click();
    await pub.waitForTimeout(500);
    await accordion.screenshot({ path: path.join(out, "10-published-accordion.png") });
    console.log("10-published-accordion.png");
    await browser.close();
})();
