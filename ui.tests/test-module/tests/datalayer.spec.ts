import { PAGE_PATH, QUESTIONS, airlineEvents, expect, test } from "./fixtures";

test.describe("Adobe Client Data Layer events (DL-01, ADR-017)", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto(PAGE_PATH);
    });

    test("expanding and collapsing an item pushes expand and collapse with ID and title", async ({ page }) => {
        const button = page.locator("#booking-infants .cmp-travelquestions__button").nth(2);
        await button.click();
        await button.click();
        const events = await airlineEvents(page);
        expect(events.map((e) => e.event)).toEqual(["airline:accordion:expand", "airline:accordion:collapse"]);
        const itemId = await button.getAttribute("data-item-id");
        for (const { eventInfo } of events) {
            expect(eventInfo.componentId).toBe(itemId);
            expect(eventInfo.path).toBe(`component.${itemId}`);
            expect(eventInfo.title).toBe(QUESTIONS[2]);
            expect(eventInfo.parentId).toBe("booking-infants");
            expect(eventInfo.trigger).toBe("click");
        }
    });

    test("item IDs are registered with Core's data layer state", async ({ page }) => {
        const itemId = await page.locator("#booking-infants .cmp-travelquestions__button").first().getAttribute("data-item-id");
        const state = await page.evaluate((id) => {
            const dl = (window as unknown as { adobeDataLayer?: { getState?: (path: string) => unknown } }).adobeDataLayer;
            return dl && typeof dl.getState === "function" ? dl.getState(`component.${id}`) : null;
        }, itemId);
        expect(state).toMatchObject({ "@type": "airline/components/travelquestions/item", "dc:title": QUESTIONS[0] });
    });

    test("clicking the Joey Club link pushes airline:teaser:click", async ({ page, context }) => {
        const link = page.locator("#joey-club a[href]").first();
        await expect(link).toHaveAttribute("target", "_blank");
        await expect(link).toHaveAttribute("rel", /noopener/);
        await context.route("https://club.airline.example/**", (route) => route.fulfill({ body: "ok" }));
        const popup = page.waitForEvent("popup");
        await link.click();
        await (await popup).close();
        const events = await airlineEvents(page);
        const click = events.find((e) => e.event === "airline:teaser:click");
        expect(click?.eventInfo).toMatchObject({
            componentId: "joey-club",
            path: "component.joey-club",
            title: "Joey Club for young flyers",
            linkUrl: "https://club.airline.example/joey",
        });
    });
});
