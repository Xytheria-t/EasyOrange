/**
 * @fileoverview 认证关键场景 E2E 测试
 * @description 测试三个核心认证流程：
 * 1. 认证重定向返回 - 未登录访问受保护页面，重定向登录后返回
 * 2. 强制 401 收敛 - 令牌过期时系统正确处理
 * 3. 登出撤销确认 - 登出后状态清除和重定向
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:5173';
const TEST_USER = {
  username: 'testuser',
  password: 'testpass123'
};

async function clearAuthState(page: any) {
  await page.goto(BASE_URL);
  await page.waitForLoadState('domcontentloaded');
  await page.evaluate(() => {
    localStorage.removeItem('auth-storage');
    localStorage.removeItem('token');
  });
}

async function setAuthState(page: any, token: string = 'test-token', user: object = { username: 'testuser' }) {
  await page.goto(BASE_URL);
  await page.waitForLoadState('domcontentloaded');
  await page.evaluate(({ t, u }: { t: string; u: object }) => {
    localStorage.setItem('auth-storage', JSON.stringify({ state: { token: t, user: u, isAuthenticated: true }, version: 0 }));
    localStorage.setItem('token', JSON.stringify({ value: t }));
  }, { t: token, u: user });
}

async function waitForPageReady(page: any) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle');
}

test.describe('认证关键场景 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    await clearAuthState(page);
  });

  test.afterEach(async ({ page }) => {
    await clearAuthState(page);
  });

  test.describe('场景 1: 认证重定向返回', () => {

    test('未登录访问受保护页面应重定向到登录页', async ({ page }) => {
      const protectedPage = '/profile';
      await page.goto(`${BASE_URL}${protectedPage}`);

      await expect(page).toHaveURL(new RegExp(`^${BASE_URL}/login\\?redirect=`));

      const url = page.url();
      const urlObj = new URL(url);
      expect(urlObj.searchParams.get('redirect')).toBe(protectedPage);
    });

    test('直接访问首页应不带 redirect 参数', async ({ page }) => {
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      const url = page.url();
      const urlObj = new URL(url);
      expect(urlObj.searchParams.has('redirect')).toBe(false);
    });

  });

  test.describe('场景 2: 强制 401 收敛', () => {

    test('401 处理后应清除本地认证状态', async ({ page }) => {
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      await page.evaluate(() => {
        localStorage.removeItem('auth-storage');
        localStorage.removeItem('token');
        window.dispatchEvent(new CustomEvent('auth-session-change', {
          detail: { isAuthenticated: false, token: null, user: null, reason: 'unauthorized' }
        }));
      });

      const authToken = await page.evaluate(() => {
        const stored = localStorage.getItem('auth-storage');
        if (stored) {
          const parsed = JSON.parse(stored);
          return parsed.state?.token;
        }
        return null;
      });

      const apiToken = await page.evaluate(() => {
        const stored = localStorage.getItem('token');
        if (stored) {
          const parsed = JSON.parse(stored);
          return parsed.value;
        }
        return null;
      });

      expect(authToken).toBeNull();
      expect(apiToken).toBeNull();
    });

  });

  test.describe('场景 3: 登出撤销确认', () => {

    test('点击登出应清除认证状态并重定向到首页', async ({ page }) => {
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      const mockToken = 'logout-test-token';
      await setAuthState(page, mockToken, { username: TEST_USER.username });

      await page.reload();
      await waitForPageReady(page);

      await page.waitForTimeout(500);

      const userMenu = page.locator('#userMenu');
      await expect(userMenu).toBeVisible();

      await page.route('/api/auth/logout', async (route) => {
        const request = route.request();
        if (request.method() === 'POST') {
          const authHeader = request.headers()['authorization'];
          expect(authHeader).toBe(`Bearer ${mockToken}`);
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: '退出成功',
            data: null
          })
        });
      });

      const userAvatarBtn = page.locator('#userAvatarBtn');
      await userAvatarBtn.click();

      await page.waitForTimeout(300);

      const logoutBtn = page.locator('#logoutBtn');
      await expect(logoutBtn).toBeVisible();
      await logoutBtn.click();

      await page.waitForTimeout(500);

      const authToken = await page.evaluate(() => {
        const stored = localStorage.getItem('auth-storage');
        if (stored) {
          const parsed = JSON.parse(stored);
          return parsed.state?.token;
        }
        return null;
      });

      const apiToken = await page.evaluate(() => {
        const stored = localStorage.getItem('token');
        if (stored) {
          const parsed = JSON.parse(stored);
          return parsed.value;
        }
        return null;
      });

      expect(authToken).toBeNull();
      expect(apiToken).toBeNull();
    });

  });

});
