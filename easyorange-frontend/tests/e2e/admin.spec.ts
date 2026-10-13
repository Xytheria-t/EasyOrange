import { test, expect } from '@playwright/test';
import { seedAdminSession, seedSession, spaNavigate } from './helpers/auth';

/**
 * 管理后台 E2E —— 契约层：钉住「守卫 + 路由可达」，不断言页面数据。
 *
 * 后台判定走 useAdminGuard（userType '00'|'02'），登录态注入与 auth/product 同款
 * 双 Token 会话恢复流（seedSession 拦截 /auth/refresh + /users/me），管理员用
 * seedAdminSession（userType '00'）。受保护路由用 spaNavigate 前进，避开 full goto
 * 与异步 restore 的竞态（见 helpers/auth.ts 注释）。
 *
 * 断言基准：
 * - 守卫通过信号 = `.admin-layout` 可见（AdminRouteGuard 放行后 AdminLayout 挂载）
 * - 页面可达 = 该页稳定标题可见（知识库管理 / 检索质量回看）
 * - 页面数据的真实渲染由 src/admin/pages/** 组件测试覆盖，不在 E2E 重复
 *
 * 2026-10-13：管理端前端裁到 AI 运维两个面（知识库 + 检索质量回看），
 * 业务 CRUD 页面（用户/商品审核/订单/分类/统计）的 E2E 随路由一并移除。
 */
test.describe('管理后台', () => {
    test('未登录访问后台跳转到登录页', async ({ page }) => {
        await page.goto('/admin');
        await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
    });

    test('未登录访问后台各页面均跳转登录', async ({ page }) => {
        const adminPaths = ['/admin/knowledge', '/admin/retrieval-quality'];
        for (const path of adminPaths) {
            await page.goto(path);
            // 深链先加载 lazy admin chunk 再执行守卫跳转，给足时间
            await expect(page).toHaveURL(/\/login/, { timeout: 20000 });
        }
    });

    test('登录后普通用户访问后台显示禁止访问', async ({ page }) => {
        await seedSession(page, { userId: '2', username: 'regularuser', nickname: '普通用户' });
        await page.goto('/');
        await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });

        await spaNavigate(page, '/admin');

        // userType '01' 非管理员 → AdminRouteGuard 渲染 ForbiddenPage
        await expect(page.locator('text=访问受限').first()).toBeVisible({ timeout: 15000 });
    });

    test('管理员用户菜单显示后台入口', async ({ page }) => {
        await seedAdminSession(page, { userId: '1', username: 'admin', nickname: '管理员' });
        await page.goto('/');
        await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });

        const userMenuBtn = page.locator('[data-testid="btn-user-menu"]');
        await userMenuBtn.click();
        // 先等菜单容器展开，再断言菜单项：菜单项的可见性依赖父级 .active，顺序对了才不会偶发超时
        await expect(page.locator('.floating-nav__user-menu')).toBeVisible();

        const adminMenuItem = page.locator('.floating-nav__menu-item').filter({ hasText: '后台管理' });
        await expect(adminMenuItem).toBeVisible({ timeout: 10000 });
    });

    test('普通用户菜单不显示后台入口', async ({ page }) => {
        await seedSession(page, { userId: '2', username: 'regularuser', nickname: '普通用户' });
        await page.goto('/');
        await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });

        const userMenuBtn = page.locator('[data-testid="btn-user-menu"]');
        await userMenuBtn.click();
        await expect(page.locator('.floating-nav__user-menu')).toBeVisible();

        const adminMenuItem = page.locator('.floating-nav__menu-item').filter({ hasText: '后台管理' });
        await expect(adminMenuItem).toHaveCount(0);
    });

    test.describe('已登录管理员', () => {
        test.beforeEach(async ({ page }) => {
            await seedAdminSession(page, { userId: '1', username: 'admin', nickname: '管理员' });
            await page.goto('/');
            // 就绪信号：用户菜单出现 = token 已写入内存、restoreSession 完成
            await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });
        });

        test('可访问后台首页', async ({ page }) => {
            await spaNavigate(page, '/admin');
            // 守卫通过 → AdminLayout 挂载；index 即检索质量回看
            await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 });
            await expect(page.locator('.sidebar-logo-text')).toHaveText('易橙管理', { timeout: 10000 });
        });

        test('可导航到知识库管理页面', async ({ page }) => {
            await spaNavigate(page, '/admin/knowledge');
            await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 });
            await expect(page.locator('text=知识库管理').first()).toBeVisible({ timeout: 15000 });
        });

        test('可导航到检索质量回看页面', async ({ page }) => {
            await spaNavigate(page, '/admin/retrieval-quality');
            await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 });
            await expect(page.locator('text=检索质量回看').first()).toBeVisible({ timeout: 15000 });
        });

        test('后台侧边栏导航可用', async ({ page }) => {
            await spaNavigate(page, '/admin');
            await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 });

            // 点击侧边栏「知识库管理」→ 应路由到 /admin/knowledge
            const knowledgeNav = page.locator('.sidebar-nav a').filter({ hasText: '知识库管理' }).first();
            await expect(knowledgeNav).toBeVisible({ timeout: 10000 });
            await knowledgeNav.click();
            await expect(page).toHaveURL(/\/admin\/knowledge/, { timeout: 10000 });
            await expect(page.locator('text=知识库管理').first()).toBeVisible({ timeout: 15000 });
        });
    });
});
