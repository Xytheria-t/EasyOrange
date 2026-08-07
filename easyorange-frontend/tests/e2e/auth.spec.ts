import { test, expect } from '@playwright/test';
import { seedSession } from './helpers/auth';

test.describe('认证流程', () => {
  test('访问登录页面', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveURL(/\/login/);
  });

  test('登录表单包含必要字段', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[type="text"], input[name="account"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"], input[name="password"]').first()).toBeVisible();
  });

  test('注册页面可访问', async ({ page }) => {
    await page.goto('/register');
    await expect(page).toHaveURL(/\/register/);
  });

  test('登录失败显示错误信息', async ({ page }) => {
    await page.goto('/login');
    // 确保登录 tab 激活
    await page.locator('[data-testid="tab-login"]').click();
    await page.waitForTimeout(200);

    // 输入无效凭证
    await page.locator('[data-testid="input-account"]').fill('nonexistent_user');
    await page.locator('[data-testid="input-password"]').fill('wrongpassword123');
    await page.locator('[data-testid="btn-login-submit"]').click();

    // 等待错误提示出现（API 调用失败会显示错误）
    await page.waitForTimeout(2000);
    // 检查是否显示了错误消息（服务端返回或 toast 提示）
    const errorEl = page.locator('[data-testid="login-error"]');
    const toastEl = page.locator('.toast-message, .toast-error, [class*="toast"]').first();
    await expect(errorEl.or(toastEl)).toBeVisible({ timeout: 10000 });
  });

  test('登录成功跳转到首页', async ({ page }) => {
    await page.goto('/login');
    await page.locator('[data-testid="tab-login"]').click();
    await page.waitForTimeout(200);

    await page.locator('[data-testid="input-account"]').fill('testuser');
    await page.locator('[data-testid="input-password"]').fill('Test123456');

    // mock 登录 API 返回成功以模拟登录
    await page.route('**/api/auth/login**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'A0000',
          message: '登录成功',
          data: {
            token: 'mock-jwt-token',
            refreshToken: 'mock-refresh-token',
            user: {
              userId: 1,
              username: 'testuser',
              nickname: '测试用户',
              avatar: null,
            },
          },
        }),
      });
    });

    await page.locator('[data-testid="btn-login-submit"]').click();

    // 等待页面跳转到首页
    await page.waitForURL('/', { timeout: 10000 });
    await expect(page.locator('body')).toBeVisible();
  });

  test('未登录时访问受保护页面跳转到登录页', async ({ page }) => {
    // 每个测试是全新浏览器上下文，天然无凭据；直接访问受保护页，应被重定向到登录页
    await page.goto('/profile');
    await page.waitForLoadState('networkidle');

    // 应该跳转到登录页，且带 redirect 参数
    await expect(page).toHaveURL(/\/login/);
    const url = page.url();
    expect(url).toContain('redirect=');
    expect(url).toContain(encodeURIComponent('/profile'));
  });

  test('退出登录', async ({ page }) => {
    // 注入登录态（双 Token 会话恢复流）
    await seedSession(page, { userId: '1', username: 'testuser', nickname: '测试用户' });
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // 打开用户菜单。浮动导航在 hover 时有 :hover 位移 + navFloat 浮动动画，Playwright
    // 的 actionability 会判定元素「不稳定」；force 跳过该稳定性检查，仍是真实点击。
    const userMenuBtn = page.locator('[data-testid="btn-user-menu"]');
    await expect(userMenuBtn).toBeVisible({ timeout: 10000 });
    await userMenuBtn.click({ force: true });

    // 点击退出登录按钮
    const logoutBtn = page.locator('[data-testid="btn-logout"]');
    await expect(logoutBtn).toBeVisible();
    await logoutBtn.click();

    // 退出后应跳转到首页
    await page.waitForURL('/', { timeout: 10000 });

    // 验证 token 已被清除 — 登录按钮应可见
    const loginBtn = page.locator('[data-testid="btn-login"]');
    await expect(loginBtn).toBeVisible({ timeout: 5000 });
  });

  test('注册表单包含必要字段和验证提示', async ({ page }) => {
    await page.goto('/login');
    await page.locator('[data-testid="tab-register"]').click();
    await page.waitForTimeout(200);

    // 注册表单字段应可见
    await expect(page.locator('[data-testid="input-register-username"]')).toBeVisible();
    await expect(page.locator('[data-testid="input-register-password"]')).toBeVisible();
    await expect(page.locator('[data-testid="input-register-confirm-password"]')).toBeVisible();
    await expect(page.locator('[data-testid="btn-register-submit"]')).toBeVisible();

    // 验证提示信息可见
    await expect(page.locator('text=3-20位')).toBeVisible();
    await expect(page.locator('text=6-20位')).toBeVisible();
  });

  test('注册时两次密码不一致显示错误', async ({ page }) => {
    await page.goto('/login');
    await page.locator('[data-testid="tab-register"]').click();
    await page.waitForTimeout(200);

    // 填写密码但两次不一致
    await page.locator('[data-testid="input-register-username"]').fill('newuser123');
    await page.locator('[data-testid="input-register-password"]').fill('Password123');
    await page.locator('[data-testid="input-register-confirm-password"]').fill('DifferentPass456');

    // 提交表单（无需勾选同意条款来触发表单验证）
    // 先勾选同意条款。terms 复选框是 Radix Checkbox（隐藏原生 input + 可见 role=checkbox
    // button），需点击可见的 role=checkbox 以触发 onCheckedChange
    await page.getByRole('checkbox').click();
    await page.locator('[data-testid="btn-register-submit"]').click();

    // 应显示密码不一致的错误
    await page.waitForTimeout(1000);
    const errorEl = page.locator('[data-testid="register-error"]');
    const toastEl = page.locator('.toast-message, .toast-error, [class*="toast"]').first();
    // 可能通过 toast 或内联错误显示
    await expect(errorEl.or(toastEl)).toBeVisible({ timeout: 10000 });
  });

  test('忘记密码页面可交互', async ({ page }) => {
    await page.goto('/login');
    // 点击忘记密码链接
    const forgotLink = page.locator('[data-testid="link-forgot-password"]');
    await expect(forgotLink).toBeVisible();
    await forgotLink.click();

    // 应跳转到忘记密码页面
    await expect(page).toHaveURL(/\/forgot-password/);

    // 页面元素应可见
    await expect(page.locator('text=忘记密码')).toBeVisible();
    await expect(page.locator('[data-testid="input-forgot-phone"]')).toBeVisible();
    await expect(page.locator('[data-testid="btn-send-code"]')).toBeVisible();
  });

  test('登录/注册 tab 切换', async ({ page }) => {
    await page.goto('/login');

    // 默认显示登录表单
    await expect(page.locator('[data-testid="tab-login"].auth-page-tab--active')).toBeVisible();

    // 切换到注册
    await page.locator('[data-testid="tab-register"]').click();
    await page.waitForTimeout(200);
    await expect(page.locator('[data-testid="tab-register"].auth-page-tab--active')).toBeVisible();
    await expect(page.locator('[data-testid="input-register-username"]')).toBeVisible();

    // 切回登录
    await page.locator('[data-testid="tab-login"]').click();
    await page.waitForTimeout(200);
    await expect(page.locator('[data-testid="tab-login"].auth-page-tab--active')).toBeVisible();
    await expect(page.locator('[data-testid="input-account"]')).toBeVisible();
  });
});
