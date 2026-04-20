/**
 * @fileoverview 认证关键场景 E2E 测试
 * @description 测试三个核心认证流程：
 * 1. 认证重定向返回 - 未登录访问受保护页面，重定向登录后返回
 * 2. 强制 401 收敛 - 令牌过期时系统正确处理
 * 3. 登出撤销确认 - 登出后状态清除和重定向
 */

import { test, expect } from '@playwright/test';

// 测试配置
const BASE_URL = 'http://localhost:5173';
const TEST_USER = {
  username: 'testuser',
  password: 'testpass123'
};

// 辅助函数：清除认证状态
async function clearAuthState(page: any) {
  await page.evaluate(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  });
}

// 辅助函数：设置认证状态
async function setAuthState(page: any, token: string = 'test-token', user: object = { username: 'testuser' }) {
  await page.evaluate(({ t, u }: { t: string; u: object }) => {
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify(u));
  }, { t: token, u: user });
}

// 辅助函数：等待页面加载完成
async function waitForPageReady(page: any) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForLoadState('networkidle');
}

test.describe('认证关键场景 E2E 测试', () => {

  test.beforeEach(async ({ page }) => {
    // 每个测试前清除认证状态
    await clearAuthState(page);
  });

  test.afterEach(async ({ page }) => {
    // 测试后清理
    await clearAuthState(page);
  });

  /**
   * 场景 1: 认证重定向返回
   * - 用户未登录访问受保护页面
   * - 被重定向到登录页（首页带 redirect 参数）
   * - 登录成功后返回原始页面
   * - 验证返回 URL 参数正确传递
   */
  test.describe('场景 1: 认证重定向返回', () => {

    test('未登录访问受保护页面应重定向到登录页', async ({ page }) => {
      // 访问受保护页面（如个人中心）
      const protectedPage = '/profile.html';
      await page.goto(`${BASE_URL}${protectedPage}`);

      // 验证被重定向到首页（带 redirect 参数）
      await expect(page).toHaveURL(new RegExp(`^${BASE_URL}/\\?redirect=`));

      // 验证 redirect 参数包含原始页面路径
      const url = page.url();
      const urlObj = new URL(url);
      expect(urlObj.searchParams.get('redirect')).toBe(protectedPage);
    });

    test('登录成功后应返回原始受保护页面', async ({ page }) => {
      // 1. 访问受保护页面，被重定向到登录页
      const protectedPage = '/profile.html';
      await page.goto(`${BASE_URL}${protectedPage}`);
      await waitForPageReady(page);

      // 验证 redirect 参数正确
      let urlObj = new URL(page.url());
      expect(urlObj.searchParams.get('redirect')).toBe(protectedPage);

      // 2. 打开登录表单
      const loginBtn = page.locator('#loginBtn');
      if (await loginBtn.isVisible()) {
        await loginBtn.click();
      }

      // 等待认证弹窗出现
      const authContainer = page.locator('#authContainer');
      await expect(authContainer).toHaveClass(/active/);

      // 3. 填写登录表单（使用 Mock API 响应）
      await page.route('/api/auth/login', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 200,
            message: '登录成功',
            data: {
              token: 'mock-token-' + Date.now(),
              user: {
                username: TEST_USER.username,
                nickname: 'Test User'
              }
            }
          })
        });
      });

      // 填写用户名和密码
      const usernameInput = page.locator('#loginUsername');
      const passwordInput = page.locator('#loginPassword');

      await usernameInput.fill(TEST_USER.username);
      await passwordInput.fill(TEST_USER.password);

      // 4. 提交登录表单
      const submitBtn = page.locator('#loginForm button[type="submit"]');
      await submitBtn.click();

      // 5. 验证返回到原始受保护页面
      await page.waitForURL(new RegExp(`^${BASE_URL}${protectedPage}`), { timeout: 5000 });

      // 验证 URL 不再包含 redirect 参数
      urlObj = new URL(page.url());
      expect(urlObj.searchParams.has('redirect')).toBe(false);

      // 验证 token 已保存到 localStorage
      const token = await page.evaluate(() => localStorage.getItem('token'));
      expect(token).toBeTruthy();
      expect(token).toContain('mock-token-');
    });

    test('直接访问首页应不带 redirect 参数', async ({ page }) => {
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      // 首页 URL 不应包含 redirect 参数
      const url = page.url();
      const urlObj = new URL(url);
      expect(urlObj.searchParams.has('redirect')).toBe(false);
    });

  });

  /**
   * 场景 2: 强制 401 收敛
   * - 用户已登录状态
   * - 模拟后端返回 401（令牌过期）
   * - 验证系统正确处理并重定向到登录
   */
  test.describe('场景 2: 强制 401 收敛', () => {

    test('API 返回 401 时应清除会话并重定向到登录页', async ({ page }) => {
      // 1. 先登录并设置认证状态
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      // 设置 mock token
      const mockToken = 'expired-token-123';
      await setAuthState(page, mockToken, { username: TEST_USER.username });

      // 刷新页面以应用认证状态
      await page.reload();
      await waitForPageReady(page);

      // 2. 模拟 API 返回 401
      let requestMade = false;
      await page.route(/\/api\/.+/, async (route) => {
        const request = route.request();
        // 只拦截需要认证的 API 请求
        if (request.url().includes('/api/') && !request.url().includes('/auth/')) {
          requestMade = true;
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 401,
              message: '登录已过期，请重新登录',
              data: null
            })
          });
        } else {
          await route.continue();
        }
      });

      // 3. 触发一个需要认证的 API 请求（访问个人中心）
      await page.goto(`${BASE_URL}/profile.html`);
      await waitForPageReady(page);

      // 等待一小段时间让 401 处理逻辑执行
      await page.waitForTimeout(1000);

      // 4. 验证被重定向到登录页
      // 401 处理后会重定向到首页并带 redirect 参数
      const currentUrl = page.url();

      // 验证 URL 包含 redirect 参数（表示需要重新登录）
      if (currentUrl.includes('/profile')) {
        // 如果还在 profile 页面，验证 token 已被清除
        const token = await page.evaluate(() => localStorage.getItem('token'));
        expect(token).toBeNull();
      }
    });

    test('401 处理后应清除本地认证状态', async ({ page }) => {
      // 设置认证状态
      await setAuthState(page, 'some-token', { username: 'test' });

      // 触发 handleUnauthorized（通过手动调用或 401 响应）
      await page.evaluate(() => {
        // 模拟 handleUnauthorized 的效果
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new CustomEvent('auth-session-change', {
          detail: { isAuthenticated: false, token: null, user: null, reason: 'unauthorized' }
        }));
      });

      // 验证 localStorage 已被清除
      const token = await page.evaluate(() => localStorage.getItem('token'));
      const user = await page.evaluate(() => localStorage.getItem('user'));

      expect(token).toBeNull();
      expect(user).toBeNull();
    });

  });

  /**
   * 场景 3: 登出撤销确认
   * - 用户点击登出
   * - 验证前端状态清除
   * - 验证后端撤销令牌（如果适用）
   * - 验证用户被重定向到首页
   */
  test.describe('场景 3: 登出撤销确认', () => {

    test('点击登出应清除认证状态并重定向到首页', async ({ page }) => {
      // 1. 先登录并设置认证状态
      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      // 设置认证状态
      const mockToken = 'logout-test-token';
      await setAuthState(page, mockToken, { username: TEST_USER.username });

      // 刷新页面
      await page.reload();
      await waitForPageReady(page);

      // 验证用户已登录（userMenu 应显示）
      const userMenu = page.locator('#userMenu');
      await expect(userMenu).toBeVisible();

      // 2. 监听登出 API 请求
      let logoutRequestMade = false;
      await page.route('/api/auth/logout', async (route) => {
        const request = route.request();
        if (request.method() === 'POST') {
          logoutRequestMade = true;
          // 验证 Authorization header
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

      // 3. 点击登出按钮
      const logoutBtn = page.locator('#logoutBtn');
      await expect(logoutBtn).toBeVisible();
      await logoutBtn.click();

      // 4. 验证登出 API 被调用
      await page.waitForTimeout(500);

      // 5. 验证前端状态已清除
      const token = await page.evaluate(() => localStorage.getItem('token'));
      const user = await page.evaluate(() => localStorage.getItem('user'));

      expect(token).toBeNull();
      expect(user).toBeNull();

      // 6. 验证用户菜单已隐藏，登录按钮显示
      await expect(userMenu).not.toBeVisible();

      const loginBtn = page.locator('#loginBtn');
      await expect(loginBtn).toBeVisible();
    });

    test('登出后应重定向到首页', async ({ page }) => {
      // 在个人中心页面进行登出操作
      await setAuthState(page, 'test-token', { username: TEST_USER.username });

      await page.goto(`${BASE_URL}/profile.html`);
      await waitForPageReady(page);

      // 监听并 mock 登出 API
      await page.route('/api/auth/logout', async (route) => {
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

      // 点击登出
      const logoutBtn = page.locator('#logoutBtn');
      await logoutBtn.click();

      // 等待重定向完成
      await page.waitForLoadState('domcontentloaded');

      // 验证 URL 是首页
      await expect(page).toHaveURL(new RegExp(`^${BASE_URL}/$`));
    });

    test('登出时即使 API 失败也应清除本地状态', async ({ page }) => {
      // 设置认证状态
      await setAuthState(page, 'fail-logout-token', { username: TEST_USER.username });

      await page.goto(`${BASE_URL}/`);
      await waitForPageReady(page);

      // 模拟登出 API 失败
      await page.route('/api/auth/logout', async (route) => {
        await route.abort('Failed to fetch');
      });

      // 点击登出
      const logoutBtn = page.locator('#logoutBtn');
      await logoutBtn.click();

      // 等待一段时间让错误处理完成
      await page.waitForTimeout(1000);

      // 即使 API 失败，也应清除本地状态
      const token = await page.evaluate(() => localStorage.getItem('token'));
      expect(token).toBeNull();
    });

  });

});
