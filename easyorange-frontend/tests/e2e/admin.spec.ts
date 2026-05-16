import { test, expect } from '@playwright/test';

test.describe('管理后台', () => {
  test('后台登录页面可访问', async ({ page }) => {
    await page.goto('/admin/login');
    await expect(page).toHaveURL(/\/admin\/login/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('未登录访问后台', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    const currentUrl = page.url();
    // 未认证状态应当跳转到登录页
    expect(currentUrl.includes('login')).toBeTruthy();
  });
});
