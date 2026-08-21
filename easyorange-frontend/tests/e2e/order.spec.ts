import { test, expect } from '@playwright/test';
import { seedSession, spaNavigate } from './helpers/auth';

/**
 * 下单流程 — mock 化端到端：登录 → 商品详情 → 立即购买弹窗 → 提交订单 → 跳转订单详情。
 * 与后端 OrderCreationIT（真实 MySQL/Redis/RabbitMQ 链路）互补，此处覆盖前端交互闭环。
 */
const PRODUCT_ID = 'prod-1';
const ORDER_ID = 'order-123';

test.describe('商品下单流程', () => {
  test.beforeEach(async ({ page }) => {
    await seedSession(page, { userId: '1', username: 'buyer', nickname: '买家' });

    // 其余商品相关接口（相似推荐/评价等）统一返回空，避免 preview 服务器 404 噪音
    await page.route('**/api/products/**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: null }),
        })
    );
    // 商品详情：在售、资产方非当前买家
    await page.route(`**/api/products/${PRODUCT_ID}`, route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                code: 'A0000',
                message: 'ok',
                data: {
                    id: PRODUCT_ID,
                    title: 'E2E 下单测试资产',
                    description: '测试描述',
                    price: 299,
                    categoryId: 'cat-1',
                    categoryName: '数码数码',
                    status: 'ONLINE',
                    sellerId: '2',
                    sellerName: '资产方',
                    stock: 1,
                    views: 10,
                    favorites: 2,
                    images: [],
                    createTime: '2026-08-01 10:00:00',
                },
            }),
        })
    );
    await page.route('**/api/ratings**', route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: { records: [], total: 0 } }),
        })
    );

    await page.goto('/');
    await expect(page.locator('[data-testid="btn-user-menu"]')).toBeVisible({ timeout: 20000 });
  });

  test('详情页立即购买可打开下单弹窗', async ({ page }) => {
    await spaNavigate(page, `/products/${PRODUCT_ID}`);

    const buyBtn = page.getByRole('button', { name: '立即购买' });
    await expect(buyBtn).toBeVisible({ timeout: 15000 });
    await buyBtn.click();

    await expect(page.getByText('确认购买').first()).toBeVisible({ timeout: 5000 });
    await expect(page.locator('input[placeholder="请输入手机号"]')).toBeVisible();
    await expect(page.getByRole('button', { name: '提交订单' })).toBeVisible();
  });

  test('提交订单调用创建接口并跳转订单详情', async ({ page }) => {
    let orderPayload: unknown = null;
    await page.route('**/api/orders', async route => {
      if (route.request().method() === 'POST') {
        orderPayload = route.request().postDataJSON();
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 'A0000', message: 'ok', data: ORDER_ID }),
        });
        return;
      }
      await route.continue();
    });
    await page.route(`**/api/orders/${ORDER_ID}**`, route =>
        route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                code: 'A0000',
                message: 'ok',
                data: {
                    id: ORDER_ID,
                    orderNo: 'ORD20260822E2E001',
                    buyerId: '1',
                    sellerId: '2',
                    totalAmount: 299,
                    status: 0,
                    statusDesc: '待付款',
                    items: [
                        {
                            itemId: 'item-1',
                            productId: PRODUCT_ID,
                            productName: 'E2E 下单测试资产',
                            productImage: '',
                            unitPrice: 299,
                            quantity: 1,
                            subtotal: 299,
                        },
                    ],
                    createTime: '2026-08-22 10:00:00',
                },
            }),
        })
    );

    await spaNavigate(page, `/products/${PRODUCT_ID}`);
    await page.getByRole('button', { name: '立即购买' }).click();
    await expect(page.getByText('确认购买').first()).toBeVisible({ timeout: 5000 });

    await page.locator('input[placeholder="请输入手机号"]').fill('13800138000');
    await page.getByRole('button', { name: '提交订单' }).click();

    await expect(page).toHaveURL(new RegExp(`/orders/${ORDER_ID}`), { timeout: 15000 });
    expect(orderPayload).toMatchObject({ items: [{ productId: PRODUCT_ID, quantity: 1 }] });
  });
});
