import { test, expect } from '@playwright/test';

test.describe('商品浏览', () => {
  test('首页可加载', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('body')).toBeVisible();
  });

  test('搜索功能可交互', async ({ page }) => {
    await page.goto('/');
    const searchInput = page.locator('input[type="text"], input[placeholder*="搜索"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('手机');
      await searchInput.press('Enter');
      await page.waitForLoadState('networkidle');
    }
  });

  test('页面导航存在', async ({ page }) => {
    await page.goto('/');
    const nav = page.locator('nav').first();
    await expect(nav).toBeVisible();
  });
});
