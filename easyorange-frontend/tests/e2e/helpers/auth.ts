import type { Page } from '@playwright/test';

/** 后台 API 统一响应信封 */
const OK = { code: 'A0000', message: 'success' };

interface SeedUser {
    userId: string;
    username: string;
    nickname?: string;
}

/**
 * 在当前页面注入一个「已登录」会话。
 *
 * 应用已升级为双 Token：access token 只存内存、不落 localStorage，认证会话由
 * restoreSession() 借助 HttpOnly refresh cookie 恢复。因此旧测试通过
 * page.evaluate 写 localStorage['auth-storage'] / ['token'] 的做法已经失效。
 *
 * 这里改为拦截应用启动时 restoreSession() 真正调用的两个端点（/auth/refresh →
 * /users/me），让应用自己把会话立起来——与真实登录态等价，且不依赖后端与种子数据。
 * userType 取 '01'（普通用户），避免被管理员判定误伤。
 */
export async function seedSession(page: Page, user: SeedUser): Promise<void> {
    await page.route('**/api/auth/refresh**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ ...OK, data: { accessToken: 'e2e-mock-access-token' } }),
        })
    );

    await page.route('**/api/users/me**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                ...OK,
                data: {
                    userId: user.userId,
                    username: user.username,
                    nickname: user.nickname,
                    email: '',
                    phone: null,
                    studentId: null,
                    realName: null,
                    avatar: null,
                    status: 0,
                    userType: '01',
                    createTime: '',
                    updateTime: '',
                },
            }),
        })
    );

    // logout 也兜底返回成功——伪造 access token 打到真实后端会 401 并触发
    // handleUnauthorized 跳登录，退出用例需要干净的清会话路径。
    await page.route('**/api/auth/logout**', route =>
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(OK) })
    );
}

/**
 * 应用内 SPA 导航（React Router v7 监听 popstate）。restoreSession 由 main.tsx 后台拉起，
 * 直接 page.goto 一个受保护路由会先渲染同步的 ProtectedRoute（token 尚未就绪）而弹回登录页；
 * 在会话已建立的页面上用 pushState+popstate 前进，绕开该竞态且保留内存中的 token。
 */
export async function spaNavigate(page: Page, path: string): Promise<void> {
    await page.evaluate(p => {
        window.history.pushState({}, '', p);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, path);
}