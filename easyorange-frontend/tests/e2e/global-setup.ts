import { chromium, type FullConfig } from '@playwright/test';

/**
 * 预热 dev server（根治冷启动导航超时）。
 *
 * Vite dev 的依赖预构建（esbuild pre-bundle）在首个模块请求时才触发，且按需按路径转换。
 * 冷启动下第一个 page.goto 要把整个入口模块图一次性转换，WSL2 下常拖爆导航超时（45s）。
 * 这里在测试开始前用真实浏览器打一次根路径并等应用挂载，让依赖缓存与转换缓存落盘——
 * 后续所有用例的 goto/断言才稳定。webServer 由 config 先行启动，本文件执行时已就绪。
 */
export default async function globalSetup(config: FullConfig): Promise<void> {
    const baseURL = (config.projects[0]?.use?.baseURL ?? 'http://localhost:5173').replace(/\/$/, '');
    const browser = await chromium.launch();
    const page = await browser.newPage();
    try {
        // waitUntil 'commit'：不等待整页 load，只需确认 dev server 已响应且开始执行
        await page.goto(baseURL + '/', { waitUntil: 'commit', timeout: 120_000 });
        // 等应用真实挂载（品牌导航出现 = 入口模块图转换完成），触发 esbuild 预构建落盘
        await page.waitForSelector('.floating-nav__brand', { timeout: 120_000 });
    } finally {
        await browser.close();
    }
}
