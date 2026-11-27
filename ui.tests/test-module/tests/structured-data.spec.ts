import { PAGE_PATH, QUESTIONS, expect, test } from "./fixtures";

type FaqPage = {
    "@context": string;
    "@type": string;
    mainEntity: { "@type": string; name: string; acceptedAnswer: { "@type": string; text: string } }[];
};

test.describe("Structured data, metadata and the persisted query (SE-01, SE-02, HD-02)", () => {
    test("one FAQPage JSON-LD block that matches the accordion", async ({ page }) => {
        await page.goto(PAGE_PATH);
        const blocks = page.locator('script[type="application/ld+json"]');
        await expect(blocks).toHaveCount(1);
        const faq = JSON.parse((await blocks.textContent()) || "{}") as FaqPage;
        expect(faq["@context"]).toBe("https://schema.org");
        expect(faq["@type"]).toBe("FAQPage");
        expect(faq.mainEntity.map((q) => q.name)).toEqual(QUESTIONS);
        for (const entity of faq.mainEntity) {
            expect(entity["@type"]).toBe("Question");
            expect(entity.acceptedAnswer["@type"]).toBe("Answer");
            expect(entity.acceptedAnswer.text).toMatch(/^<p>/);
        }
        const firstPanel = await page.locator(".cmp-travelquestions__panel").first().innerHTML();
        expect(firstPanel.trim()).toBe(faq.mainEntity[0].acceptedAnswer.text);
    });

    test("title, description and canonical come from page properties", async ({ page }) => {
        await page.goto(PAGE_PATH);
        await expect(page).toHaveTitle("Flying with infants and children");
        await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", /Plan a trip with a baby/);
        await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href",
            /^(https?:\/\/[^/]+)?\/.*travel-information\/flying-infants-children\.html$/);
    });

    test("persisted query returns the same questions in the same order", async ({ page, request }) => {
        const response = await request.get(
            "/graphql/execute.json/airline/travel-questions-by-tags-all%3Baudience%3Dairline%3Atravel%2Finfants%3Bmarket%3Dairline%3Amarket%2Fau");
        expect(response.ok()).toBeTruthy();
        const body = await response.json();
        const fromQuery = body.data.travelQuestionList.items.map((item: { question: string }) => item.question);
        await page.goto(PAGE_PATH);
        const fromPage = await page.locator("#booking-infants .cmp-travelquestions__question").allTextContents();
        expect(fromQuery).toEqual(fromPage.map((t) => t.trim()));
        expect(fromQuery).toEqual(QUESTIONS);
    });
});
