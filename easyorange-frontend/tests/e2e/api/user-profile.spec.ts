/**
 * @fileoverview 后端用户模块 E2E 测试 - 用户资料管理
 * @description 测试获取资料、更新资料、修改密码、头像上传等用户管理流程
 *
 * 测试后端 API:
 * - GET    /api/users/me          - 获取当前用户资料
 * - PUT    /api/users/me          - 更新用户资料
 * - PUT    /api/users/me/password - 修改密码
 * - POST   /api/users/avatar      - 上传头像
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

function generateUniqueTestUser() {
  const timestamp = Date.now();
  return {
    username: `profile_user_${timestamp}`,
    email: `profile_${timestamp}@example.com`,
    password: 'TestPass123!',
    phone: `138${String(timestamp).slice(-8)}`,
  };
}

async function registerAndLogin(request: any) {
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
  const loginBody = await loginResponse.json();
  expect(loginBody.code).toBe(200);

  return {
    user,
    token: loginBody.data.token,
    refreshToken: loginBody.data.refreshToken,
  };
}

test.describe('后端用户模块 - 用户资料管理 E2E', () => {

  test.describe('获取用户资料', () => {

    test('未认证访问 /api/users/me 应返回 401', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/users/me`);

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('使用有效 token 获取用户资料', async ({ request }) => {
      const { user, token } = await registerAndLogin(request);

      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);
      expect(body.data.username).toBe(user.username);
      expect(body.data.email).toBe(user.email);
    });

    test('使用过期/无效 token 应返回认证失败', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: 'Bearer expired-invalid-token-12345',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('更新用户资料', () => {

    test('更新用户头像 URL', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.put(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          avatar: 'https://example.com/new-avatar.png',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);

      // 验证更新已生效
      const profileResponse = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const profileBody = await profileResponse.json();
      expect(profileBody.data.avatar).toBe('https://example.com/new-avatar.png');
    });

    test('更新用户性别', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.put(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          sex: 1,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);
    });

    test('更新多个字段', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.put(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          avatar: 'https://example.com/multi-update.png',
          sex: 0,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);

      // 验证所有字段都已更新
      const profileResponse = await request.get(`${BASE_URL}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      const profileBody = await profileResponse.json();
      expect(profileBody.data.avatar).toBe('https://example.com/multi-update.png');
    });

    test('未认证更新资料应失败', async ({ request }) => {
      const response = await request.put(`${BASE_URL}/api/users/me`, {
        data: {
          avatar: 'https://example.com/shouldfail.png',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('修改密码', () => {

    test('使用正确的旧密码修改密码', async ({ request }) => {
      const { user, token } = await registerAndLogin(request);

      const newPassword = 'NewSecurePass123!';

      const response = await request.put(`${BASE_URL}/api/users/me/password`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          oldPassword: user.password,
          newPassword: newPassword,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);

      // 验证可以用新密码登录
      const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: newPassword,
        },
      });

      const loginBody = await loginResponse.json();
      expect(loginBody.code).toBe(200);
    });

    test('使用错误的旧密码修改密码应失败', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      const response = await request.put(`${BASE_URL}/api/users/me/password`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          oldPassword: 'WrongOldPassword123!',
          newPassword: 'NewSecurePass123!',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('新旧密码相同应失败', async ({ request }) => {
      const { user, token } = await registerAndLogin(request);

      const response = await request.put(`${BASE_URL}/api/users/me/password`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        data: {
          oldPassword: user.password,
          newPassword: user.password,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });

    test('未认证修改密码应失败', async ({ request }) => {
      const response = await request.put(`${BASE_URL}/api/users/me/password`, {
        data: {
          oldPassword: 'OldPass123!',
          newPassword: 'NewPass123!',
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('头像上传', () => {

    test('上传合法图片文件作为头像', async ({ request }) => {
      const { token } = await registerAndLogin(request);

      // 创建一个 1x1 像素的 PNG 图片（最小有效 PNG）
      const pngHeader = Buffer.from([
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
        0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
        0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
        0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x05, 0xFE,
        0xB4, 0x45, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
        0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82,
      ]);

      const response = await request.post(`${BASE_URL}/api/users/avatar`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        multipart: {
          file: {
            name: 'avatar.png',
            mimeType: 'image/png',
            buffer: pngHeader,
          },
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).toBe(200);
      expect(body.data).toBeDefined();
    });

    test('未认证上传头像应失败', async ({ request }) => {
      const pngHeader = Buffer.from([
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
      ]);

      const response = await request.post(`${BASE_URL}/api/users/avatar`, {
        multipart: {
          file: {
            name: 'avatar.png',
            mimeType: 'image/png',
            buffer: pngHeader,
          },
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.code).not.toBe(200);
    });
  });

  test.describe('资料管理完整流程', () => {

    test('注册后完整资料管理流程', async ({ request }) => {
      const { user, token } = await registerAndLogin(request);

      // 1. 获取初始资料
      const initialProfile = await request.get(`${BASE_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const initialBody = await initialProfile.json();
      expect(initialBody.code).toBe(200);
      expect(initialBody.data.username).toBe(user.username);

      // 2. 更新资料
      const updateResponse = await request.put(`${BASE_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          avatar: 'https://example.com/fullflow.png',
          sex: 1,
        },
      });
      expect((await updateResponse.json()).code).toBe(200);

      // 3. 修改密码
      const changePasswordResponse = await request.put(`${BASE_URL}/api/users/me/password`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          oldPassword: user.password,
          newPassword: 'ChangedPass123!',
        },
      });
      expect((await changePasswordResponse.json()).code).toBe(200);

      // 4. 用新密码登录
      const newLoginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          account: user.username,
          password: 'ChangedPass123!',
        },
      });
      const newLoginBody = await newLoginResponse.json();
      expect(newLoginBody.code).toBe(200);

      // 5. 获取更新后的资料
      const updatedProfile = await request.get(`${BASE_URL}/api/users/me`, {
        headers: { Authorization: `Bearer ${newLoginBody.data.token}` },
      });
      const updatedBody = await updatedProfile.json();
      expect(updatedBody.data.avatar).toBe('https://example.com/fullflow.png');
    });
  });
});
