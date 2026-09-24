import { defineConfig } from 'vitest/config';
import base from './vitest.config';

// 渲染成本测量：不进 `npm test`（主配置 include 只收 *.test.*），需显式 `npm run bench:render`
export default defineConfig({
  ...base,
  test: {
    ...base.test,
    include: ['src/**/*.bench.{ts,tsx}'],
    coverage: { enabled: false },
  },
});
