import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './api',
  timeout: 30000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'reports' }],
    ['list']
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:8080',
    trace: 'on-first-retry',
    actionTimeout: 10000,
  },
  projects: [
    {
      name: 'backend-api',
      use: {
        baseURL: process.env.BASE_URL || 'http://localhost:8080',
      },
    },
  ],
});
