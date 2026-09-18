import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@api': resolve(__dirname, 'src/api'),
      '@utils': resolve(__dirname, 'src/utils'),
      '@components': resolve(__dirname, 'src/components'),
      '@pages': resolve(__dirname, 'src/pages'),
      '@types': resolve(__dirname, 'src/types'),
      '@constants': resolve(__dirname, 'src/constants'),
      '@assets': resolve(__dirname, 'src/assets'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    environmentOptions: {
      jsdom: {
        url: 'http://localhost',
      },
    },
    setupFiles: ['./src/testUtils/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    // 默认按核数开 worker：24 核开发机上约 23 个 jsdom worker 打满内存，随机让若干用例撞 5s 超时
    // （pre-push 门禁随之误红）。限 4 个；CI 的 2 核 runner 本来到不了这个上限。
    maxWorkers: 4,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.test.*',
        'src/**/codemap.md',
        'src/**/index.ts',
        'src/vite-env.d.ts',
      ],
      reporter: ['text', 'html', 'lcov'],
      thresholds: {
        lines: 35,
        functions: 35,
        branches: 30,
        statements: 35,
      },
    },
  },
});
