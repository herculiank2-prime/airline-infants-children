import AxeBuilder from "@axe-core/playwright";

import { PAGE_PATH, expect, test } from "./fixtures";

test.describe("Accessibility and layout (PT-02, SD-08, EN-04, EN-06)", () => {
    test("axe finds no serious or critical WCAG 2.1 AA issues, collapsed and expanded", async ({ page }, testInfo) => {
        await page.goto(PAGE_PATH);
        const scan = async (label: string) => {
            const results = await new AxeBuilder({ page })
                .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
                .analyze();
            await testInfo.attach(`axe-${label}.json`, { body: JSON.stringify(results.violations, null, 2), contentType: "application/json" });
            const blocking = results.violations.filter((v) => v.impact === "serious" || v.impact === "critical");
            expect(blocking.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`)).toEqual([]);
        };
        await scan("collapsed");
        for (const button of await page.locator(".cmp-travelquestions__button").all()) {
            await button.click();
        }
        await scan("expanded");
    });

    test("landmarks, one h1 and a working skip link", async ({ page }) => {
        await page.goto(PAGE_PATH);
        await expect(page.locator("h1")).toHaveCount(1);
        await expect(page.locator("h1")).toHaveText("Flying with infants and children");
        await expect(page.locator("main")).toHaveCount(1);
        await expect(page.locator("main #main-content")).toHaveCount(1);
        await expect(page.locator("header")).toHaveCount(1);
        await expect(page.locator("footer")).toHaveCount(1);
        await expect(page.locator("nav").first()).toBeAttached();

        await page.keyboard.press("Tab");
        const skip = page.locator(":focus");
        await expect(skip).toHaveAttribute("href", "#main-content");
        await expect(skip).toBeVisible();
    });

    test("no horizontal scrolling and a screenshot per viewport", async ({ page }, testInfo) => {
        await page.goto(PAGE_PATH);
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
        expect(overflow).toBeLessThanOrEqual(0);
        await page.screenshot({ path: testInfo.outputPath(`page-${testInfo.project.name}.png`), fullPage: true });
    });

    test("hero image is eager and images below it are lazy (PF-01)", async ({ page }) => {
        await page.goto(PAGE_PATH);
        await expect(page.locator(".cmp-hero__image")).toHaveAttribute("loading", "eager");
        await expect(page.locator(".cmp-hero__image")).toHaveAttribute("fetchpriority", "high");
        for (const image of await page.locator("main img:not(.cmp-hero__image), footer img").all()) {
            await expect(image).toHaveAttribute("loading", "lazy");
        }
    });

    test("page loads without console errors", async ({ page, consoleErrors }) => {
        await page.goto(PAGE_PATH);
        await page.waitForLoadState("networkidle");
        expect(consoleErrors).toEqual([]);
    });
});
