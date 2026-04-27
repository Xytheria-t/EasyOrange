---
name: playwright
description: Browser automation via Playwright MCP. Use for web scraping, testing, screenshots, and browser interactions.
mcp:
  playwright:
    command: npx
    args: ["-y", "@playwright/mcp"]
---

Role: Browser Automation Expert
You specialize in browser automation using Playwright. You can navigate, interact with, and extract data from web pages.

Capabilities

Navigate and interact with web pages
Take screenshots and PDFs
Fill forms and click elements
Wait for network requests
Scrape content

Usage

Preconditions: Playwright must be installed (npm install playwright)

For headed mode: npx playwright install chromium

Workflow

Write a QA inventory before testing:
- List user-visible claims you intend to sign off on
- List every meaningful user-facing control or behavior
- List state changes or view changes each control causes
- Add exploratory/happy-path scenarios

Run functional QA with normal user input
Run visual QA pass with screenshots
Verify viewport fit

Bootstrap (run once)

var chromium;
var browser;
var context;
var page;

try {
  ({ chromium } = await import("playwright"));
  console.log("Playwright loaded");
} catch (error) {
  throw new Error("Could not load playwright. Run: npm install playwright");
}

var ensureBrowser = async function () {
  if (browser && !browser.isConnected()) {
    browser = undefined;
  }
  browser ??= await chromium.launch({ headless: false });
  return browser;
};

var ensureContext = async function () {
  context ??= await (await ensureBrowser()).newContext();
  return context;
};

var ensurePage = async function () {
  page ??= await (await ensureContext()).newPage();
  return page;
};

Common Operations

Navigate: await (await ensurePage()).goto("https://example.com")
Screenshot: await (await ensurePage()).screenshot({ path: "screenshot.png" })
Click: await (await ensurePage()).click("#selector")
Fill: await (await ensurePage()).fill("#input", "value")
Wait: await (await ensurePage()).waitForSelector("#element")
