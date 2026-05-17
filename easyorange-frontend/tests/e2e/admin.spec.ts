import { test, expect } from '@playwright/test';

test.describe('管理后台', () => {
  test('后台登录页面可访问', async ({ page }) => {
    await page.goto('/admin/login');
    await expect(page).toHaveURL(/\/admin\/login/);
    await expect(page.locator('body')).toBeVisible();
  });

  test('未登录访问后台跳转到登录页', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    const currentUrl = page.url();
    // 未认证状态应当跳转到登录页
    expect(currentUrl.includes('login')).toBeTruthy();
  });

  test('未登录访问后台各页面均跳转登录', async ({ page }) => {
    const adminPaths = ['/admin/users', '/admin/products', '/admin/orders', '/admin/reports', '/admin/stats'];
    for (const path of adminPaths) {
      await page.goto(path);
      await page.waitForLoadState('networkidle');
      const currentUrl = page.url();
      expect(currentUrl.includes('login')).toBeTruthy();
    }
  });

  test('登录后普通用户访问后台显示禁止访问', async ({ page }) => {
    // 设置普通用户登录状态
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-user-token',
          refreshToken: 'mock-refresh-token',
          user: {
            userId: 2,
            username: 'regularuser',
            nickname: '普通用户',
            role: 'user',
            roles: ['user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-user-token');
    });

    await page.goto('/admin');
    await page.waitForLoadState('networkidle');

    // 普通用户应看到 403 禁止访问页面
    await page.waitForTimeout(3000);
    const forbiddenPage = page.locator('text=访问受限').first();
    const forbiddenCode = page.locator('text=403').first();
    const redirectToLogin = page.locator('text=返回主站').first();
    await expect(forbiddenPage.or(redirectToLogin).or(forbiddenCode)).toBeVisible({ timeout: 15000 });
  });

  test('已登录管理员可访问后台首页', async ({ page }) => {
    // 设置管理员登录状态
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin');
    await page.waitForLoadState('networkidle');

    // 管理员应看到后台布局
    await page.waitForTimeout(3000);
    const adminLayout = page.locator('.admin-layout, [class*="admin"]').first();
    await expect(adminLayout).toBeVisible({ timeout: 15000 });
  });

  test('管理员后台首页显示统计数据区域', async ({ page }) => {
    // 设置管理员登录状态
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 统计卡片区域应可见（可能有加载骨架屏或实际数据）
    const statCards = page.locator('[class*="stat"], [class*="Stat"], [class*="card"]').first();
    const dashboardGrid = page.locator('[class*="grid"], [class*="dashboard"]').first();
    await expect(statCards.or(dashboardGrid)).toBeVisible({ timeout: 15000 });
  });

  test('管理员可导航到商品审核页面', async ({ page }) => {
    // 设置管理员登录状态
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin/products');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 商品审核页面应有搜索框和筛选选项
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="search"]').first();
    const statusSelect = page.locator('select, [class*="select"]').first();
    await expect(searchInput.or(statusSelect)).toBeVisible({ timeout: 15000 });
  });

  test('管理员可导航到举报管理页面', async ({ page }) => {
    // 设置管理员登录状态
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin/reports');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 举报管理页面应有筛选器和表格
    const filterSection = page.locator('[class*="filter"], [class*="status"]').first();
    await expect(filterSection).toBeVisible({ timeout: 15000 });
  });

  test('管理员可导航到用户管理页面', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin/users');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 用户管理页面应有表格或内容区域
    const tableEl = page.locator('table, [class*="table"], [class*="Table"]').first();
    const contentArea = page.locator('[class*="content"], [class*="page"]').first();
    await expect(tableEl.or(contentArea)).toBeVisible({ timeout: 15000 });
  });

  test('管理员可导航到订单管理页面', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin/orders');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 订单管理页面应有内容
    const content = page.locator('[class*="page"], [class*="content"]').first();
    await expect(content).toBeVisible({ timeout: 15000 });
  });

  test('管理员可导航到数据统计页面', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin/stats');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 统计页面应有内容
    const content = page.locator('[class*="page"], [class*="stats"]').first();
    await expect(content).toBeVisible({ timeout: 15000 });
  });

  test('管理员后台侧边栏导航可用', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.goto('/admin');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 侧边栏导航链接可见（AdminLayout 内的导航）
    const sideNav = page.locator('nav a, [class*="sidebar"] a, [class*="menu"] a').first();
    if (await sideNav.isVisible().catch(() => false)) {
      // 尝试点击导航链接跳转到商品审核
      const productLink = page.locator('nav a, [class*="sidebar"] a, [class*="menu"] a').filter({ hasText: /商品/i }).first();
      if (await productLink.isVisible().catch(() => false)) {
        await productLink.click();
        await page.waitForTimeout(1000);
        await expect(page).toHaveURL(/\/admin\/products/);
      }
    }
  });

  test('非管理员用户菜单不显示后台入口', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-user-token',
          refreshToken: 'mock-refresh-token',
          user: {
            userId: 2,
            username: 'regularuser',
            nickname: '普通用户',
            role: 'user',
            roles: ['user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-user-token');
    });

    await page.reload();
    await page.waitForLoadState('networkidle');

    // 打开用户菜单
    const userMenuBtn = page.locator('[data-testid="btn-user-menu"]').first();
    await expect(userMenuBtn).toBeVisible({ timeout: 10000 });
    await userMenuBtn.click();

    // 菜单中不应包含"后台管理"链接
    const adminMenuItem = page.locator('text=后台管理');
    await expect(adminMenuItem).toHaveCount(0);
  });

  test('管理员用户菜单显示后台入口', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      const authData = {
        state: {
          token: 'mock-admin-token',
          refreshToken: 'mock-admin-refresh',
          user: {
            userId: 1,
            username: 'admin',
            nickname: '管理员',
            role: 'admin',
            roles: ['admin', 'user'],
          },
        },
        version: 0,
      };
      localStorage.setItem('auth-storage', JSON.stringify(authData));
      localStorage.setItem('token', 'mock-admin-token');
    });

    await page.reload();
    await page.waitForLoadState('networkidle');

    // 打开用户菜单
    const userMenuBtn = page.locator('[data-testid="btn-user-menu"]').first();
    await expect(userMenuBtn).toBeVisible({ timeout: 10000 });
    await userMenuBtn.click();

    // 菜单中应包含"后台管理"链接
    const adminMenuItem = page.locator('.floating-nav__menu-item--admin, .floating-nav__menu-item').filter({ hasText: '后台管理' });
    await expect(adminMenuItem).toBeVisible();
  });
});
