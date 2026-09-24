import { defineConfig, devices } from '@playwright/test';

/**
 * 真后端演示流验证（不 mock 任何 API）—— 与 playwright.config.ts 隔离：
 * 默认套件（tests/e2e）钉契约、CI 无后端也能绿；本套件要求本地栈全起
 * （后端 8080 + 前端 5173 + MySQL/Redis/ES），故不进 CI 的 `npx playwright test`。
 *
 * 运行：npx playwright test --config playwright.real.config.ts
 */
export default defineConfig({
    testDir: './tests/e2e-real',
    timeout: 120_000,
    expect: { timeout: 15_000 },
    fullyParallel: false,
    retries: 1,
    workers: 1,
    reporter: [['list']],
    use: {
        baseURL: 'http://localhost:5173',
        // 双保险：CSS 动画即使漏改到交互元素，reduce 下也不跑，元素不再每帧位移
        reducedMotion: 'reduce',
        trace: 'only-on-failure',
        screenshot: 'only-on-failure',
        navigationTimeout: 60_000,
        actionTimeout: 20_000,
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    webServer: {
        command: 'npm run preview',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        timeout: 120_000,
    },
});
