import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 90000,
  expect: {
    timeout: 10000
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  // 本地与 CI 一致：重试 2 次吸收 WSL2/CI runner 偶发负载抖动
  retries: 2,
  workers: 1,
  globalSetup: './tests/e2e/global-setup.ts',
  reporter: [
    ['html', { outputFolder: 'tests/e2e/reports' }],
    ['list']
  ],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    navigationTimeout: 60000,
    actionTimeout: 15000
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: {
    // E2E 跑 vite preview（生产构建产物），需先 npm run build。dev server 按需转换 +
    // HMR WebSocket 在 WSL2 高负载下会把 page.goto 拖到 45s+ 超时（见 global-setup 注释）
    command: 'npm run preview',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 120000
  }
});
