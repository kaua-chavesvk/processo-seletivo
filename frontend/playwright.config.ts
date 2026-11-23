import { defineConfig } from "@playwright/test";
const target = process.env.TEST_BASE_URL;
export default defineConfig({
  testDir: "tests",
  workers: 1,
  timeout: 45000,
  use: {
    baseURL: target || "http://127.0.0.1:4188",
    channel: process.env.PLAYWRIGHT_CHANNEL,
    viewport: { width: 1440, height: 960 },
  },
  webServer: target
    ? undefined
    : {
        command:
          "npm run dev:demo -- --host 127.0.0.1 --port 4188 --strictPort",
        url: "http://127.0.0.1:4188",
        reuseExistingServer: false,
        timeout: 120000,
      },
});
