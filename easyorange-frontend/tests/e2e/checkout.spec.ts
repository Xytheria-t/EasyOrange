import { test, expect } from '@playwright/test';
import { seedSession, spaNavigate } from './helpers/auth';

/**
 * 收银台（支付）核心闭环冒烟 — 与 auth.spec.ts 同款模式：前端 dev server + page.route mock API，
 * 不依赖真实后端。覆盖「登录态 → 进入收银台 → 渲染订单与支付方式」的关键 UI 链路。
 * 完整支付（点立即支付→轮询成功）由 PaymentPage.test.tsx 组件级覆盖，此处仅做端到端渲染冒烟，
 * 避免对异步轮询的脆弱断言。
 */
test.describe('收银台支付流程', () => {
  test.beforeEach(async ({ page }) => {
    // 注入登录态（双 Token 会话恢复流）；等待 restoreSession 完成，token 写入内存，
    // 供后续 SPA 导航进入受保护路由时通过同步的 ProtectedRoute 校验
    await seedSession(page, { userId: '1', username: 'buyer', nickname: '买家' });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('登录用户可进入收银台并渲染订单与支付方式', async ({ page }) => {
    // mock 订单详情 API
    await page.route('**/api/orders/owned/order-123**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'A0000',
          message: 'ok',
          data: {
            id: 'order-123',
            orderNo: 'ORD202608061001',
            buyerId: '1',
            sellerId: '2',
            totalAmount: 99.99,
            status: 0,
            statusDesc: '待付款',
            items: [
              {
                itemId: 'item-1',
                productId: 'prod-1',
                productName: '测试资产',
                productImage: '',
                unitPrice: 99.99,
                quantity: 1,
                subtotal: 99.99,
              },
            ],
            createTime: '2026-08-06 10:00:00',
          },
        }),
      });
    });
    // mock 支付状态 API：未创建支付时保持「待支付」视图，避免轮询跳转
    await page.route('**/api/payments/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'A0000', message: 'ok', data: null }),
      });
    });

    // SPA 导航进入受保护路由，避免 full goto 触发 restoreSession 与同步守卫的竞态
    await spaNavigate(page, '/payment?orderId=order-123');
    await page.waitForLoadState('networkidle');

    // 收银台渲染
    await expect(page.locator('text=收银台').first()).toBeVisible({ timeout: 15000 });
    // 订单商品名
    await expect(page.locator('text=测试资产').first()).toBeVisible({ timeout: 10000 });
    // 支付方式选项
    await expect(page.locator('text=微信支付').first()).toBeVisible();
    await expect(page.locator('text=支付宝').first()).toBeVisible();
    // 提交按钮
    await expect(page.locator('text=立即支付').first()).toBeVisible();
  });

  test('无 orderId 时收银台显示无效订单兜底', async ({ page }) => {
    await spaNavigate(page, '/payment');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('text=无效的订单').first()).toBeVisible({ timeout: 15000 });
  });
});