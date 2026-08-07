import { test, expect } from '@playwright/test';

/**
 * 商品浏览与搜索 —— 契约层：mock 化（无后端）下只断「页面可达 + 纯 UI/导航」。
 * 依赖真实后端数据的渲染（排序选项数量、筛选按钮、搜索结果列表、商品详情 404 等）
 * 由组件测试（src 下各组件的 .test.tsx）+ 后端 IT 覆盖，E2E 不 mock 整组商品 API 来测数据渲染。
 * floating-nav 有无限动画，点击用 force（与 auth/admin 同款，见 helpers 注释）。
 */
test.describe('商品浏览与搜索', () => {
  test('首页可加载', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toBeVisible();
  });

  test('搜索功能可交互', async ({ page }) => {
    await page.goto('/');
    const searchInput = page.locator('input[type="text"], input[placeholder*="搜索"]').first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    await searchInput.fill('手机');
    await searchInput.press('Enter');
    // 提交后应跳转到搜索页
    await expect(page).toHaveURL(/\/search/, { timeout: 10000 });
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

  test('搜索页面可访问且有搜索输入框', async ({ page }) => {
    await page.goto('/search');
    // 搜索输入框应可见
    const searchInput = page.locator('.search-input-field, input[placeholder*="搜索"]').first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
  });

  test('搜索页面显示初始分类浏览区域', async ({ page }) => {
    await page.goto('/search');

    // 分类浏览区域应可见（初始状态下）
    const categoriesSection = page.locator('.search-categories-section').first();
    await expect(categoriesSection).toBeVisible({ timeout: 10000 });
  });

  test('商品页面可加载', async ({ page }) => {
    await page.goto('/products');
    // 商品页面容器应可见
    await expect(page.locator('.products-page-wrapper, .products-container').first()).toBeVisible({ timeout: 10000 });
  });

  test('商品导航到搜索页面', async ({ page }) => {
    await page.goto('/');
    // 点击搜索图标按钮
    const searchBtn = page.locator('.floating-nav__icon-btn[aria-label="搜索"]').first();
    await expect(searchBtn).toBeVisible();
    // floating-nav 无限动画 → element is not stable，force 跳过稳定性检查（仍是真实点击）
    await searchBtn.click({ force: true });
    await expect(page).toHaveURL(/\/search/);
  });

  test('发布按钮导航到登录或发布页', async ({ page }) => {
    await page.goto('/');
    const publishBtn = page.locator('.floating-nav__publish-btn').first();
    await expect(publishBtn).toBeVisible();
    // floating-nav 无限动画 → element is not stable，force 跳过稳定性检查（仍是真实点击）
    await publishBtn.click({ force: true });
    // 未登录状态下应跳转到登录页，带 redirect 参数
    const currentUrl = page.url();
    // 可能是 /login 或 /publish（如果已登录）
    const isLoginOrPublish = currentUrl.includes('/login') || currentUrl.includes('/publish');
    expect(isLoginOrPublish).toBeTruthy();
  });

  test('搜索结果为空显示提示内容', async ({ page }) => {
    await page.goto('/search');

    const searchInput = page.locator('.search-input-field').first();
    await expect(searchInput).toBeVisible({ timeout: 10000 });
    // 使用一个不太可能匹配到的长字符串
    await searchInput.fill('zzzzzzzzznonexistentproduct999999');
    await page.locator('.search-submit-btn, button[type="submit"]').first().click();

    // 应显示"未找到相关商品"或结果计数为 0（expect 轮询，确定性等待）
    const noResults = page.locator('.search-no-results').first();
    const zeroCount = page.locator('text=0 件商品').first();
    const emptyResults = page.locator('.search-no-results-title').first();
    await expect(noResults.or(zeroCount).or(emptyResults)).toBeVisible({ timeout: 10000 });
  });

  test('搜索页面热门搜索区域初始可见', async ({ page }) => {
    await page.goto('/search');

    // 热门搜索区域应可见（如果 API 返回数据）
    const hotSection = page.locator('.search-top-card').first();
    const initialContent = page.locator('.search-initial-content').first();
    await expect(hotSection.or(initialContent)).toBeVisible({ timeout: 10000 });
  });
});
