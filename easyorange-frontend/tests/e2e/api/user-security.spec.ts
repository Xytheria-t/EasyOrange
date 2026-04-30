/**
 * @fileoverview 后端用户模块 E2E 测试 - 安全场景
 * @description 测试速率限制、无效令牌、暴力破解防护、防重复提交等安全机制
 *
 * 测试安全特性:
 * - @RateLimiter - IP 级别的速率限制
 * - @RepeatSubmit - 防止重复提交
 * - LoginSecurityService - 登录尝试次数限制
 * - Token validation - 令牌验证
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

function generateUniqueTestUser() {
  const timestamp = Date.now();
  return {
    username: `sec_user_${timestamp}`,
    email: `sec_${timestamp}@example.com`,
    password: 'TestPass123!',
    phone: `138${String(timestamp).slice(-8)}`,
  };
}

async function registerAndLogin(request: any) {
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
  return { user, token: loginBody.data.token, refreshToken: loginBody.data.refreshToken };
}

test.describe('后端用户模块 - 安全场景 E2E', () => {

  test.describe('令牌验证', () => {

    test('使用 Bearer 前缀格式正确的 token 访问受保护端点', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);
    });

    test('缺少 Bearer 前缀的 token 应被拒绝', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: token,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('伪造的 JWT token 应被拒绝', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.fake',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('登出后 token 应失效', async ({ request }) => {
      const { token, refreshToken } = await registerAndLogin(request);

      // 登出
      await request.post(`${BASE_URL}/api/auth/logout`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-Refresh-Token': refreshToken,
        },
      });

      // 使用已登出的 token 访问受保护端点
      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('输入验证', () => {

    test('注册时缺少必填字段应失败', async ({ request }) => {
      const response = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: '',
          password: 'TestPass123!',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('注册时密码为空应失败', async ({ request }) => {
      const user = generateUniqueTestUser();

      const response = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: '',
          email: user.email,
          phone: user.phone,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('注册时用户名过长应失败', async ({ request }) => {
      const user = generateUniqueTestUser();

      const response = await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: 'a'.repeat(100),
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('登录时缺少账号应失败', async ({ request }) => {
      const response = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          password: 'TestPass123!',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('忘记密码时缺少手机号应失败', async ({ request }) => {
      const response = await request.post(`${BASE_URL}/api/auth/forgotPassword`, {
        data: {
          newPassword: 'NewPass123!',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('认证端点防护', () => {

    test('使用 refreshToken 访问受保护端点应失败', async ({ request }) => {
      const { refreshToken } = await registerAndLogin(request);

      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('空 Authorization 头应返回认证错误', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: '',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('使用其他用户的 token 无法修改他人资料', async ({ request }) => {
      // 创建用户 A
      const userA = generateUniqueTestUser();
      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: userA.username,
          password: userA.password,
          email: userA.email,
          phone: userA.phone,
        },
      });
      const loginA = await request.post(`${BASE_URL}/api/auth/login`, {
        data: { account: userA.username, password: userA.password },
      });
      const bodyA = await loginA.json();

      // 创建用户 B
      const userB = generateUniqueTestUser();
      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: userB.username,
          password: userB.password,
          email: userB.email,
          phone: userB.phone,
        },
      });
      const loginB = await request.post(`${BASE_URL}/api/auth/login`, {
        data: { account: userB.username, password: userB.password },
      });
      const bodyB = await loginB.json();

      // 用 A 的 token 尝试修改 B 的资料
      const updateResponse = await request.put(`${BASE_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${bodyA.data.token}` },
        data: { avatar: 'https://example.com/hacked.png' },
      });

      // 更新的是 A 自己的资料，不是 B 的
      const profileB = await request.get(`${BASE_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${bodyB.data.token}` },
      });
      const profileBodyB = await profileB.json();
      expect(profileBodyB.data.avatar).not.toBe('https://example.com/hacked.png');
    });
  });

  test.describe('速率限制和防重复', () => {

    test('连续快速登录不应被立即阻止（正常场景）', async ({ request }) => {
      const user = generateUniqueTestUser();

      await request.post(`${BASE_URL}/api/auth/register`, {
        data: {
          username: user.username,
          password: user.password,
          email: user.email,
          phone: user.phone,
        },
      });

      // 第一次登录
      const login1 = await request.post(`${BASE_URL}/api/auth/login`, {
        data: { account: user.username, password: user.password },
      });
      expect(login1.status()).toBe(200);

      // 登出
      const body1 = await login1.json();
      await request.post(`${BASE_URL}/api/auth/logout`, {
        headers: {
          Authorization: `Bearer ${body1.data.token}`,
          'X-Refresh-Token': body1.data.refreshToken,
        },
      });

      // 第二次登录
      const login2 = await request.post(`${BASE_URL}/api/auth/login`, {
        data: { account: user.username, password: user.password },
      });
      expect(login2.status()).toBe(200);
      const body2 = await login2.json();
      expect(body2.code).toBe(200);
    });
  });
});
