import { test, expect } from '@playwright/test';

/**
 * 商品浏览与搜索 —— 契约层：mock 化（无后端）下只断「页面可达 + 纯 UI/导航」。
 * 依赖真实后端数据的渲染（排序选项数量、筛选按钮、搜索结果列表、商品详情 404 等）
 * 由组件测试（src 下各组件的 .test.tsx）+ 后端 IT 覆盖，E2E 不 mock 整组商品 API 来测数据渲染。
 * floating-nav 有无限动画，点击用 force（与 auth/admin 同款，见 helpers 注释）。
 *
 * 检索入口收敛在商品列表页（/products?keyword=…）——原独立 /search 页与列表页
 * 检索能力重复，2026-10-13 裁撤；首页搜索与顶栏搜索图标都改指这里。
 */
test.describe('商品浏览与搜索', () => {
  test('首页可加载', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toBeVisible();
  });

  test('首页搜索提交后跳转商品列表并带上关键词', async ({ page }) => {
    await page.goto('/');
    const searchInput = page.locator('input[type="text"], input[placeholder*="搜索"]').first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    await searchInput.fill('手机');
    await searchInput.press('Enter');
    // 提交后应跳转到商品列表页，关键词进 query
    await expect(page).toHaveURL(/\/products\?keyword=/, { timeout: 10000 });
    await expect(page).toHaveURL(/%E6%89%8B%E6%9C%BA/);
  });

  test('页面导航存在', async ({ page }) => {
    await page.goto('/');
    const nav = page.locator('nav').first();
    await expect(nav).toBeVisible();
  });

  test('导航栏品牌链接可见', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.floating-nav__brand')).toBeVisible();
    await expect(page.locator('.floating-nav__brand-name')).toHaveText('EasyOrange');
  });

  test('导航栏包含首页和商品链接', async ({ page }) => {
    await page.goto('/');
    const nav = page.locator('.floating-nav__links');
    await expect(nav.locator('a').filter({ hasText: '首页' })).toBeVisible();
    await expect(nav.locator('a').filter({ hasText: '商品' })).toBeVisible();
  });

  test('商品页面可加载', async ({ page }) => {
    await page.goto('/products');
    // 商品页面容器应可见
    await expect(page.locator('.products-page-wrapper, .products-container').first()).toBeVisible({ timeout: 10000 });
  });

  test('顶栏搜索图标导航到商品列表', async ({ page }) => {
    await page.goto('/');
    // 点击搜索图标按钮
    const searchBtn = page.locator('.floating-nav__icon-btn[aria-label="搜索"]').first();
    await expect(searchBtn).toBeVisible();
    await searchBtn.click();
    await expect(page).toHaveURL(/\/products/);
  });

  test('发布按钮导航到登录或发布页', async ({ page }) => {
    await page.goto('/');
    const publishBtn = page.locator('.floating-nav__publish-btn').first();
    await expect(publishBtn).toBeVisible();
    await publishBtn.click();
    // 未登录状态下应跳转到登录页，带 redirect 参数
    const currentUrl = page.url();
    // 可能是 /login 或 /publish（如果已登录）
    const isLoginOrPublish = currentUrl.includes('/login') || currentUrl.includes('/publish');
    expect(isLoginOrPublish).toBeTruthy();
  });
});
