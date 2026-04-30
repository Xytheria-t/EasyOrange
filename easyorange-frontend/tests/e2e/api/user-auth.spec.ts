/**
 * @fileoverview 后端用户模块 E2E 测试 - 认证流程
 * @description 测试注册、登录、登出、刷新令牌等核心认证流程
 *
 * 测试后端 API:
 * - POST /api/auth/register   - 用户注册
 * - POST /api/auth/login      - 用户登录
 * - POST /api/auth/logout     - 用户登出
 * - POST /api/auth/refresh    - 刷新令牌
 * - POST /api/auth/forgotPassword - 忘记密码
 */

import { test, expect, APIRequestContext } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

function generateUniqueTestUser() {
  const timestamp = Date.now();
  return {
    username: `testuser_${timestamp}`,
    email: `test_${timestamp}@example.com`,
    password: 'TestPass123!',
    phone: `138${String(timestamp).slice(-8)}`,
  };
}

test.describe('后端用户模块 - 认证流程 E2E', () => {

  test.describe('注册流程', () => {

    test('成功注册新用户', async ({ request }) => {
      const user = generateUniqueTestUser();

      const response = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      expect(response.status()).toBe(200);

      const body = await response.json();
      expect(body.code).toBe(200);
      expect(body.data).toBeDefined();
      expect(body.data.userId).toBeDefined();
    });

    test('注册时用户名重复应失败', async ({ request }) => {
      const user = generateUniqueTestUser();

      const firstResponse = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      expect(firstResponse.status()).toBe(200);

      const secondResponse = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: 'AnotherPass123!',
          email: `other_${Date.now()}@example.com`,
          phone: `139${String(Date.now()).slice(-8)}`,
        },
      });

      expect(secondResponse.status()).toBe(200);

      const body = await secondResponse.json();
      expect(body.code).not.toBe(200);
    });

    test('注册时密码强度不足应失败', async ({ request }) => {
      const user = generateUniqueTestUser();

      const response = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: '123',
          email: user.email,
          phone: user.phone,
        },
      });

      expect(response.status()).toBe(200);

      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('登录流程', () => {

    test('使用正确凭据登录成功', async ({ request }) => {
      const user = generateUniqueTestUser();

      const registerResponse = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      expect(registerResponse.status()).toBe(200);

      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: user.password,
        },
      });

      expect(loginResponse.status()).toBe(200);

      const body = await loginResponse.json();
      expect(body.code).toBe(200);
      expect(body.data.token).toBeDefined();
      expect(body.data.refreshToken).toBeDefined();
      expect(body.data.user).toBeDefined();
      expect(body.data.user.username).toBe(user.username);
    });

    test('使用错误密码登录应失败', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: 'WrongPassword123!',
        },
      });

      expect(loginResponse.status()).toBe(200);

      const body = await loginResponse.json();
      expect(body.code).not.toBe(200);
    });

    test('使用不存在的用户名登录应失败', async ({ request }) => {
      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: 'nonexistent_user_12345',
          password: 'SomePassword123!',
        },
      });

      expect(loginResponse.status()).toBe(200);

      const body = await loginResponse.json();
      expect(body.code).not.toBe(200);
    });

    test('使用邮箱登录', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.email,
          password: user.password,
        },
      });

      expect(loginResponse.status()).toBe(200);

      const body = await loginResponse.json();
      expect(body.code).toBe(200);
      expect(body.data.token).toBeDefined();
    });

    test('登录响应中包含 refreshToken', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: user.password,
        },
      });

      const body = await loginResponse.json();
      expect(body.data.refreshToken).toBeDefined();
      expect(typeof body.data.refreshToken).toBe('string');
      expect(body.data.refreshToken.length).toBeGreaterThan(0);
    });
  });

  test.describe('完整注册到登出流程', () => {

    test('注册 -> 登录 -> 获取资料 -> 登出 完整流程', async ({ request }) => {
      const user = generateUniqueTestUser();

      // Step 1: 注册
      const registerResponse = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });
      expect(registerResponse.status()).toBe(200);

      // Step 2: 登录
      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: user.password,
        },
      });
      expect(loginResponse.status()).toBe(200);
      const loginBody = await loginResponse.json();
      expect(loginBody.code).toBe(200);

      const token = loginBody.data.token;

      // Step 3: 获取用户资料
      const profileResponse = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      expect(profileResponse.status()).toBe(200);
      const profileBody = await profileResponse.json();
      expect(profileBody.code).toBe(200);
      expect(profileBody.data.username).toBe(user.username);

      // Step 4: 登出
      const logoutResponse = await request.post(`${BASE_URL}/api/auth/logout`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-Refresh-Token': loginBody.data.refreshToken,
        },
      });
      expect(logoutResponse.status()).toBe(200);
      const logoutBody = await logoutResponse.json();
      expect(logoutBody.code).toBe(200);
    });
  });

  test.describe('令牌刷新', () => {

    test('使用 refreshToken 获取新 accessToken', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: user.password,
        },
      });
      const loginBody = await loginResponse.json();
      const refreshToken = loginBody.data.refreshToken;

      const refreshResponse = await request.post(`${BASE_URL}/api/auth/refresh`, {
        data: {
          refreshToken: refreshToken,
        },
      });

      expect(refreshResponse.status()).toBe(200);
      const refreshBody = await refreshResponse.json();
      expect(refreshBody.code).toBe(200);
      expect(refreshBody.data.token).toBeDefined();
      expect(typeof refreshBody.data.token).toBe('string');
    });

    test('使用无效 refreshToken 应失败', async ({ request }) => {
      const refreshResponse = await request.post(`${BASE_URL}/api/auth/refresh`, {
        data: {
          refreshToken: 'invalid-refresh-token-12345',
        },
      });

      expect(refreshResponse.status()).toBe(200);
      const body = await refreshResponse.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('忘记密码', () => {

    test('使用已注册手机号重置密码', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      const newPassword = 'NewPass456!';

      const resetResponse = await request.post(`${BASE_URL}/api/auth/forgotPassword`, {
        data: {
          phone: user.phone,
          newPassword: newPassword,
        },
      });

      expect(resetResponse.status()).toBe(200);
      const body = await resetResponse.json();
      expect(body.code).toBe(200);

      // 验证可以用新密码登录
      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.phone,
          password: newPassword,
        },
      });

      const loginBody = await loginResponse.json();
      expect(loginBody.code).toBe(200);
    });

    test('使用未注册手机号重置密码应失败', async ({ request }) => {
      const resetResponse = await request.post(`${BASE_URL}/api/auth/forgotPassword`, {
        data: {
          phone: '13900000000',
          newPassword: 'NewPass456!',
        },
      });

      expect(resetResponse.status()).toBe(200);
      const body = await resetResponse.json();
      expect(body.code).not.toBe(200);
    });
  });
});
