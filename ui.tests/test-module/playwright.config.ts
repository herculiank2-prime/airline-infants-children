import { defineConfig, devices } from "@playwright/test";

/**
 * Runs against AEM publish (design section 9). Locally the installed Edge is used so no browser download
 * is needed (ADR-012); in the Cloud Manager Docker image set PW_CHANNEL="" to use the bundled Chromium.
 */
const publishUrl = process.env.AEM_PUBLISH_URL || "http://localhost:4503";
const channel = process.env.PW_CHANNEL ?? "msedge";
const reports = process.env.REPORTS_PATH || "target";

export default defineConfig({
    testDir: "./tests",
    timeout: 60_000,
    workers: process.env.CI ? undefined : 2,
    retries: process.env.CI ? 1 : 0,
    reporter: [
        ["list"],
        ["junit", { outputFile: `${reports}/junit/results.xml` }],
        ["html", { outputFolder: `${reports}/html-report`, open: "never" }],
    ],
    outputDir: `${reports}/test-results`,
    use: {
        baseURL: publishUrl,
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    projects: [
        {
            name: "mobile-375",
            use: { ...devices["Desktop Edge"], channel: channel || undefined, viewport: { width: 375, height: 812 } },
        },
        {
            name: "desktop-1280",
            use: { ...devices["Desktop Edge"], channel: channel || undefined, viewport: { width: 1280, height: 900 } },
        },
    ],
});
