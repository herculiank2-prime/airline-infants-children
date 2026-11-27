import { PAGE_PATH, QUESTIONS, expect, test } from "./fixtures";

test.describe("Travel questions accordion (AC-01, ADR-018)", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto(PAGE_PATH);
    });

    test("shows the section heading and the six questions in order, collapsed", async ({ page }) => {
        const accordion = page.locator("#booking-infants");
        await expect(accordion.getByRole("heading", { level: 2 })).toHaveText("Booking for infants (under 2 years)");
        const buttons = accordion.locator(".cmp-travelquestions__button");
        await expect(buttons).toHaveText(QUESTIONS);
        for (const button of await buttons.all()) {
            await expect(button).toHaveAttribute("aria-expanded", "false");
            const panelId = await button.getAttribute("aria-controls");
            const panel = page.locator(`#${panelId}`);
            await expect(panel).toHaveAttribute("role", "region");
            await expect(panel).toHaveAttribute("hidden", "until-found");
            await expect(panel).toHaveAttribute("aria-labelledby", (await button.getAttribute("id")) || "");
            await expect(panel.locator(":scope > *").first()).toBeHidden();
        }
        await expect(accordion.locator("h3 > button")).toHaveCount(6);
    });

    test("Tab reaches each header button; Enter and Space toggle it", async ({ page }) => {
        const buttons = page.locator("#booking-infants .cmp-travelquestions__button");
        const first = buttons.nth(0);
        const firstPanel = page.locator(`#${await first.getAttribute("aria-controls")}`);

        await first.focus();
        await page.keyboard.press("Enter");
        await expect(first).toHaveAttribute("aria-expanded", "true");
        await expect(firstPanel).toBeVisible();
        await expect(firstPanel).not.toHaveAttribute("hidden", /.*/);

        await page.keyboard.press("Space");
        await expect(first).toHaveAttribute("aria-expanded", "false");
        await expect(firstPanel).toHaveAttribute("hidden", "until-found");

        await page.keyboard.press("Tab");
        await expect(buttons.nth(1)).toBeFocused();
        await page.keyboard.press("Space");
        await expect(buttons.nth(1)).toHaveAttribute("aria-expanded", "true");
        await expect(first).toHaveAttribute("aria-expanded", "false");
    });

    test("find-in-page text is in the HTML while collapsed", async ({ page }) => {
        const panel = page.locator(".cmp-travelquestions__panel").first();
        await expect(panel).toHaveAttribute("hidden", "until-found");
        expect((await panel.textContent())?.trim().length).toBeGreaterThan(0);
    });

    test("hero jump link targets the booking section that holds the accordion", async ({ page }) => {
        const link = page.locator(".cmp-hero .cmp-jumplinks__link", { hasText: "Booking for infants and children" });
        await expect(link).toHaveAttribute("href", "#booking");
        await expect(page.locator("#booking")).toHaveCount(1);
        await expect(page.locator("#booking-infants .cmp-travelquestions")).toHaveCount(1);
    });
});
